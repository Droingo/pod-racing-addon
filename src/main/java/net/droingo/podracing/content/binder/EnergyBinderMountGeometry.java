package net.droingo.podracing.content.binder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class EnergyBinderMountGeometry {
    /*
     * Socket point measured from the side of the mount's own block space.
     *
     * Your mount model sits inside the new placed block space:
     * - facing UP    -> lower part of block space
     * - facing DOWN  -> upper part of block space
     * - facing NORTH -> south side of block space
     * - facing SOUTH -> north side of block space
     * - facing EAST  -> west side of block space
     * - facing WEST  -> east side of block space
     *
     * 4px / 16 = 0.25 blocks, matching the visible connector depth better.
     */
    public static final double SOCKET_FROM_EDGE = 3.0D / 16.0D;

    private EnergyBinderMountGeometry() {
    }

    public static Vec3 socketPosition(BlockPos pos, Direction facing) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;

        return switch (facing) {
            case UP -> new Vec3(x, pos.getY() + SOCKET_FROM_EDGE, z);
            case DOWN -> new Vec3(x, pos.getY() + 1.0D - SOCKET_FROM_EDGE, z);

            case NORTH -> new Vec3(x, y, pos.getZ() + 1.0D - SOCKET_FROM_EDGE);
            case SOUTH -> new Vec3(x, y, pos.getZ() + SOCKET_FROM_EDGE);

            case EAST -> new Vec3(pos.getX() + SOCKET_FROM_EDGE, y, z);
            case WEST -> new Vec3(pos.getX() + 1.0D - SOCKET_FROM_EDGE, y, z);
        };
    }
}