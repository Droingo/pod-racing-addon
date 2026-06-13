package net.droingo.podracing.content.binder;

import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BinderMountBlockEntity extends BlockEntity {
    public BinderMountBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.BINDER_MOUNT.get(), pos, blockState);
    }
}