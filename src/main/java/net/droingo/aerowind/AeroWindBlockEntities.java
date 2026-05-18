package net.droingo.aerowind;

import net.droingo.aerowind.blockentity.SealedPontoonBlockEntity;
import net.droingo.aerowind.blockentity.WindProjectorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AeroWindBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(
                    net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    AeroWind.MOD_ID
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindProjectorBlockEntity>> WIND_PROJECTOR =
            BLOCK_ENTITIES.register(
                    "wind_projector",
                    () -> BlockEntityType.Builder.of(
                            WindProjectorBlockEntity::new,
                            AeroWindBlocks.WIND_PROJECTOR.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SealedPontoonBlockEntity>> SEALED_PONTOON =
            BLOCK_ENTITIES.register("sealed_pontoon", () ->
                    BlockEntityType.Builder.of(
                            SealedPontoonBlockEntity::new,
                            AeroWindBlocks.SEALED_PONTOON.get()
                    ).build(null)
            );

    private AeroWindBlockEntities() {
    }
}