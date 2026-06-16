package net.droingo.podracing.client.hover;

import net.droingo.podracing.content.hover.HoverRepulsorBlockEntity;
import net.droingo.podracing.content.hover.menu.HoverRepulsorMenu;
import net.droingo.podracing.network.payload.UpdateHoverRepulsorConfigPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public final class HoverRepulsorScreen extends AbstractContainerScreen<HoverRepulsorMenu> {
    private static final int TEXT = 0xE8E0C8;
    private static final int MUTED_TEXT = 0xB9AA8B;

    public HoverRepulsorScreen(HoverRepulsorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        imageWidth = 220;
        imageHeight = 238;

        titleLabelX = 10;
        titleLabelY = 8;

        inventoryLabelX = 29;
        inventoryLabelY = 143;
    }

    @Override
    protected void init() {
        super.init();

        addRowButtons(0, HoverRepulsorBlockEntity.CONFIG_TARGET_HEIGHT, -0.25D, 0.25D);
        addRowButtons(1, HoverRepulsorBlockEntity.CONFIG_DAMPING, -0.10D, 0.10D);
        addRowButtons(2, HoverRepulsorBlockEntity.CONFIG_SUSPENSION_TRAVEL, -0.05D, 0.05D);
        addRowButtons(3, HoverRepulsorBlockEntity.CONFIG_CATCH_RANGE, -0.10D, 0.10D);
        addRowButtons(4, HoverRepulsorBlockEntity.CONFIG_MAX_IMPULSE, -5.0D, 5.0D);
        addRowButtons(5, HoverRepulsorBlockEntity.CONFIG_STRENGTH, -0.25D, 0.25D);
    }

    private void addRowButtons(int row, int parameter, double minusAmount, double plusAmount) {
        int y = topPos + 64 + row * 13;

        addRenderableWidget(Button.builder(
                Component.literal("-"),
                button -> change(parameter, minusAmount)
        ).bounds(leftPos + 170, y - 2, 20, 14).build());

        addRenderableWidget(Button.builder(
                Component.literal("+"),
                button -> change(parameter, plusAmount)
        ).bounds(leftPos + 194, y - 2, 20, 14).build());
    }

    private void change(int parameter, double amount) {
        double current = menu.getConfigValue(parameter);
        double next = current + amount;

        next = clampForParameter(parameter, next);

        PacketDistributor.sendToServer(new UpdateHoverRepulsorConfigPayload(
                menu.containerId,
                parameter,
                next
        ));
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
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);

        graphics.drawString(font, Component.literal("Redstone Link"), 28, 22, 0xDDD5FF, false);

        drawRow(graphics, 0, "Target Height", HoverRepulsorBlockEntity.CONFIG_TARGET_HEIGHT, "blocks");
        drawRow(graphics, 1, "Damping", HoverRepulsorBlockEntity.CONFIG_DAMPING, "x");
        drawRow(graphics, 2, "Cushion Travel", HoverRepulsorBlockEntity.CONFIG_SUSPENSION_TRAVEL, "blocks");
        drawRow(graphics, 3, "Catch Range", HoverRepulsorBlockEntity.CONFIG_CATCH_RANGE, "blocks");
        drawRow(graphics, 4, "Max Impulse", HoverRepulsorBlockEntity.CONFIG_MAX_IMPULSE, "");
        drawRow(graphics, 5, "Strength", HoverRepulsorBlockEntity.CONFIG_STRENGTH, "x");

        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);
    }

    private void drawRow(GuiGraphics graphics, int row, String label, int parameter, String suffix) {
        int y = 65 + row * 13;
        double value = menu.getConfigValue(parameter);

        graphics.drawString(font, Component.literal(label), 14, y, TEXT, false);

        String text = format(value);

        if (!suffix.isBlank()) {
            text += " " + suffix;
        }

        graphics.drawString(font, Component.literal(text), 108, y, MUTED_TEXT, false);
    }

    private static void drawCreateStyleBackground(GuiGraphics graphics, int x, int y) {
        rect(graphics, x + 3, y + 4, 220, 238, 0x66000000);

        panel(graphics, x, y, 220, 238, 0xFF1B1512, 0xFF5C3A22, 0xFF000000);

        panel(graphics, x + 2, y + 2, 216, 17, 0xFFE5B85D, 0xFFFFE19A, 0xFF6E2B28);
        rect(graphics, x + 3, y + 16, 214, 3, 0xFF4C1D1C);

        panel(graphics, x + 6, y + 22, 208, 35, 0xFF2A100B, 0xFF5A2A16, 0xFF0B0504);

        rect(graphics, x + 10, y + 26, 200, 27, 0xFF120806);
        rect(graphics, x + 12, y + 28, 196, 23, 0xFF28100B);
        rect(graphics, x + 14, y + 30, 192, 19, 0xFF3B1B12);

        panel(graphics, x + 70, y + 33, 60, 23, 0xFF666666, 0xFFBFBFBF, 0xFF2A2A2A);
        panel(graphics, x + 74, y + 34, 22, 22, 0xFF9E3838, 0xFFC95757, 0xFF5A1D1D);
        panel(graphics, x + 98, y + 34, 22, 22, 0xFF3B65B5, 0xFF6C91E8, 0xFF1A3160);

        drawSlot(graphics, x + 75, y + 35);
        drawSlot(graphics, x + 99, y + 35);

        panel(graphics, x + 6, y + 59, 208, 79, 0xFF2A100B, 0xFF5A2A16, 0xFF0B0504);

        for (int row = 0; row < HoverRepulsorBlockEntity.CONFIG_COUNT; row++) {
            int rowY = y + 63 + row * 13;
            rect(graphics, x + 10, rowY - 2, 200, 12, row % 2 == 0 ? 0x33251008 : 0x22100604);
        }

        panel(graphics, x + 24, y + 150, 172, 84, 0xFFE1E1E1, 0xFFFFFFFF, 0xFF606060);
        rect(graphics, x + 27, y + 153, 166, 78, 0xFFBDBDBD);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventorySlot(graphics, x + 28 + column * 18, y + 153 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawInventorySlot(graphics, x + 28 + column * 18, y + 211);
        }
    }

    private static String format(double value) {
        if (Math.abs(value) >= 100.0D) {
            return String.format("%.0f", value);
        }

        if (Math.abs(value) >= 10.0D) {
            return String.format("%.1f", value);
        }

        return String.format("%.2f", value);
    }

    private static double clampForParameter(int parameter, double value) {
        return switch (parameter) {
            case HoverRepulsorBlockEntity.CONFIG_TARGET_HEIGHT -> clamp(value, 0.5D, 16.0D);
            case HoverRepulsorBlockEntity.CONFIG_DAMPING -> clamp(value, 0.25D, 3.0D);
            case HoverRepulsorBlockEntity.CONFIG_SUSPENSION_TRAVEL -> clamp(value, 0.25D, 2.0D);
            case HoverRepulsorBlockEntity.CONFIG_CATCH_RANGE -> clamp(value, 0.25D, 4.0D);
            case HoverRepulsorBlockEntity.CONFIG_MAX_IMPULSE -> clamp(value, 5.0D, 5000.0D);
            case HoverRepulsorBlockEntity.CONFIG_STRENGTH -> clamp(value, 0.25D, 12.0D);
            default -> value;
        };
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

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
    }
}