package net.droingo.podracing.client.pilot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.pilot.PodControlCoreBlock;
import net.droingo.podracing.content.pilot.PodControlCoreBlockEntity;
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

public final class PodControlCoreRenderer implements BlockEntityRenderer<PodControlCoreBlockEntity> {
    private static final ModelResourceLocation BASE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PodRacingAddon.MOD_ID, "block/pod_control_core_base")
    );

    private static final ModelResourceLocation YOKE_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(PodRacingAddon.MOD_ID, "block/pod_control_core_yoke")
    );

    /*
     * Tune these once we see your model in-game.
     */
    private static final double YOKE_PIVOT_X = 0.5D;
    private static final double YOKE_PIVOT_Y = 0.5D;
    private static final double YOKE_PIVOT_Z = 0.5D;

    private static final float MAX_YOKE_ROLL_DEGREES = 32.0F;
    private static final double MAX_YOKE_PUSH_BLOCKS = 0.125D;

    private final BlockRenderDispatcher blockRenderer;

    public PodControlCoreRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(
            PodControlCoreBlockEntity controlCore,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = controlCore.getBlockState();

        Direction facing = Direction.NORTH;

        if (state.hasProperty(PodControlCoreBlock.FACING)) {
            facing = state.getValue(PodControlCoreBlock.FACING);
        }

        poseStack.pushPose();

        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawForFacing(facing)));
        poseStack.translate(-0.5D, -0.5D, -0.5D);

        renderModel(
                poseStack,
                bufferSource,
                state,
                BASE_MODEL,
                packedLight,
                packedOverlay
        );

        float rollInput = controlCore.getVisualRoll(partialTick);
        float pitchInput = controlCore.getVisualPitch(partialTick);

        float yokeRollDegrees = rollInput * MAX_YOKE_ROLL_DEGREES;

        /*
         * W currently sends pitch -1 and S sends +1.
         * Negative pitch moves the yoke inward.
         */
        double yokePush = -pitchInput * MAX_YOKE_PUSH_BLOCKS;

        poseStack.pushPose();

        poseStack.translate(0.0D, 0.0D, yokePush);

        poseStack.translate(YOKE_PIVOT_X, YOKE_PIVOT_Y, YOKE_PIVOT_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(yokeRollDegrees));
        poseStack.translate(-YOKE_PIVOT_X, -YOKE_PIVOT_Y, -YOKE_PIVOT_Z);

        renderModel(
                poseStack,
                bufferSource,
                state,
                YOKE_MODEL,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();

        poseStack.popPose();
    }

    private void renderModel(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            ModelResourceLocation modelLocation,
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
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
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