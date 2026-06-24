package net.droingo.podracing.client.towcable;

import net.droingo.podracing.content.towcable.menu.TowCableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class TowCableScreen extends AbstractContainerScreen<TowCableMenu> {
    private static final int TEXT = 0xFFE8E0C8;
    private static final int MUTED = 0xFFB9AA88;
    private static final int ACTIVE = 0xFF7FD7FF;

    public TowCableScreen(TowCableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 222;
        imageHeight = 164;
        titleLabelX = 8;
        titleLabelY = 8;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();

        int x = leftPos + 12;
        int y = topPos + 38;

        addRenderableWidget(Button.builder(Component.literal("-1.0"), b -> sendButton(TowCableMenu.BUTTON_LENGTH_MINUS_1))
                .bounds(x, y, 38, 20).build());

        addRenderableWidget(Button.builder(Component.literal("-0.1"), b -> sendButton(TowCableMenu.BUTTON_LENGTH_MINUS_01))
                .bounds(x + 42, y, 38, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Use Current"), b -> sendButton(TowCableMenu.BUTTON_USE_CURRENT))
                .bounds(x + 84, y, 82, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+0.1"), b -> sendButton(TowCableMenu.BUTTON_LENGTH_PLUS_01))
                .bounds(x + 170, y, 38, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+1.0"), b -> sendButton(TowCableMenu.BUTTON_LENGTH_PLUS_1))
                .bounds(x, y + 26, 38, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Copy Len"), b -> sendButton(TowCableMenu.BUTTON_COPY_LENGTH))
                .bounds(x + 42, y + 26, 82, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Paste Len"), b -> sendButton(TowCableMenu.BUTTON_PASTE_LENGTH))
                .bounds(x + 128, y + 26, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Stiff -"), b -> sendButton(TowCableMenu.BUTTON_STIFFNESS_MINUS))
                .bounds(x, y + 62, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Stiff +"), b -> sendButton(TowCableMenu.BUTTON_STIFFNESS_PLUS))
                .bounds(x + 66, y + 62, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Damp -"), b -> sendButton(TowCableMenu.BUTTON_DAMPING_MINUS))
                .bounds(x, y + 88, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Damp +"), b -> sendButton(TowCableMenu.BUTTON_DAMPING_PLUS))
                .bounds(x + 66, y + 88, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Min -"), b -> sendButton(TowCableMenu.BUTTON_MINSEP_MINUS))
                .bounds(x + 146, y + 62, 62, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Min +"), b -> sendButton(TowCableMenu.BUTTON_MINSEP_PLUS))
                .bounds(x + 146, y + 88, 62, 20).build());
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
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);

        graphics.drawString(
                font,
                Component.literal("Linked: " + (menu.isLinked() ? "YES" : "NO")),
                138,
                10,
                menu.isLinked() ? ACTIVE : MUTED,
                false
        );

        graphics.drawString(
                font,
                Component.literal(String.format("Rest Length: %.2f blocks", menu.getRestLength())),
                12,
                24,
                TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(String.format("Stiffness: %.2f", menu.getStiffness())),
                12,
                124,
                TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(String.format("Damping: %.2f", menu.getDamping())),
                12,
                136,
                TEXT,
                false
        );

        graphics.drawString(
                font,
                Component.literal(String.format("Min Separation: %.2f", menu.getMinSeparation())),
                112,
                124,
                TEXT,
                false
        );
    }
}