package net.droingo.podracing.network.payload;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.EnergyBinderConnectionSnapshot;
import net.droingo.podracing.content.binder.EnergyBinderEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncEnergyBindersPayload(List<EnergyBinderConnectionSnapshot> connections) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncEnergyBindersPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "sync_energy_binders"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncEnergyBindersPayload> STREAM_CODEC =
            StreamCodec.of(SyncEnergyBindersPayload::write, SyncEnergyBindersPayload::read);

    public SyncEnergyBindersPayload {
        connections = List.copyOf(connections);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(RegistryFriendlyByteBuf buffer, SyncEnergyBindersPayload payload) {
        buffer.writeVarInt(payload.connections.size());

        for (EnergyBinderConnectionSnapshot connection : payload.connections) {
            buffer.writeUUID(connection.id());
            writeEndpoint(buffer, connection.endpointA());
            writeEndpoint(buffer, connection.endpointB());
            buffer.writeDouble(connection.targetDistance());
            buffer.writeDouble(connection.stiffness());
            buffer.writeDouble(connection.damping());
            buffer.writeInt(connection.color());
            buffer.writeBoolean(connection.enabled());
        }
    }

    private static SyncEnergyBindersPayload read(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<EnergyBinderConnectionSnapshot> connections = new ArrayList<>(size);

        for (int index = 0; index < size; index++) {
            UUID id = buffer.readUUID();
            EnergyBinderEndpoint endpointA = readEndpoint(buffer);
            EnergyBinderEndpoint endpointB = readEndpoint(buffer);
            double targetDistance = buffer.readDouble();
            double stiffness = buffer.readDouble();
            double damping = buffer.readDouble();
            int color = buffer.readInt();
            boolean enabled = buffer.readBoolean();

            connections.add(new EnergyBinderConnectionSnapshot(
                    id,
                    endpointA,
                    endpointB,
                    targetDistance,
                    stiffness,
                    damping,
                    color,
                    enabled
            ));
        }

        return new SyncEnergyBindersPayload(connections);
    }

    private static void writeEndpoint(RegistryFriendlyByteBuf buffer, EnergyBinderEndpoint endpoint) {
        buffer.writeResourceLocation(endpoint.dimension().location());
        buffer.writeBlockPos(endpoint.pos());
    }

    private static EnergyBinderEndpoint readEndpoint(RegistryFriendlyByteBuf buffer) {
        ResourceLocation dimensionId = buffer.readResourceLocation();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        BlockPos pos = buffer.readBlockPos();

        return new EnergyBinderEndpoint(dimension, pos);
    }
}