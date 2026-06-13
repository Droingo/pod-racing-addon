package net.droingo.podracing.client.binder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.droingo.podracing.content.binder.EnergyBinderConnectionSnapshot;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class EnergyBinderWorldRenderer {
    private static final ResourceLocation PLACEHOLDER_BEAM_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/end_crystal/end_crystal_beam.png");

    private static final RenderType BEAM_RENDER_TYPE =
            RenderType.entityTranslucent(PLACEHOLDER_BEAM_TEXTURE);

    private static final float BEAM_WIDTH = 0.28F;
    private static final float UV_TILES_PER_BLOCK = 0.65F;
    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private EnergyBinderWorldRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();

        if (poseStack == null) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(BEAM_RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        int renderTick = event.getRenderTick();

        for (EnergyBinderConnectionSnapshot connection : EnergyBinderClientState.connections()) {
            if (!connection.enabled()) {
                continue;
            }

            if (!connection.endpointA().dimension().equals(level.dimension())) {
                continue;
            }

            if (!connection.endpointB().dimension().equals(level.dimension())) {
                continue;
            }

            renderConnectionBeam(
                    poseStack,
                    consumer,
                    cameraPosition,
                    connection,
                    renderTick
            );
        }

        poseStack.popPose();

        bufferSource.endBatch(BEAM_RENDER_TYPE);
    }

    private static void renderConnectionBeam(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 cameraPosition,
            EnergyBinderConnectionSnapshot connection,
            int renderTick
    ) {
        Vec3 start = connection.endpointA().center();
        Vec3 end = connection.endpointB().center();

        Vec3 delta = end.subtract(start);
        double length = delta.length();

        if (length < 0.05D) {
            return;
        }

        Vec3 direction = delta.normalize();
        Vec3 middle = start.add(end).scale(0.5D);
        Vec3 cameraToMiddle = cameraPosition.subtract(middle);

        Vec3 side = direction.cross(cameraToMiddle);

        if (side.lengthSqr() < 0.0001D) {
            side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }

        if (side.lengthSqr() < 0.0001D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }

        side = side.normalize().scale(BEAM_WIDTH * 0.5D);

        Vec3 startLeft = start.add(side);
        Vec3 startRight = start.subtract(side);
        Vec3 endLeft = end.add(side);
        Vec3 endRight = end.subtract(side);

        int color = connection.color();

        int alpha = alpha(color);
        int red = red(color);
        int green = green(color);
        int blue = blue(color);

        float scroll = (renderTick % 40) / 40.0F;
        float u0 = scroll;
        float u1 = (float) (length * UV_TILES_PER_BLOCK) + scroll;

        PoseStack.Pose pose = poseStack.last();

        addVertex(consumer, pose, startLeft, red, green, blue, alpha, u0, 0.0F);
        addVertex(consumer, pose, startRight, red, green, blue, alpha, u0, 1.0F);
        addVertex(consumer, pose, endRight, red, green, blue, alpha, u1, 1.0F);
        addVertex(consumer, pose, endLeft, red, green, blue, alpha, u1, 0.0F);

        addVertex(consumer, pose, endLeft, red, green, blue, alpha, u1, 0.0F);
        addVertex(consumer, pose, endRight, red, green, blue, alpha, u1, 1.0F);
        addVertex(consumer, pose, startRight, red, green, blue, alpha, u0, 1.0F);
        addVertex(consumer, pose, startLeft, red, green, blue, alpha, u0, 0.0F);
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 position,
            int red,
            int green,
            int blue,
            int alpha,
            float u,
            float v
    ) {
        consumer.addVertex(
                        pose,
                        (float) position.x,
                        (float) position.y,
                        (float) position.z
                )
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static int alpha(int argb) {
        int value = (argb >>> 24) & 0xFF;
        return value <= 0 ? 255 : value;
    }

    private static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }
}