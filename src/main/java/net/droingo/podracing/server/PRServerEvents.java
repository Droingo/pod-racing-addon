package net.droingo.podracing.server;

import net.droingo.podracing.content.binder.EnergyBinderSync;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PRServerEvents {
    private PRServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            EnergyBinderSync.sendTo(serverPlayer);
        }
    }
}