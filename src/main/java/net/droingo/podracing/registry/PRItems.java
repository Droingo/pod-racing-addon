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

    public static final DeferredItem<BlockItem> POD_RACE_TROPHY =
            ITEMS.registerSimpleBlockItem(PRBlocks.POD_RACE_TROPHY);

    public static final DeferredItem<BlockItem> HOVER_REPULSOR =
            ITEMS.registerSimpleBlockItem(PRBlocks.HOVER_REPULSOR);

    public static final DeferredItem<BlockItem> AIR_BRAKE =
            ITEMS.registerSimpleBlockItem(PRBlocks.AIR_BRAKE);

    public static final DeferredItem<BlockItem> POD_STABILIZER =
            ITEMS.registerSimpleBlockItem(PRBlocks.POD_STABILIZER);

    public static final DeferredItem<BlockItem> ROLL_TEST_THRUSTER =
            ITEMS.registerSimpleBlockItem(PRBlocks.ROLL_TEST_THRUSTER);

    public static final DeferredItem<BlockItem> POD_CONTROL_CORE =
            ITEMS.registerSimpleBlockItem(PRBlocks.POD_CONTROL_CORE);
    public static final DeferredItem<BlockItem> ATTITUDE_FIN =
            ITEMS.registerSimpleBlockItem(PRBlocks.ATTITUDE_FIN);
    public static final DeferredItem<BlockItem> TOW_CABLE_ANCHOR =
            ITEMS.registerSimpleBlockItem(PRBlocks.TOW_CABLE_ANCHOR);

    private PRItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}