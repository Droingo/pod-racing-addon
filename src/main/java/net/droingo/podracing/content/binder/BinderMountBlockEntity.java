package net.droingo.podracing.content.binder;

import net.droingo.podracing.content.binder.menu.BinderMountMenu;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BinderMountBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int FREQUENCY_SLOT_COUNT = 2;

    private final NonNullList<ItemStack> frequencyItems =
            NonNullList.withSize(FREQUENCY_SLOT_COUNT, ItemStack.EMPTY);

    private boolean powered = false;

    public BinderMountBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.BINDER_MOUNT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BinderMountBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

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

    public ItemStack getFrequencyItem(int slot) {
        if (slot < 0 || slot >= frequencyItems.size()) {
            return ItemStack.EMPTY;
        }

        return frequencyItems.get(slot);
    }

    public boolean hasMatchingFrequencies(BinderMountBlockEntity other) {
        return ItemStack.isSameItemSameComponents(getFrequencyItem(0), other.getFrequencyItem(0))
                && ItemStack.isSameItemSameComponents(getFrequencyItem(1), other.getFrequencyItem(1))
                && !getFrequencyItem(0).isEmpty()
                && !getFrequencyItem(1).isEmpty();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.pod_racing_addon.binder_mount");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BinderMountMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, frequencyItems, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        frequencyItems.clear();
        ContainerHelper.loadAllItems(tag, frequencyItems, registries);
    }

    @Override
    public int getContainerSize() {
        return frequencyItems.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : frequencyItems) {
            if (!stack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return frequencyItems.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(frequencyItems, slot, amount);

        if (!removed.isEmpty()) {
            setChanged();
        }

        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(frequencyItems, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack storedStack = stack.copy();

        if (!storedStack.isEmpty()) {
            storedStack.setCount(1);
        }

        frequencyItems.set(slot, storedStack);
        setChanged();

        if (level instanceof ServerLevel serverLevel) {
            EnergyBinderSync.sendConnectionsToAll(serverLevel);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null) {
            return false;
        }

        if (level.getBlockEntity(worldPosition) != this) {
            return false;
        }

        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public void clearContent() {
        frequencyItems.clear();
        setChanged();
    }
}