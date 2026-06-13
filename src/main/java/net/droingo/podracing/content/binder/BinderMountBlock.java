package net.droingo.podracing.content.binder;

import com.mojang.serialization.MapCodec;
import net.droingo.podracing.util.PRItemChecks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class BinderMountBlock extends BaseEntityBlock {
    public static final MapCodec<BinderMountBlock> CODEC = simpleCodec(BinderMountBlock::new);

    public BinderMountBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BinderMountBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!PRItemChecks.isCreateWrench(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemInteractionResult.CONSUME;
        }

        EnergyBinderEndpoint clickedEndpoint = EnergyBinderEndpoint.from(level, pos);
        EnergyBinderManager.handleMountWrenched(serverLevel, player, clickedEndpoint);

        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            EnergyBinderEndpoint endpoint = EnergyBinderEndpoint.from(level, pos);
            int removed = EnergyBinderSavedData.get(serverLevel).removeConnectionsTouching(endpoint);

            if (removed > 0) {
                serverLevel.players().forEach(player -> player.displayClientMessage(
                        Component.literal("Removed " + removed + " Energy Binder connection(s).")
                                .withStyle(ChatFormatting.YELLOW),
                        true
                ));
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}