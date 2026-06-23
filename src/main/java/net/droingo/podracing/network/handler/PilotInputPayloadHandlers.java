package net.droingo.podracing.network.handler;

import net.droingo.podracing.content.pilot.PodPilotInputState;
import net.droingo.podracing.network.payload.UpdatePilotInputPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PilotInputPayloadHandlers {
    private PilotInputPayloadHandlers() {
    }

    public static void handleUpdateOnClient(
            UpdatePilotInputPayload payload,
            IPayloadContext context
    ) {
        /*
         * Server is authoritative.
         */
    }

    public static void handleUpdateOnServer(
            UpdatePilotInputPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            PodPilotInputState.updateFromPlayer(
                    serverPlayer,
                    payload.active(),
                    payload.pitch(),
                    payload.roll(),
                    payload.yaw()
            );
        });
    }
}