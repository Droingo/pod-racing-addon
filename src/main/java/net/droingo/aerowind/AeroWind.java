package net.droingo.aerowind;

import com.mojang.logging.LogUtils;
import net.droingo.aerowind.link.RigidLinkCommands;
import net.droingo.aerowind.link.RigidLinkServerEvents;
import net.droingo.aerowind.ragdoll.RagdollCommands;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(AeroWind.MOD_ID)
public final class AeroWind {
    public static final String MOD_ID = "aero_wind";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeroWind(IEventBus modEventBus) {
        AeroWindBlocks.BLOCKS.register(modEventBus);
        AeroWindBlocks.ITEMS.register(modEventBus);
        AeroWindBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(RigidLinkServerEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(RigidLinkCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(RagdollCommands::onRegisterCommands);

        LOGGER.info("Aero Wind loaded");
    }
}