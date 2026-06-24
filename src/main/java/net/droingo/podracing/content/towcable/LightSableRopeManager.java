package net.droingo.podracing.content.towcable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class LightSableRopeManager {
    /*
     * Test value.
     *
     * Sable/Rapier does not expose rope mass directly. RapierRopeHandle.create(...)
     * only passes pointRadius into Rapier3D.createRope(...), so our own rope uses
     * a near-zero radius to test whether this makes the rope effectively weightless.
     */
    private static final double TEST_POINT_RADIUS = 0.001D;

    private static final int DEFAULT_SEGMENTS = 10;
    private static final double REBUILD_LENGTH_EPSILON = 0.025D;
    private static final double MAX_REASONABLE_DISTANCE = 128.0D;

    private static final Map<Key, RuntimeRope> ROPES = new HashMap<>();

    private LightSableRopeManager() {
    }

    public static void ensure(ServerLevel level, TowCableAnchorBlockEntity anchor) {
        if (!anchor.isLinked() || anchor.getLinkedEndpoint() == null) {
            removeForEndpoint(level, TowCableEndpoint.from(level, anchor.getBlockPos()));
            return;
        }

        TowCableEndpoint self = TowCableEndpoint.from(level, anchor.getBlockPos());
        TowCableEndpoint other = anchor.getLinkedEndpoint();

        if (!self.dimension().equals(other.dimension())) {
            removeForEndpoint(level, self);
            return;
        }

        Key key = Key.of(self, other);

        /*
         * Only one side owns the native rope so we do not create duplicates.
         */
        if (!key.primary().equals(self)) {
            return;
        }

        BlockEntity otherBlockEntity = level.getBlockEntity(other.pos());

        if (!(otherBlockEntity instanceof TowCableAnchorBlockEntity otherAnchor) || !otherAnchor.isLinked()) {
            remove(key);
            return;
        }

        double distance = self.projectedWorldDistanceTo(level, other);

        if (!Double.isFinite(distance) || distance < 0.05D || distance > MAX_REASONABLE_DISTANCE) {
            /*
             * Safety self.projectedWorldDistanceTo(level, other);

        if (!Double.isFinite(distance) || distance < 0.05D || distance > MAX_REASONABLE: if this ever gets huge, something is reading subplot/logical space
             * or the endpoints are invalid. Do not create a native rope.
             */
            remove(key);
            return;
        }

        double restLength = anchor.getRestLength();

        if (!Double.isFinite(restLength) || restLength < 0.5D) {
            restLength = distance;
        }

        RuntimeRope runtime = ROPES.get(key);

        if (runtime == null || Math.abs(runtime.restLength() - restLength) > REBUILD_LENGTH_EPSILON) {
            remove(key);
            runtime = create(level, self, other, restLength);
            ROPES.put(key, runtime);
        }

        runtime.updateAttachments(level, self, other);
    }

    public static void removeForEndpoint(ServerLevel level, TowCableEndpoint endpoint) {
        ArrayList<Key> toRemove = new ArrayList<>();

        for (Key key : ROPES.keySet()) {
            if (key.a().equals(endpoint) || key.b().equals(endpoint)) {
                toRemove.add(key);
            }
        }

        for (Key key : toRemove) {
            remove(key);
        }
    }

    private static RuntimeRope create(
            ServerLevel level,
            TowCableEndpoint a,
            TowCableEndpoint b,
            double restLength
    ) {
        Vec3 start = a.projectedWorldSocketPosition(level);
        Vec3 end = b.projectedWorldSocketPosition(level);

        ArrayList<Vector3d> points = buildInitialRopePoints(start, end, restLength, DEFAULT_SEGMENTS);

        RopePhysicsObject rope = new RopePhysicsObject(points, TEST_POINT_RADIUS);
        rope.onAddition(SubLevelPhysicsSystem.require(level));

        RuntimeRope runtime = new RuntimeRope(rope, restLength);
        runtime.updateAttachments(level, a, b);

        return runtime;
    }

    private static ArrayList<Vector3d> buildInitialRopePoints(
            Vec3 start,
            Vec3 end,
            double restLength,
            int segments
    ) {
        int pointCount = Math.max(2, segments + 1);

        Vec3 delta = end.subtract(start);
        double distance = delta.length();

        if (!Double.isFinite(distance) || distance < 0.001D) {
            distance = 0.001D;
            delta = new Vec3(0.0D, 0.0D, distance);
        }

        double targetLength = Math.max(restLength, distance);
        Vec3 direction = delta.normalize();

        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));

        if (side.lengthSqr() < 0.0001D) {
            side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        }

        side = side.normalize();

        double slack = Math.max(0.0D, targetLength - distance);
        double sag = Math.min(6.0D, slack * 0.65D);

        ArrayList<Vector3d> points = new ArrayList<>(pointCount);

        for (int i = 0; i < pointCount; i++) {
            double t = (double) i / (double) (pointCount - 1);
            Vec3 point = start.lerp(end, t);

            double curve = Math.sin(Math.PI * t) * sag;
            point = point.add(side.scale(curve));

            points.add(new Vector3d(point.x, point.y, point.z));
        }

        return points;
    }

    private static void remove(Key key) {
        RuntimeRope runtime = ROPES.remove(key);

        if (runtime != null) {
            runtime.remove();
        }
    }

    private static ServerSubLevel getContainingServerSubLevel(ServerLevel level, Vec3 logicalPosition) {
        SubLevel subLevel = Sable.HELPER.getContaining(
                level,
                new Vector3d(logicalPosition.x, logicalPosition.y, logicalPosition.z)
        );

        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            return serverSubLevel;
        }

        return null;
    }

    private record RuntimeRope(RopePhysicsObject rope, double restLength) {
        private void updateAttachments(ServerLevel level, TowCableEndpoint a, TowCableEndpoint b) {
            Vec3 logicalA = a.logicalSocketPosition(level);
            Vec3 logicalB = b.logicalSocketPosition(level);

            ServerSubLevel subLevelA = getContainingServerSubLevel(level, logicalA);
            ServerSubLevel subLevelB = getContainingServerSubLevel(level, logicalB);

            rope.setAttachment(
                    RopeHandle.AttachmentPoint.START,
                    new Vector3d(logicalA.x, logicalA.y, logicalA.z),
                    subLevelA
            );

            rope.setAttachment(
                    RopeHandle.AttachmentPoint.END,
                    new Vector3d(logicalB.x, logicalB.y, logicalB.z),
                    subLevelB
            );

            rope.wakeUp();
        }

        private void remove() {
            if (rope.isActive()) {
                rope.onRemoved();
            }
        }
    }

    private record Key(TowCableEndpoint a, TowCableEndpoint b) {
        private static Key of(TowCableEndpoint first, TowCableEndpoint second) {
            if (compare(first, second) <= 0) {
                return new Key(first, second);
            }

            return new Key(second, first);
        }

        private TowCableEndpoint primary() {
            return a;
        }

        private static int compare(TowCableEndpoint first, TowCableEndpoint second) {
            int dimensionCompare = first.dimension().location().toString()
                    .compareTo(second.dimension().location().toString());

            if (dimensionCompare != 0) {
                return dimensionCompare;
            }

            return Long.compare(first.pos().asLong(), second.pos().asLong());
        }
    }
}