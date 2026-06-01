package net.droingo.aerowind.client;

import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.AeroWindBlockEntities;
import net.droingo.aerowind.client.render.RagdollPartRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = AeroWind.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class AeroWindClientRenderers {
    private AeroWindClientRenderers() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                AeroWindBlockEntities.RAGDOLL_PART.get(),
                RagdollPartRenderer::new
        );
    }
}