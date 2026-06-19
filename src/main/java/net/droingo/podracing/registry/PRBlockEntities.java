package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.airbrake.AirBrakeBlockEntity;
import net.droingo.podracing.content.binder.BinderMountBlockEntity;
import net.droingo.podracing.content.stabilizer.PodStabilizerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.droingo.podracing.content.hover.HoverRepulsorBlockEntity;

public final class PRBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PodRacingAddon.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BinderMountBlockEntity>> BINDER_MOUNT =
            BLOCK_ENTITIES.register("binder_mount", () -> BlockEntityType.Builder.of(
                    BinderMountBlockEntity::new,
                    PRBlocks.BINDER_MOUNT.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoverRepulsorBlockEntity>> HOVER_REPULSOR =
            BLOCK_ENTITIES.register("hover_repulsor", () -> BlockEntityType.Builder.of(
                    HoverRepulsorBlockEntity::new,
                    PRBlocks.HOVER_REPULSOR.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AirBrakeBlockEntity>> AIR_BRAKE =
            BLOCK_ENTITIES.register("air_brake", () ->
                    BlockEntityType.Builder.of(
                            AirBrakeBlockEntity::new,
                            PRBlocks.AIR_BRAKE.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PodStabilizerBlockEntity>> POD_STABILIZER =
            BLOCK_ENTITIES.register("pod_stabilizer", () ->
                    BlockEntityType.Builder.of(
                            PodStabilizerBlockEntity::new,
                            PRBlocks.POD_STABILIZER.get()
                    ).build(null)
            );

    private PRBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}