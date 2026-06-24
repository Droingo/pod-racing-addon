package net.droingo.podracing.content.attitudefin;

import com.mojang.serialization.MapCodec;
import net.droingo.podracing.content.pilot.PodPilotInputState;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AttitudeFinBlock extends BaseEntityBlock {
    public static final MapCodec<AttitudeFinBlock> CODEC = simpleCodec(AttitudeFinBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");
    public static final EnumProperty<AttitudeFinRole> ROLE =
            EnumProperty.create("role", AttitudeFinRole.class);

    private static final VoxelShape SHAPE = Block.box(
            1.0D, 0.0D, 1.0D,
            15.0D, 14.0D, 15.0D
    );

    public AttitudeFinBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(REVERSED, false)
                .setValue(ROLE, AttitudeFinRole.LEFT_ENGINE));
    }

    @Override
    protected MapCodec<AttitudeFinBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, REVERSED, ROLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite()
        );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AttitudeFinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                PRBlockEntities.ATTITUDE_FIN.get(),
                AttitudeFinBlockEntity::tick
        );
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

        if (player.isShiftKeyDown()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            if (blockEntity instanceof AttitudeFinBlockEntity attitudeFin) {
                PodPilotInputState.Command command =
                        PodPilotInputState.findCommandForPlayer(level, player.getUUID());

                if (command != null && command.controlFrequency() != null) {
                    attitudeFin.bindToFrequency(command.controlFrequency());

                    player.displayClientMessage(
                            Component.literal("Attitude Fin bound to core frequency " + attitudeFin.getFrequencyShortName()),
                            true
                    );

                    return InteractionResult.SUCCESS;
                }
            }

            boolean reversed = !state.getValue(REVERSED);
            level.setBlock(pos, state.setValue(REVERSED, reversed), 3);

            player.displayClientMessage(
                    Component.literal("Attitude Fin reversed: " + (reversed ? "ON" : "OFF")),
                    true
            );

            return InteractionResult.SUCCESS;
        }

        AttitudeFinRole nextRole = state.getValue(ROLE).next();
        level.setBlock(pos, state.setValue(ROLE, nextRole), 3);

        player.displayClientMessage(
                Component.literal("Attitude Fin role: " + nextRole.displayName()),
                true
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}