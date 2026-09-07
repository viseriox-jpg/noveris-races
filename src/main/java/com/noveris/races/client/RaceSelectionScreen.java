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
    private boolean selectionRequestSent;
    private int actionX, actionY, actionW, actionH, specialX, specialY, specialW, specialH;
    private int sizeX, sizeY, sizeW, sizeH;
    private int confirmX, confirmY, backX, backY, confirmW, confirmH, backW, backH;

    public RaceSelectionScreen() { super("Linhagens de Noveris"); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        // O primeiro sync do servidor pode chegar depois que a tela abriu.
        // Uma raça confirmada é definitiva e nunca deve deixar esta tela aberta.
        if (ClientRaceState.confirmed && ClientRaceState.race != Race.NONE) { minecraft.setScreen(null); return; }
        frame(g, "LINHAGENS DE NOVERIS");
        // A confirmação substitui a seleção inteira; não renderize os
        // componentes da tela anterior por baixo do modal.
        if (confirming) {
            renderConfirmation(g, mx, my);
            return;
        }
        String realmTitle = realm.title.toUpperCase();
        g.drawString(font, realmTitle, left + panelWidth - font.width(realmTitle) - 34, top + 28, DANGER, false);
        renderSizeControls(g, mx, my);
        renderTabs(g, mx, my);
        renderRaceCards(g);
        renderDetails(g, mx, my);
        actionW=240; actionH=24; actionX=left+(panelWidth-actionW)/2; actionY=top+panelHeight-32;
        button(g, actionX, actionY, actionW, actionH, "CONFIRMAR RAÇA", mx, my, true);
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
        // A confirmação é uma tela modal de verdade: cobre completamente o
        // painel de seleção para que nenhum texto ou botão de trás atravesse.
        g.fill(0, 0, width, height, 0xFF0D0C09);
        // Modal amplo e com linhas fixas: o título completo não pode invadir
        // a linha da raça nem os botões em fontes/resoluções diferentes.
        int w=Math.min(650,panelWidth-44),h=170,x=left+(panelWidth-w)/2,y=top+(panelHeight-h)/2;
        g.fill(x-3,y-3,x+w+3,y+h+3,BORDER);g.fill(x,y,x+w,y+h,0xFC0D0C09);
        g.drawCenteredString(font,"DESEJA REALMENTE ESCOLHER ESTA RAÇA?",x+w/2,y+20,WHITE);
        String choice=selected.title+(selected==Race.DRAGONBORN?" — "+lineage.title:selected==Race.FAIRY?" — "+fairyAffinity.title:"")+" — "+size.title;
        g.drawCenteredString(font,choice.toUpperCase(),x+w/2,y+50,selected.color);
        g.drawCenteredString(font,"A raça será escolhida imediatamente.",x+w/2,y+77,MUTED);
        confirmW=220;confirmH=28;backW=220;backH=28;
        int gap=16,buttonsWidth=confirmW+gap+backW;
        confirmX=x+(w-buttonsWidth)/2;confirmY=y+116;backX=confirmX+confirmW+gap;backY=confirmY;
        button(g,confirmX,confirmY,confirmW,confirmH,"CONFIRMAR",mx,my,true);button(g,backX,backY,backW,backH,"VOLTAR",mx,my,true);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(confirming){
            if(inside(mx,my,confirmX,confirmY,confirmW,confirmH)){
                if(selectionRequestSent) return true;
                selectionRequestSent=true;
                ClientRaceState.selectionPending=true;
                PacketDistributor.sendToServer(new ActionPayload("select",selected.name(),lineage.name(),fairyAffinity.name(),ancestryA.name(),ancestryB.name(),size.name()));
                return true;
            }
            if(inside(mx,my,backX,backY,backW,backH)){
                if(!selectionRequestSent) confirming=false;
                return true;
            }
            return true;
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
    private String description(Race r){return RaceInfo.description(r);}
    private String active(Race r){return RaceInfo.active(r, fairyAffinity);}
    private String mobility(Race r){return RaceInfo.mobility(r, fairyAffinity);}
    private String[] passives(Race r){return RaceInfo.passives(r);}
    private String[] weaknesses(Race r){return RaceInfo.weaknesses(r, lineage);}
}
