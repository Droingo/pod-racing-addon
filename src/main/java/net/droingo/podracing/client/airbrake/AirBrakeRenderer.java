package net.droingo.podracing.client.airbrake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.airbrake.AirBrakeBlock;
import net.droingo.podracing.content.airbrake.AirBrakeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class AirBrakeRenderer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<AirBrakeBlockEntity> {
    private static final ModelResourceLocation BASE_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "block/air_brake_base"
            ));

    private static final ModelResourceLocation FLAP_MODEL =
            ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(
                    PodRacingAddon.MOD_ID,
                    "block/air_brake_flap"
            ));

    private final BlockRenderDispatcher blockRenderer;

    public AirBrakeRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(
            AirBrakeBlockEntity airBrake,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = airBrake.getBlockState();

        if (!state.hasProperty(AirBrakeBlock.MOUNT_FACE) || !state.hasProperty(AirBrakeBlock.FACING)) {
            return;
        }

        Direction mountFace = state.getValue(AirBrakeBlock.MOUNT_FACE);
        Direction facing = state.getValue(AirBrakeBlock.FACING);

        poseStack.pushPose();

        /*
         * Move to block centre, rotate the whole model, then move back.
         * The base/flap models should be authored as:
         *
         * - mounted on top of a block
         * - facing north
         * - closed position
         */
        poseStack.translate(0.5D, 0.5D, 0.5D);
        applyMountAndFacingTransform(poseStack, mountFace, facing);
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderModel(
                poseStack,
                bufferSource,
                state,
                BASE_MODEL,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay
        );

        /*
         * For now, render flap closed.
         * Next pass: rotate this flap around its hinge when powered.
         */
        int flapColor = airBrake.getFlapColorRgb();

        float red = ((flapColor >> 16) & 255) / 255.0F;
        float green = ((flapColor >> 8) & 255) / 255.0F;
        float blue = (flapColor & 255) / 255.0F;

        renderModel(
                poseStack,
                bufferSource,
                state,
                FLAP_MODEL,
                red,
                green,
                blue,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            ModelResourceLocation modelLocation,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(modelLocation);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                consumer,
                state,
                model,
                red,
                green,
                blue,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
    }

    private static void applyMountAndFacingTransform(PoseStack poseStack, Direction mountFace, Direction facing) {
        /*
         * Model default:
         * - sitting on top of the block
         * - mounted to UP face
         * - facing north
         *
         * First rotate from top-mount into the clicked mount face.
         */
        switch (mountFace) {
            case UP -> {
                // already top-mounted
            }
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));

            case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }

        /*
         * Then rotate around the mount normal so the air brake can face along
         * the surface.
         */
        float yaw = yawForFacing(facing);

        switch (mountFace) {
            case UP -> poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            case DOWN -> poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));

            case NORTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(yaw));
            case SOUTH -> poseStack.mulPose(Axis.ZP.rotationDegrees(-yaw));

            case EAST -> poseStack.mulPose(Axis.XP.rotationDegrees(yaw));
            case WEST -> poseStack.mulPose(Axis.XP.rotationDegrees(-yaw));
        }
    }

    private static float yawForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180.0F;
            case EAST -> 90.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}