package net.droingo.aerowind.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class TrophyBlock extends Block {
    private static final DustParticleOptions GOLD_SPARKLE =
            new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.12F), 0.85F);

    public TrophyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(7) != 0) {
            return;
        }

        double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
        double y = pos.getY() + 0.2D + random.nextDouble() * 0.9D;
        double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;

        double xSpeed = (random.nextDouble() - 0.5D) * 0.01D;
        double ySpeed = 0.01D + random.nextDouble() * 0.02D;
        double zSpeed = (random.nextDouble() - 0.5D) * 0.01D;

        level.addParticle(GOLD_SPARKLE, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}