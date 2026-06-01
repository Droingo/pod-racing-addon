package net.droingo.aerowind.blockentity;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.AeroWindBlockEntities;
import net.droingo.aerowind.sable.SableWindAccess;
import net.droingo.aerowind.wind.WindField;
import net.droingo.aerowind.wind.WindSample;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(serverLevel);
        if (physicsSystem == null) {
            return;
        }

        var rigidBody = physicsSystem.getPhysicsHandle(subLevel);
        if (rigidBody == null || !rigidBody.isValid()) {
            return;
        }

        WindSample wind = WindField.sampleServer(serverLevel, Vec3.atCenterOf(pos));
        Vector3d impulse = WindField.impulse(wind);

        Vector3d dragCenter = WindField.estimateUpperDragCenter(subLevel);

        rigidBody.applyImpulseAtPoint(dragCenter, impulse);
        physicsSystem.getPipeline().wakeUp(subLevel);

        if (level.getGameTime() % 100 == 0) {
            AeroWind.LOGGER.info(
                    "Wind Projector push sublevel={} speed={} direction={} dragCenter={} impulse={} velocity={} angularVelocity={}",
                    subLevel.getUniqueId(),
                    wind.speed(),
                    wind.direction(),
                    dragCenter,
                    impulse,
                    rigidBody.getLinearVelocity(new Vector3d()),
                    rigidBody.getAngularVelocity(new Vector3d())
            );
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, WindProjectorBlockEntity blockEntity) {
    }
}