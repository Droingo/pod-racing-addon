package net.droingo.aerowind.ragdoll;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.generic.GenericConstraintConfiguration;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.droingo.aerowind.AeroWindBlocks;
import net.droingo.aerowind.link.RigidLinkSavedData;
import net.droingo.aerowind.sable.SableWindAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.droingo.aerowind.blockentity.RagdollPartBlockEntity;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import net.droingo.aerowind.blockentity.RagdollPartBlockEntity;
import com.mojang.authlib.GameProfile;
import net.droingo.aerowind.blockentity.RagdollPartBlockEntity;
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

                        .then(Commands.literal("skin")
                                .then(Commands.argument("username", StringArgumentType.word())
                                        .executes(context -> setSkin(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "username")
                                        ))
                                )
                        )
                        .then(Commands.literal("skinuuid")
                                .then(Commands.argument("username", StringArgumentType.word())
                                        .then(Commands.argument("uuid", StringArgumentType.word())
                                                .executes(context -> setSkinByUuid(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "username"),
                                                        StringArgumentType.getString(context, "uuid")
                                                ))
                                        )
                                )
                        )

                        .then(Commands.literal("connectmarked")
                                .executes(context -> connectMarked(context.getSource()))
                        )

                        .then(Commands.literal("connectnearest")
                                .executes(context -> connectNearest(context.getSource()))
                        )
                        .then(Commands.literal("linkcount")
                                .executes(context -> linkCount(context.getSource()))
                        )

                        .then(Commands.literal("debugnearest")
                                .executes(context -> debugNearest(context.getSource()))
                        )
        );
    }

    private static int linkCount(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        RigidLinkSavedData savedData = RigidLinkSavedData.get(level);

        source.sendSuccess(
                () -> Component.literal("Saved AeroWind links in this level: " + savedData.linkCount()),
                false
        );

        return 1;
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

        if (level.getBlockEntity(pos) instanceof RagdollPartBlockEntity ragdollPart) {
            ragdollPart.setRagdollRole(role.commandName);
        }

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

    private static int setSkinByUuid(CommandSourceStack source, String username, String uuidText) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        EnumMap<RagdollPartRole, BlockPos> marks = MARKED_PARTS.get(player.getUUID());

        if (marks == null || marks.isEmpty()) {
            source.sendFailure(Component.literal("No marked ragdoll parts. Mark the ragdoll first, then run /ragdoll skinuuid <username> <uuid>."));
            return 0;
        }

        UUID resolvedUuid;

        try {
            resolvedUuid = parseFlexibleUuid(uuidText);
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Invalid UUID: " + uuidText));
            return 0;
        }

        String textureValue = "";
        String textureSignature = "";

        try {
            SkinTextureData textureData = lookupSkinTextureFromMojang(resolvedUuid);

            if (textureData != null) {
                textureValue = textureData.value();
                textureSignature = textureData.signature();
            }
        } catch (Exception exception) {
            source.sendFailure(Component.literal("Mojang skin texture lookup failed for UUID '" + resolvedUuid + "': " + exception.getMessage()));
        }

        ServerLevel level = player.serverLevel();

        int updated = 0;

        for (BlockPos pos : marks.values()) {
            if (level.getBlockEntity(pos) instanceof RagdollPartBlockEntity ragdollPart) {
                ragdollPart.setSkin(username, resolvedUuid, textureValue, textureSignature);
                updated++;
            }
        }

        int finalUpdated = updated;
        UUID finalResolvedUuid = resolvedUuid;
        boolean hasTexture = !textureValue.isBlank();

        source.sendSuccess(
                () -> Component.literal("Applied skin UUID '" + finalResolvedUuid + "' to " + finalUpdated + " marked ragdoll parts. texture=" + hasTexture),
                true
        );

        return 1;
    }

    private static UUID parseFlexibleUuid(String uuidText) {
        String cleaned = uuidText.trim();

        if (cleaned.contains("-")) {
            return UUID.fromString(cleaned);
        }

        return uuidFromUndashed(cleaned);
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

        JointOffsets offsets = getJointOffsets(label);

        RigidLinkSavedData savedData = RigidLinkSavedData.get(level);

        savedData.addRagdollLink(
                subLevelA.getUniqueId(),
                posA,
                subLevelB.getUniqueId(),
                posB,
                label,
                offsets.offsetA(),
                offsets.offsetB()
        );

        source.sendSuccess(
                () -> Component.literal("Saved ragdoll joint " + label + ". It will sync within 1 second and persist after reload."),
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
    private static UUID lookupUuidFromMojang(String username) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 204 || response.body() == null || response.body().isBlank()) {
            throw new IllegalStateException("No Java Minecraft profile found for username '" + username + "'");
        }

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode() + " from Mojang profile lookup");
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (!json.has("id")) {
            throw new IllegalStateException("No UUID returned by Mojang");
        }

        return uuidFromUndashed(json.get("id").getAsString());
    }

    private static SkinTextureData lookupSkinTextureFromMojang(UUID uuid) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String undashedUuid = uuid.toString().replace("-", "");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + undashedUuid + "?unsigned=false"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (!json.has("properties")) {
            throw new IllegalStateException("No properties returned");
        }

        JsonArray properties = json.getAsJsonArray("properties");

        for (int i = 0; i < properties.size(); i++) {
            JsonObject property = properties.get(i).getAsJsonObject();

            if (!property.has("name")) {
                continue;
            }

            if (!"textures".equals(property.get("name").getAsString())) {
                continue;
            }

            String value = property.has("value") ? property.get("value").getAsString() : "";
            String signature = property.has("signature") ? property.get("signature").getAsString() : "";

            return new SkinTextureData(value, signature);
        }

        throw new IllegalStateException("No texture property returned");
    }

    private static UUID uuidFromUndashed(String undashedUuid) {
        if (undashedUuid.length() != 32) {
            throw new IllegalArgumentException("Invalid undashed UUID: " + undashedUuid);
        }

        String dashed = undashedUuid.substring(0, 8)
                + "-"
                + undashedUuid.substring(8, 12)
                + "-"
                + undashedUuid.substring(12, 16)
                + "-"
                + undashedUuid.substring(16, 20)
                + "-"
                + undashedUuid.substring(20);

        return UUID.fromString(dashed);
    }

    private record SkinTextureData(String value, String signature) {
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
    private static int setSkin(CommandSourceStack source, String username) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        EnumMap<RagdollPartRole, BlockPos> marks = MARKED_PARTS.get(player.getUUID());

        if (marks == null || marks.isEmpty()) {
            source.sendFailure(Component.literal("No marked ragdoll parts. Mark the ragdoll first, then run /ragdoll skin <username>."));
            return 0;
        }

        ServerLevel level = player.serverLevel();

        UUID resolvedUuid = null;
        String textureValue = "";
        String textureSignature = "";

        try {
            resolvedUuid = source.getServer()
                    .getProfileCache()
                    .get(username)
                    .map(GameProfile::getId)
                    .orElse(null);
        } catch (Exception ignored) {
            resolvedUuid = null;
        }

        if (resolvedUuid == null) {
            try {
                resolvedUuid = lookupUuidFromMojang(username);
            } catch (Exception exception) {
                source.sendFailure(Component.literal("Mojang UUID lookup failed for '" + username + "': " + exception.getMessage()));
            }
        }

        if (resolvedUuid != null) {
            try {
                SkinTextureData textureData = lookupSkinTextureFromMojang(resolvedUuid);

                if (textureData != null) {
                    textureValue = textureData.value();
                    textureSignature = textureData.signature();
                }
            } catch (Exception exception) {
                source.sendFailure(Component.literal("Mojang skin texture lookup failed for '" + username + "': " + exception.getMessage()));
            }
        }

        int updated = 0;

        for (BlockPos pos : marks.values()) {
            if (level.getBlockEntity(pos) instanceof RagdollPartBlockEntity ragdollPart) {
                ragdollPart.setSkin(username, resolvedUuid, textureValue, textureSignature);
                updated++;
            }
        }

        int finalUpdated = updated;
        UUID finalResolvedUuid = resolvedUuid;
        boolean hasTexture = !textureValue.isBlank();

        source.sendSuccess(
                () -> Component.literal("Applied skin '" + username + "' to " + finalUpdated + " marked ragdoll parts. UUID=" + finalResolvedUuid + ", texture=" + hasTexture),
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