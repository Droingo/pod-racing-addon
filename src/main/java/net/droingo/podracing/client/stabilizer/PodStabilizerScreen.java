package net.droingo.podracing.client.stabilizer;

import net.droingo.podracing.content.stabilizer.menu.PodStabilizerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class PodStabilizerScreen extends AbstractContainerScreen<PodStabilizerMenu> {
    public PodStabilizerScreen(PodStabilizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        imageWidth = 176;
        imageHeight = 100;

        titleLabelX = 8;
        titleLabelY = 8;

        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        int buttonY = topPos + 56;

        addRenderableWidget(Button.builder(
                Component.literal("X"),
                button -> sendButton(PodStabilizerMenu.BUTTON_AXIS_X)
        ).bounds(leftPos + 28, buttonY, 34, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Y"),
                button -> sendButton(PodStabilizerMenu.BUTTON_AXIS_Y)
        ).bounds(leftPos + 71, buttonY, 34, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Z"),
                button -> sendButton(PodStabilizerMenu.BUTTON_AXIS_Z)
        ).bounds(leftPos + 114, buttonY, 34, 20).build());
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

        int axisIndex = menu.getPhysicsAxisIndex();

        int indicatorY = top + 80;
        int indicatorWidth = 34;
        int indicatorHeight = 4;

        int indicatorX = switch (axisIndex) {
            case 1 -> left + 71;
            case 2 -> left + 114;
            default -> left + 28;
        };

        guiGraphics.fill(
                indicatorX,
                indicatorY,
                indicatorX + indicatorWidth,
                indicatorY + indicatorHeight,
                0xFF7FD7FF
        );
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

        Direction.Axis axis = menu.getPhysicsAxis();

        guiGraphics.drawString(
                font,
                Component.literal("Physics axis: " + axis.getName().toUpperCase()),
                18,
                24,
                0xDADADA,
                false
        );

        guiGraphics.drawString(
                font,
                Component.literal("Wrench rotates the visible fin."),
                18,
                38,
                0x9A9A9A,
                false
        );

        guiGraphics.drawString(
                font,
                Component.literal("GUI changes the stabilizer direction."),
                18,
                86,
                0x8A8A8A,
                false
        );
    }
}