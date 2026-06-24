package net.droingo.podracing.content.attitudefin;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.droingo.podracing.content.attitudefin.menu.AttitudeFinMenu;
import net.droingo.podracing.content.pilot.PodPilotInputState;
import net.droingo.podracing.registry.PRBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;

import java.util.UUID;

public final class AttitudeFinBlockEntity extends BlockEntity implements BlockEntitySubLevelActor, MenuProvider {
    private static final String TAG_FREQ_MOST = "FrequencyMost";
    private static final String TAG_FREQ_LEAST = "FrequencyLeast";

    private static final double TARGET_SPEED_AT_FULL_SIGNAL = 3.50D;
    private static final double MAX_DELTA_V_PER_PHYSICS_TICK = 0.45D;
    private static final double MAX_IMPULSE_AT_FULL_SIGNAL = 2500.0D;
    private static final double MIN_IMPULSE = 0.00001D;

    private UUID frequency;
    private int directRedstoneSignal = 0;

    private final ForceTotal forceTotal = new ForceTotal();

    private final Vector3d localAnchor = new Vector3d();
    private final Vector3d localBodyUp = new Vector3d(0.0D, 1.0D, 0.0D);
    private final Vector3d localBodySide = new Vector3d(0.0D, 0.0D, 1.0D);

    private final Vector3d velocityAtAnchorWorld = new Vector3d();
    private final Vector3d velocityAtAnchorLocal = new Vector3d();
    private final Vector3d impulseDirection = new Vector3d();
    private final Vector3d impulse = new Vector3d();

    public AttitudeFinBlockEntity(BlockPos pos, BlockState blockState) {
        super(PRBlockEntities.ATTITUDE_FIN.get(), pos, blockState);
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            AttitudeFinBlockEntity blockEntity
    ) {
        if (level.isClientSide()) {
            return;
        }

        int signal = level.getBestNeighborSignal(pos);
        blockEntity.setDirectRedstoneSignal(signal);
        blockEntity.updatePoweredBlockState();
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Attitude Fin");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AttitudeFinMenu(containerId, playerInventory, this);
    }

    public AttitudeFinRole getRole() {
        BlockState state = getBlockState();

        if (!state.hasProperty(AttitudeFinBlock.ROLE)) {
            return AttitudeFinRole.LEFT_ENGINE;
        }

        return state.getValue(AttitudeFinBlock.ROLE);
    }

    public int getRoleIndex() {
        return getRole().guiIndex();
    }

    public void setRole(AttitudeFinRole role) {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(AttitudeFinBlock.ROLE)) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(AttitudeFinBlock.ROLE, role), 3);
        setChanged();
    }

    public boolean isReversed() {
        BlockState state = getBlockState();

        return state.hasProperty(AttitudeFinBlock.REVERSED)
                && state.getValue(AttitudeFinBlock.REVERSED);
    }

    public void setReversed(boolean reversed) {
        if (level == null || level.isClientSide()) {
            return;
        }

        BlockState state = getBlockState();

        if (!state.hasProperty(AttitudeFinBlock.REVERSED)) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(AttitudeFinBlock.REVERSED, reversed), 3);
        setChanged();
    }

    public void bindToFrequency(UUID frequency) {
        this.frequency = frequency;
        setChanged();
    }

    public boolean hasFrequency() {
        return frequency != null;
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (handle == null || !handle.isValid()) {
            return;
        }

        AttitudeFinRole role = getRole();

        localAnchor.set(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        );

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

        if (directRedstoneSignal > 0) {
            double power01 = clamp(directRedstoneSignal / 15.0D, 0.0D, 1.0D);

            if (role.isCombinedControlRole()) {
                applyAxisImpulse(subLevel, handle, timeStep, localBodyUp, role.controlSign() * power01);
            } else {
                applyAxisImpulse(subLevel, handle, timeStep, localBodyUp, role.controlSign() * power01);
            }

            return;
        }

        if (frequency == null) {
            return;
        }

        PodPilotInputState.Command command =
                PodPilotInputState.findLatestCommand(level, frequency);

        if (command == null) {
            return;
        }

        if (role.isRollRole()) {
            applyAxisImpulse(
                    subLevel,
                    handle,
                    timeStep,
                    localBodyUp,
                    role.controlSign() * command.roll()
            );
            return;
        }

        if (role.isCombinedControlRole()) {
            applyAxisImpulse(
                    subLevel,
                    handle,
                    timeStep,
                    localBodyUp,
                    role.controlSign() * command.pitch()
            );

            applyAxisImpulse(
                    subLevel,
                    handle,
                    timeStep,
                    localBodySide,
                    role.controlSign() * command.yaw()
            );
        }
    }

    private void applyAxisImpulse(
            ServerSubLevel subLevel,
            RigidBodyHandle handle,
            double timeStep,
            Vector3d localAxis,
            double control
    ) {
        if (isReversed()) {
            control *= -1.0D;
        }

        if (Math.abs(control) < 0.01D) {
            return;
        }

        double power01 = clamp(Math.abs(control), 0.0D, 1.0D);
        int controlSign = control >= 0.0D ? 1 : -1;

        double currentAxisVelocity = velocityAtAnchorLocal.dot(localAxis);
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

        impulseDirection.set(localAxis);

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

        if (!state.hasProperty(AttitudeFinBlock.POWERED)) {
            return;
        }

        boolean powered = isPowered();

        if (state.getValue(AttitudeFinBlock.POWERED) == powered) {
            return;
        }

        level.setBlock(worldPosition, state.setValue(AttitudeFinBlock.POWERED, powered), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        if (frequency != null) {
            tag.putLong(TAG_FREQ_MOST, frequency.getMostSignificantBits());
            tag.putLong(TAG_FREQ_LEAST, frequency.getLeastSignificantBits());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains(TAG_FREQ_MOST) && tag.contains(TAG_FREQ_LEAST)) {
            frequency = new UUID(
                    tag.getLong(TAG_FREQ_MOST),
                    tag.getLong(TAG_FREQ_LEAST)
            );
        } else {
            frequency = null;
        }
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