package net.droingo.aerowind;

import net.droingo.aerowind.block.TrophyBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.droingo.aerowind.block.WindProjectorBlock;
import net.droingo.aerowind.block.SealedPontoonBlock;
import net.droingo.aerowind.block.RigidLinkMountBlock;
import net.droingo.aerowind.item.RigidLinkRodItem;

public final class AeroWindBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(AeroWind.MOD_ID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(AeroWind.MOD_ID);

    public static final DeferredItem<RigidLinkRodItem> RIGID_LINK_ROD =
            ITEMS.register("rigid_link_rod", () -> new RigidLinkRodItem(new Item.Properties()));

    public static final DeferredBlock<Block> WIND_PROJECTOR = BLOCKS.register(
            "wind_projector",
            () -> new WindProjectorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            )
    );
    public static final DeferredBlock TROPHY = BLOCKS.register(
            "trophy",
            () -> new TrophyBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.GOLD)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
                            .lightLevel(state -> 5)
            )
    );
    public static final DeferredBlock<SealedPontoonBlock> SEALED_PONTOON =
            BLOCKS.register("sealed_pontoon", SealedPontoonBlock::new);
    public static final DeferredItem TROPHY_ITEM = ITEMS.registerSimpleBlockItem(AeroWindBlocks.TROPHY);

    public static final DeferredBlock<RigidLinkMountBlock> RIGID_LINK_MOUNT =
            BLOCKS.register("rigid_link_mount", RigidLinkMountBlock::new);

    public static final DeferredItem<BlockItem> WIND_PROJECTOR_ITEM =
            ITEMS.registerSimpleBlockItem(WIND_PROJECTOR, new Item.Properties());

    public static final DeferredItem<BlockItem> RIGID_LINK_MOUNT_ITEM =
            ITEMS.registerSimpleBlockItem(AeroWindBlocks.RIGID_LINK_MOUNT);

    public static final DeferredItem<BlockItem> SEALED_PONTOON_ITEM =
            ITEMS.registerSimpleBlockItem(AeroWindBlocks.SEALED_PONTOON);

    private AeroWindBlocks() {
    }
}