package net.droingo.podracing.network.payload;

import net.droingo.podracing.PodRacingAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdatePilotInputPayload(
        boolean active,
        float pitch,
        float roll,
        float yaw,
        boolean hasControlCore,
        BlockPos controlCorePos
) implements CustomPacketPayload {
    public static final Type<UpdatePilotInputPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "update_pilot_input"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePilotInputPayload> STREAM_CODEC =
            StreamCodec.of(UpdatePilotInputPayload::write, UpdatePilotInputPayload::read);

    @Override
    public Type<UpdatePilotInputPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, UpdatePilotInputPayload payload) {
        buffer.writeBoolean(payload.active);
        buffer.writeFloat(payload.pitch);
        buffer.writeFloat(payload.roll);
        buffer.writeFloat(payload.yaw);
        buffer.writeBoolean(payload.hasControlCore);

        if (payload.hasControlCore) {
            buffer.writeBlockPos(payload.controlCorePos);
        }
    }

    private static UpdatePilotInputPayload read(RegistryFriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        float pitch = buffer.readFloat();
        float roll = buffer.readFloat();
        float yaw = buffer.readFloat();
        boolean hasControlCore = buffer.readBoolean();
        BlockPos controlCorePos = hasControlCore ? buffer.readBlockPos() : BlockPos.ZERO;

        return new UpdatePilotInputPayload(
                active,
                pitch,
                roll,
                yaw,
                hasControlCore,
                controlCorePos
        );
    }
}