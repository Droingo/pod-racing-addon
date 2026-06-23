package net.droingo.podracing.client;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.client.airbrake.AirBrakeRenderer;
import net.droingo.podracing.client.airbrake.AirBrakeScreen;
import net.droingo.podracing.client.binder.BinderMountScreen;
import net.droingo.podracing.client.binder.EnergyBinderWorldRenderer;
import net.droingo.podracing.client.hover.HoverRepulsorScreen;
import net.droingo.podracing.client.stabilizer.PodStabilizerRenderer;
import net.droingo.podracing.client.stabilizer.PodStabilizerScreen;
import net.droingo.podracing.registry.PRBlockEntities;
import net.droingo.podracing.registry.PRMenuTypes;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
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
            event.register(PRMenuTypes.HOVER_REPULSOR.get(), HoverRepulsorScreen::new);
            event.register(PRMenuTypes.AIR_BRAKE.get(), AirBrakeScreen::new);
            event.register(PRMenuTypes.POD_STABILIZER.get(), PodStabilizerScreen::new);

        }

        @SubscribeEvent
        public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    PRBlockEntities.AIR_BRAKE.get(),
                    AirBrakeRenderer::new
            );
            event.registerBlockEntityRenderer(
                    PRBlockEntities.POD_STABILIZER.get(),
                    PodStabilizerRenderer::new
            );
        }

        @SubscribeEvent
        public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
            event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            PodRacingAddon.MOD_ID,
                            "block/air_brake_base"
                    )
            ));
            event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            PodRacingAddon.MOD_ID,
                            "block/pod_stabilizer"
                    )
            ));

            event.register(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            PodRacingAddon.MOD_ID,
                            "block/air_brake_flap"
                    )
            ));

        }
    }
}