package net.droingo.podracing.content.binder.menu;

import net.droingo.podracing.content.binder.BinderMountBlockEntity;
import net.droingo.podracing.registry.PRBlocks;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class BinderMountMenu extends AbstractContainerMenu {
    private static final int BINDER_SLOT_COUNT = BinderMountBlockEntity.FREQUENCY_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_START = BINDER_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container frequencyContainer;
    private final ContainerLevelAccess access;

    public BinderMountMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                new SimpleContainer(BINDER_SLOT_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    public BinderMountMenu(int containerId, Inventory playerInventory, BinderMountBlockEntity blockEntity) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
        );
    }

    private BinderMountMenu(
            int containerId,
            Inventory playerInventory,
            Container frequencyContainer,
            ContainerLevelAccess access
    ) {
        super(PRMenuTypes.BINDER_MOUNT.get(), containerId);

        checkContainerSize(frequencyContainer, BINDER_SLOT_COUNT);

        this.playerInventory = playerInventory;
        this.frequencyContainer = frequencyContainer;
        this.access = access;

        frequencyContainer.startOpen(playerInventory.player);

        addSlot(new FrequencySlot(frequencyContainer, 0, 80, 31));
        addSlot(new FrequencySlot(frequencyContainer, 1, 98, 31));

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, PRBlocks.BINDER_MOUNT.get());
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isFrequencySlot(slotId)) {
            handleFrequencySlotClick(slotId, button, clickType);
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    private void handleFrequencySlotClick(int slotId, int button, ClickType clickType) {
        if (clickType == ClickType.PICKUP || clickType == ClickType.PICKUP_ALL) {
            ItemStack carriedStack = getCarried();

            if (carriedStack.isEmpty()) {
                setFrequencyGhost(slotId, ItemStack.EMPTY);
            } else {
                setFrequencyGhost(slotId, carriedStack);
            }

            return;
        }

        if (clickType == ClickType.QUICK_MOVE || clickType == ClickType.THROW) {
            setFrequencyGhost(slotId, ItemStack.EMPTY);
            return;
        }

        if (clickType == ClickType.SWAP) {
            if (button >= 0 && button < 9) {
                setFrequencyGhost(slotId, playerInventory.getItem(button));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int quickMovedSlotIndex) {
        if (quickMovedSlotIndex < 0 || quickMovedSlotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot quickMovedSlot = slots.get(quickMovedSlotIndex);

        if (quickMovedSlot == null || !quickMovedSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (quickMovedSlotIndex < BINDER_SLOT_COUNT) {
            setFrequencyGhost(quickMovedSlotIndex, ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }

        setFirstFrequencyGhost(quickMovedSlot.getItem());
        return ItemStack.EMPTY;
    }

    private boolean isFrequencySlot(int slotId) {
        return slotId >= 0 && slotId < BINDER_SLOT_COUNT;
    }

    private void setFirstFrequencyGhost(ItemStack sourceStack) {
        if (sourceStack.isEmpty()) {
            return;
        }

        for (int slot = 0; slot < BINDER_SLOT_COUNT; slot++) {
            if (frequencyContainer.getItem(slot).isEmpty()) {
                setFrequencyGhost(slot, sourceStack);
                return;
            }
        }

        setFrequencyGhost(0, sourceStack);
    }

    private void setFrequencyGhost(int slot, ItemStack sourceStack) {
        ItemStack ghostStack = sourceStack.copy();

        if (!ghostStack.isEmpty()) {
            ghostStack.setCount(1);
        }

        frequencyContainer.setItem(slot, ghostStack);
        frequencyContainer.setChanged();
        broadcastChanges();
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    142
            ));
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