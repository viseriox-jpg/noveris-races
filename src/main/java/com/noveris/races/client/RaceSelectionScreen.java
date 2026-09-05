package com.noveris.races.client;

import com.noveris.races.*;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.Arrays;

public final class RaceSelectionScreen extends NoverisScreen {
    private RaceRealm realm = RaceRealm.ORVANNIS;
    private Race selected = Race.ELF;
    private DragonLineage lineage = DragonLineage.FIRE;
    private FairyAffinity fairyAffinity = FairyAffinity.NATURE;
    private Race ancestryA = Race.HUMAN, ancestryB = Race.ELF;
    private RaceSize size = RaceSize.MEDIUM;
    private boolean confirming;
    private int actionX, actionY, actionW, actionH, specialX, specialY, specialW, specialH;
    private int sizeX, sizeY, sizeW, sizeH;
    private int confirmX, confirmY, backX, backY;

    public RaceSelectionScreen() { super("Linhagens de Noveris"); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        frame(g, "LINHAGENS DE NOVERIS");
        String realmTitle = realm.title.toUpperCase();
        g.drawString(font, realmTitle, left + panelWidth - font.width(realmTitle) - 34, top + 28, DANGER, false);
        renderSizeControls(g, mx, my);
        renderTabs(g, mx, my);
        renderRaceCards(g);
        renderDetails(g, mx, my);
        actionW=240; actionH=24; actionX=left+(panelWidth-actionW)/2; actionY=top+panelHeight-32;
        button(g, actionX, actionY, actionW, actionH, "SELECIONAR RAÇA", mx, my, true);
        if (confirming) renderConfirmation(g, mx, my);
    }

    private void renderTabs(GuiGraphics g,int mx,int my){
        int y=top+54,gap=6,w=(panelWidth-68-gap*2)/3;
        tab(g,left+34,y,w,"ORVANNIS",RaceRealm.ORVANNIS,mx,my);
        tab(g,left+34+w+gap,y,w,"NEUTROS",RaceRealm.NEUTRAL,mx,my);
        tab(g,left+34+(w+gap)*2,y,w,"AVARION",RaceRealm.AVARION,mx,my);
        divider(g,y+30);
    }
    private void tab(GuiGraphics g,int x,int y,int w,String label,RaceRealm value,int mx,int my){
        boolean active=realm==value,hover=inside(mx,my,x,y,w,24);
        g.fill(x,y,x+w,y+24,active?WINE:hover?WINE_HOVER:0xFF17140E);
        g.drawCenteredString(font,label,x+w/2,y+8,active||hover?WHITE:MUTED);
    }
    private void renderRaceCards(GuiGraphics g){
        Race[] races=racesForRealm(); int y=top+94,gap=6,w=(panelWidth-68-gap*(races.length-1))/races.length;
        for(int i=0;i<races.length;i++){int x=left+34+i*(w+gap);boolean active=selected==races[i];
            g.fill(x,y,x+w,y+44,active?WINE:0xFF17140E);
            drawLargeSymbol(g,symbol(races[i]),x+w/2,y+5,races[i].color);
            g.drawCenteredString(font,races[i].title.toUpperCase(),x+w/2,y+28,active?WHITE:MUTED);}
    }
    private void renderDetails(GuiGraphics g,int mx,int my){
        int y=top+150,x=left+44,right=left+panelWidth/2+16;
        g.drawString(font,selected.title.toUpperCase(),x,y,selected.color,false);
        g.drawString(font,selected.realm.title+"  •  "+((int)selected.maxHealth/2)+" corações  •  máximo "+Math.round(previewScale(RaceSize.LARGE)*100)+"%",x,y+14,LILAC,false);
        g.drawString(font,description(selected),x,y+30,MUTED,false);
        g.drawString(font,"HABILIDADES",x,y+48,WHITE,false);
        g.drawString(font,"◆ "+active(selected),x,y+62,selected.color,false);
        g.drawString(font,"◆ "+mobility(selected),x,y+76,LILAC,false);
        g.drawString(font,"PASSIVAS",right,y,WHITE,false); drawLines(g,passives(selected),right,y+15,LILAC);
        g.drawString(font,"FRAQUEZAS / DEBUFFS",right,y+43,DANGER,false); drawLines(g,weaknesses(selected),right,y+57,MUTED);
        int controlsY=top+panelHeight-66;
        specialX=x;specialY=controlsY;specialW=112;specialH=22;
        if(selected==Race.DRAGONBORN){
            option(g,specialX,specialY,"FOGO",lineage==DragonLineage.FIRE,0xFFFF8A4B,mx,my);
            option(g,specialX+specialW+6,specialY,"GELO",lineage==DragonLineage.FROST,0xFF80D9FF,mx,my);
            option(g,specialX+(specialW+6)*2,specialY,"VENENO",lineage==DragonLineage.VENOM,0xFF86D48A,mx,my);
        }else if(selected==Race.FAIRY){
            option(g,specialX,specialY,"NATUREZA",fairyAffinity==FairyAffinity.NATURE,0xFF8ED081,mx,my);
            option(g,specialX+specialW+6,specialY,"ÁGUA",fairyAffinity==FairyAffinity.WATER,0xFF75D6F5,mx,my);
            option(g,specialX+(specialW+6)*2,specialY,"AR",fairyAffinity==FairyAffinity.AIR,0xFFE8F3F5,mx,my);
        }
    }
    private void renderSizeControls(GuiGraphics g,int mx,int my){
        sizeW=76;sizeH=20;sizeY=top+18;
        int totalW=sizeW*3+12;
        sizeX=left+(panelWidth-totalW)/2;
        g.drawCenteredString(font,"PORTE "+Math.round(previewScale(size)*100)+"%",sizeX+totalW/2,sizeY-11,WHITE);
        sizeOption(g,sizeX,sizeY,"MENOR",RaceSize.SMALL,mx,my);
        sizeOption(g,sizeX+sizeW+6,sizeY,"MÉDIO",RaceSize.MEDIUM,mx,my);
        sizeOption(g,sizeX+(sizeW+6)*2,sizeY,"MAIOR",RaceSize.LARGE,mx,my);
    }
    private void sizeOption(GuiGraphics g,int x,int y,String label,RaceSize value,int mx,int my){boolean active=size==value,hover=inside(mx,my,x,y,sizeW,sizeH);g.fill(x,y,x+sizeW,y+sizeH,active?WINE:hover?WINE_HOVER:0xFF17140E);g.drawCenteredString(font,label,x+sizeW/2,y+6,active?WHITE:MUTED);}
    private void option(GuiGraphics g,int x,int y,String label,boolean active,int color,int mx,int my){
        boolean hover=inside(mx,my,x,y,specialW,specialH);
        g.fill(x,y,x+specialW,y+specialH,active?WINE:hover?WINE_HOVER:0xFF17140E);
        g.drawCenteredString(font,label,x+specialW/2,y+7,active?WHITE:MUTED);
    }
    private void renderConfirmation(GuiGraphics g,int mx,int my){
        int w=430,h=124,x=left+(panelWidth-w)/2,y=top+(panelHeight-h)/2;
        g.fill(x-3,y-3,x+w+3,y+h+3,BORDER);g.fill(x,y,x+w,y+h,0xFC0D0C09);
        g.drawCenteredString(font,"DESEJA REALMENTE ESCOLHER ESTA RAÇA?",x+w/2,y+24,WHITE);
        String choice=selected.title+(selected==Race.DRAGONBORN?" — "+lineage.title:selected==Race.FAIRY?" — "+fairyAffinity.title:"")+" — "+size.title;
        g.drawCenteredString(font,choice.toUpperCase(),x+w/2,y+47,selected.color);
        g.drawCenteredString(font,"Você iniciará o teste de 5 minutos.",x+w/2,y+64,MUTED);
        confirmX=x+34;confirmY=y+86;backX=x+226;backY=confirmY;
        button(g,confirmX,confirmY,170,26,"CONFIRMAR",mx,my,true);button(g,backX,backY,170,26,"VOLTAR",mx,my,true);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(confirming){
            if(inside(mx,my,confirmX,confirmY,170,26)){PacketDistributor.sendToServer(new ActionPayload("trial",selected.name(),lineage.name(),fairyAffinity.name(),ancestryA.name(),ancestryB.name(),size.name()));minecraft.setScreen(null);return true;}
            if(inside(mx,my,backX,backY,170,26)){confirming=false;return true;}return true;
        }
        if(inside(mx,my,actionX,actionY,actionW,actionH)){confirming=true;return true;}
        int tabY=top+54,gap=6,tabW=(panelWidth-68-gap*2)/3;
        for(int i=0;i<3;i++)if(inside(mx,my,left+34+i*(tabW+gap),tabY,tabW,24)){realm=RaceRealm.values()[i];selected=racesForRealm()[0];return true;}
        Race[] races=racesForRealm();int cardY=top+94,cardW=(panelWidth-68-gap*(races.length-1))/races.length;
        for(int i=0;i<races.length;i++)if(inside(mx,my,left+34+i*(cardW+gap),cardY,cardW,44)){selected=races[i];return true;}
        if(inside(mx,my,sizeX,sizeY,sizeW,sizeH)){size=RaceSize.SMALL;return true;}
        if(inside(mx,my,sizeX+sizeW+6,sizeY,sizeW,sizeH)){size=RaceSize.MEDIUM;return true;}
        if(inside(mx,my,sizeX+(sizeW+6)*2,sizeY,sizeW,sizeH)){size=RaceSize.LARGE;return true;}
        if(selected==Race.DRAGONBORN){
            if(inside(mx,my,specialX,specialY,specialW,specialH)){lineage=DragonLineage.FIRE;return true;}
            if(inside(mx,my,specialX+specialW+6,specialY,specialW,specialH)){lineage=DragonLineage.FROST;return true;}
            if(inside(mx,my,specialX+(specialW+6)*2,specialY,specialW,specialH)){lineage=DragonLineage.VENOM;return true;}}
        if(selected==Race.FAIRY){
            if(inside(mx,my,specialX,specialY,specialW,specialH)){fairyAffinity=FairyAffinity.NATURE;return true;}
            if(inside(mx,my,specialX+specialW+6,specialY,specialW,specialH)){fairyAffinity=FairyAffinity.WATER;return true;}
            if(inside(mx,my,specialX+(specialW+6)*2,specialY,specialW,specialH)){fairyAffinity=FairyAffinity.AIR;return true;}}
        return super.mouseClicked(mx,my,button);
    }
    private float previewScale(RaceSize chosen){return selected.scale(chosen);}
    private boolean inside(double x,double y,int bx,int by,int bw,int bh){return x>=bx&&x<bx+bw&&y>=by&&y<by+bh;}
    private Race[] racesForRealm(){return Arrays.stream(Race.values()).filter(r->r!=Race.NONE&&r.realm==realm).toArray(Race[]::new);}
    private void drawLines(GuiGraphics g,String[] lines,int x,int y,int color){for(int i=0;i<lines.length;i++)g.drawString(font,"• "+lines[i],x,y+i*13,color,false);}
    private void drawLargeSymbol(GuiGraphics g,String s,int cx,int y,int color){g.pose().pushPose();g.pose().translate(cx,y,0);g.pose().scale(1.35f,1.35f,1f);g.drawCenteredString(font,s,0,0,color);g.pose().popPose();}
    private String symbol(Race r){return switch(r){case ELF->"⌁";case FAIRY->"✦";case SATYR->"♈";case THALASSIAN->"≈";case HUMAN->"●";case NEPHILIM->"◇";case VAMPIRE->"▼";case TIEFLING->"♠";case LYCANTHROPE->"☾";case DRAGONBORN->"◆";case HARPY->"⌁";default->"?";};}
    private String description(Race r){return switch(r){case ELF->"Exploração, conhecimento, magia e combate à distância.";case FAIRY->"Magia, suporte e conexão com a natureza — sem voo.";case SATYR->"Mobilidade terrestre e sobrevivência natural.";case THALASSIAN->"Exploração oceânica e combate aquático.";case HUMAN->"Versatilidade, ferramentas e progressão.";case NEPHILIM->"Resistência sobrenatural equilibrada — sem voo.";case VAMPIRE->"Sustentação e combate noturno.";case TIEFLING->"Sobrevivência infernal e retaliação.";case LYCANTHROPE->"Predador fortalecido pela noite.";case DRAGONBORN->"Tanque ofensivo de linhagem elemental.";case HARPY->"Exploração vertical sem voo verdadeiro.";default->"";};}
    private String active(Race r){return switch(r){case ELF->"Disparo Perfurante";case FAIRY->switch(fairyAffinity){case WATER->"Véu de Maré";case AIR->"Rajada Feérica";default->"Raízes Prensoras";};case SATYR->"Vigor Silvestre";case THALASSIAN->"Guarda das Marés";case HUMAN->"Nenhuma habilidade ativa";case NEPHILIM->"Rajada de Luz";case VAMPIRE->"Drenagem de Sangue";case TIEFLING->"Pulso Infernal";case LYCANTHROPE->"Uivo de Caçada";case DRAGONBORN->"Sopro Elemental";case HARPY->"Rajada de Vento";default->"—";};}
    private String mobility(Race r){return switch(r){case ELF->"Recuo Acrobático";case FAIRY->switch(fairyAffinity){case WATER->"Salto de Maré";case AIR->"Salto Atmosférico";default->"Salto de Cipó";};case SATYR->"Investida Caprina";case THALASSIAN->"Impulso Aquático";case HUMAN->"Nenhuma habilidade de mobilidade";case NEPHILIM->"Impulso Radiante";case VAMPIRE->"Passo Sombrio";case TIEFLING->"Avanço em Fogo";case LYCANTHROPE->"Bote Predatório";case DRAGONBORN->"Investida Dracônica";case HARPY->"Impulso Alado";default->"—";};}
    private String[] passives(Race r){return switch(r){case ELF->new String[]{"Visão leve em ambientes escuros","Velocidade adicional em florestas"};case FAIRY->new String[]{"Natureza: purifica veneno em terreno natural","Água: resistência breve quando molhado; Ar: queda lenta ao cair"};case SATYR->new String[]{"Rápido no natural; salto alto","Menos queda e regeneração florestal"};case THALASSIAN->new String[]{"Respiração e mineração aquáticas","Nado rápido e visão subaquática"};case HUMAN->new String[]{"Pressa I permanente ao trabalhar","Sem poderes sobrenaturais"};case NEPHILIM->new String[]{"50% menos dano de fogo","Força temporária com pouca vida"};case VAMPIRE->new String[]{"Visão controlável e roubo de vida","Força e velocidade à noite"};case TIEFLING->new String[]{"Imunidade a fogo e lava","Visão escura e retaliação"};case LYCANTHROPE->new String[]{"Força, velocidade e regeneração à noite","Faro para criaturas"};case DRAGONBORN->new String[]{"12% de resistência física","Resistência elemental e a empurrão"};case HARPY->new String[]{"Salto alto e queda reduzida","Mobilidade a céu aberto"};default->new String[0];};}
    private String[] weaknesses(Race r){return switch(r){case ELF->new String[]{"Golpes corpo a corpo fortes causam Lentidão","Resistência física menor"};case FAIRY->new String[]{"No Nether, a regeneração é bloqueada","Lava bloqueia habilidades"};case SATYR->new String[]{"Armadura pesada reduz mobilidade","Penalidade subterrânea/artificial"};case THALASSIAN->new String[]{"Desidratação longe da água","Fogo aplica Fraqueza temporária"};case HUMAN->new String[]{"Sem resistências sobrenaturais"};case NEPHILIM->new String[]{"Cura 20% menor","Rajada exige mira e não oferece proteção"};case VAMPIRE->new String[]{"Sol suspende poderes noturnos; comida rende menos","Fogo aplica Fraqueza temporária"};case TIEFLING->new String[]{"Cura natural reduzida","Água causa Fraqueza"};case LYCANTHROPE->new String[]{"Fome limita bônus e habilidades noturnas","Durante o dia, os bônus noturnos ficam suspensos"};case DRAGONBORN->new String[]{"Movimento lento e fome aumentada","Elemento oposto aplica penalidade temporária"};case HARPY->new String[]{"Golpes físicos causam Lentidão breve","Cavernas e armadura pesada limitam as asas"};default->new String[0];};}
}
