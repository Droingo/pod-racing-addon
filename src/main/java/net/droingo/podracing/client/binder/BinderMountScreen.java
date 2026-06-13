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
        titleLabelY = 6;

        inventoryLabelX = 8;
        inventoryLabelY = 74;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        if (isHovering(76, 36, 18, 18, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Frequency 1"), mouseX, mouseY);
        }

        if (isHovering(100, 36, 18, 18, mouseX, mouseY)) {
            graphics.renderTooltip(font, Component.literal("Frequency 2"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawCreateStyleBackground(graphics, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0x3A2A18, false);
        graphics.drawString(font, Component.literal("Frequencies"), 44, 20, 0xDDD5FF, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private static void drawCreateStyleBackground(GuiGraphics graphics, int x, int y) {
        // Outer shadow
        rect(graphics, x + 3, y + 4, 176, 166, 0x66000000);

        // Main outer body
        panel(graphics, x, y, 176, 166, 0xFF1B1512, 0xFF000000, 0xFF5C3A22);

        // Brass header
        panel(graphics, x + 2, y + 2, 172, 17, 0xFFE5B85D, 0xFFFFE19A, 0xFF6E2B28);
        rect(graphics, x + 3, y + 16, 170, 3, 0xFF4C1D1C);

        // Upper housing
        panel(graphics, x + 6, y + 22, 164, 44, 0xFF2A100B, 0xFF5A2A16, 0xFF0B0504);

        // Inner machine inset
        rect(graphics, x + 10, y + 26, 156, 36, 0xFF120806);
        rect(graphics, x + 12, y + 28, 152, 32, 0xFF28100B);
        rect(graphics, x + 14, y + 30, 148, 28, 0xFF3B1B12);

        // Grey frequency plate
        panel(graphics, x + 24, y + 34, 128, 24, 0xFF666666, 0xFFBFBFBF, 0xFF2A2A2A);
        rect(graphics, x + 26, y + 36, 124, 20, 0xFF8A8A8A);

        // Left detail strip
        panel(graphics, x + 30, y + 38, 36, 12, 0xFF9B9B9B, 0xFFD8D8D8, 0xFF555555);
        rect(graphics, x + 33, y + 41, 30, 6, 0xFFBFBFBF);

        // Red / blue frequency pads
        panel(graphics, x + 74, y + 34, 22, 22, 0xFF9E3838, 0xFFC95757, 0xFF5A1D1D);
        panel(graphics, x + 98, y + 34, 22, 22, 0xFF3B65B5, 0xFF6C91E8, 0xFF1A3160);

        // Slot frames directly on top of the color pads
        drawSlot(graphics, x + 75, y + 35);
        drawSlot(graphics, x + 99, y + 35);

        // Lower inventory panel
        panel(graphics, x + 6, y + 80, 164, 84, 0xFFE1E1E1, 0xFFFFFFFF, 0xFF606060);
        rect(graphics, x + 9, y + 83, 158, 78, 0xFFBDBDBD);

        // Inventory slots
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventorySlot(graphics, x + 7 + column * 18, y + 83 + row * 18);
            }
        }

        // Hotbar
        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, x + 7 + column * 18, y + 141);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        rect(graphics, x, y, 20, 20, 0xFF151515);
        rect(graphics, x + 1, y + 1, 18, 18, 0xFF4A4A55);
        rect(graphics, x + 2, y + 2, 16, 16, 0xFF7A7A7A);
        rect(graphics, x + 2, y + 2, 16, 1, 0xFFCFCFCF);
        rect(graphics, x + 2, y + 2, 1, 16, 0xFFCFCFCF);
        rect(graphics, x + 17, y + 2, 1, 16, 0xFF383838);
        rect(graphics, x + 2, y + 17, 16, 1, 0xFF383838);
    }

    private static void drawInventorySlot(GuiGraphics graphics, int x, int y) {
        rect(graphics, x, y, 18, 18, 0xFF505050);
        rect(graphics, x + 1, y + 1, 16, 16, 0xFF969696);
        rect(graphics, x + 1, y + 1, 16, 1, 0xFFD8D8D8);
        rect(graphics, x + 1, y + 1, 1, 16, 0xFFD8D8D8);
        rect(graphics, x + 16, y + 1, 1, 16, 0xFF555555);
        rect(graphics, x + 1, y + 16, 16, 1, 0xFF555555);
    }

    private static void panel(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            int fill,
            int highlight,
            int shadow
    ) {
        rect(graphics, x, y, width, height, shadow);
        rect(graphics, x + 1, y + 1, width - 2, height - 2, fill);
        rect(graphics, x + 1, y + 1, width - 2, 1, highlight);
        rect(graphics, x + 1, y + 1, 1, height - 2, highlight);
        rect(graphics, x + width - 2, y + 1, 1, height - 2, shadow);
        rect(graphics, x + 1, y + height - 2, width - 2, 1, shadow);
    }

    private static void rect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }
}