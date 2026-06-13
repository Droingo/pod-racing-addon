package net.droingo.podracing.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class PRItemChecks {
    private static final ResourceLocation CREATE_WRENCH =
            ResourceLocation.fromNamespaceAndPath("create", "wrench");

    private PRItemChecks() {
    }

    public static boolean isCreateWrench(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return CREATE_WRENCH.equals(itemId);
    }
}