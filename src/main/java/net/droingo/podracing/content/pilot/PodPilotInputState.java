package net.droingo.podracing.content.pilot;

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

    public static void updateFromPlayer(ServerPlayer player, boolean active, float pitch, float roll, float yaw) {
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
                player.level().getGameTime()
        ));
    }

    public static Command findLatestCommand(Level level) {
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
            long gameTime
    ) {
    }
}