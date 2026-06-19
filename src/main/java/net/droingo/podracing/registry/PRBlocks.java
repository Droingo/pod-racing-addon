package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.BinderMountBlock;
import net.droingo.podracing.content.trophy.PodRaceTrophyBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.droingo.podracing.content.hover.HoverRepulsorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.droingo.podracing.content.airbrake.AirBrakeBlock;

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
    public static final DeferredBlock<HoverRepulsorBlock> HOVER_REPULSOR =
            BLOCKS.register("hover_repulsor", () -> new HoverRepulsorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<PodRaceTrophyBlock> POD_RACE_TROPHY =
            BLOCKS.registerBlock(
                    "pod_race_trophy",
                    PodRaceTrophyBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredBlock<AirBrakeBlock> AIR_BRAKE =
            BLOCKS.registerBlock(
                    "air_brake",
                    AirBrakeBlock::new,
                    BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    private PRBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}