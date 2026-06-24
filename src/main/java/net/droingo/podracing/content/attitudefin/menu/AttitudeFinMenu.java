package net.droingo.podracing.content.attitudefin.menu;

import net.droingo.podracing.content.attitudefin.AttitudeFinBlockEntity;
import net.droingo.podracing.content.attitudefin.AttitudeFinRole;
import net.droingo.podracing.content.pilot.PodPilotInputState;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

public final class AttitudeFinMenu extends AbstractContainerMenu {
    public static final int BUTTON_ROLL_LEFT = 0;
    public static final int BUTTON_ROLL_RIGHT = 1;
    public static final int BUTTON_FRONT_CONTROL = 2;
    public static final int BUTTON_REAR_CONTROL = 3;
    public static final int BUTTON_TOGGLE_REVERSED = 4;
    public static final int BUTTON_BIND_ACTIVE_CORE = 5;

    private final AttitudeFinBlockEntity blockEntity;

    private int clientRoleIndex = 0;
    private int clientReversed = 0;
    private int clientBound = 0;

    public AttitudeFinMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public AttitudeFinMenu(
            int containerId,
            Inventory playerInventory,
            AttitudeFinBlockEntity blockEntity
    ) {
        super(PRMenuTypes.ATTITUDE_FIN.get(), containerId);
        this.blockEntity = blockEntity;

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (AttitudeFinMenu.this.blockEntity == null) {
                    return clientRoleIndex;
                }

                return AttitudeFinMenu.this.blockEntity.getRoleIndex();
            }

            @Override
            public void set(int value) {
                clientRoleIndex = Math.max(0, Math.min(3, value));
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (AttitudeFinMenu.this.blockEntity == null) {
                    return clientReversed;
                }

                return AttitudeFinMenu.this.blockEntity.isReversed() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                clientReversed = value == 0 ? 0 : 1;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (AttitudeFinMenu.this.blockEntity == null) {
                    return clientBound;
                }

                return AttitudeFinMenu.this.blockEntity.hasFrequency() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                clientBound = value == 0 ? 0 : 1;
            }
        });
    }

    public int getRoleIndex() {
        if (blockEntity != null) {
            return blockEntity.getRoleIndex();
        }

        return clientRoleIndex;
    }

    public AttitudeFinRole getRole() {
        if (blockEntity != null) {
            return blockEntity.getRole();
        }

        return AttitudeFinRole.fromGuiIndex(clientRoleIndex);
    }

    public boolean isReversed() {
        if (blockEntity != null) {
            return blockEntity.isReversed();
        }

        return clientReversed != 0;
    }

    public boolean isBound() {
        if (blockEntity != null) {
            return blockEntity.hasFrequency();
        }

        return clientBound != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        switch (id) {
            case BUTTON_ROLL_LEFT -> blockEntity.setRole(AttitudeFinRole.LEFT_ENGINE);
            case BUTTON_ROLL_RIGHT -> blockEntity.setRole(AttitudeFinRole.RIGHT_ENGINE);
            case BUTTON_FRONT_CONTROL -> blockEntity.setRole(AttitudeFinRole.FRONT_CONTROL);
            case BUTTON_REAR_CONTROL -> blockEntity.setRole(AttitudeFinRole.REAR_CONTROL);
            case BUTTON_TOGGLE_REVERSED -> blockEntity.setReversed(!blockEntity.isReversed());
            case BUTTON_BIND_ACTIVE_CORE -> {
                if (blockEntity.getLevel() == null) {
                    return false;
                }

                PodPilotInputState.Command command =
                        PodPilotInputState.findCommandForPlayer(blockEntity.getLevel(), player.getUUID());

                if (command == null || command.controlFrequency() == null) {
                    return false;
                }

                blockEntity.bindToFrequency(command.controlFrequency());
            }
            default -> {
                return false;
            }
        }

        broadcastChanges();
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