package com.noveris.races.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

abstract class NoverisScreen extends Screen {
    static final int BACKDROP = 0xEB0D0C09;
    static final int BORDER = 0xFFA97A1E;
    static final int WINE = 0xFF5A4317;
    static final int WINE_HOVER = 0xFF7A5A20;
    static final int LILAC = 0xFFF3E8C5;
    static final int MUTED = 0xFFB9AC8C;
    static final int WHITE = 0xFFFBF6E7;
    static final int DANGER = 0xFFE6B83F;
    int left, top, panelWidth, panelHeight;

    NoverisScreen(String title) { super(Component.literal(title)); }

    void frame(GuiGraphics g, String title) {
        g.fill(0, 0, width, height, 0x80060402);
        panelWidth = Math.min(width - 32, 1080);
        panelHeight = Math.min(height - 30, 650);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        g.fill(left, top, left + panelWidth, top + panelHeight, BORDER);
        g.fill(left + 3, top + 3, left + panelWidth - 3, top + panelHeight - 3, BACKDROP);
        g.drawString(font, title, left + 34, top + 28, WHITE, false);
        g.drawString(font, "+", left + 11, top + 8, BORDER, false);
        g.drawString(font, "+", left + panelWidth - 18, top + 8, BORDER, false);
        g.drawString(font, "+", left + 11, top + panelHeight - 18, BORDER, false);
        g.drawString(font, "+", left + panelWidth - 18, top + panelHeight - 18, BORDER, false);
    }

    boolean button(GuiGraphics g, int x, int y, int w, int h, String text, double mx, double my, boolean enabled) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        g.fill(x, y, x + w, y + h, enabled ? (hover ? WINE_HOVER : WINE) : 0xFF1B1811);
        int color = enabled ? WHITE : MUTED;
        g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, color);
        return hover;
    }

    void divider(GuiGraphics g, int y) { g.fill(left + 34, y, left + panelWidth - 34, y + 2, 0xFFA97A1E); }
}
