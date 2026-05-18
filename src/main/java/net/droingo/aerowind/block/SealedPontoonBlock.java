package net.droingo.aerowind.block;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SealedPontoonBlock extends Block implements BlockSubLevelLiftProvider {
    private static final double BUOYANCY_IMPULSE = 28.0D;
    private static final double VERTICAL_DAMPING = 8.0D;
    private static final double WATER_DRAG = 0.55D;

    private static final double SAMPLE_RADIUS = 0.85D;

    // Strong arcade stabilizer. Allows yaw, fights pitch/roll.
    private static final double UPRIGHT_STRENGTH = 2.5D;
    private static final double ANGULAR_DAMPING = 1.2D;
    private static final double MAX_STABILIZER_IMPULSE = 1.5D;

    private static final double[][] SAMPLE_OFFSETS = {
            {0.0D, 0.0D, 0.0D},
            {SAMPLE_RADIUS, 0.0D, 0.0D},
            {-SAMPLE_RADIUS, 0.0D, 0.0D},
            {0.0D, 0.0D, SAMPLE_RADIUS},
            {0.0D, 0.0D, -SAMPLE_RADIUS}
    };

    public SealedPontoonBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion()
        );
    }

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return Direction.UP;
    }

    @Override
    public void sable$contributeLiftAndDrag(
            LiftProviderContext ctx,
            ServerSubLevel subLevel,
            @Nullable Pose3d localPose,
            double timeStep,
            Vector3dc linearVelocity,
            Vector3dc angularVelocity,
            Vector3d linearImpulse,
            Vector3d angularImpulse,
            @Nullable LiftProviderGroup group
    ) {
        double perSampleBuoyancy = BUOYANCY_IMPULSE / SAMPLE_OFFSETS.length;
        double totalSubmerged = 0.0D;

        for (double[] offset : SAMPLE_OFFSETS) {
            Vector3d localSamplePos = new Vector3d(
                    ctx.pos().getX() + 0.5D + offset[0],
                    ctx.pos().getY() + 0.5D + offset[1],
                    ctx.pos().getZ() + 0.5D + offset[2]
            );

            if (localPose != null) {
                localPose.transformPosition(localSamplePos);
            }

            Vector3d worldSamplePos = new Vector3d(
                    localSamplePos.x,
                    localSamplePos.y,
                    localSamplePos.z
            );
            subLevel.logicalPose().transformPosition(worldSamplePos);

            BlockPos surfacePos = findWaterSurface(subLevel, worldSamplePos);
            if (surfacePos == null) {
                continue;
            }

            double waterSurfaceY = surfacePos.getY() + 1.0D;
            double targetPontoonY = waterSurfaceY - 0.35D;

            double depthError = targetPontoonY - worldSamplePos.y;
            double submerged = clamp(depthError + 0.5D, 0.0D, 1.0D);

            if (submerged <= 0.0D) {
                continue;
            }

            totalSubmerged += submerged;

            double verticalSpeed = linearVelocity.y();
            double buoyancy = (perSampleBuoyancy * submerged)
                    - ((VERTICAL_DAMPING / SAMPLE_OFFSETS.length) * verticalSpeed);

            buoyancy = Math.max(0.0D, buoyancy);

            Vector3d localUpImpulse = new Vector3d(
                    0.0D,
                    buoyancy * timeStep,
                    0.0D
            );

            subLevel.logicalPose().transformNormalInverse(localUpImpulse);

            Vector3d localDragImpulse = new Vector3d(
                    linearVelocity.x(),
                    0.0D,
                    linearVelocity.z()
            ).mul(-(WATER_DRAG / SAMPLE_OFFSETS.length) * submerged * timeStep);

            linearImpulse.add(localUpImpulse);
            linearImpulse.add(localDragImpulse);
        }

        if (totalSubmerged <= 0.0D) {
            return;
        }

        double waterContact = clamp(totalSubmerged / SAMPLE_OFFSETS.length, 0.0D, 1.0D);

        Vector3d worldUpLocal = new Vector3d(0.0D, 1.0D, 0.0D);
        subLevel.logicalPose().transformNormalInverse(worldUpLocal);

        Vector3d localAngularVelocity = new Vector3d(
                angularVelocity.x(),
                angularVelocity.y(),
                angularVelocity.z()
        );
        subLevel.logicalPose().transformNormalInverse(localAngularVelocity);

        double pitchCorrection = worldUpLocal.z * UPRIGHT_STRENGTH;
        double rollCorrection = -worldUpLocal.x * UPRIGHT_STRENGTH;

        pitchCorrection += -localAngularVelocity.x * ANGULAR_DAMPING;
        rollCorrection += -localAngularVelocity.z * ANGULAR_DAMPING;

        pitchCorrection = clamp(
                pitchCorrection,
                -MAX_STABILIZER_IMPULSE,
                MAX_STABILIZER_IMPULSE
        );

        rollCorrection = clamp(
                rollCorrection,
                -MAX_STABILIZER_IMPULSE,
                MAX_STABILIZER_IMPULSE
        );

        angularImpulse.add(new Vector3d(
                pitchCorrection * waterContact,
                0.0D,
                rollCorrection * waterContact
        ));
    }

    @Nullable
    private static BlockPos findWaterSurface(ServerSubLevel subLevel, Vector3d worldPos) {
        BlockPos basePos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);

        for (int i = -2; i <= 10; i++) {
            BlockPos checkPos = basePos.above(i);
            boolean isWater = subLevel.getLevel().getFluidState(checkPos).is(FluidTags.WATER);
            boolean aboveIsWater = subLevel.getLevel().getFluidState(checkPos.above()).is(FluidTags.WATER);

            if (isWater && !aboveIsWater) {
                return checkPos;
            }
        }

        return null;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}