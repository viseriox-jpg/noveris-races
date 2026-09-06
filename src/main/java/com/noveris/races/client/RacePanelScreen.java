package com.noveris.races.client;

import com.noveris.races.Race;
import com.noveris.races.RaceInfo;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RacePanelScreen extends NoverisScreen {
    private final boolean forceConfirmation;
    private boolean confirmationSent;
    private int confirmX, confirmY, confirmW, confirmH, changeX, changeY, changeW, changeH;
    private int page, summaryTabX, powersTabX, tabY, tabW, tabH;

    public RacePanelScreen(boolean forceConfirmation) {
        super("Painel da Raça");
        this.forceConfirmation = forceConfirmation;
    }
    @Override public boolean shouldCloseOnEsc() { return !forceConfirmation; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        // O servidor confirma de forma assíncrona. Fechar somente depois que
        // o estado confirmado chegar evita tanto o duplo clique quanto o
        // painel permanecer aberto após uma confirmação válida.
        if (confirmationSent && ClientRaceState.confirmed) {
            minecraft.setScreen(null);
            return;
        }
        Race race = ClientRaceState.race;
        frame(g, "SANGUE DE " + race.title.toUpperCase());
        String status = ClientRaceState.confirmed ? "LINHAGEM CONFIRMADA" : ClientRaceState.trial > 0 ?
                "EM TESTE  " + formatTicks(ClientRaceState.trial) : "O SANGUE AGUARDA SEU VEREDITO";
        g.drawString(font, status, left + panelWidth - font.width(status) - 34, top + 28,
                ClientRaceState.confirmed ? LILAC : DANGER, false);
        divider(g, top + 56);
        renderTabs(g, mx, my);
        if (page == 0) renderOverview(g, race);
        else renderPowers(g, race);

        if (!ClientRaceState.confirmed && ClientRaceState.trial <= 0) {
            g.drawCenteredString(font, "SEU SANGUE RECONHECE ESTA LINHAGEM", left + panelWidth/2, top + panelHeight - 68, DANGER);
            confirmW=230; confirmH=26; confirmX=left+panelWidth/2-confirmW/2; confirmY=top+panelHeight-36;
            button(g, confirmX, confirmY, confirmW, confirmH, "CONFIRMAR RAÇA", mx, my, true);
        } else if (!ClientRaceState.confirmed) {
            changeW=230; changeH=26; changeX=left+panelWidth/2-changeW/2; changeY=top+panelHeight-36;
            button(g, changeX, changeY, changeW, changeH,
                    ClientRaceState.combat ? "EM COMBATE — AGUARDE" : "TESTAR OUTRA RAÇA", mx, my, !ClientRaceState.combat);
        } else {
            g.drawCenteredString(font, "[ESC] RETORNAR AO MUNDO", left + panelWidth/2, top + panelHeight - 28, MUTED);
        }
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        tabW=210;tabH=22;tabY=top+63;summaryTabX=left+panelWidth/2-tabW-4;powersTabX=left+panelWidth/2+4;
        panelTab(g,summaryTabX,"RESUMO",page==0,mx,my);
        panelTab(g,powersTabX,"PODERES E FRAQUEZAS",page==1,mx,my);
    }

    private void panelTab(GuiGraphics g,int x,String label,boolean active,int mx,int my){
        boolean hover=inside(mx,my,x,tabY,tabW,tabH);
        g.fill(x,tabY,x+tabW,tabY+tabH,active?WINE:hover?WINE_HOVER:0xFF17140E);
        g.drawCenteredString(font,label,x+tabW/2,tabY+7,active||hover?WHITE:MUTED);
    }

    private void renderOverview(GuiGraphics g, Race race) {
        int x = left + 44, y = top + 100, right = left + panelWidth / 2 + 20;
        g.drawString(font, "RESUMO DA RAÇA", x, y, WHITE, false);
        g.drawString(font, summary(race), x, y + 16, LILAC, false);
        g.drawString(font, "VIDA  " + ((int)race.maxHealth/2) + " CORAÇÕES", x, y + 34, race.color, false);
        g.drawString(font, ClientRaceState.size.title.toUpperCase(), x, y + 48, LILAC, false);
        if (race == Race.DRAGONBORN) g.drawString(font, "LINHAGEM  " + ClientRaceState.lineage.title.toUpperCase(), x, y + 62, LILAC, false);
        if (race == Race.FAIRY) g.drawString(font, "AFINIDADE  " + ClientRaceState.fairyAffinity.title.toUpperCase(), x, y + 62, LILAC, false);

        g.drawString(font, "PASSIVAS", x, y + 88, WHITE, false);
        String[] passive = passives(race);
        for (int i=0;i<passive.length;i++) g.drawString(font, "• " + passive[i], x, y+106+i*16, LILAC, false);

        g.drawString(font, "CONDIÇÕES ATUAIS", right, y, WHITE, false);
        String[] conditions = conditions(race);
        for (int i=0;i<conditions.length;i++) g.drawString(font, "• " + conditions[i], right, y+18+i*16, i==0?LILAC:MUTED, false);
        if (race == Race.THALASSIAN) {
            int hydration = ClientRaceState.hydration;
            int color = hydration > 50 ? LILAC : hydration > 25 ? 0xFFFFD84A : DANGER;
            g.drawString(font, "HIDRATAÇÃO — " + hydration + "%", right, y + 66, color, false);
        }
        if (hasRacialVision(race)) {
            String vision = !ClientRaceState.visionEnabled ? "DESATIVADA" : visionActive(race) ? "ATIVA" : "SUSPENSA";
            g.drawString(font, "VISÃO RACIAL", right, y + 98, WHITE, false);
            g.drawString(font, "[" + ClientEvents.VISION.getTranslatedKeyMessage().getString() + "] " + vision,
                    right, y + 116, vision.equals("ATIVA") ? LILAC : MUTED, false);
        }
    }

    private void renderPowers(GuiGraphics g, Race race) {
        int x = left + 44, y = top + 104, right = left + panelWidth / 2 + 20;
        g.drawString(font, "HABILIDADES", x, y, WHITE, false);
        if (race == Race.HUMAN) {
            g.drawString(font, "NENHUMA HABILIDADE ATIVA", x, y + 20, race.color, false);
            g.drawString(font, "O Humano depende de ferramentas e equipamentos.", x, y + 38, MUTED, false);
        } else {
            g.drawString(font, "◆ " + active(race), x, y + 20, race.color, false);
            g.drawString(font, activeInfo(race), x, y + 38, LILAC, false);
            g.drawString(font, "[" + ClientEvents.PRIMARY.getTranslatedKeyMessage().getString() + "]  RECARGA " + seconds(ClientRaceState.primaryCooldown), x, y + 56, MUTED, false);
            if (race != Race.NPC) {
                g.drawString(font, "◆ " + mobility(race), x, y + 88, race.color, false);
                g.drawString(font, mobilityInfo(race), x, y + 106, LILAC, false);
                String mobilityState = ClientRaceState.mobilityCooldown > 0
                        ? "RECARGA " + seconds(ClientRaceState.mobilityCooldown)
                        : "CARGAS " + ClientRaceState.mobilityCharges + "/3";
                g.drawString(font, "[" + ClientEvents.MOBILITY.getTranslatedKeyMessage().getString() + "]  " + mobilityState,
                        x, y + 124, MUTED, false);
            }
        }
        g.drawString(font, "FRAQUEZAS E LIMITAÇÕES", right, y, DANGER, false);
        String[] weak = weaknesses(race);
        for (int i=0;i<weak.length;i++) g.drawString(font, "• " + weak[i], right, y+20+i*20, MUTED, false);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (inside(mx,my,summaryTabX,tabY,tabW,tabH)) { page=0; return true; }
        if (inside(mx,my,powersTabX,tabY,tabW,tabH)) { page=1; return true; }
        if (!ClientRaceState.confirmed && ClientRaceState.trial <= 0) {
            if (inside(mx,my,confirmX,confirmY,confirmW,confirmH)) {
                // Mantém o painel aberto até o sync do servidor chegar.
                confirmationSent = true;
                PacketDistributor.sendToServer(new ActionPayload("confirm", "", "", "", "", "", "")); return true;
            }

        }

        return super.mouseClicked(mx,my,button);
    }

    private boolean inside(double x,double y,int bx,int by,int bw,int bh){return x>=bx&&x<bx+bw&&y>=by&&y<by+bh;}
    private String formatTicks(long ticks){long s=ticks/20;return String.format("%d:%02d",s/60,s%60);}
    private String seconds(long ticks){return ticks<=0?"PRONTA":String.format("%.1fs",ticks/20f);}
    private boolean hasRacialVision(Race r){return r==Race.ELF||r==Race.THALASSIAN||r==Race.TIEFLING||r==Race.VAMPIRE;}
    private boolean visionActive(Race r){if(minecraft.player==null)return false;boolean dark=minecraft.player.level().getRawBrightness(minecraft.player.blockPosition(),minecraft.player.level().getSkyDarken())<7;return dark&&(r!=Race.THALASSIAN||minecraft.player.isInWater());}
    private String summary(Race r){return RaceInfo.summary(r);}
    private String active(Race r){return RaceInfo.active(r, ClientRaceState.fairyAffinity);}
    private String mobility(Race r){return RaceInfo.mobility(r, ClientRaceState.fairyAffinity);}
    private String activeInfo(Race r){return RaceInfo.activeInfo(r, ClientRaceState.fairyAffinity);}
    private String mobilityInfo(Race r){return RaceInfo.mobilityInfo(r, ClientRaceState.fairyAffinity);}
    private String[] passives(Race r){return RaceInfo.passives(r);}
    private String[] conditions(Race r){
        return switch(r){
            case ELF -> new String[]{"Visão racial pode ser alternada; fica ativa no escuro","Velocidade adicional exige floresta"};
            case FAIRY -> switch(ClientRaceState.fairyAffinity){
                case WATER -> new String[]{"Resistência passiva exige água ou chuva","Lava bloqueia as habilidades feéricas"};
                case AIR -> new String[]{"Mobilidade aprimorada em espaços abertos","Lava bloqueia as habilidades feéricas"};
                default -> new String[]{"Purifica veneno em terreno natural","Lava bloqueia as habilidades feéricas"};
            };
            case SATYR -> new String[]{"Velocidade e salto exigem terreno natural","Armadura pesada reduz ou bloqueia a mobilidade"};
            case THALASSIAN -> new String[]{"Nado acelerado e visão noturna exigem água","Hidratação cai fora da água; fogo aplica Fraqueza"};
            case HUMAN -> new String[]{"Pressa I funciona ao trabalhar","Não possui habilidades raciais ativas ou mobilidade"};
            case NEPHILIM -> new String[]{"Força ativa com 30% ou menos de vida","Recebe 50% menos dano de fogo; cura recebida é menor"};
            case VAMPIRE -> new String[]{"Velocidade, Força e mordida regenerativa exigem noite","Fogo suspende vantagens e aplica Fraqueza"};
            case TIEFLING -> new String[]{"Imune a dano de fogo e lava; retalia ao ser queimado","Água e chuva causam dano; cura pequena é reduzida com fome alta"};
            case LYCANTHROPE -> new String[]{"Força, velocidade e regeneração exigem noite e 6+ de fome","A fome é consumida durante a noite; dia suspende os bônus"};
            case DRAGONBORN -> ClientRaceState.lineage == com.noveris.races.DragonLineage.FIRE
                    ? new String[]{"Água e chuva causam dano","Resistência ao fogo; elemento oposto causa penalidade"}
                    : new String[]{"Resistência elemental depende da linhagem escolhida","Fome aumenta com o tempo; elemento oposto causa penalidade"};
            case HARPY -> new String[]{"Velocidade adicional exige áreas abertas","Armadura pesada reduz a mobilidade"};
            case GOD -> new String[]{"Raças administrativas: resistência e regeneração elevadas","Teleporte exige espaço seguro"};
            case NPC -> new String[]{"Investida de Guarda é a única habilidade ativa","Raça exclusiva para operadores"};
            default -> new String[]{"Seleção obrigatória"};
        };
    }
    private String[] weaknesses(Race r){return RaceInfo.weaknesses(r, ClientRaceState.lineage);}
}
