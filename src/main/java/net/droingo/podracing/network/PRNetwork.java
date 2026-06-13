package net.droingo.podracing.network;

import net.droingo.podracing.network.handler.EnergyBinderPayloadHandlers;
import net.droingo.podracing.network.payload.SyncEnergyBindersPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class PRNetwork {
    private PRNetwork() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(PRNetwork::registerPayloadHandlers);
    }

    private static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playBidirectional(
                SyncEnergyBindersPayload.TYPE,
                SyncEnergyBindersPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        EnergyBinderPayloadHandlers::handleSyncOnClient,
                        EnergyBinderPayloadHandlers::handleSyncOnServer
                )
        );
    }
}