package net.droingo.podracing.network.handler;

import net.droingo.podracing.client.binder.EnergyBinderClientState;
import net.droingo.podracing.network.payload.SyncEnergyBinderSelectionPayload;
import net.droingo.podracing.network.payload.SyncEnergyBindersPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class EnergyBinderPayloadHandlers {
    private EnergyBinderPayloadHandlers() {
    }

    public static void handleSyncOnClient(SyncEnergyBindersPayload payload, IPayloadContext context) {
        EnergyBinderClientState.replaceConnections(payload.connections());
    }

    public static void handleSyncOnServer(SyncEnergyBindersPayload payload, IPayloadContext context) {
        // Server ignores client attempts to sync binder connection state.
    }

    public static void handleSelectionSyncOnClient(SyncEnergyBinderSelectionPayload payload, IPayloadContext context) {
        if (payload.hasEndpoint()) {
            EnergyBinderClientState.setSelectedEndpoint(payload.endpoint());
        } else {
            EnergyBinderClientState.clearSelectedEndpoint();
        }
    }

    public static void handleSelectionSyncOnServer(SyncEnergyBinderSelectionPayload payload, IPayloadContext context) {
        // Server owns selection state. Client does not set this directly.
    }
}