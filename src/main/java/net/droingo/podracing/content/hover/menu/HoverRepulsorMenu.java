package net.droingo.podracing.content.hover.menu;

import net.droingo.podracing.content.hover.HoverRepulsorBlockEntity;
import net.droingo.podracing.registry.PRBlocks;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class HoverRepulsorMenu extends AbstractContainerMenu {
    private static final int FREQUENCY_SLOT_COUNT = HoverRepulsorBlockEntity.FREQUENCY_SLOT_COUNT;

    private static final int PLAYER_INVENTORY_START = FREQUENCY_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory playerInventory;
    private final Container frequencyContainer;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final HoverRepulsorBlockEntity blockEntity;

    public HoverRepulsorMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                null,
                new SimpleContainer(FREQUENCY_SLOT_COUNT),
                new SimpleContainerData(HoverRepulsorBlockEntity.CONFIG_COUNT),
                ContainerLevelAccess.NULL
        );
    }

    public HoverRepulsorMenu(
            int containerId,
            Inventory playerInventory,
            HoverRepulsorBlockEntity blockEntity
    ) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                blockEntity,
                new RepulsorContainerData(blockEntity),
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos())
        );
    }

    private HoverRepulsorMenu(
            int containerId,
            Inventory playerInventory,
            HoverRepulsorBlockEntity blockEntity,
            Container frequencyContainer,
            ContainerData data,
            ContainerLevelAccess access
    ) {
        super(PRMenuTypes.HOVER_REPULSOR.get(), containerId);

        checkContainerSize(frequencyContainer, FREQUENCY_SLOT_COUNT);
        checkContainerDataCount(data, HoverRepulsorBlockEntity.CONFIG_COUNT);

        this.playerInventory = playerInventory;
        this.blockEntity = blockEntity;
        this.frequencyContainer = frequencyContainer;
        this.data = data;
        this.access = access;

        frequencyContainer.startOpen(playerInventory.player);

        addSlot(new FrequencySlot(frequencyContainer, 0, 76, 36));
        addSlot(new FrequencySlot(frequencyContainer, 1, 100, 36));

        addDataSlots(data);

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, PRBlocks.HOVER_REPULSOR.get());
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

        if (quickMovedSlotIndex < FREQUENCY_SLOT_COUNT) {
            setFrequencyGhost(quickMovedSlotIndex, ItemStack.EMPTY);
            return ItemStack.EMPTY;
        }

        setFirstFrequencyGhost(quickMovedSlot.getItem());
        return ItemStack.EMPTY;
    }

    public double getConfigValue(int parameter) {
        if (parameter < 0 || parameter >= HoverRepulsorBlockEntity.CONFIG_COUNT) {
            return 0.0D;
        }

        return data.get(parameter) / (double) HoverRepulsorBlockEntity.CONFIG_SCALE;
    }

    public void setServerConfigValue(int parameter, double value) {
        if (blockEntity == null) {
            return;
        }

        blockEntity.setConfigValue(parameter, value);
    }

    private boolean isFrequencySlot(int slotId) {
        return slotId >= 0 && slotId < FREQUENCY_SLOT_COUNT;
    }

    private void setFirstFrequencyGhost(ItemStack sourceStack) {
        if (sourceStack.isEmpty()) {
            return;
        }

        for (int slot = 0; slot < FREQUENCY_SLOT_COUNT; slot++) {
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
                        29 + column * 18,
                        154 + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(
                    playerInventory,
                    column,
                    29 + column * 18,
                    212
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

    private static final class RepulsorContainerData implements ContainerData {
        private final HoverRepulsorBlockEntity blockEntity;

        private RepulsorContainerData(HoverRepulsorBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public int get(int index) {
            return blockEntity.getEncodedConfigValue(index);
        }

        @Override
        public void set(int index, int value) {
            blockEntity.setEncodedConfigValue(index, value);
        }

        @Override
        public int getCount() {
            return HoverRepulsorBlockEntity.CONFIG_COUNT;
        }
    }
}