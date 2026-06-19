package net.droingo.podracing.content.hover;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.data.Couple;
import net.droingo.podracing.content.hover.menu.HoverRepulsorMenu;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3d;

public final class HoverRepulsorBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, MenuProvider, Container, IRedstoneLinkable {
    public static final int FREQUENCY_SLOT_COUNT = 2;

    public static final int CONFIG_TARGET_HEIGHT = 0;
    public static final int CONFIG_DAMPING = 1;
    public static final int CONFIG_SUSPENSION_TRAVEL = 2;
    public static final int CONFIG_CATCH_RANGE = 3;
    public static final int CONFIG_MAX_IMPULSE = 4;
    public static final int CONFIG_STRENGTH = 5;
    public static final int CONFIG_COUNT = 6;

    public static final int CONFIG_SCALE = 1000;

    private static final String TAG_TARGET_HEIGHT = "RepulsorTargetHeightV6";
    private static final String TAG_RAYCAST_RANGE = "RepulsorRaycastRangeV6";
    private static final String TAG_MAX_DELTA_V = "RepulsorMaxDeltaVV6";
    private static final String TAG_MAX_IMPULSE = "RepulsorMaxImpulseV6";
    private static final String TAG_DAMPING = "RepulsorDampingV6";
    private static final String TAG_SUSPENSION_TRAVEL = "RepulsorSuspensionTravelV6";
    private static final String TAG_CATCH_RANGE = "RepulsorCatchRangeV6";
    private static final String TAG_STRENGTH = "RepulsorStrengthV6";

    private static final double DEFAULT_TARGET_HEIGHT = 3.0D;
    private static final double DEFAULT_RAYCAST_RANGE = 6.0D;
    private static final double DEFAULT_SUPPORT_START_ABOVE_TARGET = 1.25D;
    private static final double DEFAULT_SUSPENSION_TRAVEL = 0.65D;
    private static final double DEFAULT_DAMPING = 1.0D;
    private static final double DEFAULT_MAX_DELTA_V = 0.12D;
    private static final double DEFAULT_MAX_IMPULSE = 65.0D;
    private static final double DEFAULT_STRENGTH = 1.0D;

    private static final double MIN_VALID_HEIGHT = 0.05D;
    private static final double MIN_IMPULSE = 0.00001D;

    /*
     * 3x3 hover footprint.
     *
     * The old setup scanned only the block centre.
     * This scans 9 points around the repulsor and spreads the same total force
     * across the footprint so it behaves more like hover suspension.
     */
    private static final int SAMPLE_GRID_RADIUS = 1;
    private static final int SAMPLE_POINT_COUNT = 9;
    private static final double SAMPLE_SPACING = 0.75D;
    private static final double SAMPLE_FORCE_SHARE = 1.0D / SAMPLE_POINT_COUNT;

    private double targetHeight = DEFAULT_TARGET_HEIGHT;
    private double raycastRange = DEFAULT_RAYCAST_RANGE;
    private double supportStartAboveTarget = DEFAULT_SUPPORT_START_ABOVE_TARGET;
    private double suspensionTravel = DEFAULT_SUSPENSION_TRAVEL;
    private double damping = DEFAULT_DAMPING;
    private double maxDeltaV = DEFAULT_MAX_DELTA_V;
    private double maxImpulse = DEFAULT_MAX_IMPULSE;
    private double strength = DEFAULT_STRENGTH;

    private final NonNullList<ItemStack> frequencyItems = NonNullList.withSize(FREQUENCY_SLOT_COUNT, ItemStack.EMPTY);

    private boolean directlyPowered = false;
    private int receivedWirelessSignal = 0;
    private boolean createNetworkRegistered = false;

    private boolean wasPowered = false;
    private boolean pendingActivationEffect = false;
    private long lastActivationEffectGameTime = -200L;

    private final Vector3d queuedForcePos = new Vector3d();
    private final Vector3d queuedForce = new Vector3d();
    private final ForceTotal forceTotal = new ForceTotal();

    public HoverRepulsorBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.HOVER_REPULSOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HoverRepulsorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.ensureCreateNetworkRegistration();

        if ((level.getGameTime() & 3L) == 0L) {
            blockEntity.setDirectlyPowered(level.hasNeighborSignal(pos));
        }

        boolean poweredNow = blockEntity.isPowered();

        if (poweredNow && !blockEntity.wasPowered) {
            blockEntity.pendingActivationEffect = true;
        }

        blockEntity.wasPowered = poweredNow;
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

        Vec3 centreForcePoint = worldPosition.getCenter();

        boolean appliedAnyImpulse = false;
        TerrainCastResult closestTerrainForEffect = null;

        for (int sampleX = -SAMPLE_GRID_RADIUS; sampleX <= SAMPLE_GRID_RADIUS; sampleX++) {
            for (int sampleZ = -SAMPLE_GRID_RADIUS; sampleZ <= SAMPLE_GRID_RADIUS; sampleZ++) {
                Vec3 samplePoint = centreForcePoint.add(
                        sampleX * SAMPLE_SPACING,
                        0.0D,
                        sampleZ * SAMPLE_SPACING
                );

                TerrainCastResult terrain = computeTerrainBelowWorldDown(subLevel, samplePoint);

                if (terrain == null) {
                    continue;
                }

                if (closestTerrainForEffect == null || terrain.height() < closestTerrainForEffect.height()) {
                    closestTerrainForEffect = terrain;
                }

                if (applySampleImpulse(subLevel, samplePoint, terrain, timeStep)) {
                    appliedAnyImpulse = true;
                }
            }
        }

        if (pendingActivationEffect) {
            if (closestTerrainForEffect != null) {
                spawnActivationEffect(closestTerrainForEffect);
            } else {
                spawnActivationEffectAtBlock(subLevel, centreForcePoint);
            }

            pendingActivationEffect = false;
        }

        if (appliedAnyImpulse) {
            handle.applyForcesAndReset(forceTotal);
        }
    }

    private boolean applySampleImpulse(
            ServerSubLevel subLevel,
            Vec3 samplePoint,
            TerrainCastResult terrain,
            double timeStep
    ) {
        double height = terrain.height();

        if (height < MIN_VALID_HEIGHT || height > raycastRange) {
            return false;
        }

        double supportTop = targetHeight + supportStartAboveTarget;

        if (height > supportTop) {
            return false;
        }

        queuedForcePos.set(samplePoint.x, samplePoint.y, samplePoint.z);

        Vector3d velocity = Sable.HELPER.getVelocity(level, new Vector3d(queuedForcePos));

        if (!isFiniteVector(velocity)) {
            return false;
        }

        Vector3d localVelocity = subLevel.logicalPose().transformNormalInverse(velocity);

        if (!isFiniteVector(localVelocity)) {
            return false;
        }

        double verticalVelocity = localVelocity.dot(terrain.localWorldUp());

        double desiredVerticalVelocity;

        if (height > targetHeight) {
            double catch01 = clamp((supportTop - height) / supportStartAboveTarget, 0.0D, 1.0D);
            catch01 = smoothStep(catch01);

            desiredVerticalVelocity = lerp(-1.20D, -0.08D, catch01);
        } else {
            double compression01 = clamp((targetHeight - height) / suspensionTravel, 0.0D, 1.0D);
            compression01 = smoothStep(compression01);

            desiredVerticalVelocity = lerp(0.03D, 0.30D, compression01);
        }

        desiredVerticalVelocity = clamp(desiredVerticalVelocity, -1.20D, 0.30D);

        double deltaV = desiredVerticalVelocity - verticalVelocity;

        if (deltaV <= 0.0D) {
            return false;
        }

        double stepScale = clamp(timeStep / 0.05D, 0.05D, 1.0D);

        double authority;

        if (height > targetHeight) {
            authority = clamp((supportTop - height) / supportStartAboveTarget, 0.25D, 1.0D);
        } else {
            authority = 1.0D;
        }

        double allowedDeltaV = maxDeltaV * damping * strength * authority * stepScale;
        deltaV = clamp(deltaV, 0.0D, allowedDeltaV);

        double effectiveMass = estimateEffectiveMass(subLevel, terrain.localWorldUp());

        double impulseMagnitude = effectiveMass * deltaV * SAMPLE_FORCE_SHARE;
        impulseMagnitude = clamp(impulseMagnitude, 0.0D, maxImpulse * strength * SAMPLE_FORCE_SHARE);

        if (!Double.isFinite(impulseMagnitude) || impulseMagnitude < MIN_IMPULSE) {
            return false;
        }

        queuedForce.set(terrain.localWorldUp()).mul(impulseMagnitude);

        forceTotal.applyImpulseAtPoint(subLevel, queuedForcePos, queuedForce);
        return true;
    }

    private TerrainCastResult computeTerrainBelowWorldDown(ServerSubLevel ownSubLevel, Vec3 localStart) {
        if (level == null) {
            return null;
        }

        Vector3d localWorldDown = ownSubLevel.logicalPose().transformNormalInverse(
                new Vector3d(0.0D, -1.0D, 0.0D)
        );

        if (localWorldDown.lengthSquared() < 0.000001D) {
            return null;
        }

        localWorldDown.normalize();

        Vec3 localEnd = localStart.add(
                localWorldDown.x * raycastRange,
                localWorldDown.y * raycastRange,
                localWorldDown.z * raycastRange
        );

        ClipContext context = new ClipContext(
                localStart,
                localEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        );

        ((ClipContextExtension) context).sable$setIgnoredSubLevel(
                Sable.HELPER.getContaining(this)
        );

        BlockHitResult hitResult = level.clip(context);

        if (hitResult.getType() == HitResult.Type.MISS) {
            return null;
        }

        SubLevel hitSubLevel = Sable.HELPER.getContaining(level, hitResult.getLocation());

        Vec3 hitLocation = hitResult.getLocation();

        Vec3 worldStart = ownSubLevel.logicalPose().transformPosition(localStart);

        Vec3 worldHitLocation = hitSubLevel == null
                ? hitLocation
                : hitSubLevel.logicalPose().transformPosition(hitLocation);

        double height = worldStart.y - worldHitLocation.y;

        if (height <= 0.00001D) {
            return null;
        }

        Vector3d worldSurfaceNormal = new Vector3d(
                hitResult.getDirection().getStepX(),
                hitResult.getDirection().getStepY(),
                hitResult.getDirection().getStepZ()
        );

        if (hitSubLevel != null) {
            hitSubLevel.logicalPose().transformNormal(worldSurfaceNormal);
        }

        if (worldSurfaceNormal.lengthSquared() < 0.000001D) {
            return null;
        }

        worldSurfaceNormal.normalize();

        if (worldSurfaceNormal.y < 0.45D) {
            return null;
        }

        Vector3d localWorldUp = ownSubLevel.logicalPose().transformNormalInverse(
                new Vector3d(0.0D, 1.0D, 0.0D)
        );

        if (localWorldUp.lengthSquared() < 0.000001D) {
            return null;
        }

        localWorldUp.normalize();

        return new TerrainCastResult(height, localWorldUp, worldHitLocation);
    }

    private void spawnActivationEffect(TerrainCastResult terrain) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (level.getGameTime() - lastActivationEffectGameTime < 8L) {
            return;
        }

        lastActivationEffectGameTime = level.getGameTime();

        Vec3 ground = terrain.worldHitPosition();

        serverLevel.playSound(
                null,
                ground.x,
                ground.y,
                ground.z,
                SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS,
                0.35F,
                1.35F
        );

        serverLevel.playSound(
                null,
                ground.x,
                ground.y,
                ground.z,
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.BLOCKS,
                0.18F,
                1.75F
        );
    }

    private void spawnActivationEffectAtBlock(ServerSubLevel subLevel, Vec3 localForcePoint) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (level.getGameTime() - lastActivationEffectGameTime < 8L) {
            return;
        }

        lastActivationEffectGameTime = level.getGameTime();

        Vec3 worldPoint = subLevel.logicalPose().transformPosition(localForcePoint);

        serverLevel.playSound(
                null,
                worldPoint.x,
                worldPoint.y,
                worldPoint.z,
                SoundEvents.PISTON_EXTEND,
                SoundSource.BLOCKS,
                0.3F,
                1.45F
        );
    }

    private double estimateEffectiveMass(ServerSubLevel subLevel, Vector3d localNormal) {
        if (subLevel.getMassTracker() == null || subLevel.getMassTracker().isInvalid()) {
            return 80.0D;
        }

        double inverseNormalMass = subLevel.getMassTracker().getInverseNormalMass(
                queuedForcePos,
                localNormal
        );

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

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void setReceivedWirelessSignal(int signal) {
        signal = Math.max(0, Math.min(15, signal));

        if (receivedWirelessSignal == signal) {
            return;
        }

        receivedWirelessSignal = signal;
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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

    public double targetHeight() {
        return targetHeight;
    }

    public int getEncodedConfigValue(int parameter) {
        return switch (parameter) {
            case CONFIG_TARGET_HEIGHT -> encodeConfig(targetHeight);
            case CONFIG_DAMPING -> encodeConfig(damping);
            case CONFIG_SUSPENSION_TRAVEL -> encodeConfig(suspensionTravel);
            case CONFIG_CATCH_RANGE -> encodeConfig(supportStartAboveTarget);
            case CONFIG_MAX_IMPULSE -> encodeConfig(maxImpulse);
            case CONFIG_STRENGTH -> encodeConfig(strength);
            default -> 0;
        };
    }

    public void setEncodedConfigValue(int parameter, int encodedValue) {
        setConfigValue(parameter, decodeConfig(encodedValue));
    }

    public double getConfigValue(int parameter) {
        return switch (parameter) {
            case CONFIG_TARGET_HEIGHT -> targetHeight;
            case CONFIG_DAMPING -> damping;
            case CONFIG_SUSPENSION_TRAVEL -> suspensionTravel;
            case CONFIG_CATCH_RANGE -> supportStartAboveTarget;
            case CONFIG_MAX_IMPULSE -> maxImpulse;
            case CONFIG_STRENGTH -> strength;
            default -> 0.0D;
        };
    }

    public void setConfigValue(int parameter, double value) {
        switch (parameter) {
            case CONFIG_TARGET_HEIGHT -> targetHeight = clamp(value, 0.5D, 16.0D);
            case CONFIG_DAMPING -> damping = clamp(value, 0.25D, 3.0D);
            case CONFIG_SUSPENSION_TRAVEL -> suspensionTravel = clamp(value, 0.25D, 2.0D);
            case CONFIG_CATCH_RANGE -> supportStartAboveTarget = clamp(value, 0.25D, 4.0D);
            case CONFIG_MAX_IMPULSE -> maxImpulse = clamp(value, 5.0D, 5000.0D);
            case CONFIG_STRENGTH -> strength = clamp(value, 0.25D, 12.0D);
            default -> {
                return;
            }
        }

        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("menu.pod_racing_addon.hover_repulsor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new HoverRepulsorMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putDouble(TAG_TARGET_HEIGHT, targetHeight);
        tag.putDouble(TAG_RAYCAST_RANGE, raycastRange);
        tag.putDouble(TAG_MAX_DELTA_V, maxDeltaV);
        tag.putDouble(TAG_MAX_IMPULSE, maxImpulse);
        tag.putDouble(TAG_DAMPING, damping);
        tag.putDouble(TAG_SUSPENSION_TRAVEL, suspensionTravel);
        tag.putDouble(TAG_CATCH_RANGE, supportStartAboveTarget);
        tag.putDouble(TAG_STRENGTH, strength);

        ContainerHelper.saveAllItems(tag, frequencyItems, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_TARGET_HEIGHT)) {
            targetHeight = clamp(tag.getDouble(TAG_TARGET_HEIGHT), 0.5D, 16.0D);
        }

        if (tag.contains(TAG_RAYCAST_RANGE)) {
            raycastRange = clamp(tag.getDouble(TAG_RAYCAST_RANGE), 1.0D, 24.0D);
        }

        if (tag.contains(TAG_MAX_DELTA_V)) {
            maxDeltaV = clamp(tag.getDouble(TAG_MAX_DELTA_V), 0.01D, 1.0D);
        }

        if (tag.contains(TAG_MAX_IMPULSE)) {
            maxImpulse = clamp(tag.getDouble(TAG_MAX_IMPULSE), 5.0D, 5000.0D);
        }

        if (tag.contains(TAG_DAMPING)) {
            damping = clamp(tag.getDouble(TAG_DAMPING), 0.25D, 3.0D);
        }

        if (tag.contains(TAG_SUSPENSION_TRAVEL)) {
            suspensionTravel = clamp(tag.getDouble(TAG_SUSPENSION_TRAVEL), 0.25D, 2.0D);
        }

        if (tag.contains(TAG_CATCH_RANGE)) {
            supportStartAboveTarget = clamp(tag.getDouble(TAG_CATCH_RANGE), 0.25D, 4.0D);
        }

        if (tag.contains(TAG_STRENGTH)) {
            strength = clamp(tag.getDouble(TAG_STRENGTH), 0.25D, 12.0D);
        }

        frequencyItems.clear();
        ContainerHelper.loadAllItems(tag, frequencyItems, registries);

        createNetworkRegistered = false;
        receivedWirelessSignal = 0;
        wasPowered = false;
        pendingActivationEffect = false;
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

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    private static int encodeConfig(double value) {
        return (int) Math.round(value * CONFIG_SCALE);
    }

    private static double decodeConfig(int encodedValue) {
        return encodedValue / (double) CONFIG_SCALE;
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static double smoothStep(double value) {
        value = clamp(value, 0.0D, 1.0D);
        return value * value * (3.0D - 2.0D * value);
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

    private record TerrainCastResult(double height, Vector3d localWorldUp, Vec3 worldHitPosition) {
    }
}