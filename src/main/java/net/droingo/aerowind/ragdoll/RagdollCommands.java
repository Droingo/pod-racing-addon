package net.droingo.aerowind.ragdoll;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintConfiguration;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.droingo.aerowind.AeroWindBlocks;
import net.droingo.aerowind.sable.SableWindAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RagdollCommands {
    private static final int SEARCH_RADIUS = 12;
    private static final double MARK_RANGE = 12.0D;

    private static final Map<UUID, EnumMap<RagdollPartRole, BlockPos>> MARKED_PARTS = new HashMap<>();

    private RagdollCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("ragdoll")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("mark")
                                .then(Commands.argument("part", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                List.of(
                                                        "head",
                                                        "torso",
                                                        "left_arm",
                                                        "right_arm",
                                                        "left_leg",
                                                        "right_leg"
                                                ),
                                                builder
                                        ))
                                        .executes(context -> markPart(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "part")
                                        ))
                                )
                        )

                        .then(Commands.literal("showmarks")
                                .executes(context -> showMarks(context.getSource()))
                        )

                        .then(Commands.literal("clearmarks")
                                .executes(context -> clearMarks(context.getSource()))
                        )

                        .then(Commands.literal("connectmarked")
                                .executes(context -> connectMarked(context.getSource()))
                        )

                        .then(Commands.literal("connectnearest")
                                .executes(context -> connectNearest(context.getSource()))
                        )

                        .then(Commands.literal("debugnearest")
                                .executes(context -> debugNearest(context.getSource()))
                        )
        );
    }

    private static int markPart(CommandSourceStack source, String partName) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        RagdollPartRole role = RagdollPartRole.fromName(partName);

        if (role == null) {
            source.sendFailure(Component.literal("Unknown ragdoll part: " + partName));
            return 0;
        }

        BlockHitResult hit = raycast(player);

        if (hit.getType() == HitResult.Type.MISS) {
            source.sendFailure(Component.literal("Look directly at a Sable ragdoll part first."));
            return 0;
        }

        BlockPos pos = hit.getBlockPos();
        ServerLevel level = player.serverLevel();

        ServerSubLevel subLevel = SableWindAccess.findSubLevelAt(level, pos);

        if (subLevel == null) {
            source.sendFailure(Component.literal(
                    "Marked position is not inside a Sable sublevel: " + formatPos(pos)
            ));
            return 0;
        }

        EnumMap<RagdollPartRole, BlockPos> marks = MARKED_PARTS.computeIfAbsent(
                player.getUUID(),
                ignored -> new EnumMap<>(RagdollPartRole.class)
        );

        marks.put(role, pos.immutable());

        source.sendSuccess(
                () -> Component.literal("Marked " + role.commandName + " at " + formatPos(pos) + " in sublevel " + subLevel.getUniqueId()),
                false
        );

        return 1;
    }

    private static int connectMarked(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        EnumMap<RagdollPartRole, BlockPos> marks = MARKED_PARTS.get(player.getUUID());

        if (marks == null) {
            source.sendFailure(Component.literal("No ragdoll parts marked yet."));
            return 0;
        }

        for (RagdollPartRole role : RagdollPartRole.values()) {
            if (!marks.containsKey(role)) {
                source.sendFailure(Component.literal("Missing marked part: " + role.commandName));
                return 0;
            }
        }

        ServerLevel level = player.serverLevel();

        BlockPos torso = marks.get(RagdollPartRole.TORSO);
        BlockPos head = marks.get(RagdollPartRole.HEAD);
        BlockPos leftArm = marks.get(RagdollPartRole.LEFT_ARM);
        BlockPos rightArm = marks.get(RagdollPartRole.RIGHT_ARM);
        BlockPos leftLeg = marks.get(RagdollPartRole.LEFT_LEG);
        BlockPos rightLeg = marks.get(RagdollPartRole.RIGHT_LEG);

        int connected = 0;

        if (tryConnectParts(source, level, torso, head, "head -> torso")) {
            connected++;
        } else {
            return 0;
        }

        if (tryConnectParts(source, level, torso, leftArm, "left arm -> torso")) {
            connected++;
        } else {
            return 0;
        }

        if (tryConnectParts(source, level, torso, rightArm, "right arm -> torso")) {
            connected++;
        } else {
            return 0;
        }

        if (tryConnectParts(source, level, torso, leftLeg, "left leg -> torso")) {
            connected++;
        } else {
            return 0;
        }

        if (tryConnectParts(source, level, torso, rightLeg, "right leg -> torso")) {
            connected++;
        } else {
            return 0;
        }

        int finalConnected = connected;

        source.sendSuccess(
                () -> Component.literal("Connected marked ragdoll body: 6 physics parts with " + finalConnected + " constraints."),
                true
        );

        return 1;
    }

    private static boolean tryConnectParts(CommandSourceStack source, ServerLevel level, BlockPos posA, BlockPos posB, String label) {
        ServerSubLevel subLevelA = SableWindAccess.findSubLevelAt(level, posA);
        ServerSubLevel subLevelB = SableWindAccess.findSubLevelAt(level, posB);

        if (subLevelA == null) {
            source.sendFailure(Component.literal(
                    "Cannot connect " + label + ". First ragdoll part is not inside a Sable sublevel: " + formatPos(posA)
            ));
            return false;
        }

        if (subLevelB == null) {
            source.sendFailure(Component.literal(
                    "Cannot connect " + label + ". Second ragdoll part is not inside a Sable sublevel: " + formatPos(posB)
            ));
            return false;
        }

        if (subLevelA.getUniqueId().equals(subLevelB.getUniqueId())) {
            source.sendFailure(Component.literal(
                    "Cannot connect " + label + ". Both parts are inside the same Sable sublevel. Each body part must be its own sublevel."
            ));
            return false;
        }

        SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);

        if (physicsSystem == null) {
            source.sendFailure(Component.literal("No Sable physics system found for this level."));
            return false;
        }

        JointOffsets offsets = getJointOffsets(label);

        Vector3d anchorA = blockCenter(posA).add(offsets.offsetA());
        Vector3d anchorB = blockCenter(posB).add(offsets.offsetB());

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

        physicsSystem.getPipeline().addConstraint(
                subLevelA,
                subLevelB,
                configuration
        );

        source.sendSuccess(
                () -> Component.literal("Connected " + label + " with anchors A=" + anchorA + " B=" + anchorB),
                true
        );

        return true;
    }

    private static int showMarks(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        EnumMap<RagdollPartRole, BlockPos> marks = MARKED_PARTS.get(player.getUUID());

        if (marks == null || marks.isEmpty()) {
            source.sendFailure(Component.literal("No ragdoll parts marked."));
            return 0;
        }

        for (RagdollPartRole role : RagdollPartRole.values()) {
            BlockPos pos = marks.get(role);

            if (pos == null) {
                source.sendSuccess(
                        () -> Component.literal(role.commandName + ": not marked"),
                        false
                );
            } else {
                source.sendSuccess(
                        () -> Component.literal(role.commandName + ": " + formatPos(pos)),
                        false
                );
            }
        }

        return 1;
    }

    private static int clearMarks(CommandSourceStack source) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        MARKED_PARTS.remove(player.getUUID());

        source.sendSuccess(
                () -> Component.literal("Cleared marked ragdoll parts."),
                false
        );

        return 1;
    }

    private static BlockHitResult raycast(ServerPlayer player) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = start.add(look.scale(MARK_RANGE));

        return player.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));
    }

    private static int connectNearest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());

        List<BlockPos> parts = findNearbyRagdollParts(level, origin);

        if (parts.size() < 2) {
            source.sendFailure(Component.literal(
                    "Need at least 2 ragdoll part blocks within " + SEARCH_RADIUS + " blocks."
            ));
            return 0;
        }

        BlockPos posA = parts.get(0);
        BlockPos posB = parts.get(1);

        if (!tryConnectParts(source, level, posA, posB, "nearest pair")) {
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Connected 2 ragdoll parts with 1 Sable constraint."),
                true
        );

        return 1;
    }

    private static int debugNearest(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos origin = BlockPos.containing(source.getPosition());

        List<BlockPos> parts = findNearbyRagdollParts(level, origin);

        if (parts.isEmpty()) {
            source.sendFailure(Component.literal(
                    "No ragdoll part blocks found within " + SEARCH_RADIUS + " blocks."
            ));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Found " + parts.size() + " normal-world ragdoll part blocks nearby:"),
                false
        );

        int count = Math.min(parts.size(), 12);

        for (int i = 0; i < count; i++) {
            BlockPos pos = parts.get(i);
            ServerSubLevel subLevel = SableWindAccess.findSubLevelAt(level, pos);

            String subLevelText = subLevel == null
                    ? "no Sable sublevel"
                    : "sublevel " + subLevel.getUniqueId();

            final int index = i + 1;
            source.sendSuccess(
                    () -> Component.literal(index + ". " + formatPos(pos) + " -> " + subLevelText),
                    false
            );
        }

        return 1;
    }

    private static Vector3d blockCenter(BlockPos pos) {
        return new Vector3d(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        );
    }

    private static JointOffsets getJointOffsets(String label) {
        /*
         * Tighter joint spacing.
         *
         * The visual model still has a small gap, but not enough to make
         * the body look stretched apart.
         */

        double verticalGap = 0.025D;
        double sideGap = 0.035D;

        if (label.contains("head")) {
            return new JointOffsets(
                    new Vector3d(0.0D, 0.375D + verticalGap * 0.5D, 0.0D),
                    new Vector3d(0.0D, -0.25D - verticalGap * 0.5D, 0.0D)
            );
        }

        if (label.contains("left arm")) {
            return new JointOffsets(
                    new Vector3d(-0.25D - sideGap * 0.5D, 0.25D, 0.0D),
                    new Vector3d(0.125D + sideGap * 0.5D, 0.25D, 0.0D)
            );
        }

        if (label.contains("right arm")) {
            return new JointOffsets(
                    new Vector3d(0.25D + sideGap * 0.5D, 0.25D, 0.0D),
                    new Vector3d(-0.125D - sideGap * 0.5D, 0.25D, 0.0D)
            );
        }

        if (label.contains("left leg")) {
            return new JointOffsets(
                    new Vector3d(-0.125D, -0.375D - verticalGap * 0.5D, 0.0D),
                    new Vector3d(0.0D, 0.375D + verticalGap * 0.5D, 0.0D)
            );
        }

        if (label.contains("right leg")) {
            return new JointOffsets(
                    new Vector3d(0.125D, -0.375D - verticalGap * 0.5D, 0.0D),
                    new Vector3d(0.0D, 0.375D + verticalGap * 0.5D, 0.0D)
            );
        }

        return new JointOffsets(
                new Vector3d(0.0D, 0.0D, 0.0D),
                new Vector3d(0.0D, 0.0D, 0.0D)
        );
    }

    private static List<BlockPos> findNearbyRagdollParts(ServerLevel level, BlockPos origin) {
        List<BlockPos> parts = new ArrayList<>();

        BlockPos min = origin.offset(-SEARCH_RADIUS, -SEARCH_RADIUS, -SEARCH_RADIUS);
        BlockPos max = origin.offset(SEARCH_RADIUS, SEARCH_RADIUS, SEARCH_RADIUS);

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (isAnyRagdollPart(level, pos)) {
                parts.add(pos.immutable());
            }
        }

        parts.sort(Comparator.comparingDouble(pos -> pos.distSqr(origin)));

        return parts;
    }

    private static boolean isAnyRagdollPart(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(AeroWindBlocks.RAGDOLL_HEAD.get())
                || level.getBlockState(pos).is(AeroWindBlocks.RAGDOLL_TORSO.get())
                || level.getBlockState(pos).is(AeroWindBlocks.RAGDOLL_ARM.get())
                || level.getBlockState(pos).is(AeroWindBlocks.RAGDOLL_LEG.get());
    }

    private static String formatPos(BlockPos pos) {
        return "x=" + pos.getX() + ", y=" + pos.getY() + ", z=" + pos.getZ();
    }

    private record JointOffsets(Vector3d offsetA, Vector3d offsetB) {
    }

    private enum RagdollPartRole {
        HEAD("head"),
        TORSO("torso"),
        LEFT_ARM("left_arm"),
        RIGHT_ARM("right_arm"),
        LEFT_LEG("left_leg"),
        RIGHT_LEG("right_leg");

        private final String commandName;

        RagdollPartRole(String commandName) {
            this.commandName = commandName;
        }

        private static RagdollPartRole fromName(String name) {
            for (RagdollPartRole role : values()) {
                if (role.commandName.equalsIgnoreCase(name)) {
                    return role;
                }
            }

            return null;
        }
    }
}