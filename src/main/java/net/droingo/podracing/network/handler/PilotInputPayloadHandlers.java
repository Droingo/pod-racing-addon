package net.droingo.podracing.network.handler;

import net.droingo.podracing.content.pilot.PodControlCoreBlockEntity;
import net.droingo.podracing.content.pilot.PodPilotInputState;
import net.droingo.podracing.network.payload.TogglePilotModePayload;
import net.droingo.podracing.network.payload.UpdatePilotInputPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class PilotInputPayloadHandlers {
    private PilotInputPayloadHandlers() {
    }

    public static void handleUpdateOnClient(
            UpdatePilotInputPayload payload,
            IPayloadContext context
    ) {
        /*
         * Server owns stored input.
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

            PodControlCoreBlockEntity controlCore = null;

            if (payload.hasControlCore()) {
                BlockEntity blockEntity = serverPlayer.level().getBlockEntity(payload.controlCorePos());

                if (blockEntity instanceof PodControlCoreBlockEntity foundControlCore) {
                    controlCore = foundControlCore;
                }
            }

            if (controlCore != null) {
                controlCore.setPilotInput(
                        serverPlayer,
                        payload.active(),
                        payload.pitch(),
                        payload.roll(),
                        payload.yaw()
                );

                PodPilotInputState.updateFromPlayer(
                        serverPlayer,
                        payload.active(),
                        payload.pitch(),
                        payload.roll(),
                        payload.yaw(),
                        payload.controlCorePos(),
                        controlCore.getFrequency()
                );
            } else {
                PodPilotInputState.updateFromPlayer(
                        serverPlayer,
                        payload.active(),
                        payload.pitch(),
                        payload.roll(),
                        payload.yaw(),
                        null,
                        null
                );
            }
        });
    }

    public static void handleToggleOnClient(
            TogglePilotModePayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() ->
                net.droingo.podracing.client.pilot.PodPilotClient.toggleFromControlBlock(
                        payload.controlCorePos()
                )
        );
    }

    public static void handleToggleOnServer(
            TogglePilotModePayload payload,
            IPayloadContext context
    ) {
        /*
         * Server never receives this in normal use.
         */
    }
}