package net.droingo.podracing.content.binder;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class EnergyBinderConnection {
    private static final String TAG_ID = "id";
    private static final String TAG_ENDPOINT_A = "endpoint_a";
    private static final String TAG_ENDPOINT_B = "endpoint_b";
    private static final String TAG_TARGET_DISTANCE = "target_distance";
    private static final String TAG_STIFFNESS = "stiffness";
    private static final String TAG_DAMPING = "damping";
    private static final String TAG_COLOR = "color";
    private static final String TAG_ENABLED = "enabled";

    private final UUID id;
    private final EnergyBinderEndpoint endpointA;
    private final EnergyBinderEndpoint endpointB;

    private double targetDistance;
    private double stiffness;
    private double damping;
    private int color;
    private boolean enabled;

    public EnergyBinderConnection(
            UUID id,
            EnergyBinderEndpoint endpointA,
            EnergyBinderEndpoint endpointB,
            double targetDistance,
            double stiffness,
            double damping,
            int color,
            boolean enabled
    ) {
        this.id = id;
        this.endpointA = endpointA;
        this.endpointB = endpointB;
        this.targetDistance = targetDistance;
        this.stiffness = stiffness;
        this.damping = damping;
        this.color = color;
        this.enabled = enabled;
    }

    public static EnergyBinderConnection create(Level level, EnergyBinderEndpoint endpointA, EnergyBinderEndpoint endpointB) {
        double distance = endpointA.distanceTo(level, endpointB);

        if (Double.isNaN(distance)) {
            distance = 0.0D;
        }

        return new EnergyBinderConnection(
                UUID.randomUUID(),
                endpointA,
                endpointB,
                Math.max(0.25D, distance),
                EnergyBinderConstants.DEFAULT_STIFFNESS,
                EnergyBinderConstants.DEFAULT_DAMPING,
                EnergyBinderConstants.DEFAULT_COLOR,
                true
        );
    }

    public UUID id() {
        return id;
    }

    public EnergyBinderEndpoint endpointA() {
        return endpointA;
    }

    public EnergyBinderEndpoint endpointB() {
        return endpointB;
    }

    public double targetDistance() {
        return targetDistance;
    }

    public void setTargetDistance(double targetDistance) {
        this.targetDistance = Math.max(0.25D, targetDistance);
    }

    public double stiffness() {
        return stiffness;
    }

    public void setStiffness(double stiffness) {
        this.stiffness = Math.max(0.0D, stiffness);
    }

    public double damping() {
        return damping;
    }

    public void setDamping(double damping) {
        this.damping = Math.max(0.0D, damping);
    }

    public int color() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean touches(EnergyBinderEndpoint endpoint) {
        return endpointA.equals(endpoint) || endpointB.equals(endpoint);
    }

    public boolean connects(EnergyBinderEndpoint first, EnergyBinderEndpoint second) {
        return (endpointA.equals(first) && endpointB.equals(second))
                || (endpointA.equals(second) && endpointB.equals(first));
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(TAG_ID, id);
        tag.put(TAG_ENDPOINT_A, endpointA.save());
        tag.put(TAG_ENDPOINT_B, endpointB.save());
        tag.putDouble(TAG_TARGET_DISTANCE, targetDistance);
        tag.putDouble(TAG_STIFFNESS, stiffness);
        tag.putDouble(TAG_DAMPING, damping);
        tag.putInt(TAG_COLOR, color);
        tag.putBoolean(TAG_ENABLED, enabled);
        return tag;
    }

    public static EnergyBinderConnection load(CompoundTag tag) {
        return new EnergyBinderConnection(
                tag.getUUID(TAG_ID),
                EnergyBinderEndpoint.load(tag.getCompound(TAG_ENDPOINT_A)),
                EnergyBinderEndpoint.load(tag.getCompound(TAG_ENDPOINT_B)),
                tag.getDouble(TAG_TARGET_DISTANCE),
                tag.getDouble(TAG_STIFFNESS),
                tag.getDouble(TAG_DAMPING),
                tag.getInt(TAG_COLOR),
                !tag.contains(TAG_ENABLED) || tag.getBoolean(TAG_ENABLED)
        );
    }
}