package net.droingo.podracing.client.stabilizer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.stabilizer.PodStabilizerBlock;
import net.droingo.podracing.content.stabilizer.PodStabilizerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class PodStabilizerRenderer implements BlockEntityRenderer<PodStabilizerBlockEntity> {
    private static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PodRacingAddon.MOD_ID, "block/pod_stabilizer")
    );

    /*
     * Change only this if the model is consistently 90 degrees off.
     *
     * If it still faces right when placed on the ground, try -90.0F.
     * If it faces backwards, try 180.0F.
     */
    private static final float MODEL_YAW_OFFSET_DEGREES = 0.0F;

    private final BlockRenderDispatcher blockRenderer;

    public PodStabilizerRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(
            PodStabilizerBlockEntity stabilizer,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = stabilizer.getBlockState();

        if (!state.hasProperty(PodStabilizerBlock.MOUNT_FACE)
                || !state.hasProperty(PodStabilizerBlock.ROLL)) {
            return;
        }

        Direction mountFace = state.getValue(PodStabilizerBlock.MOUNT_FACE);
        int roll = state.getValue(PodStabilizerBlock.ROLL);

        poseStack.pushPose();

        poseStack.translate(0.5D, 0.5D, 0.5D);

        /*
         * First attach the model to the correct face.
         * Then rotate 0/90/180/270 around that mounted face.
         */
        applyMountTransform(poseStack, mountFace);
        poseStack.mulPose(Axis.YP.rotationDegrees((roll * 90.0F) + MODEL_YAW_OFFSET_DEGREES));

        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderModel(
                poseStack,
                bufferSource,
                state,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        BakedModel model = Minecraft.getInstance()
                .getModelManager()
                .getModel(MODEL);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());

        blockRenderer.getModelRenderer().renderModel(
                poseStack.last(),
                consumer,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
    }

    /*
     * Native model assumption:
     * - your Blockbench model is authored as a floor-mounted model.
     * - roll=0 is its default forward direction.
     */
    private static void applyMountTransform(PoseStack poseStack, Direction mountFace) {
        switch (mountFace) {
            case UP -> {
                // Floor placement. Native orientation.
            }

            case DOWN -> {
                // Ceiling placement.
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            case NORTH -> {
                // North wall.
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }

            case SOUTH -> {
                // South wall.
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            }

            case EAST -> {
                // East wall.
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }

            case WEST -> {
                // West wall.
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
        }
    }
}