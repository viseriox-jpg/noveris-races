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
    private Race ancestryA = Race.ELF, ancestryB = Race.HUMAN;
    private boolean confirming;
    private int actionX, actionY, actionW, actionH, specialX, specialY, specialW, specialH;
    private int confirmX, confirmY, backX, backY;

    public RaceSelectionScreen() { super("Linhagens de Noveris"); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mx, int my, float partialTick) {
        super.render(g, mx, my, partialTick);
        frame(g, "LINHAGENS DE NOVERIS");
        String realmTitle = realm.title.toUpperCase();
        g.drawString(font, realmTitle, left + panelWidth - font.width(realmTitle) - 34, top + 28, DANGER, false);
        renderTabs(g, mx, my);
        renderRaceCards(g);
        renderDetails(g, mx, my);
        actionW=240; actionH=26; actionX=left+(panelWidth-actionW)/2; actionY=top+panelHeight-40;
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
        g.drawCenteredString(font,label,x+w/2,y+8,active?WHITE:MUTED);
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
        g.drawString(font,selected.realm.title+"  •  "+((int)selected.maxHealth/2)+" corações",x,y+14,LILAC,false);
        g.drawString(font,description(selected),x,y+30,MUTED,false);
        g.drawString(font,"HABILIDADES",x,y+52,WHITE,false);
        g.drawString(font,"◆ "+active(selected),x,y+68,selected.color,false);
        g.drawString(font,"◆ "+mobility(selected),x,y+82,LILAC,false);
        g.drawString(font,"PASSIVAS",right,y,WHITE,false); drawLines(g,passives(selected),right,y+15,LILAC);
        g.drawString(font,"FRAQUEZAS / DEBUFFS",right,y+58,DANGER,false); drawLines(g,weaknesses(selected),right,y+73,MUTED);
        specialX=x;specialY=y+106;specialW=112;specialH=22;
        if(selected==Race.DRAGONBORN){
            g.drawString(font,"LINHAGEM DRACÔNICA",x,specialY-14,WHITE,false);
            option(g,specialX,specialY,"FOGO",lineage==DragonLineage.FIRE,0xFFFF8A4B,mx,my);
            option(g,specialX+specialW+6,specialY,"GELO",lineage==DragonLineage.FROST,0xFF80D9FF,mx,my);
            option(g,specialX+(specialW+6)*2,specialY,"VENENO",lineage==DragonLineage.VENOM,0xFF86D48A,mx,my);
        }else if(selected==Race.HALF_BLOOD){
            g.drawString(font,"DUAS ASCENDÊNCIAS — versões reduzidas",x,specialY-14,WHITE,false);
            option(g,specialX,specialY,ancestryA.title.toUpperCase(),true,ancestryA.color,mx,my);
            option(g,specialX+specialW+6,specialY,ancestryB.title.toUpperCase(),true,ancestryB.color,mx,my);
            g.drawString(font,"Clique para alternar",specialX+specialW*2+18,specialY+7,MUTED,false);
        }
    }
    private void option(GuiGraphics g,int x,int y,String label,boolean active,int color,int mx,int my){
        boolean hover=inside(mx,my,x,y,specialW,specialH);
        g.fill(x,y,x+specialW,y+specialH,active?WINE:hover?WINE_HOVER:0xFF17140E);
        g.drawCenteredString(font,label,x+specialW/2,y+7,active?color:MUTED);
    }
    private void renderConfirmation(GuiGraphics g,int mx,int my){
        int w=430,h=124,x=left+(panelWidth-w)/2,y=top+(panelHeight-h)/2;
        g.fill(x-3,y-3,x+w+3,y+h+3,BORDER);g.fill(x,y,x+w,y+h,0xFC0D0C09);
        g.drawCenteredString(font,"DESEJA REALMENTE ESCOLHER ESTA RAÇA?",x+w/2,y+24,WHITE);
        String choice=selected.title+(selected==Race.DRAGONBORN?" — "+lineage.title:selected==Race.HALF_BLOOD?" — "+ancestryA.title+" + "+ancestryB.title:"");
        g.drawCenteredString(font,choice.toUpperCase(),x+w/2,y+47,selected.color);
        g.drawCenteredString(font,"Você iniciará o teste de 5 minutos.",x+w/2,y+64,MUTED);
        confirmX=x+34;confirmY=y+86;backX=x+226;backY=confirmY;
        button(g,confirmX,confirmY,170,26,"CONFIRMAR",mx,my,true);button(g,backX,backY,170,26,"VOLTAR",mx,my,true);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(confirming){
            if(inside(mx,my,confirmX,confirmY,170,26)){PacketDistributor.sendToServer(new ActionPayload("trial",selected.name(),lineage.name(),ancestryA.name(),ancestryB.name()));minecraft.setScreen(null);return true;}
            if(inside(mx,my,backX,backY,170,26)){confirming=false;return true;}return true;
        }
        if(inside(mx,my,actionX,actionY,actionW,actionH)){confirming=true;return true;}
        int tabY=top+54,gap=6,tabW=(panelWidth-68-gap*2)/3;
        for(int i=0;i<3;i++)if(inside(mx,my,left+34+i*(tabW+gap),tabY,tabW,24)){realm=RaceRealm.values()[i];selected=racesForRealm()[0];return true;}
        Race[] races=racesForRealm();int cardY=top+94,cardW=(panelWidth-68-gap*(races.length-1))/races.length;
        for(int i=0;i<races.length;i++)if(inside(mx,my,left+34+i*(cardW+gap),cardY,cardW,44)){selected=races[i];return true;}
        if(selected==Race.DRAGONBORN&&my>=specialY&&my<specialY+specialH){
            if(inside(mx,my,specialX,specialY,specialW,specialH))lineage=DragonLineage.FIRE;
            else if(inside(mx,my,specialX+specialW+6,specialY,specialW,specialH))lineage=DragonLineage.FROST;
            else if(inside(mx,my,specialX+(specialW+6)*2,specialY,specialW,specialH))lineage=DragonLineage.VENOM;return true;}
        if(selected==Race.HALF_BLOOD&&my>=specialY&&my<specialY+specialH){
            if(inside(mx,my,specialX,specialY,specialW,specialH))ancestryA=nextAncestry(ancestryA,ancestryB);
            else if(inside(mx,my,specialX+specialW+6,specialY,specialW,specialH))ancestryB=nextAncestry(ancestryB,ancestryA);return true;}
        return super.mouseClicked(mx,my,button);
    }
    private Race nextAncestry(Race current,Race other){Race[] valid=Arrays.stream(Race.values()).filter(Race::validAncestry).toArray(Race[]::new);int i=Arrays.asList(valid).indexOf(current);do{i=(i+1)%valid.length;}while(valid[i]==other);return valid[i];}
    private boolean inside(double x,double y,int bx,int by,int bw,int bh){return x>=bx&&x<bx+bw&&y>=by&&y<by+bh;}
    private Race[] racesForRealm(){return Arrays.stream(Race.values()).filter(r->r!=Race.NONE&&r.realm==realm).toArray(Race[]::new);}
    private void drawLines(GuiGraphics g,String[] lines,int x,int y,int color){for(int i=0;i<lines.length;i++)g.drawString(font,"• "+lines[i],x,y+i*13,color,false);}
    private void drawLargeSymbol(GuiGraphics g,String s,int cx,int y,int color){g.pose().pushPose();g.pose().translate(cx,y,0);g.pose().scale(1.35f,1.35f,1f);g.drawCenteredString(font,s,0,0,color);g.pose().popPose();}
    private String symbol(Race r){return switch(r){case ELF->"⌁";case FAIRY->"✦";case SATYR->"♈";case THALASSIAN->"≈";case HUMAN->"●";case NEPHILIM->"◇";case VAMPIRE->"▼";case REVENANT->"✚";case HALF_BLOOD->"∞";case TIEFLING->"♠";case LYCANTHROPE->"☾";case DRAGONBORN->"◆";case HARPY->"⌁";default->"?";};}
    private String description(Race r){return switch(r){case ELF->"Exploração, conhecimento, magia e combate à distância.";case FAIRY->"Magia, suporte e conexão com a natureza — sem voo.";case SATYR->"Mobilidade terrestre e sobrevivência natural.";case THALASSIAN->"Exploração oceânica e combate aquático.";case HUMAN->"Versatilidade, ferramentas e progressão.";case NEPHILIM->"Resistência sobrenatural equilibrada — sem voo.";case VAMPIRE->"Sustentação e combate noturno.";case REVENANT->"Resistência e sobrevivência prolongada.";case HALF_BLOOD->"Duas heranças menores, nunca dois poderes completos.";case TIEFLING->"Sobrevivência infernal e retaliação.";case LYCANTHROPE->"Predador fortalecido pela noite.";case DRAGONBORN->"Tanque ofensivo de linhagem elemental.";case HARPY->"Exploração vertical sem voo verdadeiro.";default->"";};}
    private String active(Race r){return switch(r){case ELF->"Foco do Arqueiro";case FAIRY->"Percepção Feérica";case SATYR->"Vigor Silvestre";case THALASSIAN->"Guarda das Marés";case HUMAN->"Adaptação";case NEPHILIM->"Égide Sobrenatural";case VAMPIRE->"Drenagem de Sangue";case REVENANT->"Recusa da Morte";case HALF_BLOOD->"Herança Combinada";case TIEFLING->"Pulso Infernal";case LYCANTHROPE->"Uivo de Caçada";case DRAGONBORN->"Sopro Elemental";case HARPY->"Rajada de Vento";default->"—";};}
    private String mobility(Race r){return switch(r){case ELF->"Passo Florestal";case FAIRY->"Salto Floral";case SATYR->"Investida Caprina";case THALASSIAN->"Impulso Aquático";case HUMAN->"Arrancada Determinada";case NEPHILIM->"Impulso Radiante";case VAMPIRE->"Passo Sombrio";case REVENANT->"Avanço Implacável";case HALF_BLOOD->"Mobilidade Herdada reduzida";case TIEFLING->"Avanço em Fogo";case LYCANTHROPE->"Bote Predatório";case DRAGONBORN->"Investida Dracônica";case HARPY->"Impulso Alado";default->"—";};}
    private String[] passives(Race r){return switch(r){case ELF->new String[]{"Visão leve e velocidade em florestas","Encantamentos baratos; arcos mais fortes"};case FAIRY->new String[]{"Regenera perto da natureza","Poções positivas maiores; resistência mágica"};case SATYR->new String[]{"Rápido no natural; salto alto","Menos queda e regeneração florestal"};case THALASSIAN->new String[]{"Respiração, visão e mineração aquáticas","Nado rápido; resistência submersa"};case HUMAN->new String[]{"Mais experiência e eficiência","Adaptação a equipamentos"};case NEPHILIM->new String[]{"Resistência a fogo e debuffs","Força temporária com pouca vida"};case VAMPIRE->new String[]{"Visão noturna e roubo de vida","Força e velocidade à noite"};case REVENANT->new String[]{"Imune a veneno; fome reduzida","Sobrevive a dano fatal com recarga"};case HALF_BLOOD->new String[]{"Uma característica menor de cada origem","Fraquezas parcialmente herdadas"};case TIEFLING->new String[]{"Imunidade a fogo e lava","Visão escura e retaliação"};case LYCANTHROPE->new String[]{"Força e regeneração à noite","Faro para criaturas"};case DRAGONBORN->new String[]{"12% de resistência física","Resistência elemental e a empurrão"};case HARPY->new String[]{"Salto, planagem e queda reduzida","Mobilidade a céu aberto"};default->new String[0];};}
    private String[] weaknesses(Race r){return switch(r){case ELF->new String[]{"12% mais dano físico","Golpes corpo a corpo fortes castigam"};case FAIRY->new String[]{"8 corações; ferro causa mais dano","Regeneração reduzida no infernal"};case SATYR->new String[]{"Armadura pesada reduz mobilidade","Penalidade subterrânea/artificial"};case THALASSIAN->new String[]{"Desidratação longe da água","Maior vulnerabilidade ao fogo"};case HUMAN->new String[]{"Sem resistências sobrenaturais"};case NEPHILIM->new String[]{"Recargas altas","Regeneração menos eficiente"};case VAMPIRE->new String[]{"Sol enfraquece; comida rende menos","Fogo causa dano adicional"};case REVENANT->new String[]{"Curas reduzidas","Energia sagrada causa mais dano"};case HALF_BLOOD->new String[]{"Heranças são reduzidas","Nunca herda dois poderes principais"};case TIEFLING->new String[]{"Cura natural reduzida","Água causa fraqueza"};case LYCANTHROPE->new String[]{"Sol, prata e fome acelerada"};case DRAGONBORN->new String[]{"Lentidão, fome e elemento oposto"};case HARPY->new String[]{"Fragilidade, cavernas e armadura pesada"};default->new String[0];};}
}
