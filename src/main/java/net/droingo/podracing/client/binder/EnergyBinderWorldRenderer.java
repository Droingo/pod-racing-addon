package net.droingo.podracing.client.binder;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.EnergyBinderConnectionSnapshot;
import net.droingo.podracing.content.binder.EnergyBinderEndpoint;
import net.droingo.podracing.registry.PRBlocks;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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

    private static final float MAIN_BEAM_WIDTH = 0.34F;
    private static final float CROSS_BEAM_WIDTH = 0.24F;
    private static final float PREVIEW_WIDTH_MULTIPLIER = 0.72F;
    private static final float UV_TILES_PER_BLOCK = 0.95F;

    private static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    /*
     * Visual-only prediction.
     *
     * The previous version only smoothed toward ticked endpoint positions.
     * This version estimates velocity per tick and predicts the in-between
     * render-frame position using partial ticks.
     */
    private static final double VISUAL_FOLLOW_SMOOTHING = 0.88D;
    private static final double PREDICTION_STRENGTH = 1.0D;

    /*
     * Snap instead of smoothing after teleports/assembly/load jumps.
     */
    private static final double SNAP_DISTANCE_SQR = 64.0D;

    private static final Map<UUID, BeamVisualState> BEAM_VISUAL_STATES = new HashMap<>();

    private EnergyBinderWorldRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;

        if (level == null) {
            clearSmoothedEndpoints();
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
        long gameTime = level.getGameTime();
        double partialTick = clamp01(event.getPartialTick().getGameTimeDeltaPartialTick(false));

        Set<UUID> renderedThisFrame = new HashSet<>();

        for (EnergyBinderConnectionSnapshot connection : EnergyBinderClientState.connections()) {
            if (!connection.enabled() || !connection.active()) {
                continue;
            }

            if (!connection.endpointA().dimension().equals(level.dimension())) {
                continue;
            }

            if (!connection.endpointB().dimension().equals(level.dimension())) {
                continue;
            }

            Vec3 targetStart = connection.endpointA().projectedSocketPosition(level);
            Vec3 targetEnd = connection.endpointB().projectedSocketPosition(level);

            SmoothedBeamEndpoints visualEndpoints = updateVisualState(
                    connection.id(),
                    targetStart,
                    targetEnd,
                    gameTime,
                    partialTick
            );

            renderedThisFrame.add(connection.id());

            RenderType renderType = pickRenderType(connection.id().hashCode(), level);
            VertexConsumer consumer = bufferSource.getBuffer(renderType);

            renderBeamBetween(
                    poseStack,
                    consumer,
                    cameraPosition,
                    visualEndpoints.start(),
                    visualEndpoints.end(),
                    connection.color(),
                    1.0F,
                    renderTick
            );
        }

        removeUnusedVisualStates(renderedThisFrame);

        renderPreviewBeam(
                minecraft,
                level,
                poseStack,
                bufferSource,
                cameraPosition,
                renderTick
        );

        poseStack.popPose();
        bufferSource.endBatch();
    }

    private static SmoothedBeamEndpoints updateVisualState(
            UUID id,
            Vec3 targetStart,
            Vec3 targetEnd,
            long gameTime,
            double partialTick
    ) {
        BeamVisualState state = BEAM_VISUAL_STATES.get(id);

        if (state == null) {
            state = new BeamVisualState(targetStart, targetEnd, gameTime);
            BEAM_VISUAL_STATES.put(id, state);
            return new SmoothedBeamEndpoints(targetStart, targetEnd);
        }

        boolean largeJump =
                state.lastTargetStart.distanceToSqr(targetStart) > SNAP_DISTANCE_SQR
                        || state.lastTargetEnd.distanceToSqr(targetEnd) > SNAP_DISTANCE_SQR
                        || state.renderedStart.distanceToSqr(targetStart) > SNAP_DISTANCE_SQR
                        || state.renderedEnd.distanceToSqr(targetEnd) > SNAP_DISTANCE_SQR;

        if (largeJump) {
            state.reset(targetStart, targetEnd, gameTime);
            return new SmoothedBeamEndpoints(targetStart, targetEnd);
        }

        if (gameTime != state.lastGameTime) {
            long tickDelta = Math.max(1L, gameTime - state.lastGameTime);

            state.velocityStartPerTick = targetStart.subtract(state.lastTargetStart).scale(1.0D / tickDelta);
            state.velocityEndPerTick = targetEnd.subtract(state.lastTargetEnd).scale(1.0D / tickDelta);

            state.lastTargetStart = targetStart;
            state.lastTargetEnd = targetEnd;
            state.lastGameTime = gameTime;
        }

        Vec3 predictedStart = state.lastTargetStart.add(
                state.velocityStartPerTick.scale(partialTick * PREDICTION_STRENGTH)
        );

        Vec3 predictedEnd = state.lastTargetEnd.add(
                state.velocityEndPerTick.scale(partialTick * PREDICTION_STRENGTH)
        );

        state.renderedStart = smoothVec(state.renderedStart, predictedStart, VISUAL_FOLLOW_SMOOTHING);
        state.renderedEnd = smoothVec(state.renderedEnd, predictedEnd, VISUAL_FOLLOW_SMOOTHING);

        return new SmoothedBeamEndpoints(state.renderedStart, state.renderedEnd);
    }

    private static Vec3 smoothVec(Vec3 current, Vec3 target, double smoothing) {
        return new Vec3(
                current.x + (target.x - current.x) * smoothing,
                current.y + (target.y - current.y) * smoothing,
                current.z + (target.z - current.z) * smoothing
        );
    }

    private static void removeUnusedVisualStates(Set<UUID> renderedThisFrame) {
        BEAM_VISUAL_STATES.keySet().removeIf(id -> !renderedThisFrame.contains(id));
    }

    public static void clearSmoothedEndpoints() {
        BEAM_VISUAL_STATES.clear();
    }

    private static void renderPreviewBeam(
            Minecraft minecraft,
            Level level,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            Vec3 cameraPosition,
            int renderTick
    ) {
        Optional<EnergyBinderEndpoint> selectedEndpoint = EnergyBinderClientState.selectedEndpoint();

        if (selectedEndpoint.isEmpty()) {
            return;
        }

        EnergyBinderEndpoint startEndpoint = selectedEndpoint.get();

        if (!startEndpoint.dimension().equals(level.dimension())) {
            return;
        }

        Vec3 start = startEndpoint.projectedSocketPosition(level);
        Vec3 end = previewEndPosition(minecraft, level);

        if (end == null) {
            return;
        }

        RenderType renderType = pickRenderType(startEndpoint.pos().hashCode(), level);
        VertexConsumer consumer = bufferSource.getBuffer(renderType);

        renderBeamBetween(
                poseStack,
                consumer,
                cameraPosition,
                start,
                end,
                0x99FFFFFF,
                PREVIEW_WIDTH_MULTIPLIER,
                renderTick
        );
    }

    private static Vec3 previewEndPosition(Minecraft minecraft, Level level) {
        HitResult hitResult = minecraft.hitResult;

        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos hitPos = blockHitResult.getBlockPos();
            BlockState hitState = level.getBlockState(hitPos);

            if (hitState.is(PRBlocks.BINDER_MOUNT.get())) {
                return EnergyBinderEndpoint.from(level, hitPos).projectedSocketPosition(level);
            }

            return blockHitResult.getLocation();
        }

        Player player = minecraft.player;

        if (player == null) {
            return null;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();

        return eye.add(look.scale(12.0D));
    }

    private static RenderType pickRenderType(int seed, Level level) {
        long flickerStep = level.getGameTime() / 3L;
        int index = Math.floorMod(seed + (int) flickerStep, BEAM_RENDER_TYPES.length);
        return BEAM_RENDER_TYPES[index];
    }

    private static void renderBeamBetween(
            PoseStack poseStack,
            VertexConsumer consumer,
            Vec3 cameraPosition,
            Vec3 start,
            Vec3 end,
            int color,
            float widthMultiplier,
            int renderTick
    ) {
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
                cameraFacingSide.scale(MAIN_BEAM_WIDTH * widthMultiplier * 0.5F),
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
                crossSide.scale(CROSS_BEAM_WIDTH * widthMultiplier * 0.5F),
                red,
                green,
                blue,
                alpha,
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

    private static double clamp01(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }

        if (value > 1.0D) {
            return 1.0D;
        }

        return value;
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

    private static final class BeamVisualState {
        private Vec3 renderedStart;
        private Vec3 renderedEnd;

        private Vec3 lastTargetStart;
        private Vec3 lastTargetEnd;

        private Vec3 velocityStartPerTick = new Vec3(0.0D, 0.0D, 0.0D);
        private Vec3 velocityEndPerTick = new Vec3(0.0D, 0.0D, 0.0D);

        private long lastGameTime;

        private BeamVisualState(Vec3 start, Vec3 end, long gameTime) {
            reset(start, end, gameTime);
        }

        private void reset(Vec3 start, Vec3 end, long gameTime) {
            renderedStart = start;
            renderedEnd = end;
            lastTargetStart = start;
            lastTargetEnd = end;
            velocityStartPerTick = new Vec3(0.0D, 0.0D, 0.0D);
            velocityEndPerTick = new Vec3(0.0D, 0.0D, 0.0D);
            lastGameTime = gameTime;
        }
    }

    private record SmoothedBeamEndpoints(Vec3 start, Vec3 end) {
    }
}