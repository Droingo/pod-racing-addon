package net.droingo.podracing;

import com.mojang.logging.LogUtils;
import net.droingo.podracing.registry.PRBlockEntities;
import net.droingo.podracing.registry.PRBlocks;
import net.droingo.podracing.registry.PRCreativeTabs;
import net.droingo.podracing.registry.PRItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(PodRacingAddon.MOD_ID)
public final class PodRacingAddon {
    public static final String MOD_ID = "pod_racing_addon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PodRacingAddon(IEventBus modBus) {
        PRBlocks.register(modBus);
        PRItems.register(modBus);
        PRBlockEntities.register(modBus);
        PRCreativeTabs.register(modBus);

        LOGGER.info("Pod Racing Addon loaded.");
    }
}