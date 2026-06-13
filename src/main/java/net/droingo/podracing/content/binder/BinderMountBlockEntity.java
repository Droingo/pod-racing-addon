package net.droingo.podracing.content.binder;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.createmod.catnip.data.Couple;
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

public final class BinderMountBlockEntity extends BlockEntity implements MenuProvider, Container, IRedstoneLinkable {
    public static final int FREQUENCY_SLOT_COUNT = 2;

    private final NonNullList<ItemStack> frequencyItems =
            NonNullList.withSize(FREQUENCY_SLOT_COUNT, ItemStack.EMPTY);

    private boolean directlyPowered = false;
    private int receivedWirelessSignal = 0;
    private boolean createNetworkRegistered = false;

    public BinderMountBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.BINDER_MOUNT.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BinderMountBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.ensureCreateNetworkRegistration();

        if ((level.getGameTime() & 3L) != 0L) {
            return;
        }

        boolean poweredByRedstone = level.hasNeighborSignal(pos);
        blockEntity.setDirectlyPowered(poweredByRedstone);
    }

    public boolean isPowered() {
        return directlyPowered || receivedWirelessSignal > 0;
    }

    public boolean isDirectlyPowered() {
        return directlyPowered;
    }

    public int getReceivedWirelessSignal() {
        return receivedWirelessSignal;
    }

    private void setDirectlyPowered(boolean directlyPowered) {
        if (this.directlyPowered == directlyPowered) {
            return;
        }

        this.directlyPowered = directlyPowered;
        setChanged();
        syncConnections();
    }

    private void setReceivedWirelessSignal(int signal) {
        signal = Math.max(0, Math.min(15, signal));

        if (receivedWirelessSignal == signal) {
            return;
        }

        receivedWirelessSignal = signal;
        setChanged();
        syncConnections();
    }

    public ItemStack getFrequencyItem(int slot) {
        if (slot < 0 || slot >= frequencyItems.size()) {
            return ItemStack.EMPTY;
        }

        return frequencyItems.get(slot);
    }

    public boolean hasCompleteFrequency() {
        return !getFrequencyItem(0).isEmpty() && !getFrequencyItem(1).isEmpty();
    }

    public boolean hasMatchingFrequencies(BinderMountBlockEntity other) {
        return ItemStack.isSameItemSameComponents(getFrequencyItem(0), other.getFrequencyItem(0))
                && ItemStack.isSameItemSameComponents(getFrequencyItem(1), other.getFrequencyItem(1))
                && hasCompleteFrequency();
    }

    private void syncConnections() {
        if (level instanceof ServerLevel serverLevel) {
            EnergyBinderSync.sendConnectionsToAll(serverLevel);
        }
    }

    private void ensureCreateNetworkRegistration() {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (!hasCompleteFrequency()) {
            unregisterFromCreateNetwork();
            return;
        }

        if (createNetworkRegistered) {
            return;
        }

        Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
        createNetworkRegistered = true;
    }

    private void rebuildCreateNetworkRegistration() {
        if (level == null || level.isClientSide()) {
            return;
        }

        unregisterFromCreateNetwork();
        receivedWirelessSignal = 0;

        if (hasCompleteFrequency()) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, this);
            createNetworkRegistered = true;
        }

        syncConnections();
    }

    private void unregisterFromCreateNetwork() {
        if (level == null || level.isClientSide()) {
            createNetworkRegistered = false;
            return;
        }

        if (!createNetworkRegistered) {
            return;
        }

        Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, this);
        createNetworkRegistered = false;
    }

    @Override
    public void setRemoved() {
        unregisterFromCreateNetwork();
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        createNetworkRegistered = false;
    }

    @Override
    public int getTransmittedStrength() {
        /*
         * Binder Mounts are receivers for now.
         * A Create Redstone Link transmitter powers them.
         */
        return 0;
    }

    @Override
    public void setReceivedStrength(int power) {
        setReceivedWirelessSignal(power);
    }

    @Override
    public boolean isListening() {
        return hasCompleteFrequency();
    }

    @Override
    public boolean isAlive() {
        if (level == null) {
            return false;
        }

        if (isRemoved()) {
            return false;
        }

        if (!level.isLoaded(worldPosition)) {
            return false;
        }

        return level.getBlockEntity(worldPosition) == this;
    }

    @Override
    public Couple<Frequency> getNetworkKey() {
        return Couple.create(
                Frequency.of(getFrequencyItem(0)),
                Frequency.of(getFrequencyItem(1))
        );
    }

    @Override
    public BlockPos getLocation() {
        return worldPosition;
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
        createNetworkRegistered = false;
        receivedWirelessSignal = 0;
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
            rebuildCreateNetworkRegistration();
        }

        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(frequencyItems, slot);

        if (!removed.isEmpty()) {
            rebuildCreateNetworkRegistration();
        }

        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack oldStack = frequencyItems.get(slot).copy();

        ItemStack ghostStack = stack.copy();

        if (!ghostStack.isEmpty()) {
            ghostStack.setCount(1);
        }

        frequencyItems.set(slot, ghostStack);
        setChanged();

        boolean changed = !ItemStack.isSameItemSameComponents(oldStack, ghostStack);

        if (changed) {
            rebuildCreateNetworkRegistration();
        } else {
            syncConnections();
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
        rebuildCreateNetworkRegistration();
    }
}