package net.droingo.aerowind.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.droingo.aerowind.block.RagdollPartBlock;
import net.droingo.aerowind.blockentity.RagdollPartBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import com.mojang.authlib.properties.Property;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RagdollPartRenderer implements BlockEntityRenderer<RagdollPartBlockEntity> {
    public RagdollPartRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            RagdollPartBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        ResourceLocation skinTexture = getSkinTexture(blockEntity);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skinTexture));

        poseStack.pushPose();

        RagdollPartBlock.PartShape partShape = blockEntity.getPartShape();
        String role = blockEntity.getRagdollRole();

        switch (partShape) {
            case HEAD -> renderHead(poseStack, consumer, packedLight);
            case TORSO -> renderTorso(poseStack, consumer, packedLight);
            case ARM -> renderArm(poseStack, consumer, packedLight, role);
            case LEG -> renderLeg(poseStack, consumer, packedLight, role);
        }

        poseStack.popPose();
    }

    private static ResourceLocation getSkinTexture(RagdollPartBlockEntity blockEntity) {
        UUID uuid = blockEntity.getSkinUuidAsUuid();
        String name = blockEntity.getSkinName();

        if (name == null || name.isBlank()) {
            name = "Steve";
        }

        if (uuid == null) {
            uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }

        GameProfile profile = new GameProfile(uuid, name);

        String textureValue = blockEntity.getSkinTextureValue();
        String textureSignature = blockEntity.getSkinTextureSignature();

        if (textureValue != null && !textureValue.isBlank()) {
            profile.getProperties().put(
                    "textures",
                    new Property(
                            "textures",
                            textureValue,
                            textureSignature == null || textureSignature.isBlank() ? null : textureSignature
                    )
            );
        }

        try {
            PlayerSkin skin = Minecraft.getInstance().getSkinManager().getInsecureSkin(profile);
            return skin.texture();
        } catch (Exception ignored) {
            return DefaultPlayerSkin.get(uuid).texture();
        }
    }

    private static void renderHead(PoseStack poseStack, VertexConsumer consumer, int light) {
        // Head base.
        renderCuboid(
                poseStack,
                consumer,
                light,
                4.0F / 16.0F, 4.0F / 16.0F, 4.0F / 16.0F,
                12.0F / 16.0F, 12.0F / 16.0F, 12.0F / 16.0F,
                new UvSet(
                        uv(8, 0, 16, 8),
                        uv(16, 0, 24, 8),
                        uv(8, 8, 16, 16),
                        uv(24, 8, 32, 16),
                        uv(0, 8, 8, 16),
                        uv(16, 8, 24, 16)
                )
        );

        // Head second layer / hat.
        renderCuboid(
                poseStack,
                consumer,
                light,
                3.5F / 16.0F, 3.5F / 16.0F, 3.5F / 16.0F,
                12.5F / 16.0F, 12.5F / 16.0F, 12.5F / 16.0F,
                new UvSet(
                        uv(40, 0, 48, 8),
                        uv(48, 0, 56, 8),
                        uv(40, 8, 48, 16),
                        uv(56, 8, 64, 16),
                        uv(32, 8, 40, 16),
                        uv(48, 8, 56, 16)
                )
        );
    }

    private static void renderTorso(PoseStack poseStack, VertexConsumer consumer, int light) {
        // Body base.
        renderCuboid(
                poseStack,
                consumer,
                light,
                4.0F / 16.0F, 2.0F / 16.0F, 6.0F / 16.0F,
                12.0F / 16.0F, 14.0F / 16.0F, 10.0F / 16.0F,
                new UvSet(
                        uv(20, 16, 28, 20),
                        uv(28, 16, 36, 20),
                        uv(20, 20, 28, 32),
                        uv(32, 20, 40, 32),
                        uv(16, 20, 20, 32),
                        uv(28, 20, 32, 32)
                )
        );

        // Body second layer / jacket.
        renderCuboid(
                poseStack,
                consumer,
                light,
                3.75F / 16.0F, 1.75F / 16.0F, 5.75F / 16.0F,
                12.25F / 16.0F, 14.25F / 16.0F, 10.25F / 16.0F,
                new UvSet(
                        uv(20, 32, 28, 36),
                        uv(28, 32, 36, 36),
                        uv(20, 36, 28, 48),
                        uv(32, 36, 40, 48),
                        uv(16, 36, 20, 48),
                        uv(28, 36, 32, 48)
                )
        );
    }

    private static void renderArm(PoseStack poseStack, VertexConsumer consumer, int light, String role) {
        boolean left = role != null && role.contains("left");

        if (left) {
            // Left arm base.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    6.0F / 16.0F, 2.0F / 16.0F, 6.0F / 16.0F,
                    10.0F / 16.0F, 14.0F / 16.0F, 10.0F / 16.0F,
                    new UvSet(
                            uv(36, 48, 40, 52),
                            uv(40, 48, 44, 52),
                            uv(36, 52, 40, 64),
                            uv(44, 52, 48, 64),
                            uv(32, 52, 36, 64),
                            uv(40, 52, 44, 64)
                    )
            );

            // Left sleeve.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    5.75F / 16.0F, 1.75F / 16.0F, 5.75F / 16.0F,
                    10.25F / 16.0F, 14.25F / 16.0F, 10.25F / 16.0F,
                    new UvSet(
                            uv(52, 48, 56, 52),
                            uv(56, 48, 60, 52),
                            uv(52, 52, 56, 64),
                            uv(60, 52, 64, 64),
                            uv(48, 52, 52, 64),
                            uv(56, 52, 60, 64)
                    )
            );
        } else {
            // Right arm base.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    6.0F / 16.0F, 2.0F / 16.0F, 6.0F / 16.0F,
                    10.0F / 16.0F, 14.0F / 16.0F, 10.0F / 16.0F,
                    new UvSet(
                            uv(44, 16, 48, 20),
                            uv(48, 16, 52, 20),
                            uv(44, 20, 48, 32),
                            uv(52, 20, 56, 32),
                            uv(40, 20, 44, 32),
                            uv(48, 20, 52, 32)
                    )
            );

            // Right sleeve.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    5.75F / 16.0F, 1.75F / 16.0F, 5.75F / 16.0F,
                    10.25F / 16.0F, 14.25F / 16.0F, 10.25F / 16.0F,
                    new UvSet(
                            uv(44, 32, 48, 36),
                            uv(48, 32, 52, 36),
                            uv(44, 36, 48, 48),
                            uv(52, 36, 56, 48),
                            uv(40, 36, 44, 48),
                            uv(48, 36, 52, 48)
                    )
            );
        }
    }

    private static void renderLeg(PoseStack poseStack, VertexConsumer consumer, int light, String role) {
        boolean left = role != null && role.contains("left");

        if (left) {
            // Left leg base.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    6.0F / 16.0F, 2.0F / 16.0F, 6.0F / 16.0F,
                    10.0F / 16.0F, 14.0F / 16.0F, 10.0F / 16.0F,
                    new UvSet(
                            uv(20, 48, 24, 52),
                            uv(24, 48, 28, 52),
                            uv(20, 52, 24, 64),
                            uv(28, 52, 32, 64),
                            uv(16, 52, 20, 64),
                            uv(24, 52, 28, 64)
                    )
            );

            // Left pants layer.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    5.75F / 16.0F, 1.75F / 16.0F, 5.75F / 16.0F,
                    10.25F / 16.0F, 14.25F / 16.0F, 10.25F / 16.0F,
                    new UvSet(
                            uv(4, 48, 8, 52),
                            uv(8, 48, 12, 52),
                            uv(4, 52, 8, 64),
                            uv(12, 52, 16, 64),
                            uv(0, 52, 4, 64),
                            uv(8, 52, 12, 64)
                    )
            );
        } else {
            // Right leg base.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    6.0F / 16.0F, 2.0F / 16.0F, 6.0F / 16.0F,
                    10.0F / 16.0F, 14.0F / 16.0F, 10.0F / 16.0F,
                    new UvSet(
                            uv(4, 16, 8, 20),
                            uv(8, 16, 12, 20),
                            uv(4, 20, 8, 32),
                            uv(12, 20, 16, 32),
                            uv(0, 20, 4, 32),
                            uv(8, 20, 12, 32)
                    )
            );

            // Right pants layer.
            renderCuboid(
                    poseStack,
                    consumer,
                    light,
                    5.75F / 16.0F, 1.75F / 16.0F, 5.75F / 16.0F,
                    10.25F / 16.0F, 14.25F / 16.0F, 10.25F / 16.0F,
                    new UvSet(
                            uv(4, 32, 8, 36),
                            uv(8, 32, 12, 36),
                            uv(4, 36, 8, 48),
                            uv(12, 36, 16, 48),
                            uv(0, 36, 4, 48),
                            uv(8, 36, 12, 48)
                    )
            );
        }
    }

    private static void renderCuboid(
            PoseStack poseStack,
            VertexConsumer consumer,
            int light,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            UvSet uv
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();

        // Up
        quad(consumer, matrix, pose, light,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                maxX, maxY, minZ,
                0.0F, 1.0F, 0.0F,
                uv.top
        );

        // Down
        quad(consumer, matrix, pose, light,
                minX, minY, maxZ,
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                0.0F, -1.0F, 0.0F,
                uv.bottom
        );

        // Front / south
        quad(consumer, matrix, pose, light,
                minX, minY, maxZ,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                0.0F, 0.0F, 1.0F,
                uv.front
        );

        // Back / north
        quad(consumer, matrix, pose, light,
                maxX, minY, minZ,
                minX, minY, minZ,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                0.0F, 0.0F, -1.0F,
                uv.back
        );

        // Right / west side
        quad(consumer, matrix, pose, light,
                minX, minY, minZ,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                minX, maxY, minZ,
                -1.0F, 0.0F, 0.0F,
                uv.right
        );

        // Left / east side
        quad(consumer, matrix, pose, light,
                maxX, minY, maxZ,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                1.0F, 0.0F, 0.0F,
                uv.left
        );
    }

    private static void quad(
            VertexConsumer consumer,
            Matrix4f matrix,
            PoseStack.Pose pose,
            int light,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float normalX,
            float normalY,
            float normalZ,
            Uv uv
    ) {
        vertex(consumer, matrix, pose, light, x1, y1, z1, uv.u1, uv.v2, normalX, normalY, normalZ);
        vertex(consumer, matrix, pose, light, x2, y2, z2, uv.u2, uv.v2, normalX, normalY, normalZ);
        vertex(consumer, matrix, pose, light, x3, y3, z3, uv.u2, uv.v1, normalX, normalY, normalZ);
        vertex(consumer, matrix, pose, light, x4, y4, z4, uv.u1, uv.v1, normalX, normalY, normalZ);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            PoseStack.Pose pose,
            int light,
            float x,
            float y,
            float z,
            float u,
            float v,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private static Uv uv(float x1, float y1, float x2, float y2) {
        return new Uv(
                x1 / 64.0F,
                y1 / 64.0F,
                x2 / 64.0F,
                y2 / 64.0F
        );
    }

    private record Uv(float u1, float v1, float u2, float v2) {
    }

    private record UvSet(Uv top, Uv bottom, Uv front, Uv back, Uv right, Uv left) {
    }
}