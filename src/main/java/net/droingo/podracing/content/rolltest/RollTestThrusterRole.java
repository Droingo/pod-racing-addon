package net.droingo.podracing.content.rolltest;

import net.minecraft.util.StringRepresentable;

public enum RollTestThrusterRole implements StringRepresentable {
    LEFT_ENGINE("left_engine", 1),
    RIGHT_ENGINE("right_engine", -1),

    PITCH_FRONT("pitch_front", 1),
    PITCH_REAR("pitch_rear", -1),

    /*
     * Proper yaw prototype:
     * Put YAW_FRONT near the front of each engine.
     * Put YAW_REAR near the rear of each engine.
     *
     * These push sideways, not forward/back.
     */
    YAW_FRONT("yaw_front", 1),
    YAW_REAR("yaw_rear", -1),

    /*
     * Old saved prototype names.
     * Kept so existing worlds do not choke if these states already exist.
     * They now behave like yaw-front/yaw-rear aliases.
     */
    YAW_LEFT_ENGINE("yaw_left_engine", 1),
    YAW_RIGHT_ENGINE("yaw_right_engine", -1);

    private final String serializedName;
    private final int controlSign;

    RollTestThrusterRole(String serializedName, int controlSign) {
        this.serializedName = serializedName;
        this.controlSign = controlSign;
    }

    public int verticalSign() {
        return controlSign;
    }

    public int controlSign() {
        return controlSign;
    }

    public RollTestThrusterRole next() {
        return switch (this) {
            case LEFT_ENGINE -> RIGHT_ENGINE;
            case RIGHT_ENGINE -> PITCH_FRONT;
            case PITCH_FRONT -> PITCH_REAR;
            case PITCH_REAR -> YAW_FRONT;
            case YAW_FRONT -> YAW_REAR;
            case YAW_REAR -> LEFT_ENGINE;

            /*
             * If you had old yaw-left/right blocks placed, wrenching them moves
             * back into the current clean cycle.
             */
            case YAW_LEFT_ENGINE -> YAW_FRONT;
            case YAW_RIGHT_ENGINE -> YAW_REAR;
        };
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
            case YAW_LEFT_ENGINE -> "Yaw Front";
            case YAW_RIGHT_ENGINE -> "Yaw Rear";
        };
    }

    public boolean isRollRole() {
        return this == LEFT_ENGINE || this == RIGHT_ENGINE;
    }

    public boolean isPitchRole() {
        return this == PITCH_FRONT || this == PITCH_REAR;
    }

    public boolean isYawRole() {
        return this == YAW_FRONT
                || this == YAW_REAR
                || this == YAW_LEFT_ENGINE
                || this == YAW_RIGHT_ENGINE;
    }
}