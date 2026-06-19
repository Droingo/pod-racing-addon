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
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
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
    private static final String TAG_DIRECT_SIGNAL = "DirectSignal";
    private static final String TAG_WIRELESS_SIGNAL = "WirelessSignal";

    private static final double MIN_SPEED = 0.08D;
    private static final double SPEED_FOR_FULL_BRAKE = 4.0D;
    private static final double MAX_DELTA_V = 0.10D;
    private static final double MAX_BRAKE_IMPULSE = 75.0D;
    private static final double BRAKE_STRENGTH = 1.0D;

    private static final float ANIMATION_OPEN_RESPONSE = 0.075F;
    private static final float ANIMATION_CLOSE_RESPONSE = 0.065F;
    private static final float ANIMATION_SNAP_EPSILON = 0.002F;

    private DyeColor flapColor = DyeColor.RED;

    private final NonNullList<ItemStack> frequencyItems = NonNullList.withSize(FREQUENCY_SLOT_COUNT, ItemStack.EMPTY);

    private int directRedstoneSignal = 0;
    private int receivedWirelessSignal = 0;
    private boolean createNetworkRegistered = false;

    private float flapOpenAmount = 0.0F;
    private float previousFlapOpenAmount = 0.0F;

    private final ForceTotal forceTotal = new ForceTotal();
    private final Vector3d localAnchor = new Vector3d();
    private final Vector3d brakeImpulse = new Vector3d();

    public AirBrakeBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.AIR_BRAKE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AirBrakeBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.tickClientAnimation();
            return;
        }

        blockEntity.ensureCreateNetworkRegistration();

        int directSignal = level.getBestNeighborSignal(pos);
        blockEntity.setDirectRedstoneSignal(directSignal);

        blockEntity.updatePoweredBlockState();
    }

    private void tickClientAnimation() {
        previousFlapOpenAmount = flapOpenAmount;

        float target = (float) getBrakeAmount();

        float response = target > flapOpenAmount
                ? ANIMATION_OPEN_RESPONSE
                : ANIMATION_CLOSE_RESPONSE;

        flapOpenAmount += (target - flapOpenAmount) * response;

        if (Math.abs(target - flapOpenAmount) <= ANIMATION_SNAP_EPSILON) {
            flapOpenAmount = target;
        }
    }

    public float getFlapOpenAmount(float partialTick) {
        return previousFlapOpenAmount + (flapOpenAmount - previousFlapOpenAmount) * partialTick;
    }

    public int getBrakeSignal() {
        return Math.max(directRedstoneSignal, receivedWirelessSignal);
    }

    public double getBrakeAmount() {
        return clamp(getBrakeSignal() / 15.0D, 0.0D, 1.0D);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries
    ) {
        CompoundTag tag = packet.getTag();

        if (tag != null) {
            loadAdditional(tag, registries);
        }
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (level == null || level.isClientSide()) {
            return;
        }

        double brakeAmount = getBrakeAmount();

        if (brakeAmount <= 0.0D) {
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

        if (!isFiniteVector(worldVelocity)) {
            return;
        }

        double speed = worldVelocity.length();

        if (speed < MIN_SPEED) {
            return;
        }

        Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(worldVelocity);

        if (!isFiniteVector(localVelocity) || localVelocity.lengthSquared() < 0.000001D) {
            return;
        }

        Vector3d brakeDirection = new Vector3d(localVelocity).normalize().negate();

        double speed01 = clamp(speed / SPEED_FOR_FULL_BRAKE, 0.0D, 1.0D);
        double stepScale = clamp(timeStep / 0.05D, 0.05D, 1.0D);

        double deltaV = MAX_DELTA_V * speed01 * BRAKE_STRENGTH * brakeAmount * stepScale;

        if (deltaV <= 0.0D) {
            return;
        }

        double effectiveMass = estimateEffectiveMass(subLevel, localAnchor, brakeDirection);
        double impulseMagnitude = effectiveMass * deltaV;

        impulseMagnitude = clamp(
                impulseMagnitude,
                0.0D,
                MAX_BRAKE_IMPULSE * BRAKE_STRENGTH * brakeAmount
        );

        if (!Double.isFinite(impulseMagnitude) || impulseMagnitude <= 0.00001D) {
            return;
        }

        brakeImpulse.set(brakeDirection).mul(impulseMagnitude);

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
        return getBrakeSignal() > 0;
    }

    public boolean isDirectlyPowered() {
        return directRedstoneSignal > 0;
    }

    public int getDirectRedstoneSignal() {
        return directRedstoneSignal;
    }

    public int getReceivedWirelessSignal() {
        return receivedWirelessSignal;
    }

    private void setDirectRedstoneSignal(int signal) {
        signal = clampSignal(signal);

        if (directRedstoneSignal == signal) {
            return;
        }

        directRedstoneSignal = signal;
        setChanged();
        sendSyncUpdate();
    }

    private void setReceivedWirelessSignal(int signal) {
        signal = clampSignal(signal);

        if (receivedWirelessSignal == signal) {
            return;
        }

        receivedWirelessSignal = signal;
        setChanged();
        updatePoweredBlockState();
        sendSyncUpdate();
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
        sendSyncUpdate();
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
        sendSyncUpdate();
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

    private void sendSyncUpdate() {
        if (level == null) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.getChunkSource().blockChanged(worldPosition);
            serverLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } else {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
        tag.putInt(TAG_DIRECT_SIGNAL, directRedstoneSignal);
        tag.putInt(TAG_WIRELESS_SIGNAL, receivedWirelessSignal);

        ContainerHelper.saveAllItems(tag, frequencyItems, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_FLAP_COLOR)) {
            DyeColor loadedColor = DyeColor.byName(tag.getString(TAG_FLAP_COLOR), DyeColor.RED);
            flapColor = loadedColor == null ? DyeColor.RED : loadedColor;
        }

        if (tag.contains(TAG_DIRECT_SIGNAL)) {
            directRedstoneSignal = clampSignal(tag.getInt(TAG_DIRECT_SIGNAL));
        }

        if (tag.contains(TAG_WIRELESS_SIGNAL)) {
            receivedWirelessSignal = clampSignal(tag.getInt(TAG_WIRELESS_SIGNAL));
        }

        frequencyItems.clear();
        ContainerHelper.loadAllItems(tag, frequencyItems, registries);

        createNetworkRegistered = false;

        if (level != null && !level.isClientSide()) {
            directRedstoneSignal = 0;
            receivedWirelessSignal = 0;
        }
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

    private static int clampSignal(int signal) {
        return Math.max(0, Math.min(15, signal));
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    private static float moveTowards(float current, float target, float maxStep) {
        if (current < target) {
            return Math.min(target, current + maxStep);
        }

        if (current > target) {
            return Math.max(target, current - maxStep);
        }

        return current;
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