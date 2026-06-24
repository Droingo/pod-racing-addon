package net.droingo.podracing.content.pilot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class PodPilotInputState {
    private static final int MAX_INPUT_AGE_TICKS = 12;

    private static final Map<UUID, Command> COMMANDS = new HashMap<>();

    private PodPilotInputState() {
    }

    public static void updateFromPlayer(
            ServerPlayer player,
            boolean active,
            float pitch,
            float roll,
            float yaw,
            BlockPos controlCorePos,
            UUID controlFrequency
    ) {
        if (!active) {
            COMMANDS.remove(player.getUUID());
            return;
        }

        pitch = clamp(pitch, -1.0F, 1.0F);
        roll = clamp(roll, -1.0F, 1.0F);
        yaw = clamp(yaw, -1.0F, 1.0F);

        COMMANDS.put(player.getUUID(), new Command(
                player.getUUID(),
                player.level().dimension(),
                player.position(),
                pitch,
                roll,
                yaw,
                controlCorePos,
                controlFrequency,
                player.level().getGameTime()
        ));
    }

    /*
     * Prototype fallback used by old test thrusters.
     */
    public static Command findLatestCommand(Level level) {
        return findLatestCommand(level, null, false);
    }

    /*
     * Frequency-safe command lookup used by Attitude Fins.
     */
    public static Command findLatestCommand(Level level, UUID requiredFrequency) {
        if (requiredFrequency == null) {
            return null;
        }

        return findLatestCommand(level, requiredFrequency, true);
    }

    public static Command findCommandForPlayer(Level level, UUID playerId) {
        long now = level.getGameTime();

        Command command = COMMANDS.get(playerId);

        if (command == null) {
            return null;
        }

        if (now - command.gameTime() > MAX_INPUT_AGE_TICKS) {
            COMMANDS.remove(playerId);
            return null;
        }

        if (!command.dimension().equals(level.dimension())) {
            return null;
        }

        return command;
    }

    private static Command findLatestCommand(Level level, UUID requiredFrequency, boolean frequencyRequired) {
        long now = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();

        Command newest = null;

        Iterator<Map.Entry<UUID, Command>> iterator = COMMANDS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Command> entry = iterator.next();
            Command command = entry.getValue();

            if (now - command.gameTime() > MAX_INPUT_AGE_TICKS) {
                iterator.remove();
                continue;
            }

            if (!command.dimension().equals(dimension)) {
                continue;
            }

            if (frequencyRequired) {
                if (command.controlFrequency() == null) {
                    continue;
                }

                if (!command.controlFrequency().equals(requiredFrequency)) {
                    continue;
                }
            }

            if (newest == null || command.gameTime() > newest.gameTime()) {
                newest = command;
            }
        }

        return newest;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }

    public record Command(
            UUID playerId,
            ResourceKey<Level> dimension,
            Vec3 position,
            float pitch,
            float roll,
            float yaw,
            BlockPos controlCorePos,
            UUID controlFrequency,
            long gameTime
    ) {
    }
}