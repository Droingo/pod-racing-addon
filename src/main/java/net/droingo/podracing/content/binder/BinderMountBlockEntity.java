package net.droingo.podracing.content.binder;

import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BinderMountBlockEntity extends BlockEntity {
    private boolean powered = false;

    public BinderMountBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.BINDER_MOUNT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BinderMountBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // Cheap polling avoids fragile neighbourChanged/version issues.
        if ((level.getGameTime() & 3L) != 0L) {
            return;
        }

        boolean directlyPowered = level.hasNeighborSignal(pos);
        blockEntity.setPowered(directlyPowered);
    }

    public boolean isPowered() {
        return powered;
    }

    private void setPowered(boolean powered) {
        if (this.powered == powered) {
            return;
        }

        this.powered = powered;
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            EnergyBinderSync.sendConnectionsToAll(serverLevel);
        }
    }
}