package net.droingo.podracing.content.stabilizer;

import net.droingo.podracing.content.stabilizer.menu.PodStabilizerMenu;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class PodStabilizerBlockEntity extends BlockEntity implements MenuProvider {
    public PodStabilizerBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.POD_STABILIZER.get(), pos, blockState);
    }

    public int getStrength() {
        BlockState state = getBlockState();

        if (!state.hasProperty(PodStabilizerBlock.STRENGTH)) {
            return 0;
        }

        return state.getValue(PodStabilizerBlock.STRENGTH);
    }

    public void setStrength(int strength) {
        if (level == null || level.isClientSide()) {
            return;
        }

        strength = clampStrength(strength);

        BlockState state = getBlockState();

        if (!state.hasProperty(PodStabilizerBlock.STRENGTH)) {
            return;
        }

        if (state.getValue(PodStabilizerBlock.STRENGTH) == strength) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(PodStabilizerBlock.STRENGTH, strength), 3);
        setChanged();
    }

    public void adjustStrength(int delta) {
        setStrength(getStrength() + delta);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.pod_racing_addon.pod_stabilizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PodStabilizerMenu(containerId, playerInventory, this);
    }

    public static int clampStrength(int strength) {
        return Math.max(0, Math.min(15, strength));
    }
}