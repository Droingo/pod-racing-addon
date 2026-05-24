package net.droingo.aerowind.mixin.client;

import net.droingo.aerowind.AeroWindBlocks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void aeroWind$holdTrophyPose(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!aeroWind$isHoldingTrophy(entity)) {
            return;
        }

        PlayerModel<?> playerModel = (PlayerModel<?>)(Object)this;
        HumanoidModel<?> humanoidModel = playerModel;

        // Lower forward hold pose.
        humanoidModel.rightArm.xRot = -0.82F;
        humanoidModel.leftArm.xRot = -0.82F;

        // Rotate arms inward toward the trophy.
        humanoidModel.rightArm.yRot = -0.38F;
        humanoidModel.leftArm.yRot = 0.38F;

        // Slight inward tilt.
        humanoidModel.rightArm.zRot = 0.12F;
        humanoidModel.leftArm.zRot = -0.12F;

        // Stable shoulder positions.
        humanoidModel.rightArm.x = -5.0F;
        humanoidModel.leftArm.x = 5.0F;
        humanoidModel.rightArm.y = 2.5F;
        humanoidModel.leftArm.y = 2.5F;

        // This fixes the second skin layer / sleeves not following the arms.
        playerModel.rightSleeve.copyFrom(humanoidModel.rightArm);
        playerModel.leftSleeve.copyFrom(humanoidModel.leftArm);
    }

    private static boolean aeroWind$isHoldingTrophy(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();

        return mainHand.is(AeroWindBlocks.TROPHY.asItem())
                || offHand.is(AeroWindBlocks.TROPHY.asItem());
    }
}