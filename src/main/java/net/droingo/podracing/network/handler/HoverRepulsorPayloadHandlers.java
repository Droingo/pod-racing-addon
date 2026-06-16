package net.droingo.podracing.network.handler;

import net.droingo.podracing.content.hover.menu.HoverRepulsorMenu;
import net.droingo.podracing.network.payload.UpdateHoverRepulsorConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class HoverRepulsorPayloadHandlers {
    private HoverRepulsorPayloadHandlers() {
    }

    public static void handleUpdateOnClient(
            UpdateHoverRepulsorConfigPayload payload,
            IPayloadContext context
    ) {
        /*
         * Server is authoritative. Client does not apply config changes directly.
         */
    }

    public static void handleUpdateOnServer(
            UpdateHoverRepulsorConfigPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (serverPlayer.containerMenu.containerId != payload.containerId()) {
                return;
            }

            if (!(serverPlayer.containerMenu instanceof HoverRepulsorMenu menu)) {
                return;
            }

            menu.setServerConfigValue(payload.parameter(), payload.value());
        });
    }
}