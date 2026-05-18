package net.droingo.aerowind.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RigidLinkMountBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(
            4.0D, 4.0D, 4.0D,
            12.0D, 12.0D, 12.0D
    );

    public RigidLinkMountBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion()
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
}