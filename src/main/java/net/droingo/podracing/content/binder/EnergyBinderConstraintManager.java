package net.droingo.podracing.content.binder;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnergyBinderConstraintManager {
    /*
     * No hard locks.
     *
     * This is important. We do not want a fixed weld.
     * The Energy Binder should behave like a powered spring/cable:
     * - it wants to keep its length
     * - it wants to stay straight
     * - it wants the mounts to face each other
     * - it wants to resist roll/pirouetting
     * - but it can still flex, twist, lag, and recover
     */
    private static final Set<ConstraintJointAxis> LOCKED_AXES =
            EnumSet.noneOf(ConstraintJointAxis.class);

    /*
     * Main length spring.
     * Higher = stronger pull toward the saved target distance.
     */
    private static final double LENGTH_STIFFNESS_SCALE = 1600.0D;
    private static final double LENGTH_DAMPING_SCALE = 180.0D;
    private static final double MAX_LENGTH_FORCE = 90000.0D;

    /*
     * Side-straightening springs.
     * These resist the binder bending sideways, but do not hard-lock it.
     */
    private static final double SIDE_STIFFNESS_SCALE = 700.0D;
    private static final double SIDE_DAMPING_SCALE = 90.0D;
    private static final double MAX_SIDE_FORCE = 45000.0D;

    /*
     * Soft face alignment.
     * These make the mount faces want to point along the binder.
     */
    private static final double ANGULAR_STIFFNESS_SCALE = 320.0D;
    private static final double ANGULAR_DAMPING_SCALE = 70.0D;
    private static final double MAX_ALIGNMENT_TORQUE = 75000.0D;

    /*
     * Soft roll alignment.
     * This stops two engines from freely pirouetting around a single binder.
     * Keep this weaker than angular Y/Z so it feels like torsion, not a weld.
     */
    private static final double ROLL_STIFFNESS_SCALE = 95.0D;
    private static final double ROLL_DAMPING_SCALE = 32.0D;
    private static final double MAX_ROLL_TORQUE = 26000.0D;

    private static final Map<UUID, ActiveConstraint> ACTIVE_CONSTRAINTS = new ConcurrentHashMap<>();

    /*
     * Sable can run more than one physics step per Minecraft tick.
     * The native constraints continue solving internally, so we only need to scan
     * binder saved data and create/remove constraints once per level tick.
     */
    private static final Map<ResourceKey<Level>, Long> LAST_PROCESSED_TICK = new ConcurrentHashMap<>();

    private EnergyBinderConstraintManager() {
    }

    public static void onPrePhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
        ServerLevel level = physicsSystem.getLevel();

        long gameTime = level.getGameTime();
        Long lastProcessedTick = LAST_PROCESSED_TICK.put(level.dimension(), gameTime);

        if (lastProcessedTick != null && lastProcessedTick == gameTime) {
            return;
        }

        EnergyBinderSavedData data = EnergyBinderSavedData.get(level);
        Set<UUID> seenThisLevel = new HashSet<>();

        for (EnergyBinderConnection connection : data.connections()) {
            if (!connection.endpointA().dimension().equals(level.dimension())) {
                continue;
            }

            if (!connection.endpointB().dimension().equals(level.dimension())) {
                continue;
            }

            seenThisLevel.add(connection.id());

            if (!shouldHaveConstraint(level, connection)) {
                removeConstraint(connection.id());
                continue;
            }

            ensureConstraint(physicsSystem, level, connection);
        }

        removeStaleConstraintsForLevel(level, seenThisLevel);
    }

    public static void removeConstraint(UUID connectionId) {
        ActiveConstraint activeConstraint = ACTIVE_CONSTRAINTS.remove(connectionId);

        if (activeConstraint == null) {
            return;
        }

        if (activeConstraint.handle().isValid()) {
            activeConstraint.handle().remove();
        }
    }

    public static void removeAll() {
        for (UUID id : Set.copyOf(ACTIVE_CONSTRAINTS.keySet())) {
            removeConstraint(id);
        }

        LAST_PROCESSED_TICK.clear();
    }

    private static boolean shouldHaveConstraint(ServerLevel level, EnergyBinderConnection connection) {
        if (!connection.enabled()) {
            return false;
        }

        if (!isConnectionPowered(level, connection)) {
            return false;
        }

        ServerSubLevel bodyA = subLevelForEndpoint(level, connection.endpointA());
        ServerSubLevel bodyB = subLevelForEndpoint(level, connection.endpointB());

        /*
         * At least one end needs to belong to a Sable physics body.
         * Two normal world blocks can show a beam, but there is nothing to move.
         */
        if (bodyA == null && bodyB == null) {
            return false;
        }

        /*
         * If both endpoints are on the same sublevel, constraining them together
         * would just fight the craft itself.
         */
        return bodyA != bodyB;
    }

    private static boolean isConnectionPowered(ServerLevel level, EnergyBinderConnection connection) {
        return isEndpointPowered(level, connection.endpointA())
                || isEndpointPowered(level, connection.endpointB());
    }

    private static boolean isEndpointPowered(ServerLevel level, EnergyBinderEndpoint endpoint) {
        if (!endpoint.dimension().equals(level.dimension())) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(endpoint.pos());

        if (blockEntity instanceof BinderMountBlockEntity binderMount) {
            return binderMount.isPowered();
        }

        return level.hasNeighborSignal(endpoint.pos());
    }

    private static void ensureConstraint(
            SubLevelPhysicsSystem physicsSystem,
            ServerLevel level,
            EnergyBinderConnection connection
    ) {
        ActiveConstraint existing = ACTIVE_CONSTRAINTS.get(connection.id());

        /*
         * Optimization:
         * Once a native Sable constraint exists, Sable keeps solving it internally.
         * Do not retune it every physics tick unless we later add a GUI that marks
         * the connection dirty.
         */
        if (existing != null && existing.handle().isValid()) {
            return;
        }

        removeConstraint(connection.id());

        ServerSubLevel bodyA = subLevelForEndpoint(level, connection.endpointA());
        ServerSubLevel bodyB = subLevelForEndpoint(level, connection.endpointB());

        if (bodyA == null && bodyB == null) {
            return;
        }

        if (bodyA == bodyB) {
            return;
        }

        Vector3d anchorA = localSocketPosition(level, connection.endpointA());
        Vector3d anchorB = localSocketPosition(level, connection.endpointB());

        /*
         * The joint X axis is the binder/spring axis.
         *
         * Endpoint A:
         * +X follows mount A's facing direction.
         *
         * Endpoint B:
         * +X follows the opposite of mount B's facing direction.
         *
         * This makes the two mounts want to face each other. Because all axes are
         * soft-motored instead of hard-locked, it can still flex.
         */
        Quaterniond localOrientationA = endpointJointOrientation(level, connection.endpointA(), false);
        Quaterniond localOrientationB = endpointJointOrientation(level, connection.endpointB(), true);

        GenericConstraintConfiguration configuration = new GenericConstraintConfiguration(
                anchorA,
                anchorB,
                localOrientationA,
                localOrientationB,
                LOCKED_AXES
        );

        GenericConstraintHandle handle = physicsSystem.getPipeline().addConstraint(
                bodyA,
                bodyB,
                configuration
        );

        if (handle == null) {
            return;
        }

        handle.setContactsEnabled(false);
        tuneConstraint(handle, connection);

        ACTIVE_CONSTRAINTS.put(
                connection.id(),
                new ActiveConstraint(connection.id(), level.dimension(), handle)
        );
    }

    private static void tuneConstraint(GenericConstraintHandle handle, EnergyBinderConnection connection) {
        double lengthStiffness = Math.max(1.0D, connection.stiffness() * LENGTH_STIFFNESS_SCALE);
        double lengthDamping = Math.max(1.0D, connection.damping() * LENGTH_DAMPING_SCALE);

        double sideStiffness = Math.max(1.0D, connection.stiffness() * SIDE_STIFFNESS_SCALE);
        double sideDamping = Math.max(1.0D, connection.damping() * SIDE_DAMPING_SCALE);

        double angularStiffness = Math.max(1.0D, connection.stiffness() * ANGULAR_STIFFNESS_SCALE);
        double angularDamping = Math.max(1.0D, connection.damping() * ANGULAR_DAMPING_SCALE);

        double rollStiffness = Math.max(1.0D, connection.stiffness() * ROLL_STIFFNESS_SCALE);
        double rollDamping = Math.max(1.0D, connection.damping() * ROLL_DAMPING_SCALE);

        /*
         * Main length spring.
         * Target is the distance saved when the two Binder Mounts were linked.
         */
        handle.setMotor(
                ConstraintJointAxis.LINEAR_X,
                connection.targetDistance(),
                lengthStiffness,
                lengthDamping,
                true,
                MAX_LENGTH_FORCE
        );

        /*
         * Sideways straightening.
         * These make the binder want to stay straight, but not rigid.
         */
        handle.setMotor(
                ConstraintJointAxis.LINEAR_Y,
                0.0D,
                sideStiffness,
                sideDamping,
                true,
                MAX_SIDE_FORCE
        );

        handle.setMotor(
                ConstraintJointAxis.LINEAR_Z,
                0.0D,
                sideStiffness,
                sideDamping,
                true,
                MAX_SIDE_FORCE
        );

        /*
         * Soft face alignment.
         * These make the mount faces want to point along the beam.
         */
        handle.setMotor(
                ConstraintJointAxis.ANGULAR_Y,
                0.0D,
                angularStiffness,
                angularDamping,
                true,
                MAX_ALIGNMENT_TORQUE
        );

        handle.setMotor(
                ConstraintJointAxis.ANGULAR_Z,
                0.0D,
                angularStiffness,
                angularDamping,
                true,
                MAX_ALIGNMENT_TORQUE
        );

        /*
         * Soft torsion / roll alignment.
         *
         * This is the missing piece for single-binder engine pairs.
         * It discourages endless pirouetting around the beam axis, but because it
         * is a weak motor instead of a locked axis, the connection can still flex.
         */
        handle.setMotor(
                ConstraintJointAxis.ANGULAR_X,
                0.0D,
                rollStiffness,
                rollDamping,
                true,
                MAX_ROLL_TORQUE
        );
    }

    private static ServerSubLevel subLevelForEndpoint(ServerLevel level, EnergyBinderEndpoint endpoint) {
        if (!endpoint.dimension().equals(level.dimension())) {
            return null;
        }

        SubLevel subLevel = Sable.HELPER.getContaining(level, endpoint.pos());

        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            return serverSubLevel;
        }

        return null;
    }

    private static Vector3d localSocketPosition(Level level, EnergyBinderEndpoint endpoint) {
        Vec3 socket = endpoint.socketPosition(level);
        return new Vector3d(socket.x, socket.y, socket.z);
    }

    private static Quaterniond endpointJointOrientation(
            Level level,
            EnergyBinderEndpoint endpoint,
            boolean invertNormal
    ) {
        Vec3 normal = endpoint.socketNormal(level);

        Vector3d axis = new Vector3d(normal.x, normal.y, normal.z);

        if (invertNormal) {
            axis.negate();
        }

        if (axis.lengthSquared() < 0.000001D) {
            return new Quaterniond();
        }

        axis.normalize();

        return new Quaterniond().rotationTo(
                1.0D,
                0.0D,
                0.0D,
                axis.x,
                axis.y,
                axis.z
        );
    }

    private static void removeStaleConstraintsForLevel(ServerLevel level, Set<UUID> seenThisLevel) {
        for (ActiveConstraint activeConstraint : Set.copyOf(ACTIVE_CONSTRAINTS.values())) {
            if (!activeConstraint.dimension().equals(level.dimension())) {
                continue;
            }

            if (seenThisLevel.contains(activeConstraint.id())) {
                continue;
            }

            removeConstraint(activeConstraint.id());
        }
    }

    private record ActiveConstraint(
            UUID id,
            ResourceKey<Level> dimension,
            GenericConstraintHandle handle
    ) {
    }
}