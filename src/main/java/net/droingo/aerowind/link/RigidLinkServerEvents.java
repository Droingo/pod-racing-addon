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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

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
                    "AeroWind link sync: level={}, savedLinks={}, activeConstraints={}",
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

            createBallJointConstraint(level, physicsSystem, levelConstraints, link, shouldDebug);
        }
    }

    private static void createBallJointConstraint(
            ServerLevel level,
            SubLevelPhysicsSystem physicsSystem,
            Map<RigidLinkSavedData.RigidLink, GenericConstraintHandle> levelConstraints,
            RigidLinkSavedData.RigidLink link,
            boolean shouldDebug
    ) {
        ServerSubLevel subLevelA = SableWindAccess.findSubLevelAt(level, link.posA());
        ServerSubLevel subLevelB = SableWindAccess.findSubLevelAt(level, link.posB());

        if (subLevelA == null || subLevelB == null) {
            if (shouldDebug) {
                AeroWind.LOGGER.info(
                        "AeroWind link waiting for sublevels: kind={} label={} posA={} foundA={} posB={} foundB={}",
                        link.linkKind(),
                        link.label(),
                        link.posA(),
                        subLevelA != null,
                        link.posB(),
                        subLevelB != null
                );
            }

            return;
        }

        if (subLevelA.getUniqueId().equals(subLevelB.getUniqueId())) {
            if (shouldDebug) {
                AeroWind.LOGGER.warn(
                        "AeroWind link rejected because both ends are same sublevel: kind={} label={} posA={} posB={} subLevel={}",
                        link.linkKind(),
                        link.label(),
                        link.posA(),
                        link.posB(),
                        subLevelA.getUniqueId()
                );
            }

            return;
        }

        boolean isRagdoll = "ragdoll".equalsIgnoreCase(link.linkKind());

        /*
         * Normal rigid links keep strict UUID validation.
         * Ragdoll links are allowed to re-detect sublevels by position after reload,
         * because Sable sublevel UUIDs can change between sessions.
         */
        if (!isRagdoll) {
            if (!subLevelA.getUniqueId().equals(link.subLevelA()) || !subLevelB.getUniqueId().equals(link.subLevelB())) {
                if (shouldDebug) {
                    AeroWind.LOGGER.warn(
                            "Rigid link UUID mismatch: expectedA={} actualA={} expectedB={} actualB={}",
                            link.subLevelA(),
                            subLevelA.getUniqueId(),
                            link.subLevelB(),
                            subLevelB.getUniqueId()
                    );
                }

                return;
            }
        }

        Vector3d anchorA = anchorFor(link.posA(), link.anchorOffsetA());
        Vector3d anchorB = anchorFor(link.posB(), link.anchorOffsetB());

        GenericConstraintConfiguration configuration = new GenericConstraintConfiguration(
                anchorA,
                anchorB,
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

        handle.setContactsEnabled(true);

        levelConstraints.put(link, handle);

        physicsSystem.getPipeline().wakeUp(subLevelA);
        physicsSystem.getPipeline().wakeUp(subLevelB);

        AeroWind.LOGGER.info(
                "AeroWind {} joint created: label={} posA={} actualA={} posB={} actualB={} valid={}",
                link.linkKind(),
                link.label(),
                link.posA(),
                subLevelA.getUniqueId(),
                link.posB(),
                subLevelB.getUniqueId(),
                handle.isValid()
        );
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        clearRuntimeConstraints();
        AeroWind.LOGGER.info("AeroWind cleared runtime rigid/ragdoll constraints on server stop.");
    }

    private static Vector3d anchorFor(BlockPos pos, Vector3d offset) {
        return new Vector3d(
                pos.getX() + 0.5D + offset.x,
                pos.getY() + 0.5D + offset.y,
                pos.getZ() + 0.5D + offset.z
        );
    }
}