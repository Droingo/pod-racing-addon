package net.droingo.podracing.content.binder;

import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public record EnergyBinderEndpoint(ResourceKey<Level> dimension, BlockPos pos) {
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";

    public static EnergyBinderEndpoint from(Level level, BlockPos pos) {
        return new EnergyBinderEndpoint(level.dimension(), pos.immutable());
    }

    public Vec3 center() {
        return Vec3.atCenterOf(pos);
    }

    /**
     * Raw socket position in the block's own level coordinate space.
     *
     * For normal world blocks, this is world space.
     * For Sable sublevel blocks, this may still be sublevel/plot space.
     */
    public Vec3 socketPosition(Level level) {
        BlockState state = level.getBlockState(pos);

        if (state.hasProperty(BinderMountBlock.FACING)) {
            Direction facing = state.getValue(BinderMountBlock.FACING);
            return EnergyBinderMountGeometry.socketPosition(pos, facing);
        }

        return center();
    }

    /**
     * Real world-space socket position.
     *
     * Use this for rendering and visual distance checks across Sable sublevels.
     */
    public Vec3 projectedSocketPosition(Level level) {
        Vec3 socket = socketPosition(level);

        Vector3d projected = Sable.HELPER.projectOutOfSubLevel(
                level,
                new Vector3d(socket.x, socket.y, socket.z),
                new Vector3d()
        );

        return new Vec3(projected.x, projected.y, projected.z);
    }

    public Direction socketFacing(Level level) {
        BlockState state = level.getBlockState(pos);

        if (state.hasProperty(BinderMountBlock.FACING)) {
            return state.getValue(BinderMountBlock.FACING);
        }

        return Direction.UP;
    }

    /**
     * Direction the Binder Mount wants the spring to leave from.
     *
     * This gives the physics constraint a preferred local axis, so the mount
     * wants to face the other endpoint without becoming a hard weld.
     */
    public Vec3 socketNormal(Level level) {
        Direction facing = socketFacing(level);
        return Vec3.atLowerCornerOf(facing.getNormal()).normalize();
    }

    public double distanceTo(Level level, EnergyBinderEndpoint other) {
        if (!dimension.equals(other.dimension)) {
            return Double.NaN;
        }

        Vec3 a = socketPosition(level);
        Vec3 b = other.socketPosition(level);

        double distanceSquared = Sable.HELPER.distanceSquaredWithSubLevels(
                level,
                new Vector3d(a.x, a.y, a.z),
                new Vector3d(b.x, b.y, b.z)
        );

        return Math.sqrt(distanceSquared);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DIMENSION, dimension.location().toString());
        tag.putInt(TAG_X, pos.getX());
        tag.putInt(TAG_Y, pos.getY());
        tag.putInt(TAG_Z, pos.getZ());
        return tag;
    }

    public static EnergyBinderEndpoint load(CompoundTag tag) {
        ResourceLocation dimensionId = ResourceLocation.parse(tag.getString(TAG_DIMENSION));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);

        BlockPos pos = new BlockPos(
                tag.getInt(TAG_X),
                tag.getInt(TAG_Y),
                tag.getInt(TAG_Z)
        );

        return new EnergyBinderEndpoint(dimension, pos);
    }
}