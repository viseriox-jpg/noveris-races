package com.noveris.races.client;

import com.noveris.races.NoverisRaces;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Editor visual do apelido; o nome real nunca é editado. */
public final class FakeNameScreen extends NoverisScreen {
    private EditBox nickname, pronouns;
    private boolean pronounTab;
    private String format, color;
    private static final String[] FORMATS = {"normal", "bold", "italic", "underlined", "strikethrough", "uniform"};
    private static final String[] COLORS = {"white", "yellow", "gold", "red", "green", "blue", "purple", "aqua", "gray"};

    public FakeNameScreen() { super("APELIDO DE NOVERIS"); format=FakeNameClientState.format; color=FakeNameClientState.color; }
    @Override protected void init() {
        panelWidth = Math.min(width - 32, 1080); panelHeight = Math.min(height - 30, 650);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int x=left+50;
        nickname = new EditBox(font, x, top+92, 420, 22, Component.literal("Apelido")); nickname.setMaxLength(20); nickname.setValue(FakeNameClientState.nickname); addRenderableWidget(nickname);
        pronouns = new EditBox(font, x, top+150, 420, 22, Component.literal("Complemento")); pronouns.setMaxLength(10); pronouns.setValue(FakeNameClientState.pronouns); addRenderableWidget(pronouns);
    }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        frame(g, "APELIDO DE NOVERIS");
        button(g,left+34,top+50,480,32,"APELIDO",mx,my,!pronounTab); button(g,left+520,top+50,480,32,"PRONOMES / COMPLEMENTO",mx,my,pronounTab);
        if (!pronounTab) {
            g.drawString(font,"APELIDO (ATÉ 20 CARACTERES)",left+50,top+76,WHITE,false);
            g.drawString(font,"O nome real continua visível para a administração.",left+50,top+125,MUTED,false);
            g.drawString(font,"FORMATAÇÃO",left+50,top+195,WHITE,false);
            for(int i=0;i<FORMATS.length;i++) button(g,left+50+(i%3)*155,top+215+(i/3)*32,145,26,FORMATS[i].toUpperCase(),mx,my,format.equals(FORMATS[i]));
            g.drawString(font,"COR",left+50,top+325,WHITE,false);
            for(int i=0;i<COLORS.length;i++) button(g,left+50+(i%5)*140,top+345+(i/5)*32,130,26,COLORS[i].toUpperCase(),mx,my,color.equals(COLORS[i]));
        } else {
            g.drawString(font,"PRONOMES OU COMPLEMENTO (ATÉ 10 LETRAS)",left+50,top+76,WHITE,false);
            g.drawString(font,"Exemplo: ela, ele, elu ou título curto. Pode deixar vazio.",left+50,top+184,MUTED,false);
        }
        button(g,left+50,top+panelHeight-58,260,34,"RESTAURAR NOME REAL",mx,my,true);
        button(g,left+panelWidth-330,top+panelHeight-58,280,34,"SALVAR",mx,my,true);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(double mx,double my,int btn) {
        if(super.mouseClicked(mx,my,btn)) return true;
        if(btn!=0) return false;
        if(inside(mx,my,left+520,top+50,480,32)){ pronounTab=true; return true; }
        if(inside(mx,my,left+34,top+50,480,32)){ pronounTab=false; return true; }
        if(!pronounTab){
            for(int i=0;i<FORMATS.length;i++) if(inside(mx,my,left+50+(i%3)*155,top+215+(i/3)*32,145,26)){format=FORMATS[i];return true;}
            for(int i=0;i<COLORS.length;i++) if(inside(mx,my,left+50+(i%5)*140,top+345+(i/5)*32,130,26)){color=COLORS[i];return true;}
        }
        if(inside(mx,my,left+50,top+panelHeight-58,260,34)){ FakeNameClientState.nickname=""; nickname.setValue(""); send("fakename_reset"); onClose(); return true; }
        if(inside(mx,my,left+panelWidth-330,top+panelHeight-58,280,34)){ send("fakename_save"); onClose(); return true; }
        return true;
    }
    private void send(String action){ PacketDistributor.sendToServer(new ActionPayload(action, nickname.getValue(), pronouns.getValue(), format, color, "", "", "")); }
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}
