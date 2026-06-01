package net.droingo.aerowind.block;

import net.droingo.aerowind.blockentity.RagdollPartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

public class RagdollPartBlock extends Block implements EntityBlock {
    private final PartShape partShape;

    private static final VoxelShape HEAD_SHAPE = Block.box(
            4.0D, 4.0D, 4.0D,
            12.0D, 12.0D, 12.0D
    );

    private static final VoxelShape TORSO_SHAPE = Block.box(
            4.0D, 2.0D, 6.0D,
            12.0D, 14.0D, 10.0D
    );

    private static final VoxelShape ARM_SHAPE = Block.box(
            6.0D, 2.0D, 6.0D,
            10.0D, 14.0D, 10.0D
    );

    private static final VoxelShape LEG_SHAPE = Block.box(
            6.0D, 2.0D, 6.0D,
            10.0D, 14.0D, 10.0D
    );

    private static final VoxelShape HEAD_COLLISION = Block.box(
            5.0D, 5.0D, 5.0D,
            11.0D, 11.0D, 11.0D
    );

    private static final VoxelShape TORSO_COLLISION = Block.box(
            4.5D, 2.5D, 6.0D,
            11.5D, 13.5D, 10.0D
    );

    private static final VoxelShape ARM_COLLISION = Block.box(
            6.75D, 3.0D, 6.75D,
            9.25D, 13.0D, 9.25D
    );

    private static final VoxelShape LEG_COLLISION = Block.box(
            6.75D, 3.0D, 6.75D,
            9.25D, 13.0D, 9.25D
    );

    public RagdollPartBlock(PartShape partShape) {
        super(
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_GRAY)
                        .strength(1.0F, 6.0F)
                        .sound(SoundType.METAL)
                        .noOcclusion()
        );

        this.partShape = partShape;
    }

    public PartShape getPartShape() {
        return this.partShape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RagdollPartBlockEntity(pos, state, this.partShape);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getVisualShape();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.getPhysicsShape();
    }

    @Override
    public VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return this.getVisualShape();
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RagdollPartBlockEntity ragdollPart) {
            ragdollPart.setPartShape(this.partShape);
        }
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



        return InteractionResult.SUCCESS;
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
        if (!player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }



        return ItemInteractionResult.SUCCESS;
    }

    private VoxelShape getVisualShape() {
        return switch (this.partShape) {
            case HEAD -> HEAD_SHAPE;
            case TORSO -> TORSO_SHAPE;
            case ARM -> ARM_SHAPE;
            case LEG -> LEG_SHAPE;
        };
    }

    private VoxelShape getPhysicsShape() {
        return switch (this.partShape) {
            case HEAD -> HEAD_COLLISION;
            case TORSO -> TORSO_COLLISION;
            case ARM -> ARM_COLLISION;
            case LEG -> LEG_COLLISION;
        };
    }

    public enum PartShape implements StringRepresentable {
        HEAD("head"),
        TORSO("torso"),
        ARM("arm"),
        LEG("leg");

        private final String name;

        PartShape(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static PartShape byName(String name) {
            for (PartShape shape : values()) {
                if (shape.name.equalsIgnoreCase(name)) {
                    return shape;
                }
            }

            return TORSO;
        }
    }
}