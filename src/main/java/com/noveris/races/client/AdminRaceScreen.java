package com.noveris.races.client;

import com.noveris.races.Race;
import com.noveris.races.RaceSize;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

/** Painel separado para raças administrativas. A autorização real fica no servidor. */
public final class AdminRaceScreen extends NoverisScreen {
    private Race selected = Race.GOD;
    private RaceSize size = RaceSize.MEDIUM;
    private int godX, npcX, sizeX, sizeY, actionX, actionY;

    public AdminRaceScreen() { super("Raças administrativas"); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean shouldCloseOnEsc() { return true; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        frame(g, "RAÇAS ADMINISTRATIVAS");
        g.drawString(font, "SOMENTE OPERADORES", left + panelWidth - 190, top + 28, DANGER, false);
        divider(g, top + 56);
        int cardY = top + 86, cardW = 260, gap = 18;
        godX = left + panelWidth / 2 - cardW - gap / 2; npcX = left + panelWidth / 2 + gap / 2;
        card(g, godX, cardY, cardW, "DEUS", Race.GOD, "1000 de vida (500 corações)", "Regeneração VII • Resistência III", mx, my);
        card(g, npcX, cardY, cardW, "NPC", Race.NPC, "80 de vida (40 corações)", "Regeneração III • Resistência II", mx, my);
        int infoY = top + 150;
        g.drawCenteredString(font, "RAÇA: " + selected.title.toUpperCase(), left + panelWidth / 2, infoY, selected.color);
        g.drawCenteredString(font, selected == Race.GOD ? "Julgamento Divino • Teleporte Celestial" : "Investida de Guarda", left + panelWidth / 2, infoY + 22, LILAC);
        g.drawCenteredString(font, selected == Race.GOD ? "Raio a 20 blocos (7 corações) e teleporte a 28 blocos." : "Investida com empurrão e 1,5 coração de dano.", left + panelWidth / 2, infoY + 40, MUTED);
        // O bloco de porte ocupa uma faixa própria no rodapé. O rótulo fica
        // acima dos botões e nunca reutiliza a linha das habilidades.
        sizeY = top + panelHeight - 90; int sizeW = 100, total = sizeW * 3 + 12; sizeX = left + (panelWidth - total) / 2;
        g.drawCenteredString(font, "ALTURA  " + Math.round(selected.scale(size) * 100) + "%", left + panelWidth / 2, sizeY + 34, WHITE);
        sizeOption(g, sizeX, sizeY, sizeW, "MENOR", RaceSize.SMALL, mx, my);
        sizeOption(g, sizeX + sizeW + 6, sizeY, sizeW, "MÉDIO", RaceSize.MEDIUM, mx, my);
        sizeOption(g, sizeX + (sizeW + 6) * 2, sizeY, sizeW, "MAIOR", RaceSize.LARGE, mx, my);
        actionX = left + panelWidth / 2 - 130; actionY = top + panelHeight - 38;
        button(g, actionX, actionY, 260, 26, "INICIAR TESTE ADMIN", mx, my, true);
    }

    private void card(GuiGraphics g, int x, int y, int w, String label, Race race, String health, String effects, int mx, int my) {
        boolean active = selected == race, hover = inside(mx, my, x, y, w, 60);
        g.fill(x, y, x + w, y + 60, active ? WINE : hover ? WINE_HOVER : 0xFF17140E);
        g.drawCenteredString(font, label, x + w / 2, y + 10, active ? WHITE : MUTED);
        g.drawCenteredString(font, health, x + w / 2, y + 28, active ? WHITE : LILAC);
        g.drawCenteredString(font, effects, x + w / 2, y + 44, active ? WHITE : MUTED);
    }
    private void sizeOption(GuiGraphics g, int x, int y, int w, String label, RaceSize value, int mx, int my) {
        boolean active = size == value, hover = inside(mx, my, x, y, w, 22);
        g.fill(x, y, x + w, y + 22, active ? WINE : hover ? WINE_HOVER : 0xFF17140E);
        g.drawCenteredString(font, label, x + w / 2, y + 7, active ? WHITE : MUTED);
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        int cardY = top + 86, cardW = 260, gap = 18;
        if (inside(mx, my, godX, cardY, cardW, 60)) { selected = Race.GOD; return true; }
        if (inside(mx, my, npcX, cardY, cardW, 60)) { selected = Race.NPC; return true; }
        int sizeW = 100;
        if (inside(mx, my, sizeX, sizeY, sizeW, 22)) { size = RaceSize.SMALL; return true; }
        if (inside(mx, my, sizeX + sizeW + 6, sizeY, sizeW, 22)) { size = RaceSize.MEDIUM; return true; }
        if (inside(mx, my, sizeX + (sizeW + 6) * 2, sizeY, sizeW, 22)) { size = RaceSize.LARGE; return true; }
        if (inside(mx, my, actionX, actionY, 260, 26)) {
            PacketDistributor.sendToServer(new ActionPayload("trial", selected.name(), "NONE", "NONE", "NONE", "NONE", size.name()));
            minecraft.setScreen(null); return true;
        }
        return super.mouseClicked(mx, my, button);
    }
    private boolean inside(double x, double y, int bx, int by, int bw, int bh) { return x >= bx && x < bx + bw && y >= by && y < by + bh; }
}
