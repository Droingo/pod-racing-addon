package net.droingo.podracing.content.stabilizer;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import net.droingo.podracing.util.PRItemChecks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public final class PodStabilizerBlock extends BaseEntityBlock implements BlockSubLevelLiftProvider {
    public static final MapCodec<PodStabilizerBlock> CODEC = simpleCodec(PodStabilizerBlock::new);

    /*
     * AXIS is the important physics property.
     * Sable reads this like a symmetrical sail.
     */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    /*
     * FACING is only visual/model roll around the active AXIS.
     * It must always be perpendicular to AXIS.
     */
    public static final DirectionProperty FACING = DirectionProperty.create("facing");

    /*
     * Kept for GUI for now.
     * This proof version uses fixed sail drag so we can lock placement/model first.
     */
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 15);

    public static final int DEFAULT_STRENGTH = 6;

    /*
     * 1.75F = roughly one real symmetrical sail.
     * 7.0F = about four sails in one block.
     */
    private static final float PARALLEL_DRAG = 7.0F;
    private static final float DIRECTIONLESS_DRAG = 0.06888202261F;

    /*
     * Simple broad hitboxes. These follow the visible roll direction, not just the physics axis.
     * We can tighten these later once the visual orientation is locked.
     */
    private static final VoxelShape SHAPE_FACING_X = Block.box(
            0.0D, 2.0D, 3.0D,
            16.0D, 14.0D, 13.0D
    );

    private static final VoxelShape SHAPE_FACING_Y = Block.box(
            3.0D, 0.0D, 3.0D,
            13.0D, 16.0D, 13.0D
    );

    private static final VoxelShape SHAPE_FACING_Z = Block.box(
            3.0D, 2.0D, 0.0D,
            13.0D, 14.0D, 16.0D
    );

    public PodStabilizerBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(FACING, Direction.NORTH)
                .setValue(STRENGTH, DEFAULT_STRENGTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction playerFacing = context.getHorizontalDirection().getOpposite();

        Direction.Axis axis;
        Direction facing;

        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            /*
             * Floor/ceiling:
             * Put the fin upright and make its physics axis horizontal.
             *
             * Looking north/south gives X-axis drag.
             * Looking east/west gives Z-axis drag.
             */
            axis = playerFacing.getClockWise().getAxis();
            facing = coerceFacingForAxis(playerFacing, axis);
        } else {
            /*
             * Wall placement:
             * Use the wall's axis as the sail normal, then start with the fin visually upright.
             */
            axis = clickedFace.getAxis();
            facing = coerceFacingForAxis(Direction.UP, axis);
        }

        return defaultBlockState()
                .setValue(AXIS, axis)
                .setValue(FACING, facing)
                .setValue(STRENGTH, DEFAULT_STRENGTH);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, FACING, STRENGTH);
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

    private static VoxelShape shapeForFacing(Direction facing) {
        return switch (facing.getAxis()) {
            case X -> SHAPE_FACING_X;
            case Y -> SHAPE_FACING_Y;
            case Z -> SHAPE_FACING_Z;
        };
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

        if (player.isShiftKeyDown()) {
            cyclePhysicsAxis(serverLevel, pos, state, player);
        } else {
            rotateVisualFacing(serverLevel, pos, state, player);
        }

        return ItemInteractionResult.CONSUME;
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

        if (player.isShiftKeyDown()) {
            pickup(serverLevel, pos, state, player);
            return InteractionResult.CONSUME;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof PodStabilizerBlockEntity stabilizer) {
            Direction.Axis axis = state.getValue(AXIS);
            Direction facing = state.getValue(FACING);

            serverPlayer.displayClientMessage(
                    Component.literal("Pod Stabilizer: ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("Axis=" + axis.getName() + ", ")
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Facing=" + facing.getName() + ", ")
                                    .withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("Drag=" + PARALLEL_DRAG + "x")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE)),
                    false
            );

            serverPlayer.openMenu(stabilizer);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.literal("Pod Stabilizer missing block entity."), false);
        return InteractionResult.CONSUME;
    }

    private static void rotateVisualFacing(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        Direction.Axis axis = state.getValue(AXIS);
        Direction currentFacing = state.getValue(FACING);
        Direction nextFacing = nextFacingAroundAxis(axis, currentFacing);

        level.setBlock(pos, state.setValue(FACING, nextFacing), 3);

        player.displayClientMessage(
                Component.literal("Pod Stabilizer facing: " + nextFacing.getName())
                        .withStyle(ChatFormatting.AQUA),
                true
        );
    }

    private static void cyclePhysicsAxis(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        Direction.Axis currentAxis = state.getValue(AXIS);
        Direction currentFacing = state.getValue(FACING);

        Direction.Axis nextAxis = switch (currentAxis) {
            case X -> Direction.Axis.Z;
            case Z -> Direction.Axis.Y;
            case Y -> Direction.Axis.X;
        };

        Direction nextFacing = coerceFacingForAxis(currentFacing, nextAxis);

        level.setBlock(pos, state
                .setValue(AXIS, nextAxis)
                .setValue(FACING, nextFacing), 3);

        player.displayClientMessage(
                Component.literal("Pod Stabilizer physics axis: " + nextAxis.getName())
                        .withStyle(ChatFormatting.YELLOW),
                true
        );
    }

    private static Direction nextFacingAroundAxis(Direction.Axis axis, Direction currentFacing) {
        Direction[] valid = validFacingsForAxis(axis);

        for (int i = 0; i < valid.length; i++) {
            if (valid[i] == currentFacing) {
                return valid[(i + 1) % valid.length];
            }
        }

        return valid[0];
    }

    private static Direction coerceFacingForAxis(Direction requestedFacing, Direction.Axis axis) {
        if (requestedFacing.getAxis() != axis) {
            return requestedFacing;
        }

        return validFacingsForAxis(axis)[0];
    }

    private static Direction[] validFacingsForAxis(Direction.Axis axis) {
        return switch (axis) {
            /*
             * Physics normal is X, so model can roll toward north/up/south/down.
             */
            case X -> new Direction[]{
                    Direction.NORTH,
                    Direction.UP,
                    Direction.SOUTH,
                    Direction.DOWN
            };

            /*
             * Physics normal is Y, so model can face around the floor plane.
             */
            case Y -> new Direction[]{
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST
            };

            /*
             * Physics normal is Z, so model can roll toward east/up/west/down.
             */
            case Z -> new Direction[]{
                    Direction.EAST,
                    Direction.UP,
                    Direction.WEST,
                    Direction.DOWN
            };
        };
    }

    private static void pickup(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        ItemStack pickedStack = new ItemStack(state.getBlock().asItem());

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof PodStabilizerBlockEntity) {
            CompoundTag blockEntityTag = blockEntity.saveWithoutMetadata(level.registryAccess());

            if (!blockEntityTag.isEmpty()) {
                pickedStack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockEntityTag));
            }
        }

        level.removeBlock(pos, false);

        if (!player.getInventory().add(pickedStack)) {
            player.drop(pickedStack, false);
        }
    }

    /*
     * Symmetric-sail style Sable physics.
     */

    @Override
    public float sable$getLiftScalar() {
        return 0.0F;
    }

    @Override
    public float sable$getParallelDragScalar() {
        return PARALLEL_DRAG;
    }

    @Override
    public float sable$getDirectionlessDragScalar() {
        return DIRECTIONLESS_DRAG;
    }

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(AXIS));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PodStabilizerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return null;
    }
}