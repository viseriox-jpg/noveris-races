package com.noveris.races.client;

import com.noveris.races.Race;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RacePanelScreen extends NoverisScreen {
    private final boolean forceConfirmation;
    private int confirmX, confirmY, confirmW, confirmH, changeX, changeY, changeW, changeH;

    public RacePanelScreen(boolean forceConfirmation) {
        super("Painel da Raça");
        this.forceConfirmation = forceConfirmation;
    }
    @Override public boolean shouldCloseOnEsc() { return !forceConfirmation; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        Race race = ClientRaceState.race;
        frame(g, "SANGUE DE " + race.title.toUpperCase());
        String status = ClientRaceState.confirmed ? "LINHAGEM CONFIRMADA" : ClientRaceState.trial > 0 ?
                "EM TESTE  " + formatTicks(ClientRaceState.trial) : "O SANGUE AGUARDA SEU VEREDITO";
        g.drawString(font, status, left + panelWidth - font.width(status) - 34, top + 28,
                ClientRaceState.confirmed ? LILAC : DANGER, false);
        divider(g, top + 62);
        int x = left + 44, y = top + 86, right = left + panelWidth / 2 + 20;
        g.drawString(font, "RESUMO DA RAÇA", x, y, WHITE, false);
        g.drawString(font, summary(race), x, y + 22, LILAC, false);
        g.drawString(font, "VIDA  " + ((int)race.maxHealth/2) + " CORAÇÕES", x, y + 50, race.color, false);
        g.drawString(font, "ALTURA  " + Math.round(race.scale*100) + "%", x, y + 68, LILAC, false);
        if (race == Race.DRAGONBORN) g.drawString(font, "LINHAGEM  " + ClientRaceState.lineage.title.toUpperCase(), x, y + 86, LILAC, false);

        g.drawString(font, "HABILIDADES", x, y + 126, WHITE, false);
        g.drawString(font, "◆ " + active(race), x, y + 148, race.color, false);
        g.drawString(font, "   [" + ClientEvents.PRIMARY.getTranslatedKeyMessage().getString() + "]  RECARGA " + seconds(ClientRaceState.primaryCooldown), x, y + 166, MUTED, false);
        g.drawString(font, "◆ " + mobility(race), x, y + 194, race.color, false);
        g.drawString(font, "   [" + ClientEvents.MOBILITY.getTranslatedKeyMessage().getString() + "]  RECARGA " + seconds(ClientRaceState.mobilityCooldown), x, y + 212, MUTED, false);

        g.drawString(font, "CONDIÇÕES ATUAIS", right, y, WHITE, false);
        String[] conditions = conditions(race);
        for (int i=0;i<conditions.length;i++) g.drawString(font, "• " + conditions[i], right, y+22+i*18, i==0?LILAC:MUTED, false);
        g.drawString(font, "FRAQUEZAS", right, y + 126, DANGER, false);
        String[] weak = weaknesses(race);
        for (int i=0;i<weak.length;i++) g.drawString(font, "• " + weak[i], right, y+148+i*18, MUTED, false);
        divider(g, top + panelHeight - 112);
        g.drawString(font, "[" + ClientEvents.PANEL.getTranslatedKeyMessage().getString() + "] ABRIR PAINEL", x, top + panelHeight - 94, MUTED, false);

        if (!ClientRaceState.confirmed && ClientRaceState.trial <= 0) {
            g.drawCenteredString(font, "SEU SANGUE RECONHECE ESTA LINHAGEM", left + panelWidth/2, top + panelHeight - 92, DANGER);
            confirmW=230; confirmH=34; confirmX=left+panelWidth/2-245; confirmY=top+panelHeight-62;
            changeW=230; changeH=34; changeX=left+panelWidth/2+15; changeY=confirmY;
            button(g, confirmX, confirmY, confirmW, confirmH, "CONFIRMAR RAÇA", mx, my, true);
            button(g, changeX, changeY, changeW, changeH, "TESTAR OUTRA", mx, my, !ClientRaceState.combat);
        } else if (!ClientRaceState.confirmed) {
            changeW=230; changeH=34; changeX=left+panelWidth/2-changeW/2; changeY=top+panelHeight-68;
            button(g, changeX, changeY, changeW, changeH,
                    ClientRaceState.combat ? "EM COMBATE — AGUARDE" : "TESTAR OUTRA RAÇA", mx, my, !ClientRaceState.combat);
        } else {
            g.drawCenteredString(font, "[ESC] RETORNAR AO MUNDO", left + panelWidth/2, top + panelHeight - 52, MUTED);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (!ClientRaceState.confirmed && ClientRaceState.trial <= 0) {
            if (inside(mx,my,confirmX,confirmY,confirmW,confirmH)) {
                PacketDistributor.sendToServer(new ActionPayload("confirm", "", "")); minecraft.setScreen(null); return true;
            }
            if (!ClientRaceState.combat && inside(mx,my,changeX,changeY,changeW,changeH)) {
                minecraft.setScreen(new RaceSelectionScreen()); return true;
            }
        }
        if (!ClientRaceState.confirmed && ClientRaceState.trial > 0 && !ClientRaceState.combat
                && inside(mx,my,changeX,changeY,changeW,changeH)) {
            minecraft.setScreen(new RaceSelectionScreen()); return true;
        }
        return super.mouseClicked(mx,my,button);
    }

    private boolean inside(double x,double y,int bx,int by,int bw,int bh){return x>=bx&&x<bx+bw&&y>=by&&y<by+bh;}
    private String formatTicks(long ticks){long s=ticks/20;return String.format("%d:%02d",s/60,s%60);}
    private String seconds(long ticks){return ticks<=0?"PRONTA":String.format("%.1fs",ticks/20f);}
    private String summary(Race r){return switch(r){case TIEFLING->"Resistente a ambientes infernais.";case LYCANTHROPE->"Predador fortalecido pela noite.";case DRAGONBORN->"Tanque ofensivo elemental.";case HARPY->"Exploradora vertical extremamente ágil.";default->"Nenhuma raça escolhida.";};}
    private String active(Race r){return switch(r){case TIEFLING->"PULSO INFERNAL";case LYCANTHROPE->"UIVO DE CAÇADA";case DRAGONBORN->"SOPRO ELEMENTAL";case HARPY->"RAJADA DE VENTO";default->"—";};}
    private String mobility(Race r){return switch(r){case TIEFLING->"AVANÇO EM FOGO";case LYCANTHROPE->"BOTE PREDATÓRIO";case DRAGONBORN->"INVESTIDA DRACÔNICA";case HARPY->"IMPULSO ALADO";default->"—";};}
    private String[] conditions(Race r){return switch(r){case TIEFLING->new String[]{"Imunidade a fogo ativa","Visão infernal depende da luz","Cura natural reduzida"};case LYCANTHROPE->new String[]{minecraft.level!=null&&minecraft.level.isNight()?"Poder lunar ativo":"Forma diurna enfraquecida","Combate: "+(ClientRaceState.combat?"ATIVO":"livre"),"Prata: IDs aguardando configuração"};case DRAGONBORN->new String[]{"Elemento: "+ClientRaceState.lineage.title,"Resistência física ativa","Metabolismo aumentado"};case HARPY->new String[]{minecraft.player!=null&&minecraft.player.level().canSeeSky(minecraft.player.blockPosition())?"Céu aberto":"Ambiente fechado","Planagem sem ganho de altitude","Peso da armadura afeta asas"};default->new String[]{"Seleção obrigatória"};};}
    private String[] weaknesses(Race r){return switch(r){case TIEFLING->new String[]{"Regeneração por saturação 25% menor"};case LYCANTHROPE->new String[]{"Sol, prata e fome acelerada"};case DRAGONBORN->new String[]{"Lentidão, fome e elemento oposto"};case HARPY->new String[]{"Fragilidade, cavernas e armadura pesada"};default->new String[0];};}
}
