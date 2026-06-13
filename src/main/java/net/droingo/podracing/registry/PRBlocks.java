package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.BinderMountBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PRBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(PodRacingAddon.MOD_ID);

    public static final DeferredBlock<BinderMountBlock> BINDER_MOUNT =
            BLOCKS.register("binder_mount", () -> new BinderMountBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            ));

    private PRBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}