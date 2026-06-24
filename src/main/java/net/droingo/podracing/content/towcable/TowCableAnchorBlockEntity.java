package net.droingo.podracing.content.towcable;

import net.droingo.podracing.content.towcable.menu.TowCableMenu;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class TowCableAnchorBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_LINKED = "linked";
    private static final String TAG_ENDPOINT = "endpoint";
    private static final String TAG_REST_LENGTH = "rest_length";
    private static final String TAG_STIFFNESS = "stiffness";
    private static final String TAG_DAMPING = "damping";
    private static final String TAG_MIN_SEPARATION = "min_separation";

    private TowCableEndpoint linkedEndpoint;

    /*
     * These still exist so the existing GUI compiles.
     * For the native Sable rope test, restLength is used for rope creation.
     * Stiffness/damping/min separation are ignored for now.
     */
    private double restLength = 8.0D;
    private double stiffness = 0.0D;
    private double damping = 0.0D;
    private double minSeparation = 0.0D;

    public TowCableAnchorBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.TOW_CABLE_ANCHOR.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TowCableAnchorBlockEntity blockEntity) {
        if (level instanceof ServerLevel serverLevel) {
            LightSableRopeManager.ensure(serverLevel, blockEntity);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Light Sable Rope Anchor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TowCableMenu(containerId, playerInventory, this);
    }

    public boolean isLinked() {
        return linkedEndpoint != null;
    }

    public TowCableEndpoint getLinkedEndpoint() {
        return linkedEndpoint;
    }

    public double getRestLength() {
        return restLength;
    }

    public double getStiffness() {
        return stiffness;
    }

    public double getDamping() {
        return damping;
    }

    public double getMinSeparation() {
        return minSeparation;
    }

    public void linkTo(TowCableEndpoint endpoint, double distance) {
        linkedEndpoint = endpoint;
        restLength = clamp(distance, 0.5D, 64.0D);
        stiffness = 0.0D;
        damping = 0.0D;
        minSeparation = 0.0D;
        syncChanged();
    }

    public void unlinkBothSides() {
        TowCableEndpoint oldLinkedEndpoint = linkedEndpoint;
        clearLinkLocal();

        if (level == null || oldLinkedEndpoint == null) {
            return;
        }

        BlockEntity other = level.getBlockEntity(oldLinkedEndpoint.pos());

        if (other instanceof TowCableAnchorBlockEntity otherAnchor) {
            otherAnchor.clearLinkIfMatches(TowCableEndpoint.from(level, worldPosition));
        }
    }

    public void clearLinkIfMatches(TowCableEndpoint endpoint) {
        if (linkedEndpoint == null || !linkedEndpoint.equals(endpoint)) {
            return;
        }

        clearLinkLocal();
    }

    private void clearLinkLocal() {
        if (level instanceof ServerLevel serverLevel) {
            LightSableRopeManager.removeForEndpoint(
                    serverLevel,
                    TowCableEndpoint.from(level, worldPosition)
            );
        }

        linkedEndpoint = null;
        syncChanged();
    }

    public void setRestLength(double value) {
        restLength = clamp(value, 0.5D, 64.0D);
        pushSettingsToLinked();
        syncChanged();
    }

    public void adjustRestLength(double amount) {
        setRestLength(restLength + amount);
    }

    public void setStiffness(double value) {
        stiffness = clamp(value, 0.0D, 1.0D);
        pushSettingsToLinked();
        syncChanged();
    }

    public void adjustStiffness(double amount) {
        setStiffness(stiffness + amount);
    }

    public void setDamping(double value) {
        damping = clamp(value, 0.0D, 1.0D);
        pushSettingsToLinked();
        syncChanged();
    }

    public void adjustDamping(double amount) {
        setDamping(damping + amount);
    }

    public void setMinSeparation(double value) {
        minSeparation = clamp(value, 0.0D, restLength);
        pushSettingsToLinked();
        syncChanged();
    }

    public void adjustMinSeparation(double amount) {
        setMinSeparation(minSeparation + amount);
    }

    public void useCurrentDistanceAsRestLength() {
        double distance = currentDistance();

        if (!Double.isFinite(distance)) {
            return;
        }

        setRestLength(distance);
    }

    private double currentDistance() {
        if (level == null || linkedEndpoint == null) {
            return Double.NaN;
        }

        return TowCableEndpoint.from(level, worldPosition).projectedWorldDistanceTo(level, linkedEndpoint);
    }

    private void pushSettingsToLinked() {
        if (level == null || linkedEndpoint == null) {
            return;
        }

        BlockEntity other = level.getBlockEntity(linkedEndpoint.pos());

        if (other instanceof TowCableAnchorBlockEntity otherAnchor) {
            otherAnchor.applySettingsFromOther(restLength, stiffness, damping, minSeparation);
        }
    }

    private void applySettingsFromOther(
            double restLength,
            double stiffness,
            double damping,
            double minSeparation
    ) {
        this.restLength = clamp(restLength, 0.5D, 64.0D);
        this.stiffness = clamp(stiffness, 0.0D, 1.0D);
        this.damping = clamp(damping, 0.0D, 1.0D);
        this.minSeparation = clamp(minSeparation, 0.0D, this.restLength);
        syncChanged();
    }

    private void syncChanged() {
        setChanged();

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putBoolean(TAG_LINKED, linkedEndpoint != null);

        if (linkedEndpoint != null) {
            tag.put(TAG_ENDPOINT, linkedEndpoint.save());
        }

        tag.putDouble(TAG_REST_LENGTH, restLength);
        tag.putDouble(TAG_STIFFNESS, stiffness);
        tag.putDouble(TAG_DAMPING, damping);
        tag.putDouble(TAG_MIN_SEPARATION, minSeparation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.getBoolean(TAG_LINKED) && tag.contains(TAG_ENDPOINT)) {
            linkedEndpoint = TowCableEndpoint.load(tag.getCompound(TAG_ENDPOINT));
        } else {
            linkedEndpoint = null;
        }

        restLength = tag.contains(TAG_REST_LENGTH) ? tag.getDouble(TAG_REST_LENGTH) : 8.0D;
        stiffness = tag.contains(TAG_STIFFNESS) ? tag.getDouble(TAG_STIFFNESS) : 0.0D;
        damping = tag.contains(TAG_DAMPING) ? tag.getDouble(TAG_DAMPING) : 0.0D;
        minSeparation = tag.contains(TAG_MIN_SEPARATION) ? tag.getDouble(TAG_MIN_SEPARATION) : 0.0D;

        restLength = clamp(restLength, 0.5D, 64.0D);
        stiffness = clamp(stiffness, 0.0D, 1.0D);
        damping = clamp(damping, 0.0D, 1.0D);
        minSeparation = clamp(minSeparation, 0.0D, restLength);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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