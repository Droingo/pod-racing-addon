package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.airbrake.AirBrakeBlock;
import net.droingo.podracing.content.binder.BinderMountBlock;
import net.droingo.podracing.content.hover.HoverRepulsorBlock;
import net.droingo.podracing.content.rolltest.RollTestThrusterBlock;
import net.droingo.podracing.content.stabilizer.PodStabilizerBlock;
import net.droingo.podracing.content.trophy.PodRaceTrophyBlock;
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

    public static final DeferredBlock<HoverRepulsorBlock> HOVER_REPULSOR =
            BLOCKS.register("hover_repulsor", () -> new HoverRepulsorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
            ));

    public static final DeferredBlock<PodStabilizerBlock> POD_STABILIZER =
            BLOCKS.registerBlock(
                    "pod_stabilizer",
                    PodStabilizerBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredBlock<PodRaceTrophyBlock> POD_RACE_TROPHY =
            BLOCKS.registerBlock(
                    "pod_race_trophy",
                    PodRaceTrophyBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredBlock<AirBrakeBlock> AIR_BRAKE =
            BLOCKS.registerBlock(
                    "air_brake",
                    AirBrakeBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            );

    public static final DeferredBlock<RollTestThrusterBlock> ROLL_TEST_THRUSTER =
            BLOCKS.registerBlock(
                    "roll_test_thruster",
                    RollTestThrusterBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
            );

    private PRBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
