package net.droingo.podracing.content.pilot;

import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class PodControlCoreBlockEntity extends BlockEntity {
    private static final String TAG_ACTIVE = "PilotActive";
    private static final String TAG_ROLL = "PilotRoll";
    private static final String TAG_PITCH = "PilotPitch";
    private static final String TAG_YAW = "PilotYaw";
    private static final String TAG_FREQ_MOST = "FrequencyMost";
    private static final String TAG_FREQ_LEAST = "FrequencyLeast";

    private UUID frequency = UUID.randomUUID();

    private UUID activePilot;
    private boolean pilotActive;
    private float targetRoll;
    private float targetPitch;
    private float targetYaw;

    private float previousVisualRoll;
    private float previousVisualPitch;
    private float visualRoll;
    private float visualPitch;

    public PodControlCoreBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.POD_CONTROL_CORE.get(), pos, blockState);
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            PodControlCoreBlockEntity blockEntity
    ) {
        if (!level.isClientSide()) {
            return;
        }

        blockEntity.previousVisualRoll = blockEntity.visualRoll;
        blockEntity.previousVisualPitch = blockEntity.visualPitch;

        float wantedRoll = blockEntity.pilotActive ? blockEntity.targetRoll : 0.0F;
        float wantedPitch = blockEntity.pilotActive ? blockEntity.targetPitch : 0.0F;

        blockEntity.visualRoll += (wantedRoll - blockEntity.visualRoll) * 0.35F;
        blockEntity.visualPitch += (wantedPitch - blockEntity.visualPitch) * 0.35F;
    }

    public UUID getFrequency() {
        if (frequency == null) {
            frequency = UUID.randomUUID();
            setChanged();
        }

        return frequency;
    }

    public String getFrequencyShortName() {
        String text = getFrequency().toString();
        return text.substring(0, 8);
    }

    public void setPilotInput(ServerPlayer player, boolean active, float pitch, float roll, float yaw) {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean changed = false;

        if (!active) {
            if (pilotActive && (activePilot == null || activePilot.equals(player.getUUID()))) {
                pilotActive = false;
                activePilot = null;
                targetPitch = 0.0F;
                targetRoll = 0.0F;
                targetYaw = 0.0F;
                changed = true;
            }
        } else {
            pitch = clamp(pitch, -1.0F, 1.0F);
            roll = clamp(roll, -1.0F, 1.0F);
            yaw = clamp(yaw, -1.0F, 1.0F);

            if (!player.getUUID().equals(activePilot)) {
                activePilot = player.getUUID();
                changed = true;
            }

            if (!pilotActive) {
                pilotActive = true;
                changed = true;
            }

            if (Math.abs(targetPitch - pitch) > 0.001F) {
                targetPitch = pitch;
                changed = true;
            }

            if (Math.abs(targetRoll - roll) > 0.001F) {
                targetRoll = roll;
                changed = true;
            }

            if (Math.abs(targetYaw - yaw) > 0.001F) {
                targetYaw = yaw;
                changed = true;
            }
        }

        if (changed) {
            setChanged();
            syncToClients();
        }
    }

    public boolean isPilotActive() {
        return pilotActive;
    }

    public float getVisualRoll(float partialTick) {
        return lerp(partialTick, previousVisualRoll, visualRoll);
    }

    public float getVisualPitch(float partialTick) {
        return lerp(partialTick, previousVisualPitch, visualPitch);
    }

    private void syncToClients() {
        if (level == null || level.isClientSide()) {
            return;
        }

        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        UUID safeFrequency = getFrequency();

        tag.putLong(TAG_FREQ_MOST, safeFrequency.getMostSignificantBits());
        tag.putLong(TAG_FREQ_LEAST, safeFrequency.getLeastSignificantBits());

        tag.putBoolean(TAG_ACTIVE, pilotActive);
        tag.putFloat(TAG_PITCH, targetPitch);
        tag.putFloat(TAG_ROLL, targetRoll);
        tag.putFloat(TAG_YAW, targetYaw);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_FREQ_MOST) && tag.contains(TAG_FREQ_LEAST)) {
            frequency = new UUID(
                    tag.getLong(TAG_FREQ_MOST),
                    tag.getLong(TAG_FREQ_LEAST)
            );
        } else if (frequency == null) {
            frequency = UUID.randomUUID();
        }

        pilotActive = tag.getBoolean(TAG_ACTIVE);
        targetPitch = tag.getFloat(TAG_PITCH);
        targetRoll = tag.getFloat(TAG_ROLL);
        targetYaw = tag.getFloat(TAG_YAW);

        if (!pilotActive) {
            targetPitch = 0.0F;
            targetRoll = 0.0F;
            targetYaw = 0.0F;
        }
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

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    private static float lerp(float partialTick, float previous, float current) {
        return previous + (current - previous) * partialTick;
    }
}