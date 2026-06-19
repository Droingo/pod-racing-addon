package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PRItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(PodRacingAddon.MOD_ID);

    public static final DeferredItem<BlockItem> BINDER_MOUNT =
            ITEMS.register("binder_mount", () -> new BlockItem(
                    PRBlocks.BINDER_MOUNT.get(),
                    new Item.Properties()

            ));

    public static final DeferredItem<BlockItem> HOVER_REPULSOR =
            ITEMS.registerSimpleBlockItem(PRBlocks.HOVER_REPULSOR);

    public static final DeferredItem<BlockItem> AIR_BRAKE =
            ITEMS.registerSimpleBlockItem(PRBlocks.AIR_BRAKE);


    private PRItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}