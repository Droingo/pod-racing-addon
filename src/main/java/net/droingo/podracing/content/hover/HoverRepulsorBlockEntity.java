package net.droingo.podracing.content.hover;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;

public final class HoverRepulsorBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private static final String TAG_TARGET_HEIGHT = "TargetHeight";
    private static final String TAG_RAYCAST_RANGE = "RaycastRange";
    private static final String TAG_SUSPENSION_STRENGTH = "SuspensionStrength";
    private static final String TAG_PRELOAD = "Preload";

    /*
     * Wheel-mount style suspension values.
     *
     * targetHeight:
     *   desired ride height.
     *
     * raycastRange:
     *   how far below the repulsor it can see terrain.
     *
     * suspensionStrength:
     *   similar idea to Offroad's scrollable suspension strength.
     *
     * preload:
     *   gives a small amount of support before it is deeply compressed,
     *   otherwise it only starts supporting after it has already sagged.
     */
    private static final double DEFAULT_TARGET_HEIGHT = 3.0D;
    private static final double DEFAULT_RAYCAST_RANGE = 6.0D;
    private static final double DEFAULT_SUSPENSION_STRENGTH = 12.0D;
    private static final double DEFAULT_PRELOAD = 0.18D;

    private static final double SUPPORT_MARGIN = 0.75D;
    private static final double MAX_COMPRESSION = 1.75D;
    private static final double MAX_IMPULSE_PER_STEP = 1400.0D;
    private static final double MIN_VALID_HEIGHT = 0.05D;
    private static final double MIN_IMPULSE = 0.00001D;

    private double targetHeight = DEFAULT_TARGET_HEIGHT;
    private double raycastRange = DEFAULT_RAYCAST_RANGE;
    private double suspensionStrength = DEFAULT_SUSPENSION_STRENGTH;
    private double preload = DEFAULT_PRELOAD;

    private final Vector3d queuedForcePos = new Vector3d();
    private final Vector3d queuedForce = new Vector3d();
    private final ForceTotal forceTotal = new ForceTotal();

    public HoverRepulsorBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.HOVER_REPULSOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HoverRepulsorBlockEntity blockEntity) {
        /*
         * No normal ticking.
         * Force is handled by Sable through BlockEntitySubLevelActor.
         */
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (handle == null || !handle.isValid()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(HoverRepulsorBlock.FACING)) {
            return;
        }

        /*
         * First stable version: underside repulsors only.
         * Once this works, we can support angled/side repulsors deliberately.
         */
        Direction facing = state.getValue(HoverRepulsorBlock.FACING);

        if (facing != Direction.DOWN) {
            return;
        }

        Vec3 forcePoint = worldPosition.getCenter();

        queuedForcePos.set(forcePoint.x, forcePoint.y, forcePoint.z);

        TerrainCastResult terrain = computeTerrainBelow(subLevel, forcePoint);

        if (terrain == null) {
            return;
        }

        double height = terrain.height();

        if (height < MIN_VALID_HEIGHT || height > targetHeight + SUPPORT_MARGIN) {
            return;
        }

        double effectiveMass = estimateEffectiveMass(subLevel);

        /*
         * This scaling is copied in spirit from Offroad:
         * heavier bodies get more suspension authority, but it is capped.
         */
        double massScale = Math.min(effectiveMass / suspensionStrength, 1.0D) * 10.0D;

        double spring = suspensionStrength * massScale * 45.0D;
        double damping = suspensionStrength * massScale * 15.0D;

        /*
         * Compression is positive when the generator is lower than target height.
         * Preload gives it steady support around the target instead of only reacting
         * after it has already fallen too far.
         */
        double compression = targetHeight - height + preload;
        compression = clamp(compression, 0.0D, MAX_COMPRESSION);

        /*
         * Get velocity at the force point, same style as Offroad.
         * Then convert into this sublevel's local space.
         */
        Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(queuedForcePos));
        Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(velocity);

        /*
         * Positive when falling into the suspension.
         * Negative when already moving up.
         */
        double verticalDamping = -localVelocity.y * damping;

        double impulseMagnitude = (compression * spring + verticalDamping) * timeStep;
        impulseMagnitude = clamp(impulseMagnitude, 0.0D, MAX_IMPULSE_PER_STEP);

        if (!Double.isFinite(impulseMagnitude) || impulseMagnitude < MIN_IMPULSE) {
            return;
        }

        /*
         * Use the terrain normal converted into the sublevel's local space.
         * On flat ground this is straight up, but it still behaves properly
         * on slopes like a wheel mount.
         */
        queuedForce.set(terrain.localNormal()).mul(impulseMagnitude);

        forceTotal.applyImpulseAtPoint(subLevel, queuedForcePos, queuedForce);

        /*
         * Apply through ForceTotal like Offroad does, instead of raw direct impulses.
         */
        handle.applyForcesAndReset(forceTotal);
    }

    private TerrainCastResult computeTerrainBelow(ServerSubLevel ownSubLevel, Vec3 localStart) {
        if (level == null) {
            return null;
        }

        Vec3 localEnd = localStart.subtract(0.0D, raycastRange, 0.0D);

        ClipContext context = new ClipContext(
                localStart,
                localEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );

        /*
         * Critical Offroad detail:
         * Do not let the raycast hit the vehicle/sublevel it is mounted on.
         */
        ((ClipContextExtension) context).sable$setIgnoredSubLevel(
                Sable.HELPER.getContaining(this)
        );

        BlockHitResult hitResult = level.clip(context);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return null;
        }

        SubLevel hitSubLevel = Sable.HELPER.getContaining(level, hitResult.getLocation());

        /*
         * Convert the hit point into the repulsor's local sublevel space.
         * This mirrors Offroad's transform flow.
         */
        Vec3 hitLocation = hitResult.getLocation();

        Vec3 worldHitLocation = hitSubLevel == null
                ? hitLocation
                : hitSubLevel.logicalPose().transformPosition(hitLocation);

        Vec3 localHitLocation = ownSubLevel.logicalPose().transformPositionInverse(worldHitLocation);

        if (localHitLocation.y > localStart.y) {
            return null;
        }

        double height = localStart.y - localHitLocation.y;

        if (height <= 0.00001D) {
            return null;
        }

        /*
         * Convert contact normal into this sublevel's local space.
         */
        Direction hitDirection = hitResult.getDirection();

        Vector3d localNormal = new Vector3d(
                hitDirection.getStepX(),
                hitDirection.getStepY(),
                hitDirection.getStepZ()
        );

        if (hitSubLevel != null) {
            hitSubLevel.logicalPose().transformNormal(localNormal);
        }

        ownSubLevel.logicalPose().transformNormalInverse(localNormal);

        if (localNormal.lengthSquared() < 0.000001D) {
            return null;
        }

        localNormal.normalize();

        /*
         * Require a mostly-upward surface. This avoids grabbing walls/side faces.
         */
        if (localNormal.y < 0.45D) {
            return null;
        }

        return new TerrainCastResult(height, localNormal);
    }

    private double estimateEffectiveMass(ServerSubLevel subLevel) {
        if (subLevel.getMassTracker() == null || subLevel.getMassTracker().isInvalid()) {
            return 80.0D;
        }

        double inverseNormalMass = subLevel.getMassTracker().getInverseNormalMass(
                queuedForcePos,
                OrientedBoundingBox3d.UP
        );

        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.000001D) {
            return 80.0D;
        }

        double effectiveMass = 1.0D / inverseNormalMass;

        if (!Double.isFinite(effectiveMass)) {
            return 80.0D;
        }

        return clamp(effectiveMass, 20.0D, 2500.0D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putDouble(TAG_TARGET_HEIGHT, targetHeight);
        tag.putDouble(TAG_RAYCAST_RANGE, raycastRange);
        tag.putDouble(TAG_SUSPENSION_STRENGTH, suspensionStrength);
        tag.putDouble(TAG_PRELOAD, preload);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_TARGET_HEIGHT)) {
            targetHeight = clamp(tag.getDouble(TAG_TARGET_HEIGHT), 0.5D, 16.0D);
        }

        if (tag.contains(TAG_RAYCAST_RANGE)) {
            raycastRange = clamp(tag.getDouble(TAG_RAYCAST_RANGE), 1.0D, 24.0D);
        }

        if (tag.contains(TAG_SUSPENSION_STRENGTH)) {
            suspensionStrength = clamp(tag.getDouble(TAG_SUSPENSION_STRENGTH), 1.0D, 80.0D);
        }

        if (tag.contains(TAG_PRELOAD)) {
            preload = clamp(tag.getDouble(TAG_PRELOAD), 0.0D, 2.0D);
        }
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private record TerrainCastResult(double height, Vector3d localNormal) {
    }
}