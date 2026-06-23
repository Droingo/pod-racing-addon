package net.droingo.podracing.content.stabilizer.menu;

import net.droingo.podracing.content.stabilizer.PodStabilizerBlockEntity;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class PodStabilizerMenu extends AbstractContainerMenu {
    public static final int BUTTON_AXIS_X = 0;
    public static final int BUTTON_AXIS_Y = 1;
    public static final int BUTTON_AXIS_Z = 2;

    private final PodStabilizerBlockEntity blockEntity;

    private int clientAxisIndex = 0;

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
                    return clientAxisIndex;
                }

                return PodStabilizerMenu.this.blockEntity.getPhysicsAxisIndex();
            }

            @Override
            public void set(int value) {
                clientAxisIndex = Math.max(0, Math.min(2, value));
            }
        });
    }

    public Direction.Axis getPhysicsAxis() {
        if (blockEntity != null) {
            return blockEntity.getPhysicsAxis();
        }

        return PodStabilizerBlockEntity.indexToAxis(clientAxisIndex);
    }

    public int getPhysicsAxisIndex() {
        if (blockEntity != null) {
            return blockEntity.getPhysicsAxisIndex();
        }

        return clientAxisIndex;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        switch (id) {
            case BUTTON_AXIS_X -> blockEntity.setPhysicsAxis(Direction.Axis.X);
            case BUTTON_AXIS_Y -> blockEntity.setPhysicsAxis(Direction.Axis.Y);
            case BUTTON_AXIS_Z -> blockEntity.setPhysicsAxis(Direction.Axis.Z);
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