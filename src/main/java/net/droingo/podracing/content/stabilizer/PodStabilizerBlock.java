package net.droingo.podracing.content.stabilizer;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider.LiftProviderContext;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider.LiftProviderGroup;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.droingo.podracing.registry.PRBlockEntities;
import net.droingo.podracing.util.PRItemChecks;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class PodStabilizerBlock extends BaseEntityBlock implements BlockSubLevelLiftProvider {
    public static final MapCodec<PodStabilizerBlock> CODEC = simpleCodec(PodStabilizerBlock::new);

    public static final DirectionProperty MOUNT_FACE = DirectionProperty.create("mount_face");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty STRENGTH = IntegerProperty.create("strength", 0, 15);

    public static final int DEFAULT_STRENGTH = 6;

    private static final float MAX_PARALLEL_DRAG = 7.0F;
    private static final float MAX_DIRECTIONLESS_DRAG = 0.12F;

    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(3.0D, 2.0D, 0.0D, 13.0D, 14.0D, 16.0D);
    private static final VoxelShape SHAPE_EAST_WEST = Block.box(0.0D, 2.0D, 3.0D, 16.0D, 14.0D, 13.0D);

    public PodStabilizerBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(MOUNT_FACE, Direction.UP)
                .setValue(FACING, Direction.NORTH)
                .setValue(STRENGTH, DEFAULT_STRENGTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction mountFace = context.getClickedFace();
        Direction facing = defaultFacingForMount(
                mountFace,
                context.getHorizontalDirection().getOpposite()
        );

        return defaultBlockState()
                .setValue(MOUNT_FACE, mountFace)
                .setValue(FACING, facing)
                .setValue(STRENGTH, DEFAULT_STRENGTH);
    }

    private static Direction defaultFacingForMount(Direction mountFace, Direction playerFacing) {
        if (mountFace == Direction.UP || mountFace == Direction.DOWN) {
            return playerFacing;
        }

        if (mountFace == Direction.NORTH || mountFace == Direction.SOUTH) {
            return playerFacing == Direction.WEST ? Direction.WEST : Direction.EAST;
        }

        return playerFacing == Direction.SOUTH ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOUNT_FACE, FACING, STRENGTH);
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
        return facing == Direction.EAST || facing == Direction.WEST
                ? SHAPE_EAST_WEST
                : SHAPE_NORTH_SOUTH;
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
        if (PRItemChecks.isCreateWrench(stack)) {
            if (level.isClientSide()) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!(level instanceof ServerLevel serverLevel)) {
                return ItemInteractionResult.CONSUME;
            }

            if (player.isShiftKeyDown()) {
                pickup(serverLevel, pos, state, player);
            } else {
                rotateFacing(serverLevel, pos, state);
            }

            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
            serverPlayer.openMenu(stabilizer);
            return InteractionResult.CONSUME;
        }

        player.displayClientMessage(Component.literal("Pod Stabilizer missing block entity."), false);
        return InteractionResult.CONSUME;
    }

    private static void rotateFacing(ServerLevel level, BlockPos pos, BlockState state) {
        Direction mountFace = state.getValue(MOUNT_FACE);
        Direction currentFacing = state.getValue(FACING);
        Direction nextFacing = nextFacingForMount(mountFace, currentFacing);

        level.setBlock(pos, state.setValue(FACING, nextFacing), 3);
    }

    private static Direction nextFacingForMount(Direction mountFace, Direction currentFacing) {
        return switch (mountFace) {
            case UP, DOWN -> currentFacing.getClockWise();

            case NORTH, SOUTH -> currentFacing == Direction.EAST
                    ? Direction.WEST
                    : Direction.EAST;

            case EAST, WEST -> currentFacing == Direction.NORTH
                    ? Direction.SOUTH
                    : Direction.NORTH;
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

    @Override
    public @NotNull Direction sable$getNormal(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public float sable$getLiftScalar() {
        return 0.0F;
    }

    @Override
    public float sable$getParallelDragScalar() {
        /*
         * Non-zero so Sable treats this as an active drag provider.
         * Actual strength scaling still happens inside sable$contributeLiftAndDrag(...).
         */
        return MAX_PARALLEL_DRAG;
    }

    @Override
    public float sable$getDirectionlessDragScalar() {
        /*
         * Non-zero for the same reason. The custom contribution method still scales
         * by the GUI strength value.
         */
        return MAX_DIRECTIONLESS_DRAG;
    }

    @Override
    public void sable$contributeLiftAndDrag(
            final LiftProviderContext ctx,
            final ServerSubLevel subLevel,
            @NotNull final Pose3d localPose,
            final double timeStep,
            final Vector3dc linearVelocity,
            final Vector3dc angularVelocity,
            final Vector3d linearImpulse,
            final Vector3d angularImpulse,
            @Nullable final LiftProviderGroup group
    ) {
        BlockState state = ctx.state();

        if (!state.hasProperty(STRENGTH) || !state.hasProperty(FACING)) {
            return;
        }

        int strength = state.getValue(STRENGTH);

        if (strength <= 0) {
            return;
        }

        double strength01 = strength / 15.0D;

        double parallelDragScalar = MAX_PARALLEL_DRAG * strength01;
        double directionlessDragScalar = MAX_DIRECTIONLESS_DRAG * strength01;

        Direction facing = state.getValue(FACING);

        Vector3d normal = new Vector3d(
                facing.getStepX(),
                facing.getStepY(),
                facing.getStepZ()
        );

        Vector3d liftPos = new Vector3d(
                ctx.pos().getX() + 0.5D,
                ctx.pos().getY() + 0.5D,
                ctx.pos().getZ() + 0.5D
        );

        if (localPose != null) {
            localPose.transformNormal(normal);
            localPose.transformPosition(liftPos);
        }

        if (normal.lengthSquared() < 0.000001D) {
            return;
        }

        normal.normalize();

        Pose3d pose = subLevel.logicalPose();

        Vector3d temp = new Vector3d();
        Vector3d velocity = new Vector3d();
        Vector3d drag = new Vector3d();
        Vector3d force = new Vector3d();

        double pressure = DimensionPhysicsData.getAirPressure(
                subLevel.getLevel(),
                pose.transformPosition(liftPos, temp)
        );

        pose.transformPosition(liftPos, temp).sub(pose.position());

        velocity.set(linearVelocity).add(angularVelocity.cross(temp, temp));
        pose.transformNormalInverse(velocity);

        if (!isFiniteVector(velocity)) {
            return;
        }

        if (parallelDragScalar > 0.0D) {
            double dragStrength = normal.dot(velocity) * parallelDragScalar * pressure * timeStep;

            Vector3d parallelDrag = normal.mul(dragStrength, drag);
            force.add(parallelDrag);

            if (group != null) {
                group.totalDrag().sub(parallelDrag);
                group.dragCenter().fma(Math.abs(dragStrength), liftPos);
                group.totalDragStrength += Math.abs(dragStrength);
            }
        }

        if (directionlessDragScalar > 0.0D) {
            double dragStrength = directionlessDragScalar * pressure * timeStep;

            Vector3d directionlessDrag = velocity.mul(dragStrength, temp);
            force.add(directionlessDrag);

            if (group != null) {
                group.totalDrag().sub(directionlessDrag);
                group.dragCenter().fma(directionlessDrag.length(), liftPos);
                group.totalDragStrength += directionlessDrag.length();
            }
        }

        linearImpulse.sub(force);

        liftPos.sub(subLevel.getMassTracker().getCenterOfMass(), temp);
        angularImpulse.sub(temp.cross(force));
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
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