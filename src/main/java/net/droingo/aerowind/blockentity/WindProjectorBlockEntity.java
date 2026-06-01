package net.droingo.aerowind.blockentity;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.AeroWindBlockEntities;
import net.droingo.aerowind.sable.SableWindAccess;
import net.droingo.aerowind.wind.WindModel;
import net.droingo.aerowind.wind.WindSample;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class WindProjectorBlockEntity extends BlockEntity {
    private boolean loggedLevelClass = false;

    public WindProjectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(AeroWindBlockEntities.WIND_PROJECTOR.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WindProjectorBlockEntity blockEntity) {
        if (!blockEntity.loggedLevelClass) {
            blockEntity.loggedLevelClass = true;
            AeroWind.LOGGER.info("Level implementation at {}: {}", pos, level.getClass().getName());
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        ServerSubLevel subLevel = SableWindAccess.findSubLevelAt(serverLevel, pos);
        if (subLevel == null) {
            return;
        }

        if (level.getGameTime() % 100 == 0) {
            int liftProviderCount = subLevel.getPlot().getLiftProviders().size();

            AeroWind.LOGGER.info(
                    "Sublevel {} has {} lift providers",
                    subLevel.getUniqueId(),
                    liftProviderCount
            );
        }

        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(serverLevel);
        if (physicsSystem == null) {
            return;
        }

        var rigidBody = physicsSystem.getPhysicsHandle(subLevel);
        if (rigidBody == null || !rigidBody.isValid()) {
            return;
        }

        WindSample wind = WindModel.sample(serverLevel, Vec3.atCenterOf(pos));
        Vec3 direction = wind.direction();

        int layerHeight = 64;
        int heightLayer = Math.floorDiv(pos.getY(), layerHeight);

        long layerSeed = serverLevel.getSeed() ^ (heightLayer * 341873128712L);
        RandomSource layerRandom = RandomSource.create(layerSeed);

        // Each height layer bends the normal daily wind by up to +/- 45 degrees.
        double layerOffset = (layerRandom.nextDouble() - 0.5D) * Math.toRadians(90.0D);

        double baseAngle = Math.atan2(direction.z, direction.x);
        double finalAngle = baseAngle + layerOffset;

        Vec3 heightDirection = new Vec3(
                Math.cos(finalAngle),
                direction.y,
                Math.sin(finalAngle)
        ).normalize();

        long gustWindow = level.getGameTime() / 20L; // 1 second chunks.
        long gustSeed = serverLevel.getSeed() ^ subLevel.getUniqueId().getMostSignificantBits() ^ gustWindow;

        RandomSource gustRandom = RandomSource.create(gustSeed);

        // About 5% chance each second.
        boolean gusting = gustRandom.nextDouble() < 0.05D;

        Vector3d impulse;

        if (gusting) {
            double angle = gustRandom.nextDouble() * Math.PI * 2.0D;
            double gustStrength = wind.speed() * 4.0D;

            impulse = new Vector3d(
                    Math.cos(angle) * gustStrength,
                    0.0D,
                    Math.sin(angle) * gustStrength
            );
        } else {
            impulse = new Vector3d(
                    heightDirection.x * wind.speed(),
                    heightDirection.y * wind.speed(),
                    heightDirection.z * wind.speed()
            );
        }

        Vector3d dragCenter = estimateDragCenter(subLevel);

        rigidBody.applyImpulseAtPoint(dragCenter, impulse);
        physicsSystem.getPipeline().wakeUp(subLevel);

        if (level.getGameTime() % 100 == 0) {
            AeroWind.LOGGER.info(
                    "Wind impulse at drag center sublevel={} speed={} direction={} dragCenter={} impulse={} velocity={} angularVelocity={}",
                    subLevel.getUniqueId(),
                    wind.speed(),
                    heightDirection,
                    dragCenter,
                    impulse,
                    rigidBody.getLinearVelocity(new Vector3d()),
                    rigidBody.getAngularVelocity(new Vector3d())
            );
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WindProjectorBlockEntity blockEntity) {
    }

    private static Vector3d estimateDragCenter(ServerSubLevel subLevel) {
        var bounds = subLevel.getPlot().getBoundingBox();

        return new Vector3d(
                (bounds.minX() + bounds.maxX() + 1.0D) * 0.5D,
                (bounds.minY() + bounds.maxY() + 1.0D) * 0.5D,
                (bounds.minZ() + bounds.maxZ() + 1.0D) * 0.5D
        );
    }
}