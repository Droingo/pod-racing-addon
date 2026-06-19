package net.droingo.podracing.client.airbrake;

import net.droingo.podracing.content.airbrake.menu.AirBrakeMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AirBrakeScreen extends AbstractContainerScreen<AirBrakeMenu> {
    private static final int TEXT = 0xE8E0C8;
    private static final int MUTED_TEXT = 0xB9AA88;

    public AirBrakeScreen(AirBrakeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        imageWidth = 176;
        imageHeight = 168;

        titleLabelX = 8;
        titleLabelY = 8;

        inventoryLabelX = 8;
        inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF1B0E0A);
        graphics.fill(x + 4, y + 4, x + imageWidth - 4, y + 18, 0xFF6B3A1C);
        graphics.fill(x + 6, y + 6, x + imageWidth - 6, y + 16, 0xFFD6A34A);

        graphics.fill(x + 8, y + 26, x + imageWidth - 8, y + 66, 0xFF2A120C);
        graphics.drawString(font, "Redstone Link", x + 50, y + 24, TEXT, false);

        drawSlot(graphics, x + 75, y + 35);
        drawSlot(graphics, x + 99, y + 35);

        drawInventorySlots(graphics, x + 7, y + 85);
        drawHotbarSlots(graphics, x + 7, y + 143);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 18, y + 18, 0xFF0D0806);
        graphics.fill(x, y, x + 17, y + 17, 0xFF777777);
        graphics.fill(x + 2, y + 2, x + 15, y + 15, 0xFFB0B0B0);
    }

    private void drawInventorySlots(GuiGraphics graphics, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, x + column * 18, y + row * 18);
            }
        }
    }

    private void drawHotbarSlots(GuiGraphics graphics, int x, int y) {
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, x + column * 18, y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED_TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}