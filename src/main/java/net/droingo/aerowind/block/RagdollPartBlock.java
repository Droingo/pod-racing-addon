package net.droingo.aerowind.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

public class RagdollPartBlock extends Block {
    private final PartShape partShape;

    /*
     * Visual / selection shapes.
     * These should match the model size.
     */
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

    /*
     * Physics / collision shapes.
     * These are intentionally smaller than the visual model.
     *
     * This helps connected ragdoll limbs stop fighting each other
     * while still letting the body collide with the ground/world.
     */
    private static final VoxelShape HEAD_COLLISION = Block.box(
            5.0D, 5.0D, 5.0D,
            11.0D, 11.0D, 11.0D
    );

    private static final VoxelShape TORSO_COLLISION = Block.box(
            5.0D, 3.0D, 6.5D,
            11.0D, 13.0D, 9.5D
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
    }
}