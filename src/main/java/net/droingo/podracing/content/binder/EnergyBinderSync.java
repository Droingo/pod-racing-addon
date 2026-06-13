package net.droingo.podracing.content.binder;

import net.droingo.podracing.network.payload.SyncEnergyBinderSelectionPayload;
import net.droingo.podracing.network.payload.SyncEnergyBindersPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class EnergyBinderSync {
    private EnergyBinderSync() {
    }

    public static void sendConnectionsTo(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PacketDistributor.sendToPlayer(player, createPayload(serverLevel));
    }

    public static void sendConnectionsToAll(ServerLevel level) {
        PacketDistributor.sendToAllPlayers(createPayload(level));
    }

    public static void sendSelectionTo(ServerPlayer player, EnergyBinderEndpoint endpoint) {
        PacketDistributor.sendToPlayer(player, new SyncEnergyBinderSelectionPayload(endpoint));
    }

    public static void clearSelectionFor(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SyncEnergyBinderSelectionPayload.clear());
    }

    private static SyncEnergyBindersPayload createPayload(ServerLevel level) {
        EnergyBinderSavedData data = EnergyBinderSavedData.get(level);

        List<EnergyBinderConnectionSnapshot> snapshots = data.connections()
                .stream()
                .map(EnergyBinderConnectionSnapshot::from)
                .toList();

        return new SyncEnergyBindersPayload(snapshots);
    }
}