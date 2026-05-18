package net.droingo.aerowind.link;

import com.mojang.brigadier.CommandDispatcher;
import net.droingo.aerowind.AeroWind;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RigidLinkCommands {
    private RigidLinkCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("aerowindrigidlinks")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("clear")
                                .executes(context -> clearLinks(context.getSource()))
                        )
                        .then(Commands.literal("count")
                                .executes(context -> countLinks(context.getSource()))
                        )
        );
    }

    private static int clearLinks(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        RigidLinkSavedData savedData = RigidLinkSavedData.get(level);

        int oldCount = savedData.linkCount();
        savedData.clearLinks();

        source.sendSuccess(
                () -> Component.literal("Cleared " + oldCount + " AeroWind rigid links in " + level.dimension().location()),
                true
        );

        AeroWind.LOGGER.info(
                "Rigid Link command cleared {} links in {}",
                oldCount,
                level.dimension().location()
        );

        return oldCount;
    }

    private static int countLinks(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        RigidLinkSavedData savedData = RigidLinkSavedData.get(level);

        int count = savedData.linkCount();

        source.sendSuccess(
                () -> Component.literal("AeroWind rigid links in " + level.dimension().location() + ": " + count),
                false
        );

        return count;
    }
}