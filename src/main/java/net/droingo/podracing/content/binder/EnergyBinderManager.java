package net.droingo.podracing.content.binder;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EnergyBinderManager {
    private static final Map<UUID, EnergyBinderEndpoint> ACTIVE_SELECTIONS = new HashMap<>();

    private EnergyBinderManager() {
    }

    public static Optional<EnergyBinderEndpoint> getSelection(Player player) {
        return Optional.ofNullable(ACTIVE_SELECTIONS.get(player.getUUID()));
    }

    public static void setSelection(Player player, EnergyBinderEndpoint endpoint) {
        ACTIVE_SELECTIONS.put(player.getUUID(), endpoint);
    }

    public static void clearSelection(Player player) {
        ACTIVE_SELECTIONS.remove(player.getUUID());
    }

    public static void handleMountWrenched(ServerLevel level, Player player, EnergyBinderEndpoint clickedEndpoint) {
        Optional<EnergyBinderEndpoint> existingSelection = getSelection(player);

        if (player.isShiftKeyDown()) {
            clearSelection(player);
            player.displayClientMessage(
                    Component.literal("Energy Binder selection cleared.").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (existingSelection.isEmpty()) {
            setSelection(player, clickedEndpoint);
            player.displayClientMessage(
                    Component.literal("Energy Binder start selected. Right-click a second Binder Mount.")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
            return;
        }

        EnergyBinderEndpoint firstEndpoint = existingSelection.get();

        if (firstEndpoint.equals(clickedEndpoint)) {
            clearSelection(player);
            player.displayClientMessage(
                    Component.literal("Energy Binder selection cancelled.").withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (!firstEndpoint.dimension().equals(clickedEndpoint.dimension())) {
            setSelection(player, clickedEndpoint);
            player.displayClientMessage(
                    Component.literal("Binder Mounts must be in the same dimension. Selection moved to this mount.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        EnergyBinderSavedData data = EnergyBinderSavedData.get(level);
        boolean alreadyExists = data.findConnection(firstEndpoint, clickedEndpoint).isPresent();

        EnergyBinderConnection connection = data.addConnection(firstEndpoint, clickedEndpoint);
        clearSelection(player);

        if (alreadyExists) {
            player.displayClientMessage(
                    Component.literal("Those Binder Mounts are already connected.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        EnergyBinderSync.sendToAll(level);

        player.displayClientMessage(
                Component.literal("Energy Binder connected. Target distance: ")
                        .withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(String.format("%.2f blocks", connection.targetDistance()))
                                .withStyle(ChatFormatting.WHITE)),
                true
        );
    }
}