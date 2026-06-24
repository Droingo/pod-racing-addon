package net.droingo.podracing.client.towcable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.towcable.TowCableAnchorBlockEntity;
import net.droingo.podracing.content.towcable.TowCableEndpoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public final class TowCableAnchorRenderer implements BlockEntityRenderer<TowCableAnchorBlockEntity> {
    /*
     * Temporary rope-looking visual.
     *
     * The actual physics rope is our near-zero-radius Sable RopePhysicsObject.
     * Sable does not render that rope for us, so this renderer draws the visible cable.
     */
    private static final ResourceLocation ROPE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/brown_wool.png");

    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(ROPE_TEXTURE);
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    private static final int SEGMENTS = 18;
    private static final float STRAND_WIDTH = 0.045F;

    public TowCableAnchorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            TowCableAnchorBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (!blockEntity.isLinked()) {
            return;
        }

        TowCableEndpoint linkedEndpoint = blockEntity.getLinkedEndpoint();

        if (linkedEndpoint == null) {
            return;
        }

        /*
         * Only one end renders the visual, otherwise we get duplicate ropes.
         */
        if (blockEntity.getBlockPos().asLong() > linkedEndpoint.pos().asLong()) {
            return;
        }

        Level level = blockEntity.getLevel();

        if (level == null || !linkedEndpoint.dimension().equals(level.dimension())) {
            return;
        }

        TowCableEndpoint self = TowCableEndpoint.from(level, blockEntity.getBlockPos());

        Vec3 startWorld = self.projectedWorldSocketPosition(level);
        Vec3 endWorld = linkedEndpoint.projectedWorldSocketPosition(level);

        double distance = startWorld.distanceTo(endWorld);

        if (!Double.isFinite(distance) || distance < 0.05D || distance > 128.0D) {
            return;
        }

        Vector3d projectedOrigin = Sable.HELPER.projectOutOfSubLevel(
                level,
                new Vector3d(
                        blockEntity.getBlockPos().getX(),
                        blockEntity.getBlockPos().getY(),
                        blockEntity.getBlockPos().getZ()
                ),
                new Vector3d()
        );

        Vec3 origin = new Vec3(projectedOrigin.x, projectedOrigin.y, projectedOrigin.z);

        Vec3 start = startWorld.subtract(origin);
        Vec3 end = endWorld.subtract(origin);

        List<Vec3> ropePoints = buildVisualRope(start, end, blockEntity.getRestLength());

        VertexConsumer consumer = bufferSource.getBuffer(RENDER_TYPE);
        Vec3 cameraPosition = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(origin);

        /*
         * Draw two tiny offset strands. This reads more like a rope/cable than one big black beam.
         */
        renderStrand(poseStack, consumer, ropePoints, cameraPosition, 0.0D);
        renderStrand(poseStack, consumer, ropePoints, cameraPosition, 0.035D);
    }

    private static List<Vec3> buildVisualRope(Vec3 start, Vec3 end, double restLength) {
        ArrayList<Vec3> points = new ArrayList<>(SEGMENTS + 1);

        Vec3 delta = end.subtract(start);
        double distance = delta.length();

        if (!Double.isFinite(distance) || distance < 0.001D) {
            points.add(start);
            points.add(end);
            return points;
        }

        double slack = Math.max(0.0D, restLength - distance);

        /*
         * Even with no slack, give it a small visual bow so it does not look like a laser beam.
         */
        double sag = Math.min(3.0D, 0.12D + slack * 0.55D);

        Vec3 down = new Vec3(0.0D, -1.0D, 0.0D);

        for (int i = 0; i <= SEGMENTS; i++) {
            double t = (double) i / (double) SEGMENTS;
            Vec3 point = start.lerp(end, t);

            /*
             * Parabolic sag: zero at anchors, strongest in middle.
             */
            double curve = 4.0D * t * (1.0D - t);
            point = point.add(down.scale(curve * sag));

            points.add(point);
        }

        return points;
    }

    private static void renderStrand(
            PoseStack poseStack,
            VertexConsumer consumer,
            List<Vec3> points,
            Vec3 cameraPosition,
            double strandOffset
    ) {
        PoseStack.Pose pose = poseStack.last();

        double travelled = 0.0D;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 start = points.get(i);
            Vec3 end = points.get(i + 1);
            Vec3 delta = end.subtract(start);

            double length = delta.length();

            if (length < 0.001D) {
                continue;
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

            side = side.normalize();

            /*
             * Offset the second strand slightly around the billboard side vector.
             */
            Vec3 strandShift = side.scale(strandOffset);
            Vec3 halfWidth = side.scale(STRAND_WIDTH * 0.5F);

            renderRibbonSegment(
                    consumer,
                    pose,
                    start.add(strandShift),
                    end.add(strandShift),
                    halfWidth,
                    (float) travelled,
                    (float) (travelled + length)
            );

            travelled += length;
        }
    }

    private static void renderRibbonSegment(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 start,
            Vec3 end,
            Vec3 halfWidth,
            float u0,
            float u1
    ) {
        Vec3 startLeft = start.add(halfWidth);
        Vec3 startRight = start.subtract(halfWidth);
        Vec3 endLeft = end.add(halfWidth);
        Vec3 endRight = end.subtract(halfWidth);

        addVertex(consumer, pose, startLeft, u0, 0.0F);
        addVertex(consumer, pose, startRight, u0, 1.0F);
        addVertex(consumer, pose, endRight, u1, 1.0F);
        addVertex(consumer, pose, endLeft, u1, 0.0F);

        addVertex(consumer, pose, endLeft, u1, 0.0F);
        addVertex(consumer, pose, endRight, u1, 1.0F);
        addVertex(consumer, pose, startRight, u0, 1.0F);
        addVertex(consumer, pose, startLeft, u0, 0.0F);
    }

    private static void addVertex(
            VertexConsumer consumer,
            PoseStack.Pose pose,
            Vec3 position,
            float u,
            float v
    ) {
        consumer.addVertex(
                        pose,
                        (float) position.x,
                        (float) position.y,
                        (float) position.z
                )
                .setColor(64, 44, 28, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}