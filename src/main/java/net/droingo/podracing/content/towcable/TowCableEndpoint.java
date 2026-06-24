package net.droingo.podracing.content.towcable;

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

public record TowCableEndpoint(ResourceKey<Level> dimension, BlockPos pos) {
    private static final String TAG_DIMENSION = "dimension";
    private static final String TAG_X = "x";
    private static final String TAG_Y = "y";
    private static final String TAG_Z = "z";

    public static TowCableEndpoint from(Level level, BlockPos pos) {
        return new TowCableEndpoint(level.dimension(), pos.immutable());
    }

    public Vec3 logicalCenter() {
        return Vec3.atCenterOf(pos);
    }

    /*
     * Logical socket position.
     *
     * This is the point Sable physics force should be applied at.
     * It may be inside a Sable subplot, not where the player sees it.
     */
    public Vec3 logicalSocketPosition(Level level) {
        BlockState state = level.getBlockState(pos);

        if (state.hasProperty(TowCableAnchorBlock.FACING)) {
            Direction facing = state.getValue(TowCableAnchorBlock.FACING);
            Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
            return logicalCenter().add(normal.scale(0.32D));
        }

        return logicalCenter();
    }

    /*
     * Projected / rendered world socket position.
     *
     * This is the point the cable must use for:
     * - rest length
     * - current distance
     * - visible cable direction
     * - deciding whether the cockpit and engine are close/far
     *
     * This accounts for Sable sublevels/subplots.
     */
    public Vec3 projectedWorldSocketPosition(Level level) {
        Vec3 logicalSocket = logicalSocketPosition(level);

        Vector3d projected = Sable.HELPER.projectOutOfSubLevel(
                level,
                new Vector3d(logicalSocket.x, logicalSocket.y, logicalSocket.z),
                new Vector3d()
        );

        return new Vec3(projected.x, projected.y, projected.z);
    }

    /*
     * Keep old method name for renderer compatibility.
     */
    public Vec3 socketPosition(Level level) {
        return logicalSocketPosition(level);
    }

    /*
     * Keep old method name for renderer compatibility.
     */
    public Vec3 projectedSocketPosition(Level level) {
        return projectedWorldSocketPosition(level);
    }

    /*
     * Critical: this distance is projected world distance, not logical subplot distance.
     */
    public double projectedWorldDistanceTo(Level level, TowCableEndpoint other) {
        if (!dimension.equals(other.dimension)) {
            return Double.NaN;
        }

        Vec3 a = projectedWorldSocketPosition(level);
        Vec3 b = other.projectedWorldSocketPosition(level);

        return a.distanceTo(b);
    }

    public double distanceTo(Level level, TowCableEndpoint other) {
        return projectedWorldDistanceTo(level, other);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_DIMENSION, dimension.location().toString());
        tag.putInt(TAG_X, pos.getX());
        tag.putInt(TAG_Y, pos.getY());
        tag.putInt(TAG_Z, pos.getZ());
        return tag;
    }

    public static TowCableEndpoint load(CompoundTag tag) {
        ResourceLocation dimensionId = ResourceLocation.parse(tag.getString(TAG_DIMENSION));
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);

        BlockPos pos = new BlockPos(
                tag.getInt(TAG_X),
                tag.getInt(TAG_Y),
                tag.getInt(TAG_Z)
        );

        return new TowCableEndpoint(dimension, pos);
    }
}