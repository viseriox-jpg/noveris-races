package com.noveris.races.client;

import com.noveris.races.*;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RaceSelectionScreen extends NoverisScreen {
    private Race selected = Race.TIEFLING;
    private DragonLineage lineage = DragonLineage.FIRE;
    private int startX, startY, startW, startH;
    private int lineageX, lineageY, lineageW, lineageH;

    public RaceSelectionScreen() { super("Linhagens de Noveris"); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        frame(g, "LINHAGENS DE NOVERIS");
        g.drawString(font, "ESCOLHA O SANGUE QUE GUIARÁ SUA JORNADA", left + panelWidth - 330, top + 28, DANGER, false);
        divider(g, top + 56);
        int symbolsY = top + 68;
        Race[] races = {Race.TIEFLING, Race.LYCANTHROPE, Race.DRAGONBORN, Race.HARPY};
        String[] symbols = {"♠", "☾", "◆", "⌁"};
        int cell = (panelWidth - 68) / 4;
        for (int i = 0; i < races.length; i++) {
            int x = left + 34 + i * cell;
            boolean active = selected == races[i];
            g.fill(x, symbolsY, x + cell - 10, symbolsY + 54, active ? WINE : 0xFF1B181E);
            drawLargeSymbol(g, symbols[i], x + (cell - 10) / 2, symbolsY + 7, races[i].color);
            g.drawCenteredString(font, races[i].title.toUpperCase(), x + (cell - 10) / 2, symbolsY + 36, active ? WHITE : MUTED);
        }
        int detailY = symbolsY + 68;
        int split = left + panelWidth / 2;
        g.drawString(font, selected.title.toUpperCase(), left + 44, detailY, selected.color, false);
        g.drawString(font, hearts(selected) + " CORAÇÕES  •  ALTURA " + Math.round(selected.scale * 100) + "%", left + 44, detailY + 16, LILAC, false);
        g.drawString(font, style(selected), left + 44, detailY + 32, MUTED, false);
        g.drawString(font, "HABILIDADE ATIVA", left + 44, detailY + 54, WHITE, false);
        g.drawString(font, primary(selected), left + 44, detailY + 68, LILAC, false);
        g.drawString(font, "MOBILIDADE", left + 44, detailY + 88, WHITE, false);
        g.drawString(font, mobility(selected), left + 44, detailY + 102, LILAC, false);
        g.drawString(font, "PASSIVAS", split, detailY, WHITE, false);
        drawLines(g, passives(selected), split, detailY + 16, LILAC);
        g.drawString(font, "FRAQUEZAS", split, detailY + 62, DANGER, false);
        drawLines(g, weaknesses(selected), split, detailY + 78, MUTED);
        if (selected == Race.DRAGONBORN) {
            g.drawString(font, "ESCOLHA A LINHAGEM", left + 44, detailY + 120, WHITE, false);
            lineageX = left + 44;
            lineageY = detailY + 136;
            lineageW = 72;
            lineageH = 22;
            lineageButton(g, lineageX, lineageY, "FOGO", DragonLineage.FIRE, 0xFFFF6754, mx, my);
            lineageButton(g, lineageX + lineageW + 6, lineageY, "GELO", DragonLineage.FROST, 0xFF80D9FF, mx, my);
            lineageButton(g, lineageX + (lineageW + 6) * 2, lineageY, "VENENO", DragonLineage.VENOM, 0xFF86D48A, mx, my);
        }
        startW = 260; startH = 28; startX = left + (panelWidth - startW) / 2; startY = top + panelHeight - 46;
        button(g, startX, startY, startW, startH, "INICIAR TESTE — 5:00", mx, my, true);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        // The primary action always wins over every secondary hit area.
        if (mx >= startX && mx < startX + startW && my >= startY && my < startY + startH) {
            PacketDistributor.sendToServer(new ActionPayload("trial", selected.name(), lineage.name()));
            minecraft.setScreen(null); return true;
        }
        int cell = (panelWidth - 68) / 4;
        int y = top + 68;
        Race[] races = {Race.TIEFLING, Race.LYCANTHROPE, Race.DRAGONBORN, Race.HARPY};
        for (int i = 0; i < races.length; i++) {
            int x = left + 34 + i * cell;
            if (mx >= x && mx < x + cell - 10 && my >= y && my < y + 54) { selected = races[i]; return true; }
        }
        if (selected == Race.DRAGONBORN && my >= lineageY && my < lineageY + lineageH) {
            if (mx >= lineageX && mx < lineageX + lineageW) lineage = DragonLineage.FIRE;
            else if (mx >= lineageX + lineageW + 6 && mx < lineageX + lineageW * 2 + 6) lineage = DragonLineage.FROST;
            else if (mx >= lineageX + (lineageW + 6) * 2 && mx < lineageX + lineageW * 3 + 12) lineage = DragonLineage.VENOM;
            else return super.mouseClicked(mx, my, button);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void drawLines(GuiGraphics g, String[] lines, int x, int y, int color) { for (int i=0;i<lines.length;i++) g.drawString(font, "• " + lines[i], x, y+i*13, color, false); }
    private void drawLargeSymbol(GuiGraphics g, String symbol, int centerX, int y, int color) {
        g.pose().pushPose();
        g.pose().translate(centerX, y, 0);
        g.pose().scale(1.6f, 1.6f, 1f);
        g.drawCenteredString(font, symbol, 0, 0, color);
        g.pose().popPose();
    }
    private int hearts(Race r) { return (int)r.maxHealth / 2; }
    private void lineageButton(GuiGraphics g, int x, int y, String label, DragonLineage value, int color, int mx, int my) {
        boolean active = lineage == value;
        boolean hover = mx >= x && mx < x + lineageW && my >= y && my < y + lineageH;
        g.fill(x, y, x + lineageW, y + lineageH, active ? WINE : hover ? WINE_HOVER : 0xFF1B181E);
        g.drawCenteredString(font, label, x + lineageW / 2, y + 7, active ? color : MUTED);
    }
    private String style(Race r) { return switch(r){case TIEFLING->"Sobrevivência infernal e retaliação.";case LYCANTHROPE->"Predador poderoso durante a noite.";case DRAGONBORN->"Tanque ofensivo de linhagem elemental.";case HARPY->"Exploração vertical e agilidade.";default->"";}; }
    private String primary(Race r) { return switch(r){case TIEFLING->"Pulso Infernal";case LYCANTHROPE->"Uivo de Caçada";case DRAGONBORN->"Sopro Elemental";case HARPY->"Rajada de Vento";default->"";}; }
    private String mobility(Race r) { return switch(r){case TIEFLING->"Avanço em Fogo";case LYCANTHROPE->"Bote Predatório";case DRAGONBORN->"Investida Dracônica";case HARPY->"Impulso Alado (sem voo)";default->"";}; }
    private String[] passives(Race r) { return switch(r){case TIEFLING->new String[]{"Imunidade ao fogo e lava","Visão em ambientes escuros","Poder após contato com fogo"};case LYCANTHROPE->new String[]{"Força e velocidade à noite","Regeneração lunar leve","Faro para criaturas"};case DRAGONBORN->new String[]{"12% de resistência física","Resistência elemental","20% de resistência a empurrão"};case HARPY->new String[]{"Movimento e salto aprimorados","Queda lenta e 80% menos dano de queda","Bônus a céu aberto"};default->new String[0];}; }
    private String[] weaknesses(Race r) { return switch(r){case TIEFLING->new String[]{"Regeneração natural 25% menor","Água comum causa fraqueza prolongada"};case LYCANTHROPE->new String[]{"Enfraquecido sob o sol","Prata causa dano adicional","35% mais fome à noite"};case DRAGONBORN->new String[]{"8% mais lento","25% mais consumo de comida","Vulnerável ao elemento oposto"};case HARPY->new String[]{"9 corações e 12% mais dano físico","Penalidade em cavernas","Armadura pesada limita as asas"};default->new String[0];}; }
}
