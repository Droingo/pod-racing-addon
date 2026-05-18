package net.droingo.aerowind.blockentity;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.AeroWindBlockEntities;
import net.droingo.aerowind.sable.SableWindAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;


public class SealedPontoonBlockEntity extends BlockEntity {
    public SealedPontoonBlockEntity(BlockPos pos, BlockState blockState) {
        super(AeroWindBlockEntities.SEALED_PONTOON.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SealedPontoonBlockEntity blockEntity) {
        if (level.getGameTime() % 100 != 0) {
            return;
        }

        boolean waterHere = level.getFluidState(pos).is(FluidTags.WATER);
        boolean waterBelow = level.getFluidState(pos.below()).is(FluidTags.WATER);

        if (waterHere || waterBelow) {

            if (!(level instanceof ServerLevel serverLevel)) {
                return;
            }

            var subLevel = SableWindAccess.findSubLevelAt(serverLevel, pos);
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

            rigidBody.applyLinearImpulse(new Vector3d(0.0D, 0.08D, 0.0D));
            physicsSystem.getPipeline().wakeUp(subLevel);
            AeroWind.LOGGER.info(
                    "Sealed Pontoon at {} is touching water. waterHere={} waterBelow={}",
                    pos,
                    waterHere,
                    waterBelow
            );
        }
    }
}