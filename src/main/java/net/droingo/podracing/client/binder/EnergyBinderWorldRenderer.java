package net.droingo.podracing.client.binder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.droingo.podracing.PodRacingAddon;
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
    private static final ResourceLocation[] BEAM_TEXTURES = new ResourceLocation[]{
            ResourceLocation.fromNamespaceAndPath(PodRacingAddon.MOD_ID, "textures/effect/energy_beam_0.png"),
            ResourceLocation.fromNamespaceAndPath(PodRacingAddon.MOD_ID, "textures/effect/energy_beam_1.png"),
            ResourceLocation.fromNamespaceAndPath(PodRacingAddon.MOD_ID, "textures/effect/energy_beam_2.png")
    };

    private static final RenderType[] BEAM_RENDER_TYPES = new RenderType[]{
            RenderType.entityTranslucent(BEAM_TEXTURES[0]),
            RenderType.entityTranslucent(BEAM_TEXTURES[1]),
            RenderType.entityTranslucent(BEAM_TEXTURES[2])
    };

    private static final float MAIN_BEAM_WIDTH = 0.64F;
    private static final float CROSS_BEAM_WIDTH = 0.44F;
    private static final float UV_TILES_PER_BLOCK = 0.95F;

    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;
    private static final int MIN_ALPHA = 210;

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

            RenderType renderType = pickRenderType(connection, level);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            renderConnectionBeam(
                    poseStack,
                    consumer,
                    cameraPosition,
                    level,
                    connection,
                    renderTick
            );
        }

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static RenderType pickRenderType(EnergyBinderConnectionSnapshot connection, Level level) {
        long flickerStep = level.getGameTime() / 2L;
        int index = Math.floorMod(connection.id().hashCode() + (int) flickerStep, BEAM_RENDER_TYPES.length);
        return BEAM_RENDER_TYPES[index];
    }

    private static void renderConnectionBeam(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 cameraPosition,
            Level level,
            EnergyBinderConnectionSnapshot connection,
            int renderTick
    ) {
        Vec3 start = connection.endpointA().socketPosition(level);
        Vec3 end = connection.endpointB().socketPosition(level);

        Vec3 delta = end.subtract(start);
        double length = delta.length();

        if (length < 0.05D) {
            return;
        }

        Vec3 direction = delta.normalize();
        Vec3 middle = start.add(end).scale(0.5D);
        Vec3 cameraToMiddle = cameraPosition.subtract(middle);

        Vec3 cameraFacingSide = direction.cross(cameraToMiddle);

        if (cameraFacingSide.lengthSqr() < 0.0001D) {
            cameraFacingSide = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        }

        if (cameraFacingSide.lengthSqr() < 0.0001D) {
            cameraFacingSide = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }

        cameraFacingSide = cameraFacingSide.normalize();

        Vec3 crossSide = direction.cross(cameraFacingSide);

        if (crossSide.lengthSqr() < 0.0001D) {
            crossSide = new Vec3(0.0D, 1.0D, 0.0D);
        }

        crossSide = crossSide.normalize();

        int color = connection.color();

        int alpha = alpha(color);
        int red = red(color);
        int green = green(color);
        int blue = blue(color);

        float scroll = (renderTick % 40) / 40.0F;
        float u0 = scroll;
        float u1 = (float) (length * UV_TILES_PER_BLOCK) + scroll;

        PoseStack.Pose pose = poseStack.last();

        renderRibbon(
                consumer,
                pose,
                start,
                end,
                cameraFacingSide.scale(MAIN_BEAM_WIDTH * 0.5D),
                red,
                green,
                blue,
                alpha,
                u0,
                u1
        );

        renderRibbon(
                consumer,
                pose,
                start,
                end,
                crossSide.scale(CROSS_BEAM_WIDTH * 0.5D),
                red,
                green,
                blue,
                Math.min(255, alpha + 20),
                u0 + 0.15F,
                u1 + 0.15F
        );
    }

    private static void renderRibbon(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 start,
            Vec3 end,
            Vec3 side,
            int red,
            int green,
            int blue,
            int alpha,
            float u0,
            float u1
    ) {
        Vec3 startLeft = start.add(side);
        Vec3 startRight = start.subtract(side);
        Vec3 endLeft = end.add(side);
        Vec3 endRight = end.subtract(side);

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