package net.droingo.podracing.content.towcable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TowCableManager {
    private static final Map<UUID, TowCableEndpoint> ACTIVE_SELECTIONS = new HashMap<>();

    private TowCableManager() {
    }

    public static Optional<TowCableEndpoint> getSelection(Player player) {
        return Optional.ofNullable(ACTIVE_SELECTIONS.get(player.getUUID()));
    }

    public static void clearSelection(Player player) {
        ACTIVE_SELECTIONS.remove(player.getUUID());
    }

    public static void handleAnchorWrenched(ServerLevel level, Player player, TowCableEndpoint clickedEndpoint) {
        if (player.isShiftKeyDown()) {
            BlockEntity clickedBlockEntity = level.getBlockEntity(clickedEndpoint.pos());

            if (clickedBlockEntity instanceof TowCableAnchorBlockEntity clickedAnchor
                    && clickedAnchor.isLinked()) {
                clickedAnchor.unlinkBothSides();
                player.displayClientMessage(
                        Component.literal("Tow Cable unlinked.").withStyle(ChatFormatting.YELLOW),
                        true
                );
                return;
            }

            clearSelection(player);
            player.displayClientMessage(
                    Component.literal("Tow Cable selection cleared.").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        Optional<TowCableEndpoint> existingSelection = getSelection(player);

        if (existingSelection.isEmpty()) {
            ACTIVE_SELECTIONS.put(player.getUUID(), clickedEndpoint);

            player.displayClientMessage(
                    Component.literal("Tow Cable start selected. Right-click a second Tow Cable Anchor.")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
            return;
        }

        TowCableEndpoint firstEndpoint = existingSelection.get();

        if (firstEndpoint.equals(clickedEndpoint)) {
            clearSelection(player);

            player.displayClientMessage(
                    Component.literal("Tow Cable selection cancelled.").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (!firstEndpoint.dimension().equals(clickedEndpoint.dimension())) {
            ACTIVE_SELECTIONS.put(player.getUUID(), clickedEndpoint);

            player.displayClientMessage(
                    Component.literal("Tow Cable anchors must be in the same dimension. Selection moved.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        BlockEntity firstBlockEntity = level.getBlockEntity(firstEndpoint.pos());
        BlockEntity secondBlockEntity = level.getBlockEntity(clickedEndpoint.pos());

        if (!(firstBlockEntity instanceof TowCableAnchorBlockEntity firstAnchor)
                || !(secondBlockEntity instanceof TowCableAnchorBlockEntity secondAnchor)) {
            clearSelection(player);

            player.displayClientMessage(
                    Component.literal("Both ends must be Tow Cable Anchors.").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        double distance = firstEndpoint.distanceTo(level, clickedEndpoint);

        if (!Double.isFinite(distance)) {
            distance = 8.0D;
        }

        distance = Math.max(0.5D, distance);

        firstAnchor.linkTo(clickedEndpoint, distance);
        secondAnchor.linkTo(firstEndpoint, distance);

        clearSelection(player);

        player.displayClientMessage(
                Component.literal("Tow Cable connected. Rest length: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("%.2f blocks", distance))
                                .withStyle(ChatFormatting.WHITE)),
                true
        );
    }
}