package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.menu.BinderMountMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PRMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, PodRacingAddon.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<BinderMountMenu>> BINDER_MOUNT =
            MENUS.register("binder_mount", () -> new MenuType<>(
                    BinderMountMenu::new,
                    FeatureFlags.DEFAULT_FLAGS
            ));

    private PRMenuTypes() {
    }

    public static void register(IEventBus modBus) {
        MENUS.register(modBus);
    }
}