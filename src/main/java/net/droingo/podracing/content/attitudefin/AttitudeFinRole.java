package net.droingo.podracing.content.attitudefin;

import net.minecraft.util.StringRepresentable;

public enum AttitudeFinRole implements StringRepresentable {
    LEFT_ENGINE("left_engine", 1),
    RIGHT_ENGINE("right_engine", -1),

    PITCH_FRONT("pitch_front", 1),
    PITCH_REAR("pitch_rear", -1),

    YAW_FRONT("yaw_front", 1),
    YAW_REAR("yaw_rear", -1);

    private final String serializedName;
    private final int controlSign;

    AttitudeFinRole(String serializedName, int controlSign) {
        this.serializedName = serializedName;
        this.controlSign = controlSign;
    }

    public int controlSign() {
        return controlSign;
    }

    public AttitudeFinRole next() {
        AttitudeFinRole[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String displayName() {
        return switch (this) {
            case LEFT_ENGINE -> "Left Engine";
            case RIGHT_ENGINE -> "Right Engine";
            case PITCH_FRONT -> "Pitch Front";
            case PITCH_REAR -> "Pitch Rear";
            case YAW_FRONT -> "Yaw Front";
            case YAW_REAR -> "Yaw Rear";
        };
    }

    public boolean isRollRole() {
        return this == LEFT_ENGINE || this == RIGHT_ENGINE;
    }

    public boolean isPitchRole() {
        return this == PITCH_FRONT || this == PITCH_REAR;
    }

    public boolean isYawRole() {
        return this == YAW_FRONT || this == YAW_REAR;
    }
}