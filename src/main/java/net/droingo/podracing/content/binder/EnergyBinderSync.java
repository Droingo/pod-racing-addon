package net.droingo.podracing.content.binder;

import net.droingo.podracing.network.payload.SyncEnergyBindersPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class EnergyBinderSync {
    private EnergyBinderSync() {
    }

    public static void sendTo(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        PacketDistributor.sendToPlayer(player, createPayload(serverLevel));
    }

    public static void sendToAll(ServerLevel level) {
        PacketDistributor.sendToAllPlayers(createPayload(level));
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