package net.droingo.podracing.content.rolltest;

import com.mojang.serialization.MapCodec;
import net.droingo.podracing.registry.PRBlockEntities;
import net.droingo.podracing.util.PRItemChecks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RollTestThrusterBlock extends BaseEntityBlock {
    public static final MapCodec<RollTestThrusterBlock> CODEC = simpleCodec(RollTestThrusterBlock::new);

    public static final EnumProperty<RollTestThrusterRole> ROLE =
            EnumProperty.create("role", RollTestThrusterRole.class);

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty REVERSED = BooleanProperty.create("reversed");

    private static final VoxelShape SHAPE = Block.box(
            3.0D, 3.0D, 3.0D,
            13.0D, 13.0D, 13.0D
    );

    public RollTestThrusterBlock(Properties properties) {
        super(properties);

        registerDefaultState(stateDefinition.any()
                .setValue(ROLE, RollTestThrusterRole.LEFT_ENGINE)
                .setValue(POWERED, false)
                .setValue(REVERSED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROLE, POWERED, REVERSED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RollTestThrusterBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                PRBlockEntities.ROLL_TEST_THRUSTER.get(),
                RollTestThrusterBlockEntity::tick
        );
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
            pickup(serverLevel, pos, state, player);
            return ItemInteractionResult.CONSUME;
        }

        RollTestThrusterRole nextRole = state.getValue(ROLE).next();
        level.setBlock(pos, state.setValue(ROLE, nextRole), 3);

        player.displayClientMessage(
                Component.literal("Roll Test Thruster role: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(nextRole.displayName())
                                .withStyle(ChatFormatting.YELLOW)),
                true
        );

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

        boolean reversed = !state.getValue(REVERSED);
        level.setBlock(pos, state.setValue(REVERSED, reversed), 3);

        player.displayClientMessage(
                Component.literal("Roll Test Thruster reversed: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(reversed ? "ON" : "OFF")
                                .withStyle(reversed ? ChatFormatting.RED : ChatFormatting.GREEN)),
                true
        );

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof RollTestThrusterBlockEntity thruster) {
            player.displayClientMessage(
                    Component.literal("Signal=" + thruster.getDirectRedstoneSignal()
                            + ", Role=" + state.getValue(ROLE).displayName()
                            + ", Reversed=" + reversed),
                    false
            );
        }

        return InteractionResult.CONSUME;
    }

    private static void pickup(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        ItemStack pickedStack = new ItemStack(state.getBlock().asItem());

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof RollTestThrusterBlockEntity) {
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
}
