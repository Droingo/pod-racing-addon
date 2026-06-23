package net.droingo.podracing.content.rolltest;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.droingo.podracing.content.pilot.PodPilotInputState;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

public final class RollTestThrusterBlockEntity extends BlockEntity implements BlockEntitySubLevelActor {
    private static final double TARGET_SPEED_AT_FULL_SIGNAL = 3.50D;
    private static final double MAX_DELTA_V_PER_PHYSICS_TICK = 0.45D;
    private static final double MAX_IMPULSE_AT_FULL_SIGNAL = 2500.0D;
    private static final double MIN_IMPULSE = 0.00001D;

    private int directRedstoneSignal = 0;

    private final ForceTotal forceTotal = new ForceTotal();

    private final Vector3d localAnchor = new Vector3d();

    /*
     * Body-local axes for this prototype:
     *
     * Y = up/down for roll and pitch.
     * Z = side-to-side for yaw.
     *
     * We tested X and it moved the pod forward/back, so X is not side-to-side
     * on your current engine/sublevel orientation.
     */
    private final Vector3d localBodyUp = new Vector3d(0.0D, 1.0D, 0.0D);
    private final Vector3d localBodySide = new Vector3d(0.0D, 0.0D, 1.0D);

    private final Vector3d localControlAxis = new Vector3d();
    private final Vector3d velocityAtAnchorWorld = new Vector3d();
    private final Vector3d velocityAtAnchorLocal = new Vector3d();
    private final Vector3d impulseDirection = new Vector3d();
    private final Vector3d impulse = new Vector3d();

    public RollTestThrusterBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.ROLL_TEST_THRUSTER.get(), pos, blockState);
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            RollTestThrusterBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }

        int signal = level.getBestNeighborSignal(pos);
        blockEntity.setDirectRedstoneSignal(signal);
        blockEntity.updatePoweredBlockState();
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (handle == null || !handle.isValid()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(RollTestThrusterBlock.ROLE)) {
            return;
        }

        RollTestThrusterRole role = state.getValue(RollTestThrusterBlock.ROLE);

        double control = getControlValue(role, state);

        if (Math.abs(control) < 0.01D) {
            return;
        }

        double power01 = clamp(Math.abs(control), 0.0D, 1.0D);
        int controlSign = control >= 0.0D ? 1 : -1;

        localAnchor.set(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        );

        /*
         * Roll / pitch:
         * - force is body-local up/down
         *
         * Yaw:
         * - force is body-local side-to-side
         * - front and rear roles apply opposite side forces
         * - front + rear separation creates yaw torque
         */
        if (role.isYawRole()) {
            localControlAxis.set(localBodySide);
        } else {
            localControlAxis.set(localBodyUp);
        }

        velocityAtAnchorWorld.set(
                Sable.HELPER.getVelocity(level, new Vector3d(localAnchor))
        );

        if (!isFiniteVector(velocityAtAnchorWorld)) {
            return;
        }

        velocityAtAnchorLocal.set(
                subLevel.logicalPose().transformNormalInverse(velocityAtAnchorWorld)
        );

        if (!isFiniteVector(velocityAtAnchorLocal)) {
            return;
        }

        double currentAxisVelocity = velocityAtAnchorLocal.dot(localControlAxis);
        double targetAxisVelocity = TARGET_SPEED_AT_FULL_SIGNAL * power01 * controlSign;

        double deltaV = targetAxisVelocity - currentAxisVelocity;

        if (Math.abs(deltaV) < 0.005D) {
            return;
        }

        double stepScale = clamp(timeStep / 0.05D, 0.05D, 1.0D);
        double maxDeltaV = MAX_DELTA_V_PER_PHYSICS_TICK * power01 * stepScale;

        deltaV = clamp(deltaV, -maxDeltaV, maxDeltaV);

        if (Math.abs(deltaV) < 0.00001D) {
            return;
        }

        impulseDirection.set(localControlAxis);

        if (deltaV < 0.0D) {
            impulseDirection.negate();
        }

        double effectiveMass = estimateEffectiveMass(subLevel, localAnchor, impulseDirection);
        double impulseMagnitude = effectiveMass * Math.abs(deltaV);

        impulseMagnitude = clamp(
                impulseMagnitude,
                0.0D,
                MAX_IMPULSE_AT_FULL_SIGNAL * power01
        );

        if (!Double.isFinite(impulseMagnitude) || impulseMagnitude < MIN_IMPULSE) {
            return;
        }

        impulse.set(impulseDirection).mul(impulseMagnitude);

        forceTotal.applyImpulseAtPoint(subLevel, localAnchor, impulse);
        handle.applyForcesAndReset(forceTotal);
    }

    private double getControlValue(RollTestThrusterRole role, BlockState state) {
        double control = 0.0D;

        if (directRedstoneSignal > 0) {
            double power01 = clamp(directRedstoneSignal / 15.0D, 0.0D, 1.0D);
            control = role.controlSign() * power01;
        } else {
            PodPilotInputState.Command command =
                    PodPilotInputState.findLatestCommand(level);

            if (command != null) {
                if (role.isRollRole()) {
                    control = role.controlSign() * command.roll();
                } else if (role.isPitchRole()) {
                    control = role.controlSign() * command.pitch();
                } else if (role.isYawRole()) {
                    control = role.controlSign() * command.yaw();
                }
            }
        }

        if (state.hasProperty(RollTestThrusterBlock.REVERSED)
                && state.getValue(RollTestThrusterBlock.REVERSED)) {
            control *= -1.0D;
        }

        return clamp(control, -1.0D, 1.0D);
    }

    public int getDirectRedstoneSignal() {
        return directRedstoneSignal;
    }

    public boolean isPowered() {
        return directRedstoneSignal > 0;
    }

    private void setDirectRedstoneSignal(int signal) {
        signal = clampSignal(signal);

        if (directRedstoneSignal == signal) {
            return;
        }

        directRedstoneSignal = signal;
        setChanged();
    }

    private void updatePoweredBlockState() {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(RollTestThrusterBlock.POWERED)) {
            return;
        }

        boolean powered = isPowered();

        if (state.getValue(RollTestThrusterBlock.POWERED) == powered) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(RollTestThrusterBlock.POWERED, powered), 3);
    }

    private double estimateEffectiveMass(ServerSubLevel subLevel, Vector3d point, Vector3d direction) {
        if (subLevel.getMassTracker() == null || subLevel.getMassTracker().isInvalid()) {
            return 80.0D;
        }

        double inverseNormalMass = subLevel.getMassTracker().getInverseNormalMass(point, direction);

        if (!Double.isFinite(inverseNormalMass) || inverseNormalMass <= 0.000001D) {
            return 80.0D;
        }

        double effectiveMass = 1.0D / inverseNormalMass;

        if (!Double.isFinite(effectiveMass)) {
            return 80.0D;
        }

        return clamp(effectiveMass, 20.0D, 2500.0D);
    }

    private static int clampSignal(int signal) {
        return Math.max(0, Math.min(15, signal));
    }

    private static boolean isFiniteVector(Vector3d vector) {
        return Double.isFinite(vector.x)
                && Double.isFinite(vector.y)
                && Double.isFinite(vector.z);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}