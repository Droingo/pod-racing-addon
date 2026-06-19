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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

public final class HoverRepulsorBlock extends BaseEntityBlock {
    public static final MapCodec<HoverRepulsorBlock> CODEC = simpleCodec(HoverRepulsorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /*
     * Small centered mount shapes.
     *
     * These are 12x12 wide and 6 pixels deep.
     * They rotate based on the side of the block you place them on.
     *
     * If the model still feels too big/small, tweak the numbers here only.
     */
    private static final VoxelShape SHAPE_UP = Block.box(
            2.0D, 0.0D, 2.0D,
            14.0D, 6.0D, 14.0D
    );

    private static final VoxelShape SHAPE_DOWN = Block.box(
            2.0D, 10.0D, 2.0D,
            14.0D, 16.0D, 14.0D
    );

    private static final VoxelShape SHAPE_NORTH = Block.box(
            2.0D, 2.0D, 10.0D,
            14.0D, 14.0D, 16.0D
    );

    private static final VoxelShape SHAPE_SOUTH = Block.box(
            2.0D, 2.0D, 0.0D,
            14.0D, 14.0D, 6.0D
    );

    private static final VoxelShape SHAPE_EAST = Block.box(
            0.0D, 2.0D, 2.0D,
            6.0D, 14.0D, 14.0D
    );

    private static final VoxelShape SHAPE_WEST = Block.box(
            10.0D, 2.0D, 2.0D,
            16.0D, 14.0D, 14.0D
    );

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
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shapeForFacing(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getInteractionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
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

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (player.isShiftKeyDown()) {
            if (blockEntity instanceof HoverRepulsorBlockEntity hoverRepulsor) {
                serverPlayer.openMenu(hoverRepulsor);
                return InteractionResult.CONSUME;
            }
        }

        boolean hasCorrectBlockEntity = blockEntity instanceof HoverRepulsorBlockEntity;

        boolean powered = false;
        boolean directPower = false;
        int wirelessPower = 0;

        if (blockEntity instanceof HoverRepulsorBlockEntity hoverRepulsor) {
            powered = hoverRepulsor.isPowered();
            directPower = hoverRepulsor.isDirectlyPowered();
            wirelessPower = hoverRepulsor.getReceivedWirelessSignal();
        }

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
                        .append(Component.literal("Powered=" + powered + ", ")
                                .withStyle(powered ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(Component.literal("Direct=" + directPower + ", ")
                                .withStyle(directPower ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                        .append(Component.literal("Wireless=" + wirelessPower + ", ")
                                .withStyle(wirelessPower > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY))
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