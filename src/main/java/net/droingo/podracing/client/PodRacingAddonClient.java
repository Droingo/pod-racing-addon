package net.droingo.podracing.client;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.client.binder.BinderMountScreen;
import net.droingo.podracing.client.binder.EnergyBinderWorldRenderer;
import net.droingo.podracing.registry.PRMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
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

    @EventBusSubscriber(
            modid = PodRacingAddon.MOD_ID,
            value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD
    )
    public static final class ModBusEvents {
        private ModBusEvents() {
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(PRMenuTypes.BINDER_MOUNT.get(), BinderMountScreen::new);
        }
    }
}