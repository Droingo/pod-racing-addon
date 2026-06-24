package net.droingo.podracing.content.attitudefin;

import net.minecraft.util.StringRepresentable;

public enum AttitudeFinRole implements StringRepresentable {
    LEFT_ENGINE("left_engine", 1),
    RIGHT_ENGINE("right_engine", -1),

    FRONT_CONTROL("front_control", 1),
    REAR_CONTROL("rear_control", -1),

    /*
     * Compatibility aliases for old placed blocks.
     */
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

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public String displayName() {
        return switch (this) {
            case LEFT_ENGINE -> "Roll Left";
            case RIGHT_ENGINE -> "Roll Right";
            case FRONT_CONTROL, PITCH_FRONT, YAW_FRONT -> "Front Control";
            case REAR_CONTROL, PITCH_REAR, YAW_REAR -> "Rear Control";
        };
    }

    public boolean isRollRole() {
        return this == LEFT_ENGINE || this == RIGHT_ENGINE;
    }

    public boolean isFrontControlRole() {
        return this == FRONT_CONTROL || this == PITCH_FRONT || this == YAW_FRONT;
    }

    public boolean isRearControlRole() {
        return this == REAR_CONTROL || this == PITCH_REAR || this == YAW_REAR;
    }

    public boolean isCombinedControlRole() {
        return isFrontControlRole() || isRearControlRole();
    }

    public int guiIndex() {
        return switch (this) {
            case LEFT_ENGINE -> 0;
            case RIGHT_ENGINE -> 1;
            case FRONT_CONTROL, PITCH_FRONT, YAW_FRONT -> 2;
            case REAR_CONTROL, PITCH_REAR, YAW_REAR -> 3;
        };
    }

    public static AttitudeFinRole fromGuiIndex(int index) {
        return switch (index) {
            case 1 -> RIGHT_ENGINE;
            case 2 -> FRONT_CONTROL;
            case 3 -> REAR_CONTROL;
            default -> LEFT_ENGINE;
        };
    }
}