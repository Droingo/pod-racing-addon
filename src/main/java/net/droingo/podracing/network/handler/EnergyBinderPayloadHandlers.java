package net.droingo.podracing.network.handler;

import net.droingo.podracing.client.binder.EnergyBinderClientState;
import net.droingo.podracing.network.payload.SyncEnergyBindersPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class EnergyBinderPayloadHandlers {
    private EnergyBinderPayloadHandlers() {
    }

    public static void handleSyncOnClient(SyncEnergyBindersPayload payload, IPayloadContext context) {
        EnergyBinderClientState.replaceConnections(payload.connections());
    }

    public static void handleSyncOnServer(SyncEnergyBindersPayload payload, IPayloadContext context) {
        // This payload is server -> client only in normal use.
        // The server handler exists because we register it as bidirectional for the 1.21.1 payload API shape.
    }
}