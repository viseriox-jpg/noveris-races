package com.noveris.races.client;

import com.noveris.races.Race;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RacePanelScreen extends NoverisScreen {
    private final boolean forceConfirmation;
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
        if (race == Race.HALF_BLOOD) g.drawString(font, "ASCENDÊNCIAS  " + ClientRaceState.ancestryA.title.toUpperCase() + " + " + ClientRaceState.ancestryB.title.toUpperCase(), x, y + 62, LILAC, false);

        g.drawString(font, "PASSIVAS", x, y + 88, WHITE, false);
        String[] passive = passives(race);
        for (int i=0;i<passive.length;i++) g.drawString(font, "• " + passive[i], x, y+106+i*16, LILAC, false);

        g.drawString(font, "CONDIÇÕES ATUAIS", right, y, WHITE, false);
        String[] conditions = conditions(race);
        for (int i=0;i<conditions.length;i++) g.drawString(font, "• " + conditions[i], right, y+18+i*16, i==0?LILAC:MUTED, false);
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
            g.drawString(font, "[" + ClientEvents.MOBILITY.getTranslatedKeyMessage().getString() + "]  RECARGA " + seconds(ClientRaceState.mobilityCooldown), x, y + 124, MUTED, false);
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
                PacketDistributor.sendToServer(new ActionPayload("confirm", "", "", "", "", "")); minecraft.setScreen(null); return true;
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
    private boolean hasRacialVision(Race r){return r==Race.ELF||r==Race.THALASSIAN||r==Race.TIEFLING||r==Race.VAMPIRE||(r==Race.HALF_BLOOD&&(ClientRaceState.ancestryA==Race.ELF||ClientRaceState.ancestryB==Race.ELF));}
    private boolean visionActive(Race r){if(minecraft.player==null)return false;boolean dark=minecraft.player.level().getMaxLocalRawBrightness(minecraft.player.blockPosition())<7;return dark&&(r!=Race.THALASSIAN||minecraft.player.isInWater());}
    private String summary(Race r){return switch(r){case ELF->"Explorador arcano e arqueiro.";case FAIRY->"Suporte natural sem voo.";case SATYR->"Mobilidade terrestre silvestre.";case THALASSIAN->"Especialista em ambientes aquáticos.";case HUMAN->"A experiência comum, sem poderes raciais.";case NEPHILIM->"Resistência sobrenatural sem voo.";case VAMPIRE->"Caçador noturno com sustentação.";case HALF_BLOOD->"Duas heranças raciais reduzidas.";case TIEFLING->"Resistente a ambientes infernais.";case LYCANTHROPE->"Predador fortalecido pela noite.";case DRAGONBORN->"Tanque ofensivo elemental.";case HARPY->"Exploradora vertical extremamente ágil.";default->"Nenhuma raça escolhida.";};}
    private String active(Race r){return switch(r){case ELF->"FOCO DO ARQUEIRO";case FAIRY->"PERCEPÇÃO FEÉRICA";case SATYR->"VIGOR SILVESTRE";case THALASSIAN->"GUARDA DAS MARÉS";case NEPHILIM->"ÉGIDE SOBRENATURAL";case VAMPIRE->"DRENAGEM DE SANGUE";case HALF_BLOOD->"HERANÇA COMBINADA";case TIEFLING->"PULSO INFERNAL";case LYCANTHROPE->"UIVO DE CAÇADA";case DRAGONBORN->"SOPRO ELEMENTAL";case HARPY->"RAJADA DE VENTO";default->"—";};}
    private String mobility(Race r){return switch(r){case ELF->"PASSO FLORESTAL";case FAIRY->"SALTO FLORAL";case SATYR->"INVESTIDA CAPRINA";case THALASSIAN->"IMPULSO AQUÁTICO";case NEPHILIM->"IMPULSO RADIANTE";case VAMPIRE->"PASSO SOMBRIO";case HALF_BLOOD->"MOBILIDADE HERDADA";case TIEFLING->"AVANÇO EM FOGO";case LYCANTHROPE->"BOTE PREDATÓRIO";case DRAGONBORN->"INVESTIDA DRACÔNICA";case HARPY->"IMPULSO ALADO";default->"—";};}
    private String activeInfo(Race r){return switch(r){case ELF->"Fortalece flechas carregadas por 8 segundos.";case FAIRY->"Revela criaturas próximas e concede regeneração.";case SATYR->"Concede velocidade e regeneração temporárias.";case THALASSIAN->"Cria uma guarda mais forte quando submerso.";case NEPHILIM->"Concede resistência e corações de absorção.";case VAMPIRE->"Fere um alvo próximo e recupera vida.";case HALF_BLOOD->"Ativa uma versão reduzida das duas heranças.";case TIEFLING->"Queima e fere criaturas ao redor.";case LYCANTHROPE->"Revela presas; recebe mobilidade extra à noite.";case DRAGONBORN->"Dispara o elemento da linhagem em uma área frontal.";case HARPY->"Empurra criaturas com uma rajada frontal.";default->"";};}
    private String mobilityInfo(Race r){return switch(r){case ELF->"Avanço curto, ampliado dentro de florestas.";case FAIRY->"Salto direcionado com proteção na queda seguinte.";case SATYR->"Investida terrestre veloz com salto curto.";case THALASSIAN->"Arrancada forte na água e reduzida em terra.";case NEPHILIM->"Impulso radiante que exige contato com o chão.";case VAMPIRE->"Deslocamento rápido na direção do olhar.";case HALF_BLOOD->"Avanço menor inspirado pelas ascendências.";case TIEFLING->"Avanço envolto em fogo na direção do olhar.";case LYCANTHROPE->"Bote longo para alcançar rapidamente uma presa.";case DRAGONBORN->"Investida pesada, curta e predominantemente horizontal.";case HARPY->"Impulso vertical único; não permite voo contínuo.";default->"";};}
    private String[] passives(Race r){return switch(r){case ELF->new String[]{"Visão controlável em baixa luz","Velocidade florestal e arcos carregados fortes"};case FAIRY->new String[]{"Regenera perto de flores, fora de combate","Resiste parcialmente a dano mágico"};case SATYR->new String[]{"Mais rápido e ágil sobre terreno natural","Metade do dano de queda; cura em florestas"};case THALASSIAN->new String[]{"Respira, nada e minera normalmente na água","Resistência física enquanto estiver submerso"};case HUMAN->new String[]{"Pressa leve permanente","Não possui poderes ou resistências raciais"};case NEPHILIM->new String[]{"Resistência parcial a fogo e efeitos negativos","Recebe força quando está com pouca vida"};case VAMPIRE->new String[]{"Visão controlável e roubo de vida corpo a corpo","Força e velocidade adicionais durante a noite"};case HALF_BLOOD->new String[]{"Uma característica reduzida de cada ascendência","Nunca recebe os poderes completos das duas"};case TIEFLING->new String[]{"Imune a fogo e lava; visão controlável","Recebe poder temporário ao tocar fogo"};case LYCANTHROPE->new String[]{"Força, velocidade e regeneração à noite","Detecta criaturas próximas durante a transformação"};case DRAGONBORN->new String[]{"12% de resistência física e a empurrão","Resiste ao elemento de sua linhagem"};case HARPY->new String[]{"Salto alto, planagem e queda reduzida","Mais móvel em céu aberto e lugares elevados"};default->new String[0];};}
    private String[] conditions(Race r){return switch(r){case ELF->new String[]{"Afinidade florestal","Visão leve em baixa luz"};case FAIRY->new String[]{"Natureza fortalece regeneração","Nenhuma habilidade de voo"};case SATYR->new String[]{"Terreno natural favorece mobilidade","Armadura pesada limita bônus"};case THALASSIAN->new String[]{minecraft.player!=null&&minecraft.player.isInWater()?"Hidratado e fortalecido":"Desidratação em progresso","Respiração aquática permanente"};case HUMAN->new String[]{"Pressa leve ativa","Sem habilidades nas teclas raciais"};case NEPHILIM->new String[]{"Resistência parcial ativa","Nenhuma habilidade de voo"};case VAMPIRE->new String[]{minecraft.level!=null&&minecraft.level.isNight()?"Poder noturno ativo":"Bônus noturnos suspensos","Roubo de vida corpo a corpo"};case HALF_BLOOD->new String[]{ClientRaceState.ancestryA.title+" + "+ClientRaceState.ancestryB.title,"Heranças reduzidas"};case TIEFLING->new String[]{"Imunidade a fogo ativa","Visão infernal depende da luz","Cura natural reduzida"};case LYCANTHROPE->new String[]{minecraft.level!=null&&minecraft.level.isNight()?"Poder lunar ativo":"Forma diurna enfraquecida","Combate: "+(ClientRaceState.combat?"ATIVO":"livre"),"Prata configurável"};case DRAGONBORN->new String[]{"Elemento: "+ClientRaceState.lineage.title,"Resistência física ativa","Metabolismo aumentado"};case HARPY->new String[]{minecraft.player!=null&&minecraft.player.level().canSeeSky(minecraft.player.blockPosition())?"Céu aberto":"Ambiente fechado","Planagem sem ganho de altitude","Peso da armadura afeta asas"};default->new String[]{"Seleção obrigatória"};};}
    private String[] weaknesses(Race r){return switch(r){case ELF->new String[]{"Recebe 10% mais dano corpo a corpo","Golpes fortes causam Lentidão breve"};case FAIRY->new String[]{"Armas de ferro causam 25% mais dano","Cura natural reduzida em ambientes infernais"};case SATYR->new String[]{"Armadura pesada remove bônus de mobilidade","Ambientes subterrâneos reduzem velocidade"};case THALASSIAN->new String[]{"Desidrata ao permanecer longe da água","Fogo causa 30% mais dano"};case HUMAN->new String[]{"Sem resistências naturais especiais","Sem habilidades ativa ou de mobilidade"};case NEPHILIM->new String[]{"Habilidades possuem recargas altas","Toda cura recebida é 20% menor"};case VAMPIRE->new String[]{"Sol suspende poderes noturnos e roubo de vida","Fogo causa dano extra; comida rende menos"};case HALF_BLOOD->new String[]{"Poderes herdados são versões reduzidas","Também herda parte das fraquezas escolhidas"};case TIEFLING->new String[]{"Regeneração natural é 25% menor","Água aplica Fraqueza temporária"};case LYCANTHROPE->new String[]{"Perde os bônus raciais durante o dia","Prata causa dano extra; transformação aumenta fome"};case DRAGONBORN->new String[]{"Movimento 8% menor e fome aumentada","Elemento oposto causa dano adicional"};case HARPY->new String[]{"Recebe 12% mais dano físico","Cavernas e armadura pesada limitam as asas"};default->new String[0];};}
}
