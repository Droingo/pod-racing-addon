package net.droingo.podracing.client.attitudefin;

import net.droingo.podracing.content.attitudefin.menu.AttitudeFinMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AttitudeFinScreen extends AbstractContainerScreen<AttitudeFinMenu> {
    private static final int TEXT = 0xFFE8E0C8;
    private static final int MUTED = 0xFFB9AA88;
    private static final int ACTIVE = 0xFF7FD7FF;

    public AttitudeFinScreen(AttitudeFinMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 190;
        imageHeight = 136;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 14;
        int y = topPos + 38;

        addRenderableWidget(Button.builder(
                Component.literal("Roll Left"),
                button -> sendButton(AttitudeFinMenu.BUTTON_ROLL_LEFT)
        ).bounds(x, y, 76, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Roll Right"),
                button -> sendButton(AttitudeFinMenu.BUTTON_ROLL_RIGHT)
        ).bounds(x + 86, y, 76, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Front Control"),
                button -> sendButton(AttitudeFinMenu.BUTTON_FRONT_CONTROL)
        ).bounds(x, y + 26, 76, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Rear Control"),
                button -> sendButton(AttitudeFinMenu.BUTTON_REAR_CONTROL)
        ).bounds(x + 86, y + 26, 76, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Reverse"),
                button -> sendButton(AttitudeFinMenu.BUTTON_TOGGLE_REVERSED)
        ).bounds(x, y + 58, 76, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Bind Core"),
                button -> sendButton(AttitudeFinMenu.BUTTON_BIND_ACTIVE_CORE)
        ).bounds(x + 86, y + 58, 76, 20).build());
    }

    private void sendButton(int buttonId) {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }

        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xEE101010);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, 0xEE252525);

        int roleIndex = menu.getRoleIndex();

        if (roleIndex == 0) {
            drawIndicator(graphics, left + 14, top + 60);
        } else if (roleIndex == 1) {
            drawIndicator(graphics, left + 100, top + 60);
        } else if (roleIndex == 2) {
            drawIndicator(graphics, left + 14, top + 86);
        } else if (roleIndex == 3) {
            drawIndicator(graphics, left + 100, top + 86);
        }
    }

    private void drawIndicator(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 76, y + 3, ACTIVE);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);

        graphics.drawString(
                font,
                Component.literal("Role: " + menu.getRole().displayName()),
                14,
                20,
                TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal("Reverse: " + (menu.isReversed() ? "ON" : "OFF")),
                14,
                116,
                menu.isReversed() ? ACTIVE : MUTED,
                false
        );

        graphics.drawString(
                font,
                Component.literal("Bound: " + (menu.isBound() ? "YES" : "NO")),
                104,
                116,
                menu.isBound() ? ACTIVE : MUTED,
                false
        );
    }
}