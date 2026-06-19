package net.droingo.podracing.content.airbrake;

import com.mojang.serialization.MapCodec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.droingo.podracing.registry.PRBlockEntities;
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
import net.minecraft.world.item.DyeItem;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class AirBrakeBlock extends BaseEntityBlock {
    public static final MapCodec<AirBrakeBlock> CODEC = simpleCodec(AirBrakeBlock::new);

    public static final DirectionProperty MOUNT_FACE = DirectionProperty.create("mount_face");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape TOP_BASE_NORTH = Shapes.or(
            Block.box(1.0D, 0.0D, -7.0D, 15.0D, 3.0D, 16.0D),
            Block.box(0.0D, 0.0D, 11.0D, 16.0D, 6.0D, 16.0D)
    );

    private static final VoxelShape TOP_CLOSED_NORTH = Shapes.or(
            TOP_BASE_NORTH,
            Block.box(1.0D, 3.0D, -7.0D, 15.0D, 6.5D, 13.0D)
    );

    private static final VoxelShape TOP_OPEN_NORTH = Shapes.or(
            TOP_BASE_NORTH,
            Block.box(1.0D, 3.0D, 8.0D, 15.0D, 18.0D, 14.0D),
            Block.box(1.0D, 13.0D, -3.0D, 15.0D, 18.0D, 10.0D)
    );

    private static final VoxelShape TOP_BASE_SOUTH = Shapes.or(
            Block.box(1.0D, 0.0D, 0.0D, 15.0D, 3.0D, 23.0D),
            Block.box(0.0D, 0.0D, 0.0D, 16.0D, 6.0D, 5.0D)
    );

    private static final VoxelShape TOP_CLOSED_SOUTH = Shapes.or(
            TOP_BASE_SOUTH,
            Block.box(1.0D, 3.0D, 3.0D, 15.0D, 6.5D, 23.0D)
    );

    private static final VoxelShape TOP_OPEN_SOUTH = Shapes.or(
            TOP_BASE_SOUTH,
            Block.box(1.0D, 3.0D, 2.0D, 15.0D, 18.0D, 8.0D),
            Block.box(1.0D, 13.0D, 6.0D, 15.0D, 18.0D, 19.0D)
    );

    private static final VoxelShape TOP_BASE_EAST = Shapes.or(
            Block.box(0.0D, 0.0D, 1.0D, 23.0D, 3.0D, 15.0D),
            Block.box(0.0D, 0.0D, 0.0D, 5.0D, 6.0D, 16.0D)
    );

    private static final VoxelShape TOP_CLOSED_EAST = Shapes.or(
            TOP_BASE_EAST,
            Block.box(3.0D, 3.0D, 1.0D, 23.0D, 6.5D, 15.0D)
    );

    private static final VoxelShape TOP_OPEN_EAST = Shapes.or(
            TOP_BASE_EAST,
            Block.box(2.0D, 3.0D, 1.0D, 8.0D, 18.0D, 15.0D),
            Block.box(6.0D, 13.0D, 1.0D, 19.0D, 18.0D, 15.0D)
    );

    private static final VoxelShape TOP_BASE_WEST = Shapes.or(
            Block.box(-7.0D, 0.0D, 1.0D, 16.0D, 3.0D, 15.0D),
            Block.box(11.0D, 0.0D, 0.0D, 16.0D, 6.0D, 16.0D)
    );

    private static final VoxelShape TOP_CLOSED_WEST = Shapes.or(
            TOP_BASE_WEST,
            Block.box(-7.0D, 3.0D, 1.0D, 13.0D, 6.5D, 15.0D)
    );

    private static final VoxelShape TOP_OPEN_WEST = Shapes.or(
            TOP_BASE_WEST,
            Block.box(8.0D, 3.0D, 1.0D, 14.0D, 18.0D, 15.0D),
            Block.box(-3.0D, 13.0D, 1.0D, 10.0D, 18.0D, 15.0D)
    );

    private static final VoxelShape BOTTOM_CLOSED_NORTH = mirrorY(TOP_CLOSED_NORTH);
    private static final VoxelShape BOTTOM_CLOSED_SOUTH = mirrorY(TOP_CLOSED_SOUTH);
    private static final VoxelShape BOTTOM_CLOSED_EAST = mirrorY(TOP_CLOSED_EAST);
    private static final VoxelShape BOTTOM_CLOSED_WEST = mirrorY(TOP_CLOSED_WEST);

    private static final VoxelShape BOTTOM_OPEN_NORTH = mirrorY(TOP_OPEN_NORTH);
    private static final VoxelShape BOTTOM_OPEN_SOUTH = mirrorY(TOP_OPEN_SOUTH);
    private static final VoxelShape BOTTOM_OPEN_EAST = mirrorY(TOP_OPEN_EAST);
    private static final VoxelShape BOTTOM_OPEN_WEST = mirrorY(TOP_OPEN_WEST);

    private static final VoxelShape WALL_NORTH_CLOSED = Shapes.or(
            Block.box(1.0D, 2.0D, 8.0D, 15.0D, 14.0D, 16.0D),
            Block.box(-7.0D, 3.0D, 10.0D, 23.0D, 13.0D, 15.5D)
    );

    private static final VoxelShape WALL_NORTH_OPEN = Shapes.or(
            WALL_NORTH_CLOSED,
            Block.box(-7.0D, 8.0D, -2.0D, 23.0D, 16.0D, 15.5D)
    );

    private static final VoxelShape WALL_SOUTH_CLOSED = Shapes.or(
            Block.box(1.0D, 2.0D, 0.0D, 15.0D, 14.0D, 8.0D),
            Block.box(-7.0D, 3.0D, 0.5D, 23.0D, 13.0D, 6.0D)
    );

    private static final VoxelShape WALL_SOUTH_OPEN = Shapes.or(
            WALL_SOUTH_CLOSED,
            Block.box(-7.0D, 8.0D, 0.5D, 23.0D, 16.0D, 18.0D)
    );

    private static final VoxelShape WALL_EAST_CLOSED = Shapes.or(
            Block.box(0.0D, 2.0D, 1.0D, 8.0D, 14.0D, 15.0D),
            Block.box(0.5D, 3.0D, -7.0D, 6.0D, 13.0D, 23.0D)
    );

    private static final VoxelShape WALL_EAST_OPEN = Shapes.or(
            WALL_EAST_CLOSED,
            Block.box(0.5D, 8.0D, -7.0D, 18.0D, 16.0D, 23.0D)
    );

    private static final VoxelShape WALL_WEST_CLOSED = Shapes.or(
            Block.box(8.0D, 2.0D, 1.0D, 16.0D, 14.0D, 15.0D),
            Block.box(10.0D, 3.0D, -7.0D, 15.5D, 13.0D, 23.0D)
    );

    private static final VoxelShape WALL_WEST_OPEN = Shapes.or(
            WALL_WEST_CLOSED,
            Block.box(-2.0D, 8.0D, -7.0D, 15.5D, 16.0D, 23.0D)
    );

    private static VoxelShape mirrorY(VoxelShape shape) {
        VoxelShape[] result = new VoxelShape[]{Shapes.empty()};

        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> result[0] = Shapes.or(
                result[0],
                Block.box(
                        minX * 16.0D,
                        (1.0D - maxY) * 16.0D,
                        minZ * 16.0D,
                        maxX * 16.0D,
                        (1.0D - minY) * 16.0D,
                        maxZ * 16.0D
                )
        ));

        return result[0];
    }

    public AirBrakeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(MOUNT_FACE, Direction.UP)
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction mountFace = context.getClickedFace();
        Direction facing = defaultFacingForMount(mountFace, context.getHorizontalDirection().getOpposite());

        return defaultBlockState()
                .setValue(MOUNT_FACE, mountFace)
                .setValue(FACING, facing)
                .setValue(POWERED, false);
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
        builder.add(MOUNT_FACE, FACING, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeForState(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState unpoweredState = state.hasProperty(POWERED)
                ? state.setValue(POWERED, false)
                : state;

        return shapeForState(unpoweredState);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return shapeForState(state);
    }

    private static VoxelShape shapeForState(BlockState state) {
        Direction mountFace = state.getValue(MOUNT_FACE);
        Direction facing = state.getValue(FACING);
        boolean powered = state.hasProperty(POWERED) && state.getValue(POWERED);

        if (mountFace == Direction.UP) {
            return switch (facing) {
                case SOUTH -> powered ? TOP_OPEN_SOUTH : TOP_CLOSED_SOUTH;
                case EAST -> powered ? TOP_OPEN_EAST : TOP_CLOSED_EAST;
                case WEST -> powered ? TOP_OPEN_WEST : TOP_CLOSED_WEST;
                default -> powered ? TOP_OPEN_NORTH : TOP_CLOSED_NORTH;
            };
        }

        if (mountFace == Direction.DOWN) {
            return switch (facing) {
                case SOUTH -> powered ? BOTTOM_OPEN_SOUTH : BOTTOM_CLOSED_SOUTH;
                case EAST -> powered ? BOTTOM_OPEN_EAST : BOTTOM_CLOSED_EAST;
                case WEST -> powered ? BOTTOM_OPEN_WEST : BOTTOM_CLOSED_WEST;
                default -> powered ? BOTTOM_OPEN_NORTH : BOTTOM_CLOSED_NORTH;
            };
        }

        return switch (mountFace) {
            case SOUTH -> powered ? WALL_SOUTH_OPEN : WALL_SOUTH_CLOSED;
            case EAST -> powered ? WALL_EAST_OPEN : WALL_EAST_CLOSED;
            case WEST -> powered ? WALL_WEST_OPEN : WALL_WEST_CLOSED;
            default -> powered ? WALL_NORTH_OPEN : WALL_NORTH_CLOSED;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (PRItemChecks.isCreateWrench(stack)) {
            if (level.isClientSide()) {
                return ItemInteractionResult.SUCCESS;
            }

            if (!(level instanceof ServerLevel serverLevel)) {
                return ItemInteractionResult.CONSUME;
            }

            if (player.isShiftKeyDown()) {
                pickupWithWrench(serverLevel, pos, state, player);
            } else {
                rotateWithWrench(serverLevel, pos, state);
            }

            return ItemInteractionResult.CONSUME;
        }

        if (!(stack.getItem() instanceof DyeItem dyeItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof AirBrakeBlockEntity airBrake)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            airBrake.setFlapColor(dyeItem.getDyeColor());

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void rotateWithWrench(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(MOUNT_FACE) || !state.hasProperty(FACING)) {
            return;
        }

        Direction mountFace = state.getValue(MOUNT_FACE);
        Direction currentFacing = state.getValue(FACING);
        Direction nextFacing = nextFacingForMount(mountFace, currentFacing);

        level.setBlock(pos, state.setValue(FACING, nextFacing), 3);
    }

    private static Direction nextFacingForMount(Direction mountFace, Direction currentFacing) {
        return switch (mountFace) {
            case UP, DOWN -> currentFacing.getClockWise();
            case NORTH, SOUTH -> currentFacing == Direction.EAST ? Direction.WEST : Direction.EAST;
            case EAST, WEST -> currentFacing == Direction.NORTH ? Direction.SOUTH : Direction.NORTH;
        };
    }

    private static void pickupWithWrench(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        ItemStack pickedStack = new ItemStack(state.getBlock().asItem());

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof AirBrakeBlockEntity) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
            if (blockEntity instanceof AirBrakeBlockEntity airBrake) {
                serverPlayer.openMenu(airBrake);
                return InteractionResult.CONSUME;
            }
        }

        boolean hasCorrectBlockEntity = blockEntity instanceof AirBrakeBlockEntity;
        boolean powered = state.getValue(POWERED);
        Direction mountFace = state.getValue(MOUNT_FACE);
        Direction facing = state.getValue(FACING);

        String color = "unknown";
        int wirelessPower = 0;
        boolean directPower = false;

        if (blockEntity instanceof AirBrakeBlockEntity airBrake) {
            color = airBrake.getFlapColor().getName();
            wirelessPower = airBrake.getReceivedWirelessSignal();
            directPower = airBrake.isDirectlyPowered();
        }

        SubLevel subLevel = Sable.HELPER.getContaining(serverLevel, pos);
        boolean isServerSubLevel = subLevel instanceof ServerSubLevel;

        serverPlayer.displayClientMessage(
                Component.literal("Air Brake Debug: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("BE=" + hasCorrectBlockEntity + ", ").withStyle(hasCorrectBlockEntity ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(Component.literal("SubLevel=" + isServerSubLevel + ", ").withStyle(isServerSubLevel ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(Component.literal("Powered=" + powered + ", ").withStyle(powered ? ChatFormatting.GREEN : ChatFormatting.RED))
                        .append(Component.literal("Direct=" + directPower + ", ").withStyle(directPower ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                        .append(Component.literal("Wireless=" + wirelessPower + ", ").withStyle(wirelessPower > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY))
                        .append(Component.literal("Mount=" + mountFace.getName() + ", ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("Facing=" + facing.getName() + ", ").withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("Color=" + color).withStyle(ChatFormatting.LIGHT_PURPLE)),
                false
        );

        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirBrakeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, PRBlockEntities.AIR_BRAKE.get(), AirBrakeBlockEntity::tick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }
}