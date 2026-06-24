package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PRCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PodRacingAddon.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.pod_racing_addon.main"))
                    .icon(() -> PRItems.BINDER_MOUNT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(PRItems.BINDER_MOUNT.get());
                        output.accept(PRItems.HOVER_REPULSOR.get());
                        output.accept(PRItems.AIR_BRAKE.get());
                        output.accept(PRItems.POD_STABILIZER.get());
                        output.accept(PRItems.ROLL_TEST_THRUSTER.get());
                        output.accept(PRItems.POD_CONTROL_CORE.get());
                        output.accept(PRItems.ATTITUDE_FIN.get());
                        output.accept(PRItems.TOW_CABLE_ANCHOR.get());
                    })
                    .build()
            );

    private PRCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_TABS.register(modBus);
    }
}