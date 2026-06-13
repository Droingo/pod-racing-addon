package net.droingo.podracing.client.binder;

import net.droingo.podracing.content.binder.menu.BinderMountMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BinderMountScreen extends AbstractContainerScreen<BinderMountMenu> {
    public BinderMountScreen(BinderMountMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 7;
        inventoryLabelX = 8;
        inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xDD11111A);
        graphics.fill(x + 7, y + 7, x + imageWidth - 7, y + 70, 0xDD202033);
        graphics.fill(x + 72, y + 24, x + 124, y + 53, 0xAA000000);
        graphics.fill(x + 79, y + 30, x + 97, y + 48, 0xFF3A3A4A);
        graphics.fill(x + 97, y + 30, x + 115, y + 48, 0xFF3A3A4A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFF, false);
        graphics.drawString(font, Component.literal("Frequencies"), 64, 18, 0xCFCFFF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF, false);
    }
}