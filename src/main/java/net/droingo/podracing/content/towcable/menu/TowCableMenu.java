package net.droingo.podracing.content.towcable.menu;

import net.droingo.podracing.content.towcable.TowCableAnchorBlockEntity;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TowCableMenu extends AbstractContainerMenu {
    public static final int BUTTON_LENGTH_MINUS_1 = 0;
    public static final int BUTTON_LENGTH_MINUS_01 = 1;
    public static final int BUTTON_USE_CURRENT = 2;
    public static final int BUTTON_LENGTH_PLUS_01 = 3;
    public static final int BUTTON_LENGTH_PLUS_1 = 4;
    public static final int BUTTON_STIFFNESS_MINUS = 5;
    public static final int BUTTON_STIFFNESS_PLUS = 6;
    public static final int BUTTON_DAMPING_MINUS = 7;
    public static final int BUTTON_DAMPING_PLUS = 8;
    public static final int BUTTON_MINSEP_MINUS = 9;
    public static final int BUTTON_MINSEP_PLUS = 10;
    public static final int BUTTON_COPY_LENGTH = 11;
    public static final int BUTTON_PASTE_LENGTH = 12;

    private static final Map<UUID, Double> LENGTH_CLIPBOARD = new HashMap<>();

    private final TowCableAnchorBlockEntity blockEntity;

    private int clientRestLength = 0;
    private int clientStiffness = 0;
    private int clientDamping = 0;
    private int clientMinSeparation = 0;
    private int clientLinked = 0;

    public TowCableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public TowCableMenu(
            int containerId,
            Inventory playerInventory,
            TowCableAnchorBlockEntity blockEntity
    ) {
        super(PRMenuTypes.TOW_CABLE_ANCHOR.get(), containerId);
        this.blockEntity = blockEntity;

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (TowCableMenu.this.blockEntity == null) {
                    return clientRestLength;
                }

                return (int) Math.round(TowCableMenu.this.blockEntity.getRestLength() * 100.0D);
            }

            @Override
            public void set(int value) {
                clientRestLength = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (TowCableMenu.this.blockEntity == null) {
                    return clientStiffness;
                }

                return (int) Math.round(TowCableMenu.this.blockEntity.getStiffness() * 100.0D);
            }

            @Override
            public void set(int value) {
                clientStiffness = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (TowCableMenu.this.blockEntity == null) {
                    return clientDamping;
                }

                return (int) Math.round(TowCableMenu.this.blockEntity.getDamping() * 100.0D);
            }

            @Override
            public void set(int value) {
                clientDamping = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (TowCableMenu.this.blockEntity == null) {
                    return clientMinSeparation;
                }

                return (int) Math.round(TowCableMenu.this.blockEntity.getMinSeparation() * 100.0D);
            }

            @Override
            public void set(int value) {
                clientMinSeparation = value;
            }
        });

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (TowCableMenu.this.blockEntity == null) {
                    return clientLinked;
                }

                return TowCableMenu.this.blockEntity.isLinked() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                clientLinked = value == 0 ? 0 : 1;
            }
        });
    }

    public double getRestLength() {
        if (blockEntity != null) {
            return blockEntity.getRestLength();
        }

        return clientRestLength / 100.0D;
    }

    public double getStiffness() {
        if (blockEntity != null) {
            return blockEntity.getStiffness();
        }

        return clientStiffness / 100.0D;
    }

    public double getDamping() {
        if (blockEntity != null) {
            return blockEntity.getDamping();
        }

        return clientDamping / 100.0D;
    }

    public double getMinSeparation() {
        if (blockEntity != null) {
            return blockEntity.getMinSeparation();
        }

        return clientMinSeparation / 100.0D;
    }

    public boolean isLinked() {
        if (blockEntity != null) {
            return blockEntity.isLinked();
        }

        return clientLinked != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (blockEntity == null) {
            return false;
        }

        switch (id) {
            case BUTTON_LENGTH_MINUS_1 -> blockEntity.adjustRestLength(-1.0D);
            case BUTTON_LENGTH_MINUS_01 -> blockEntity.adjustRestLength(-0.1D);
            case BUTTON_USE_CURRENT -> blockEntity.useCurrentDistanceAsRestLength();
            case BUTTON_LENGTH_PLUS_01 -> blockEntity.adjustRestLength(0.1D);
            case BUTTON_LENGTH_PLUS_1 -> blockEntity.adjustRestLength(1.0D);
            case BUTTON_STIFFNESS_MINUS -> blockEntity.adjustStiffness(-0.05D);
            case BUTTON_STIFFNESS_PLUS -> blockEntity.adjustStiffness(0.05D);
            case BUTTON_DAMPING_MINUS -> blockEntity.adjustDamping(-0.05D);
            case BUTTON_DAMPING_PLUS -> blockEntity.adjustDamping(0.05D);
            case BUTTON_MINSEP_MINUS -> blockEntity.adjustMinSeparation(-0.1D);
            case BUTTON_MINSEP_PLUS -> blockEntity.adjustMinSeparation(0.1D);
            case BUTTON_COPY_LENGTH -> LENGTH_CLIPBOARD.put(player.getUUID(), blockEntity.getRestLength());
            case BUTTON_PASTE_LENGTH -> {
                Double copied = LENGTH_CLIPBOARD.get(player.getUUID());

                if (copied == null) {
                    return false;
                }

                blockEntity.setRestLength(copied);
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