package net.droingo.podracing.client;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.client.binder.EnergyBinderWorldRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = PodRacingAddon.MOD_ID,
        value = Dist.CLIENT
)
public final class PodRacingAddonClient {
    private PodRacingAddonClient() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        EnergyBinderWorldRenderer.onRenderLevelStage(event);
    }
}