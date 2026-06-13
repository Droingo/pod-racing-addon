package net.droingo.podracing.registry;

import net.droingo.podracing.PodRacingAddon;
import net.droingo.podracing.content.binder.BinderMountBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PRBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, PodRacingAddon.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BinderMountBlockEntity>> BINDER_MOUNT =
            BLOCK_ENTITIES.register("binder_mount", () -> BlockEntityType.Builder.of(
                    BinderMountBlockEntity::new,
                    PRBlocks.BINDER_MOUNT.get()
            ).build(null));

    private PRBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}