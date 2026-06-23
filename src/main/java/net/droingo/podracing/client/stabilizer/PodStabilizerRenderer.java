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
     * Change this only if the whole model is consistently 90/180 degrees off.
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
         * Important:
         * Apply the mount transform first, then roll around the model's native Y axis.
         *
         * This makes the fin rotate flat on its own base first, then the whole
         * model gets mounted to the floor/wall/ceiling. That keeps the base stuck
         * to the wall instead of tipping vertically away from it.
         */
        applyMountTransform(poseStack, mountFace);
        applyNativeRollTransform(poseStack, roll);

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
     * Your Blockbench model is authored as a floor-mounted model.
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

    /*
     * This is the key fix.
     *
     * Do NOT roll around Z for north/south walls or X for east/west walls.
     * That makes the fin tip vertically.
     *
     * Always roll around the model's native Y axis. Since this is applied after
     * the mount transform in the PoseStack, it behaves like rotating the model
     * on its base before mounting it.
     */
    private static void applyNativeRollTransform(PoseStack poseStack, int roll) {
        float degrees = (((roll % 4) + 4) % 4) * 90.0F;

        float finalDegrees = degrees + MODEL_YAW_OFFSET_DEGREES;

        if (finalDegrees == 0.0F) {
            return;
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(finalDegrees));
    }
}