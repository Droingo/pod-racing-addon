package net.droingo.podracing;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(PodRacingAddon.MOD_ID)
public final class PodRacingAddon {
    public static final String MOD_ID = "pod_racing_addon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PodRacingAddon() {
        LOGGER.info("Pod Racing Addon loaded.");
    }
}