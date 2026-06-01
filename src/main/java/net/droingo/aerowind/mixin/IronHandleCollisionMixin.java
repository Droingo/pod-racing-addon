package net.droingo.aerowind.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class IronHandleCollisionMixin {
    private static final ResourceLocation SIMULATED_IRON_HANDLE =
            ResourceLocation.fromNamespaceAndPath("simulated", "iron_handle");

    /*
     * Tiny clickable hitbox.
     * This does NOT collide physically. It only makes the handle easier/cleaner to click.
     *
     * Full block coords are 0-16.
     */
    private static final VoxelShape TINY_HANDLE_SELECTION_SHAPE = Block.box(
            6.5D, 6.5D, 6.5D,
            9.5D, 9.5D, 9.5D
    );

    @Shadow
    public abstract Block getBlock();

    @Inject(
            method = "getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aerowind$removeIronHandleCollision(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (isIronHandle()) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(
            method = "getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void aerowind$shrinkIronHandleSelectionShape(
            BlockGetter level,
            BlockPos pos,
            CollisionContext context,
            CallbackInfoReturnable<VoxelShape> cir
    ) {
        if (isIronHandle()) {
            cir.setReturnValue(TINY_HANDLE_SELECTION_SHAPE);
        }
    }

    private boolean isIronHandle() {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(this.getBlock());
        return SIMULATED_IRON_HANDLE.equals(blockId);
    }
}