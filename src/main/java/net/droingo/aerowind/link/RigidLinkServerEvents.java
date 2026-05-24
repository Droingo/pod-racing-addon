package net.droingo.aerowind.link;

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.droingo.aerowind.AeroWind;
import net.droingo.aerowind.sable.SableWindAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class RigidLinkServerEvents {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final int DEBUG_INTERVAL_TICKS = 100;

    private static final Map<ResourceKey<Level>, Map<RigidLinkSavedData.RigidLink, GenericConstraintHandle>> CONSTRAINTS_BY_LEVEL = new HashMap<>();

    private RigidLinkServerEvents() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        boolean shouldCheck = server.getTickCount() % CHECK_INTERVAL_TICKS == 0;
        boolean shouldDebug = server.getTickCount() % DEBUG_INTERVAL_TICKS == 0;

        if (!shouldCheck && !shouldDebug) {
            return;
        }

        for (ServerLevel level : server.getAllLevels()) {
            syncConstraintsForLevel(level, shouldDebug);
        }
    }

    public static void clearRuntimeConstraints() {
        for (Map<RigidLinkSavedData.RigidLink, GenericConstraintHandle> levelConstraints : CONSTRAINTS_BY_LEVEL.values()) {
            for (GenericConstraintHandle handle : levelConstraints.values()) {
                if (handle.isValid()) {
                    handle.remove();
                }
            }

            levelConstraints.clear();
        }

        CONSTRAINTS_BY_LEVEL.clear();
    }

    private static void syncConstraintsForLevel(ServerLevel level, boolean shouldDebug) {
        RigidLinkSavedData savedData = RigidLinkSavedData.get(level);

        Map<RigidLinkSavedData.RigidLink, GenericConstraintHandle> levelConstraints =
                CONSTRAINTS_BY_LEVEL.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());

        Set<RigidLinkSavedData.RigidLink> savedLinks = new HashSet<>(savedData.links());

        levelConstraints.entrySet().removeIf(entry -> {
            boolean shouldRemove = !savedLinks.contains(entry.getKey()) || !entry.getValue().isValid();

            if (shouldRemove && entry.getValue().isValid()) {
                entry.getValue().remove();
            }

            return shouldRemove;
        });

        if (savedData.linkCount() <= 0) {
            return;
        }

        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
        if (physicsSystem == null) {
            return;
        }

        if (shouldDebug) {
            AeroWind.LOGGER.info(
                    "Rigid Link native sync: level={}, savedLinks={}, activeConstraints={}",
                    level.dimension().location(),
                    savedData.linkCount(),
                    levelConstraints.size()
            );
        }

        for (RigidLinkSavedData.RigidLink link : savedData.links()) {
            GenericConstraintHandle existingHandle = levelConstraints.get(link);

            if (existingHandle != null && existingHandle.isValid()) {
                continue;
            }

            createBallJointConstraint(level, physicsSystem, levelConstraints, link);
        }
    }

    private static void createBallJointConstraint(
            ServerLevel level,
            SubLevelPhysicsSystem physicsSystem,
            Map<RigidLinkSavedData.RigidLink, GenericConstraintHandle> levelConstraints,
            RigidLinkSavedData.RigidLink link
    ) {
        ServerSubLevel subLevelA = SableWindAccess.findSubLevelAt(level, link.posA());
        ServerSubLevel subLevelB = SableWindAccess.findSubLevelAt(level, link.posB());

        if (subLevelA == null || subLevelB == null) {
            return;
        }

        if (!subLevelA.getUniqueId().equals(link.subLevelA()) || !subLevelB.getUniqueId().equals(link.subLevelB())) {
            return;
        }

        GenericConstraintConfiguration configuration = new GenericConstraintConfiguration(
                mountCenter(link.posA()),
                mountCenter(link.posB()),
                new Quaterniond(),
                new Quaterniond(),
                EnumSet.of(
                        ConstraintJointAxis.LINEAR_X,
                        ConstraintJointAxis.LINEAR_Y,
                        ConstraintJointAxis.LINEAR_Z
                )
        );

        GenericConstraintHandle handle = physicsSystem.getPipeline().addConstraint(
                subLevelA,
                subLevelB,
                configuration
        );

        handle.setContactsEnabled(false);

        levelConstraints.put(link, handle);

        physicsSystem.getPipeline().wakeUp(subLevelA);
        physicsSystem.getPipeline().wakeUp(subLevelB);

        AeroWind.LOGGER.info(
                "Rigid Link native ball joint created: A={} posA={} B={} posB={} targetLength={} valid={}",
                link.subLevelA(),
                link.posA(),
                link.subLevelB(),
                link.posB(),
                link.targetLength(),
                handle.isValid()
        );
    }


    private static Vector3d mountCenter(BlockPos pos) {
        return new Vector3d(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }
}