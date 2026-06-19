package net.droingo.podracing.client.stabilizer;

import net.droingo.podracing.content.stabilizer.menu.PodStabilizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PodStabilizerScreen extends AbstractContainerScreen<PodStabilizerMenu> {
    public PodStabilizerScreen(PodStabilizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        imageWidth = 176;
        imageHeight = 92;

        titleLabelX = 8;
        titleLabelY = 8;

        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = topPos + 54;

        addRenderableWidget(Button.builder(
                Component.literal("-5"),
                button -> sendButton(PodStabilizerMenu.BUTTON_STRENGTH_MINUS_5)
        ).bounds(leftPos + 26, buttonY, 28, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("-"),
                button -> sendButton(PodStabilizerMenu.BUTTON_STRENGTH_MINUS_1)
        ).bounds(leftPos + 58, buttonY, 28, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("+"),
                button -> sendButton(PodStabilizerMenu.BUTTON_STRENGTH_PLUS_1)
        ).bounds(leftPos + 90, buttonY, 28, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("+5"),
                button -> sendButton(PodStabilizerMenu.BUTTON_STRENGTH_PLUS_5)
        ).bounds(leftPos + 122, buttonY, 28, 20).build());
    }

    private void sendButton(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xEE101010);
        guiGraphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, 0xEE252525);

        int strength = menu.getStrength();

        int barX = left + 18;
        int barY = top + 35;
        int barWidth = 140;
        int barHeight = 10;

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF111111);
        guiGraphics.fill(barX + 1, barY + 1, barX + barWidth - 1, barY + barHeight - 1, 0xFF3A3A3A);

        int filledWidth = Math.round((barWidth - 2) * (strength / 15.0F));
        guiGraphics.fill(barX + 1, barY + 1, barX + 1 + filledWidth, barY + barHeight - 1, 0xFF7FD7FF);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(
                font,
                title,
                titleLabelX,
                titleLabelY,
                0xFFFFFF,
                false
        );

        int strength = menu.getStrength();

        guiGraphics.drawString(
                font,
                Component.literal("Stabilizer strength: " + strength + " / 15"),
                18,
                22,
                0xDADADA,
                false
        );

        guiGraphics.drawString(
                font,
                Component.literal("Higher = more drag stability"),
                18,
                76,
                0x8A8A8A,
                false
        );
    }
}