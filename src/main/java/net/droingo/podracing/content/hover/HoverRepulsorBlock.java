package net.droingo.podracing.content.hover;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class HoverRepulsorBlock extends BaseEntityBlock {
    public static final MapCodec<HoverRepulsorBlock> CODEC = simpleCodec(HoverRepulsorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public HoverRepulsorBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        /*
         * The facing is the downwash/output direction.
         *
         * Click underside of a craft -> facing DOWN -> pushes air downward
         * and reacts upward.
         */
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HoverRepulsorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                PRBlockEntities.HOVER_REPULSOR.get(),
                HoverRepulsorBlockEntity::tick
        );
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!state.is(oldState.getBlock())) {
            HoverRepulsorManager.register(level, pos);
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())) {
            HoverRepulsorManager.unregister(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        HoverRepulsorManager.register(level, pos);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean hasCorrectBlockEntity = blockEntity instanceof HoverRepulsorBlockEntity;

        SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, pos);
        boolean isServerSubLevel = subLevel instanceof ServerSubLevel;

        Direction facing = state.getValue(FACING);

        serverPlayer.displayClientMessage(
                Component.literal("Repulsorlift Debug: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("BE=" + hasCorrectBlockEntity + ", ")
                                .withStyle(hasCorrectBlockEntity ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(Component.literal("SubLevel=" + isServerSubLevel + ", ")
                                .withStyle(isServerSubLevel ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(Component.literal("Facing=" + facing.getName())
                                .withStyle(ChatFormatting.YELLOW)),
                false
        );

        return InteractionResult.CONSUME;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}