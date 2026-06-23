package net.droingo.podracing.content.stabilizer;

import com.mojang.serialization.MapCodec;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PodStabilizerBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<PodStabilizerBlock> CODEC = simpleCodec(PodStabilizerBlock::new);

    /*
     * FACE = what surface it is mounted on.
     * FACING = which way the front points visually.
     * ROLL = 0/1/2/3 = 90-degree steps when wrenched.
     * AXIS = physics axis used by the sail effect.
     * STRENGTH can remain if your sail logic still reads it.
     */
    public static final DirectionProperty FACE = BlockStateProperties.FACING;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);
    public static final EnumProperty<Direction.Axis> AXIS = EnumProperty.create("axis", Direction.Axis.class);
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 15);

    /*
     * Simple selection / hitbox box.
     * Small and centered, because the model is rendered by BER now.
     * Tweak these numbers if you want it a little taller/wider.
     */
    private static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);

    public PodStabilizerBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACE, Direction.UP)
                        .setValue(FACING, Direction.NORTH)
                        .setValue(ROLL, 0)
                        .setValue(AXIS, Direction.Axis.Z)
                        .setValue(STRENGTH, 7)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, ROLL, AXIS, STRENGTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction playerHorizontal = context.getHorizontalDirection().getOpposite();

        Direction face;
        Direction facing;

        if (clickedFace == Direction.UP || clickedFace == Direction.DOWN) {
            face = clickedFace;
            facing = playerHorizontal;
        } else {
            face = clickedFace;
            facing = playerHorizontal;

            /*
             * If placed on a wall and the player's look direction happens to line up
             * into the wall, force a sensible sideways default.
             */
            if (facing.getAxis() == clickedFace.getAxis()) {
                facing = switch (clickedFace) {
                    case NORTH, SOUTH -> Direction.EAST;
                    case EAST, WEST -> Direction.NORTH;
                    default -> Direction.NORTH;
                };
            }
        }

        Direction.Axis physicsAxis = axisFromFacing(facing);

        return defaultBlockState()
                .setValue(FACE, face)
                .setValue(FACING, facing)
                .setValue(ROLL, 0)
                .setValue(AXIS, physicsAxis);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction face = state.getValue(FACE);

        if (face == Direction.UP || face == Direction.DOWN) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        return createTickerHelper(
                blockEntityType,
                PRBlockEntities.POD_STABILIZER.get(),
                PodStabilizerBlockEntity::tick
        );
    }

    /*
     * IMPORTANT:
     * We render this through the block entity renderer only.
     * This avoids duplicate block-model rendering and weird lighting/shadow junk.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
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
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof PodStabilizerBlockEntity stabilizer)) {
            return InteractionResult.CONSUME;
        }

        /*
         * Shift-right-click = pick up
         */
        if (player.isShiftKeyDown()) {
            if (!player.getAbilities().instabuild) {
                popResource(level, pos, new ItemStack(asItem()));
            }
            level.removeBlock(pos, false);
            return InteractionResult.CONSUME;
        }

        /*
         * Normal right click = open GUI
         */
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu((MenuProvider) stabilizer);

            Direction face = state.getValue(FACE);
            Direction facing = state.getValue(FACING);
            int roll = state.getValue(ROLL);
            Direction.Axis axis = state.getValue(AXIS);

            serverPlayer.displayClientMessage(
                    Component.literal("Pod Stabilizer: ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("Face=" + face.getName() + ", ")
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Facing=" + facing.getName() + ", ")
                                    .withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("Roll=" + roll + ", ")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal("Axis=" + axis.getName())
                                    .withStyle(ChatFormatting.GOLD)),
                    false
            );
        }

        return InteractionResult.CONSUME;
    }

    /*
     * Called by your wrench logic:
     * rotate the visible fin by 90 degrees each time.
     */
    public static BlockState cycleRoll(BlockState state) {
        int next = (state.getValue(ROLL) + 1) & 3;
        return state.setValue(ROLL, next);
    }

    /*
     * A sensible default physics axis from the block's visual facing.
     */
    public static Direction.Axis axisFromFacing(Direction facing) {
        return switch (facing.getAxis()) {
            case X -> Direction.Axis.X;
            case Y -> Direction.Axis.Y;
            case Z -> Direction.Axis.Z;
        };
    }
}