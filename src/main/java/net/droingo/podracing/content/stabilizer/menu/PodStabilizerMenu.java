package net.droingo.podracing.content.stabilizer.menu;

import net.droingo.podracing.content.stabilizer.PodStabilizerBlockEntity;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class PodStabilizerMenu extends AbstractContainerMenu {
    public static final int BUTTON_STRENGTH_MINUS_5 = 0;
    public static final int BUTTON_STRENGTH_MINUS_1 = 1;
    public static final int BUTTON_STRENGTH_PLUS_1 = 2;
    public static final int BUTTON_STRENGTH_PLUS_5 = 3;

    private final PodStabilizerBlockEntity blockEntity;

    private int clientStrength = PodStabilizerBlockEntity.clampStrength(6);

    public PodStabilizerMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public PodStabilizerMenu(
            int containerId,
            Inventory playerInventory,
            PodStabilizerBlockEntity blockEntity
    ) {
        super(PRMenuTypes.POD_STABILIZER.get(), containerId);

        this.blockEntity = blockEntity;

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (PodStabilizerMenu.this.blockEntity == null) {
                    return clientStrength;
                }

                return PodStabilizerMenu.this.blockEntity.getStrength();
            }

            @Override
            public void set(int value) {
                clientStrength = PodStabilizerBlockEntity.clampStrength(value);
            }
        });
    }

    public int getStrength() {
        if (blockEntity != null) {
            return blockEntity.getStrength();
        }

        return clientStrength;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        switch (id) {
            case BUTTON_STRENGTH_MINUS_5 -> blockEntity.adjustStrength(-5);
            case BUTTON_STRENGTH_MINUS_1 -> blockEntity.adjustStrength(-1);
            case BUTTON_STRENGTH_PLUS_1 -> blockEntity.adjustStrength(1);
            case BUTTON_STRENGTH_PLUS_5 -> blockEntity.adjustStrength(5);
            default -> {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) {
            return true;
        }

        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5D,
                blockEntity.getBlockPos().getY() + 0.5D,
                blockEntity.getBlockPos().getZ() + 0.5D
        ) <= 64.0D;
    }
}