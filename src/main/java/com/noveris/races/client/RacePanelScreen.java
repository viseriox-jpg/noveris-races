package com.noveris.races.client;

import com.noveris.races.Race;
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
            confirmW=230; confirmH=26; confirmX=left+panelWidth/2-245; confirmY=top+panelHeight-36;
            changeW=230; changeH=26; changeX=left+panelWidth/2+15; changeY=confirmY;
            button(g, confirmX, confirmY, confirmW, confirmH, "CONFIRMAR RAÇA", mx, my, true);
            button(g, changeX, changeY, changeW, changeH, "TESTAR OUTRA", mx, my, !ClientRaceState.combat);
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
            g.drawString(font, "◆ " + mobility(race), x, y + 88, race.color, false);
            g.drawString(font, mobilityInfo(race), x, y + 106, LILAC, false);
            String mobilityState = ClientRaceState.mobilityCooldown > 0
                    ? "RECARGA " + seconds(ClientRaceState.mobilityCooldown)
                    : "CARGAS " + ClientRaceState.mobilityCharges + "/3";
            g.drawString(font, "[" + ClientEvents.MOBILITY.getTranslatedKeyMessage().getString() + "]  " + mobilityState,
                    x, y + 124, MUTED, false);
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
    private boolean hasRacialVision(Race r){return r==Race.ELF||r==Race.THALASSIAN||r==Race.TIEFLING||r==Race.VAMPIRE;}
    private boolean visionActive(Race r){if(minecraft.player==null)return false;boolean dark=minecraft.player.level().getRawBrightness(minecraft.player.blockPosition(),minecraft.player.level().getSkyDarken())<7;return dark&&(r!=Race.THALASSIAN||minecraft.player.isInWater());}
    private String summary(Race r){return switch(r){case ELF->"Explorador arcano e arqueiro.";case FAIRY->"Suporte natural sem voo.";case SATYR->"Mobilidade terrestre silvestre.";case THALASSIAN->"Especialista em ambientes aquáticos.";case HUMAN->"Pessoa comum com eficiência básica.";case NEPHILIM->"Resistência sobrenatural sem voo.";case VAMPIRE->"Caçador noturno com sustentação.";case TIEFLING->"Resistente a ambientes infernais.";case LYCANTHROPE->"Predador fortalecido pela noite.";case DRAGONBORN->"Tanque ofensivo elemental.";case HARPY->"Exploradora vertical extremamente ágil.";case GOD->"Entidade administrativa de poder absoluto.";case NPC->"Personagem secundário para dar vida ao servidor.";default->"Nenhuma raça escolhida.";};}
    private String active(Race r){return switch(r){case ELF->"DISPARO PERFURANTE";case FAIRY->switch(ClientRaceState.fairyAffinity){case WATER->"VÉU DE MARÉ";case AIR->"RAJADA FEÉRICA";default->"RAÍZES PRENSORAS";};case SATYR->"VIGOR SILVESTRE";case THALASSIAN->"GUARDA DAS MARÉS";case NEPHILIM->"RAJADA DE LUZ";case VAMPIRE->"DRENAGEM DE SANGUE";case TIEFLING->"PULSO INFERNAL";case LYCANTHROPE->"UIVO DE CAÇADA";case DRAGONBORN->"SOPRO ELEMENTAL";case HARPY->"RAJADA DE VENTO";case GOD->"JULGAMENTO DIVINO";case NPC->"INVESTIDA DE GUARDA";default->"—";};}
    private String mobility(Race r){return switch(r){case ELF->"RECUO ACROBÁTICO";case FAIRY->switch(ClientRaceState.fairyAffinity){case WATER->"SALTO DE MARÉ";case AIR->"SALTO ATMOSFÉRICO";default->"SALTO DE CIPÓ";};case SATYR->"INVESTIDA CAPRINA";case THALASSIAN->"IMPULSO AQUÁTICO";case NEPHILIM->"IMPULSO RADIANTE";case VAMPIRE->"PASSO SOMBRIO";case TIEFLING->"AVANÇO EM FOGO";case LYCANTHROPE->"BOTE PREDATÓRIO";case DRAGONBORN->"INVESTIDA DRACÔNICA";case HARPY->"IMPULSO ALADO";case GOD->"TELEPORTE CELESTIAL";case NPC->"INVESTIDA DE GUARDA";default->"—";};}
    private String activeInfo(Race r){return switch(r){case ELF->"Projétil mágico perfurante: 2,5 corações em linha.";case FAIRY->switch(ClientRaceState.fairyAffinity){case WATER->"Rajada aquática causa 2 corações, empurra, apaga fogo e protege.";case AIR->"Rajada de ar causa 2 corações e empurra entidades à frente.";default->"Rajada natural causa 2 corações e aplica Lentidão em área curta.";};case SATYR->"Concede velocidade e regeneração temporárias.";case THALASSIAN->"Concede resistência por 6s e apaga fogo, em qualquer ambiente.";case NEPHILIM->"Feixe frontal que causa 2 corações e Lentidão por 5 segundos.";case VAMPIRE->"Drenagem rouba até 3 corações; mordida cura 0,5 à noite a cada 10s.";case TIEFLING->"Queima e fere criaturas ao redor.";case LYCANTHROPE->"Revela presas; recebe velocidade extra à noite.";case DRAGONBORN->"Dispara o elemento da linhagem em uma área frontal.";case HARPY->"Empurra criaturas com uma rajada frontal.";case GOD->"Raio direcionado: 7 corações a até 20 blocos.";case NPC->"Investida com empurrão e 1,5 coração de dano.";default->"";};}
    private String mobilityInfo(Race r){return switch(r){case ELF->"Recuo de ~3 blocos; remove Lentidão e protege 0,5s. 3 cargas, recarga 45s.";case FAIRY->switch(ClientRaceState.fairyAffinity){case WATER->"Impulso curto, mais forte dentro ou ao lado da água. 3 cargas, recarga 45s.";case AIR->"Impulso vertical no chão, sem voo contínuo. 3 cargas, recarga 45s.";default->"Impulso sobre chão natural, com salto curto. 3 cargas, recarga 45s.";};case SATYR->"Investida terrestre veloz com salto curto. 3 cargas, recarga 45s.";case THALASSIAN->"Arrancada livre na água; em terra exige chão. 3 cargas, recarga 45s.";case NEPHILIM->"Impulso radiante que exige contato com o chão. 3 cargas, recarga 45s.";case VAMPIRE->"Avanço sombrio terrestre na direção do olhar. 3 cargas, recarga 45s.";case TIEFLING->"Avanço terrestre envolto em fogo. 3 cargas, recarga 45s.";case LYCANTHROPE->"Bote terrestre longo para alcançar uma presa. 3 cargas, recarga 45s.";case DRAGONBORN->"Investida terrestre pesada e horizontal. 3 cargas, recarga 45s.";case HARPY->"Impulso vertical único; não permite voo contínuo. 3 cargas, recarga 45s.";case GOD->"Teleporta até 28 blocos, se houver espaço seguro.";case NPC->"Investida curta para avançar e empurrar alvos.";default->"";};}
    private String[] passives(Race r){return switch(r){case ELF->new String[]{"Visão controlável em baixa luz","Velocidade adicional dentro de florestas"};case FAIRY->new String[]{"Natureza: purifica veneno em terreno natural","Água: resistência quando molhado; Ar: queda lenta ao cair"};case SATYR->new String[]{"Mais rápido e ágil sobre terreno natural","Metade do dano de queda; cura em florestas"};case THALASSIAN->new String[]{"Respira e minera normalmente na água","Nado acelerado e visão aquática"};case HUMAN->new String[]{"Pressa I permanente ao trabalhar","Sem habilidades ativas ou poderes sobrenaturais"};case NEPHILIM->new String[]{"50% menos dano de fogo","Força temporária quando está com pouca vida"};case VAMPIRE->new String[]{"Visão controlável; mordida cura 0,5 à noite a cada 10s","Força e velocidade adicionais durante a noite"};case TIEFLING->new String[]{"Imune a dano de fogo e lava; visão controlável","Ao receber fogo, ganha Força temporária"};case LYCANTHROPE->new String[]{"Força, velocidade e regeneração à noite","Detecta criaturas em um raio curto de 6 blocos"};case DRAGONBORN->new String[]{"12% de resistência física e a empurrão","Resiste ao elemento de sua linhagem"};case HARPY->new String[]{"Salto alto e queda reduzida","Mais móvel acima de Y=100 com céu aberto"};default->new String[0];};}
    private String[] conditions(Race r){return switch(r){case ELF->new String[]{"Afinidade florestal","Visão leve em baixa luz"};case FAIRY->new String[]{"Natureza purifica veneno no terreno natural","Nenhuma habilidade de voo"};case SATYR->new String[]{"Terreno natural favorece mobilidade","Armadura pesada limita bônus"};case THALASSIAN->new String[]{minecraft.player!=null&&minecraft.player.isInWater()?"Hidratado e fortalecido":"Desidratação em progresso","Respiração aquática permanente"};case HUMAN->new String[]{"Pressa I ativa","Sem habilidades nas teclas raciais"};case NEPHILIM->new String[]{"Resistência ao fogo ativa","Nenhuma habilidade de voo"};case VAMPIRE->new String[]{minecraft.level!=null&&minecraft.level.isNight()?"Poder noturno ativo":"Bônus noturnos suspensos","Roubo de vida só à noite"};case TIEFLING->new String[]{"Imunidade a fogo ativa","Visão infernal depende da luz","Cura natural reduzida"};case LYCANTHROPE->new String[]{minecraft.level!=null&&minecraft.level.isNight()?"Poder lunar ativo":"Forma diurna enfraquecida","Combate: "+(ClientRaceState.combat?"ATIVO":"livre"),"Fome atual limita os bônus"};case DRAGONBORN->new String[]{"Elemento: "+ClientRaceState.lineage.title,"Resistência física ativa","Metabolismo aumentado"};case HARPY->new String[]{minecraft.player!=null&&minecraft.player.level().canSeeSky(minecraft.player.blockPosition())?"Céu aberto":"Ambiente fechado","Queda lenta sem ganho de altitude","Peso da armadura afeta mobilidade"};default->new String[]{"Seleção obrigatória"};};}
    private String[] weaknesses(Race r){return switch(r){case ELF->new String[]{"Golpes corpo a corpo fortes causam Lentidão"};case FAIRY->new String[]{"No Nether, a regeneração é bloqueada","Lava bloqueia habilidades"};case SATYR->new String[]{"Armadura pesada reduz bônus por etapas","Ambientes subterrâneos reduzem velocidade"};case THALASSIAN->new String[]{"Desidrata ao permanecer longe da água","Fogo aplica Fraqueza temporária"};case HUMAN->new String[]{"Sem resistências naturais especiais","Sem habilidades ativas ou de mobilidade"};case NEPHILIM->new String[]{"Toda cura recebida é 20% menor","Rajada exige mira e não oferece proteção"};case VAMPIRE->new String[]{"Sol suspende poderes noturnos e roubo de vida","Fogo aplica Fraqueza temporária; comida rende menos"};case TIEFLING->new String[]{"Curas pequenas são 25% menores com fome alta","Água aplica Fraqueza temporária"};case LYCANTHROPE->new String[]{"Velocidade e regeneração exigem 6 de fome","Durante o dia, os bônus noturnos ficam suspensos"};case DRAGONBORN->new String[]{"Movimento lento e fome aumentada","Elemento oposto aplica penalidade temporária"};case HARPY->new String[]{"Golpes físicos causam Lentidão breve","Cavernas e armadura pesada limitam a mobilidade"};default->new String[0];};}
}
