package net.droingo.podracing.network.payload;

import net.droingo.podracing.PodRacingAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TogglePilotModePayload(BlockPos controlCorePos) implements CustomPacketPayload {
    public static final Type<TogglePilotModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "toggle_pilot_mode"
            )
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TogglePilotModePayload> STREAM_CODEC =
            StreamCodec.of(TogglePilotModePayload::write, TogglePilotModePayload::read);

    @Override
    public Type<TogglePilotModePayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, TogglePilotModePayload payload) {
        buffer.writeBlockPos(payload.controlCorePos);
    }

    private static TogglePilotModePayload read(RegistryFriendlyByteBuf buffer) {
        return new TogglePilotModePayload(buffer.readBlockPos());
    }
}