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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public final class PodStabilizerBlock extends BaseEntityBlock implements BlockSubLevelLiftProvider {
    public static final MapCodec<PodStabilizerBlock> CODEC = simpleCodec(PodStabilizerBlock::new);

    /*
     * MOUNT_FACE is only for visual mounting:
     * UP = floor
     * DOWN = ceiling
     * NORTH/SOUTH/EAST/WEST = wall.
     *
     * Important: do NOT use BlockStateProperties.FACING here,
     * because that property name is already "facing".
     */
    public static final DirectionProperty MOUNT_FACE = DirectionProperty.create("mount_face");

    /*
     * ROLL is visual only.
     * Wrench cycles this 0 -> 1 -> 2 -> 3.
     */
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);

    /*
     * AXIS is the actual native Sable/symmetrical-sail physics axis.
     * The GUI changes this.
     */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    /*
     * Kept for later. The current reliable physics uses fixed sail drag.
     */
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 15);

    public static final int DEFAULT_STRENGTH = 7;

    /*
     * Native symmetrical-sail style physics.
     * 1.75F is roughly one symmetric sail.
     * 7.0F makes this one stabilizer act like several sails.
     */
    private static final float PARALLEL_DRAG = 7.0F;
    private static final float DIRECTIONLESS_DRAG = 0.06888202261F;

    /*
     * Small simple box. The renderer draws the fin; this is just for clicking/collision.
     */
    private static final VoxelShape SHAPE = Block.box(
            3.0D, 0.0D, 3.0D,
            13.0D, 12.0D, 13.0D
    );

    public PodStabilizerBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(MOUNT_FACE, Direction.UP)
                .setValue(ROLL, 0)
                .setValue(AXIS, Direction.Axis.Z)
                .setValue(STRENGTH, DEFAULT_STRENGTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOUNT_FACE, ROLL, AXIS, STRENGTH);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction playerFacing = context.getHorizontalDirection().getOpposite();

        int roll = initialRollForPlacement(clickedFace, playerFacing);
        Direction.Axis axis = defaultPhysicsAxis(clickedFace, roll);

        return defaultBlockState()
                .setValue(MOUNT_FACE, clickedFace)
                .setValue(ROLL, roll)
                .setValue(AXIS, axis)
                .setValue(STRENGTH, DEFAULT_STRENGTH);
    }

    /*
     * Ground/ceiling placement should visually face the player.
     * Renderer uses this roll value around the mounted face.
     */
    private static int initialRollForPlacement(Direction mountFace, Direction playerFacing) {
        if (mountFace == Direction.UP || mountFace == Direction.DOWN) {
            return switch (playerFacing) {
                case EAST -> 1;
                case SOUTH -> 2;
                case WEST -> 3;
                default -> 0;
            };
        }

        /*
         * Wall placement starts upright.
         * Wrench rotates it around the wall.
         */
        return 0;
    }

    /*
     * Just a decent default. The GUI is the real physics-axis control.
     */
    private static Direction.Axis defaultPhysicsAxis(Direction mountFace, int roll) {
        if (mountFace == Direction.UP || mountFace == Direction.DOWN) {
            return (roll & 1) == 0 ? Direction.Axis.Z : Direction.Axis.X;
        }

        return mountFace.getAxis();
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

    /*
     * BER-only rendering.
     * Prevents duplicate model rendering and the dark/shadowy junk.
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
    protected VoxelShape getInteractionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
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
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
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

        /*
         * Wrench only rotates the visible fin.
         * Physics axis is controlled in the GUI.
         */
        rotateVisualRoll(serverLevel, pos, state, player);

        return ItemInteractionResult.CONSUME;
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

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            pickup(serverLevel, pos, state, player);
            return InteractionResult.CONSUME;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof PodStabilizerBlockEntity stabilizer)) {
            return InteractionResult.CONSUME;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(stabilizer);

            Direction mountFace = state.getValue(MOUNT_FACE);
            int roll = state.getValue(ROLL);
            Direction.Axis axis = state.getValue(AXIS);

            serverPlayer.displayClientMessage(
                    Component.literal("Pod Stabilizer: ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("Mount=" + mountFace.getName() + ", ")
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Roll=" + roll + ", ")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal("Axis=" + axis.getName())
                                    .withStyle(ChatFormatting.GOLD)),
                    false
            );
        }

        return InteractionResult.CONSUME;
    }

    private static void rotateVisualRoll(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        int nextRoll = (state.getValue(ROLL) + 1) & 3;

        level.setBlock(pos, state.setValue(ROLL, nextRoll), 3);

        player.displayClientMessage(
                Component.literal("Pod Stabilizer roll: " + nextRoll)
                        .withStyle(ChatFormatting.AQUA),
                true
        );
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
     * Native symmetrical-sail style physics.
     * Keep this path. It is the working one.
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
}