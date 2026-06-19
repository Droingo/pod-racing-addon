package net.droingo.podracing.content.airbrake;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.data.Couple;
import net.droingo.podracing.content.airbrake.menu.AirBrakeMenu;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public final class AirBrakeBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, MenuProvider, Container, IRedstoneLinkable {
    public static final int FREQUENCY_SLOT_COUNT = 2;

    private static final String TAG_FLAP_COLOR = "FlapColor";

    private static final double MIN_SPEED = 0.08D;
    private static final double SPEED_FOR_FULL_BRAKE = 4.0D;
    private static final double MAX_DELTA_V = 0.10D;
    private static final double MAX_BRAKE_IMPULSE = 75.0D;
    private static final double BRAKE_STRENGTH = 1.0D;

    private DyeColor flapColor = DyeColor.RED;

    private final NonNullList<ItemStack> frequencyItems = NonNullList.withSize(FREQUENCY_SLOT_COUNT, ItemStack.EMPTY);

    private boolean directlyPowered = false;
    private int receivedWirelessSignal = 0;
    private boolean createNetworkRegistered = false;

    private final ForceTotal forceTotal = new ForceTotal();
    private final Vector3d localAnchor = new Vector3d();
    private final Vector3d brakeImpulse = new Vector3d();

    public AirBrakeBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.AIR_BRAKE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AirBrakeBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.ensureCreateNetworkRegistration();

        if ((level.getGameTime() & 3L) == 0L) {
            blockEntity.setDirectlyPowered(level.hasNeighborSignal(pos));
        }

        blockEntity.updatePoweredBlockState();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (!isPowered()) {
            return;
        }

        if (handle == null || !handle.isValid()) {
            return;
        }

        localAnchor.set(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        );

        Vector3d worldVelocity = Sable.HELPER.getVelocity(level, new Vector3d(localAnchor));

        if (!Double.isFinite(worldVelocity.x) || !Double.isFinite(worldVelocity.y) || !Double.isFinite(worldVelocity.z)) {
            return;
        }

        double speed = worldVelocity.length();

        if (speed < MIN_SPEED) {
            return;
        }

        Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(worldVelocity);

        if (localVelocity.lengthSquared() < 0.000001D) {
            return;
        }

        Vector3d brakeDirection = new Vector3d(localVelocity).normalize().negate();

        double speed01 = clamp(speed / SPEED_FOR_FULL_BRAKE, 0.0D, 1.0D);
        double stepScale = clamp(timeStep / 0.05D, 0.05D, 1.0D);

        double deltaV = MAX_DELTA_V * speed01 * BRAKE_STRENGTH * stepScale;

        if (deltaV <= 0.0D) {
            return;
        }

        double effectiveMass = estimateEffectiveMass(subLevel, localAnchor, brakeDirection);
        double impulseMagnitude = effectiveMass * deltaV;

        impulseMagnitude = clamp(impulseMagnitude, 0.0D, MAX_BRAKE_IMPULSE * BRAKE_STRENGTH);

        if (!Double.isFinite(impulseMagnitude) || impulseMagnitude <= 0.00001D) {
            return;
        }

        brakeImpulse.set(brakeDirection).mul(impulseMagnitude);

        /*
         * Applying at the air brake position means off-centre brakes can create
         * some useful turning torque.
         */
        forceTotal.applyImpulseAtPoint(subLevel, localAnchor, brakeImpulse);
        handle.applyForcesAndReset(forceTotal);
    }

    private double estimateEffectiveMass(ServerSubLevel subLevel, Vector3d point, Vector3d direction) {
        if (subLevel.getMassTracker() == null || subLevel.getMassTracker().isInvalid()) {
            return 80.0D;
        }

        double inverseNormalMass = subLevel.getMassTracker().getInverseNormalMass(point, direction);

        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.000001D) {
            return 80.0D;
        }

        double effectiveMass = 1.0D / inverseNormalMass;

        if (!Double.isFinite(effectiveMass)) {
            return 80.0D;
        }

        return clamp(effectiveMass, 20.0D, 2500.0D);
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
        updatePoweredBlockState();
    }

    private void setReceivedWirelessSignal(int signal) {
        signal = Math.max(0, Math.min(15, signal));

        if (receivedWirelessSignal == signal) {
            return;
        }

        receivedWirelessSignal = signal;
        setChanged();
        updatePoweredBlockState();
    }

    private void updatePoweredBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(AirBrakeBlock.POWERED)) {
            return;
        }

        boolean powered = isPowered();

        if (state.getValue(AirBrakeBlock.POWERED) != powered) {
            level.setBlock(worldPosition, state.setValue(AirBrakeBlock.POWERED, powered), 3);
        }
    }

    public DyeColor getFlapColor() {
        return flapColor;
    }

    public void setFlapColor(DyeColor flapColor) {
        if (flapColor == null || this.flapColor == flapColor) {
            return;
        }

        this.flapColor = flapColor;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public int getFlapColorRgb() {
        return flapColor.getTextureDiffuseColor();
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

        setChanged();
        updatePoweredBlockState();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        return Component.translatable("menu.pod_racing_addon.air_brake");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AirBrakeMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putString(TAG_FLAP_COLOR, flapColor.getName());
        ContainerHelper.saveAllItems(tag, frequencyItems, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_FLAP_COLOR)) {
            DyeColor loadedColor = DyeColor.byName(tag.getString(TAG_FLAP_COLOR), DyeColor.RED);
            flapColor = loadedColor == null ? DyeColor.RED : loadedColor;
        }

        frequencyItems.clear();
        ContainerHelper.loadAllItems(tag, frequencyItems, registries);

        createNetworkRegistered = false;
        receivedWirelessSignal = 0;
        directlyPowered = false;
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

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}