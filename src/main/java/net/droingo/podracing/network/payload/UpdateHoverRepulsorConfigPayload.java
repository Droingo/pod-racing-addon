package net.droingo.podracing.network.payload;

import net.droingo.podracing.PodRacingAddon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateHoverRepulsorConfigPayload(
        int containerId,
        int parameter,
        double value
) implements CustomPacketPayload {
    public static final Type<UpdateHoverRepulsorConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "update_hover_repulsor_config"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateHoverRepulsorConfigPayload> STREAM_CODEC =
            StreamCodec.of(UpdateHoverRepulsorConfigPayload::write, UpdateHoverRepulsorConfigPayload::read);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, UpdateHoverRepulsorConfigPayload payload) {
        buffer.writeVarInt(payload.containerId);
        buffer.writeVarInt(payload.parameter);
        buffer.writeDouble(payload.value);
    }

    private static UpdateHoverRepulsorConfigPayload read(RegistryFriendlyByteBuf buffer) {
        return new UpdateHoverRepulsorConfigPayload(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readDouble()
        );
    }
}