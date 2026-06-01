package net.droingo.aerowind.wind;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class WindField {
    private WindField() {
    }

    public static WindSample sampleServer(ServerLevel level, Vec3 worldPosition) {
        return WindModel.sample(level, worldPosition);
    }

    public static WindSample sampleClientVisual(Level level, Vec3 worldPosition) {
        long day = level.getDayTime() / 24000L;
        long timeOfDay = level.getDayTime() % 24000L;

        double daySeed = hashToUnit(day * 31L);
        double baseAngle = daySeed * Math.TAU;
        double dayRotation = (timeOfDay / 24000.0D) * Math.TAU * 0.35D;

        double height = worldPosition.y;
        double heightLayer = Math.floor(height / 64.0D);
        double heightAngleOffset = heightLayer * 0.35D;

        double gust = visualGust(level.getGameTime(), daySeed, height);

        double heightMultiplier = Mth.clamp(0.6D + (height / 192.0D), 0.5D, 2.2D);
        double daySpeedWave = 0.75D + (Math.sin((timeOfDay / 24000.0D) * Math.TAU - 1.2D) + 1.0D) * 0.25D;

        double angle = baseAngle + dayRotation + heightAngleOffset + gust * 0.25D;

        Vec3 direction = new Vec3(
                Math.cos(angle),
                0.03D,
                Math.sin(angle)
        ).normalize();

        double speed = 1.0D * heightMultiplier * daySpeedWave * (1.0D + gust);

        return new WindSample(direction, speed);
    }

    public static Vector3d impulse(WindSample sample) {
        Vec3 direction = sample.direction();

        return new Vector3d(
                direction.x * sample.speed(),
                direction.y * sample.speed(),
                direction.z * sample.speed()
        );
    }

    public static Vector3d estimateUpperDragCenter(ServerSubLevel subLevel) {
        var bounds = subLevel.getPlot().getBoundingBox();

        double minX = bounds.minX();
        double minY = bounds.minY();
        double minZ = bounds.minZ();

        double maxX = bounds.maxX() + 1.0D;
        double maxY = bounds.maxY() + 1.0D;
        double maxZ = bounds.maxZ() + 1.0D;

        double centerX = (minX + maxX) * 0.5D;
        double centerZ = (minZ + maxZ) * 0.5D;

        /*
         * First balloon-friendly approximation:
         * push high in the build instead of at the total sublevel center.
         *
         * For hot air balloons this should put the force closer to the balloon
         * envelope instead of the basket.
         */
        double dragY = Mth.lerp(0.75D, minY, maxY);

        return new Vector3d(centerX, dragY, centerZ);
    }

    public static Vector3d projectorCenter(BlockPos pos) {
        return new Vector3d(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }

    private static double visualGust(long gameTime, double daySeed, double height) {
        double slow = Math.sin((gameTime * 0.006D) + daySeed * 40.0D + height * 0.03D);
        double fast = Math.sin((gameTime * 0.031D) + daySeed * 91.0D);
        double combined = (slow * 0.7D) + (fast * 0.3D);
        return Math.max(0.0D, combined) * 0.45D;
    }

    private static double hashToUnit(long value) {
        long x = value;
        x ^= x >>> 33;
        x *= 0xff51afd7ed558ccdL;
        x ^= x >>> 33;
        x *= 0xc4ceb9fe1a85ec53L;
        x ^= x >>> 33;
        return (x & 0xFFFFFF) / (double) 0xFFFFFF;
    }
}