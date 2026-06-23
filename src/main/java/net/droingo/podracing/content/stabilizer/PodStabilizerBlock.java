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
     * AXIS is the real Sable/symmetrical-sail physics axis.
     * Keep this. This is why the physics is now working.
     */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

    /*
     * MOUNT_FACE is visual placement only:
     * UP = floor
     * DOWN = ceiling
     * NORTH/SOUTH/EAST/WEST = wall-mounted
     */
    public static final DirectionProperty MOUNT_FACE = DirectionProperty.create("mount_face");

    /*
     * ROLL is visual rotation around the mounted face.
     * Wrench cycles this 0 -> 1 -> 2 -> 3.
     */
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);

    /*
     * GUI value. Physics still uses fixed native sail drag for now.
     */
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 15);

    public static final int DEFAULT_STRENGTH = 6;

    /*
     * Symmetric sail is about 1.75F.
     * 7.0F means this one clean block behaves like several sails.
     */
    private static final float PARALLEL_DRAG = 7.0F;
    private static final float DIRECTIONLESS_DRAG = 0.06888202261F;

    /*
     * Big simple selection box. This is intentionally not fin-shaped.
     */
    private static final VoxelShape SELECTION_SHAPE = Block.box(
            -4.0D, -4.0D, -4.0D,
            20.0D, 20.0D, 20.0D
    );

    /*
     * Collision kept sane so the block does not become an annoying invisible wall.
     */
    private static final VoxelShape COLLISION_SHAPE = Block.box(
            0.0D, 0.0D, 0.0D,
            16.0D, 16.0D, 16.0D
    );

    public PodStabilizerBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(AXIS, Direction.Axis.X)
                .setValue(MOUNT_FACE, Direction.UP)
                .setValue(ROLL, 0)
                .setValue(STRENGTH, DEFAULT_STRENGTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction mountFace = context.getClickedFace();
        Direction playerFacing = context.getHorizontalDirection().getOpposite();

        int roll = initialRollForPlacement(mountFace, playerFacing);
        Direction.Axis axis = axisForMountAndRoll(mountFace, roll);

        return defaultBlockState()
                .setValue(MOUNT_FACE, mountFace)
                .setValue(ROLL, roll)
                .setValue(AXIS, axis)
                .setValue(STRENGTH, DEFAULT_STRENGTH);
    }

    /*
     * Floor/ceiling placement:
     * make the model face the player by default.
     *
     * Wall placement:
     * model mounts to the wall and starts upright.
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

        return 0;
    }

    /*
     * This keeps the working Sable sail physics matched to the visual orientation.
     *
     * Floor/ceiling:
     * roll 0/2 = X axis
     * roll 1/3 = Z axis
     *
     * Wall:
     * the sail normal is the wall axis, and rolling the visual model around the wall
     * does not change that physics axis.
     */
    private static Direction.Axis axisForMountAndRoll(Direction mountFace, int roll) {
        if (mountFace == Direction.UP || mountFace == Direction.DOWN) {
            return (roll & 1) == 0
                    ? Direction.Axis.X
                    : Direction.Axis.Z;
        }

        return mountFace.getAxis();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, MOUNT_FACE, ROLL, STRENGTH);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return SELECTION_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return COLLISION_SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        /*
         * Model is drawn by PodStabilizerRenderer.
         */
        return RenderShape.INVISIBLE;
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
            pickup(serverLevel, pos, state, player);
        } else {
            rotateWithWrench(serverLevel, pos, state, player);
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
            Direction mountFace = state.getValue(MOUNT_FACE);
            int roll = state.getValue(ROLL);
            int strength = state.getValue(STRENGTH);

            serverPlayer.displayClientMessage(
                    Component.literal("Pod Stabilizer: ")
                            .withStyle(ChatFormatting.AQUA)
                            .append(Component.literal("Mount=" + mountFace.getName() + ", ")
                                    .withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("Axis=" + axis.getName() + ", ")
                                    .withStyle(ChatFormatting.GREEN))
                            .append(Component.literal("Roll=" + roll + ", ")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE))
                            .append(Component.literal("Strength=" + strength + "/15")
                                    .withStyle(ChatFormatting.GRAY)),
                    false
            );

            serverPlayer.openMenu(stabilizer);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.literal("Pod Stabilizer missing block entity."), false);
        return InteractionResult.CONSUME;
    }

    private static void rotateWithWrench(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        Direction mountFace = state.getValue(MOUNT_FACE);

        int currentRoll = state.getValue(ROLL);
        int nextRoll = (currentRoll + 1) & 3;

        Direction.Axis nextAxis = axisForMountAndRoll(mountFace, nextRoll);

        level.setBlock(pos, state
                .setValue(ROLL, nextRoll)
                .setValue(AXIS, nextAxis), 3);

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
     * Native symmetrical-sail-style Sable physics.
     * Do not replace this with manual impulses.
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