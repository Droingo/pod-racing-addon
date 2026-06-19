package net.droingo.podracing.content.airbrake.menu;

import net.droingo.podracing.content.airbrake.AirBrakeBlockEntity;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class AirBrakeMenu extends AbstractContainerMenu {
    public static final int FREQUENCY_SLOT_A = 0;
    public static final int FREQUENCY_SLOT_B = 1;

    private static final int FREQUENCY_SLOT_COUNT = 2;

    private final Container container;

    public AirBrakeMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(FREQUENCY_SLOT_COUNT));
    }

    public AirBrakeMenu(int containerId, Inventory playerInventory, Container container) {
        super(PRMenuTypes.AIR_BRAKE.get(), containerId);

        this.container = container;
        this.container.startOpen(playerInventory.player);

        addSlot(new FrequencySlot(container, FREQUENCY_SLOT_A, 76, 36));
        addSlot(new FrequencySlot(container, FREQUENCY_SLOT_B, 100, 36));

        addPlayerInventory(playerInventory, 8, 86);
        addPlayerHotbar(playerInventory, 8, 144);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < FREQUENCY_SLOT_COUNT) {
            Slot slot = slots.get(slotId);

            if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) {
                ItemStack carried = getCarried();

                if (carried.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    ItemStack ghost = carried.copy();
                    ghost.setCount(1);
                    slot.set(ghost);
                }

                broadcastChanges();
                return;
            }
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        if (quickMovedSlotIndex < 0 || quickMovedSlotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot sourceSlot = slots.get(quickMovedSlotIndex);

        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();

        if (quickMovedSlotIndex < FREQUENCY_SLOT_COUNT) {
            sourceSlot.set(ItemStack.EMPTY);
            broadcastChanges();
            return ItemStack.EMPTY;
        }

        for (int i = 0; i < FREQUENCY_SLOT_COUNT; i++) {
            Slot frequencySlot = slots.get(i);

            if (!frequencySlot.hasItem()) {
                ItemStack ghost = sourceStack.copy();
                ghost.setCount(1);
                frequencySlot.set(ghost);
                broadcastChanges();
                return ItemStack.EMPTY;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    private void addPlayerInventory(Inventory playerInventory, int x, int y) {
        for (int row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        x + column * 18,
                        y + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory, int x, int y) {
        for (int column = 0; column < 9; ++column) {
            addSlot(new Slot(playerInventory, column, x + column * 18, y));
        }
    }

    private static final class FrequencySlot extends Slot {
        private FrequencySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}