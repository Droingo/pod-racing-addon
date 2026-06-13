package net.droingo.podracing.network.payload;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.EnergyBinderEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record SyncEnergyBinderSelectionPayload(EnergyBinderEndpoint endpoint) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncEnergyBinderSelectionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "sync_energy_binder_selection"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEnergyBinderSelectionPayload> STREAM_CODEC =
            StreamCodec.of(SyncEnergyBinderSelectionPayload::write, SyncEnergyBinderSelectionPayload::read);

    public static SyncEnergyBinderSelectionPayload clear() {
        return new SyncEnergyBinderSelectionPayload(null);
    }

    public boolean hasEndpoint() {
        return endpoint != null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, SyncEnergyBinderSelectionPayload payload) {
        buffer.writeBoolean(payload.endpoint != null);

        if (payload.endpoint == null) {
            return;
        }

        buffer.writeResourceLocation(payload.endpoint.dimension().location());
        buffer.writeBlockPos(payload.endpoint.pos());
    }

    private static SyncEnergyBinderSelectionPayload read(RegistryFriendlyByteBuf buffer) {
        boolean hasEndpoint = buffer.readBoolean();

        if (!hasEndpoint) {
            return SyncEnergyBinderSelectionPayload.clear();
        }

        ResourceLocation dimensionId = buffer.readResourceLocation();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        BlockPos pos = buffer.readBlockPos();

        return new SyncEnergyBinderSelectionPayload(new EnergyBinderEndpoint(dimension, pos));
    }
}