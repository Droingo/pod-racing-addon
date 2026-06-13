package net.droingo.podracing.content.binder;

import com.mojang.serialization.MapCodec;
import net.droingo.podracing.registry.PRBlockEntities;
import net.droingo.podracing.util.PRItemChecks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BinderMountBlock extends BaseEntityBlock {
    public static final MapCodec<BinderMountBlock> CODEC = simpleCodec(BinderMountBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_UP = box(5.0D, 0.0D, 5.0D, 11.0D, 4.0D, 11.0D);
    private static final VoxelShape SHAPE_DOWN = box(5.0D, 12.0D, 5.0D, 11.0D, 16.0D, 11.0D);

    private static final VoxelShape SHAPE_NORTH = box(5.0D, 5.0D, 12.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 4.0D);

    private static final VoxelShape SHAPE_EAST = box(0.0D, 5.0D, 5.0D, 4.0D, 11.0D, 11.0D);
    private static final VoxelShape SHAPE_WEST = box(12.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);

    public BinderMountBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BinderMountBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                PRBlockEntities.BINDER_MOUNT.get(),
                BinderMountBlockEntity::tick
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeForFacing(state.getValue(FACING));
    }

    private static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing) {
            case UP -> SHAPE_UP;
            case DOWN -> SHAPE_DOWN;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        MenuProvider menuProvider = state.getMenuProvider(level, pos);

        if (menuProvider == null) {
            return InteractionResult.PASS;
        }

        serverPlayer.openMenu(menuProvider);
        return InteractionResult.CONSUME;
    }

    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof BinderMountBlockEntity binderMount) {
            return binderMount;
        }

        return null;
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
                EnergyBinderSync.sendConnectionsToAll(serverLevel);

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