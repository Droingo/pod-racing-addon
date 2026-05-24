package net.droingo.aerowind.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class TrophyBlock extends Block {
    private static final DustParticleOptions WHITE_SPARKLE =
            new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 0.85F);

    /*
     * Matches the Blockbench model better than the default full cube.
     * Your model is taller than one normal block, so this selection box is also taller.
     */
    private static final VoxelShape SHAPE = Block.box(
            -1.0D, 0.0D, 2.0D,
            17.0D, 21.0D, 14.0D
    );

    /*
     * Smaller collision so players don't get blocked by the trophy handles/top.
     */
    private static final VoxelShape COLLISION_SHAPE = Block.box(
            2.0D, 0.0D, 2.0D,
            14.0D, 10.0D, 14.0D
    );

    public TrophyBlock(Properties properties) {
        super(properties);
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
        return COLLISION_SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }

        double x = pos.getX() + 0.15D + random.nextDouble() * 0.7D;
        double y = pos.getY() + 0.35D + random.nextDouble() * 1.0D;
        double z = pos.getZ() + 0.15D + random.nextDouble() * 0.7D;

        double xSpeed = (random.nextDouble() - 0.5D) * 0.015D;
        double ySpeed = 0.015D + random.nextDouble() * 0.025D;
        double zSpeed = (random.nextDouble() - 0.5D) * 0.015D;

        level.addParticle(WHITE_SPARKLE, x, y, z, xSpeed, ySpeed, zSpeed);

        if (random.nextInt(3) == 0) {
            level.addParticle(
                    WHITE_SPARKLE,
                    pos.getX() + 0.5D,
                    pos.getY() + 1.15D,
                    pos.getZ() + 0.5D,
                    (random.nextDouble() - 0.5D) * 0.02D,
                    0.025D,
                    (random.nextDouble() - 0.5D) * 0.02D
            );
        }
    }
}