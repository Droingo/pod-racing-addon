package net.droingo.podracing.content.binder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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

    public Vec3 socketPosition(Level level) {
        BlockState state = level.getBlockState(pos);

        if (state.hasProperty(BinderMountBlock.FACING)) {
            Direction facing = state.getValue(BinderMountBlock.FACING);
            return EnergyBinderMountGeometry.socketPosition(pos, facing);
        }

        return center();
    }

    public double distanceTo(Level level, EnergyBinderEndpoint other) {
        if (!dimension.equals(other.dimension)) {
            return Double.NaN;
        }

        return socketPosition(level).distanceTo(other.socketPosition(level));
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