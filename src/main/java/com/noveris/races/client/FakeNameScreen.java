package com.noveris.races.client;

import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
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
    /** A tela já desenha o painel opaco; o background padrão do Screen adicionaria blur por cima dele. */
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) { }
    @Override protected void init() {
        panelWidth = Math.min(width - 32, 1080); panelHeight = Math.min(height - 30, 650);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int x=left+50;
        int fieldW = Math.min(520, panelWidth - 100);
        nickname = new EditBox(font, x, top+82, fieldW, 22, Component.literal("Apelido"));
        nickname.setMaxLength(20); nickname.setValue(FakeNameClientState.nickname); addRenderableWidget(nickname);
        pronouns = new EditBox(font, x, top+82, fieldW, 22, Component.literal("Complemento"));
        pronouns.setMaxLength(10); pronouns.setValue(FakeNameClientState.pronouns); addRenderableWidget(pronouns);
        updateFieldVisibility();
    }
    private void updateFieldVisibility() { nickname.visible = !pronounTab; nickname.active = !pronounTab; pronouns.visible = pronounTab; pronouns.active = pronounTab; }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        frame(g, "APELIDO DE NOVERIS");
        int tabW=(panelWidth-74)/2;
        button(g,left+34,top+50,tabW,28,"APELIDO",mx,my,!pronounTab); button(g,left+40+tabW,top+50,tabW,28,"PRONOMES / COMPLEMENTO",mx,my,pronounTab);
        if (!pronounTab) {
            g.drawString(font,"APELIDO (ATÉ 20 CARACTERES)",left+50,top+68,WHITE,false);
            g.drawString(font,"O nome real continua visível para a administração.",left+50,top+114,MUTED,false);
            g.drawString(font,"FORMATAÇÃO",left+50,top+140,WHITE,false);
            int formatW=Math.min(150,(panelWidth-120)/3);
            for(int i=0;i<FORMATS.length;i++) button(g,left+50+(i%3)*(formatW+6),top+152+(i/3)*27,formatW,23,FORMATS[i].toUpperCase(),mx,my,format.equals(FORMATS[i]));
            g.drawString(font,"COR",left+50,top+214,WHITE,false);
            int colorW=Math.min(110,(panelWidth-120)/5);
            for(int i=0;i<COLORS.length;i++) button(g,left+50+(i%5)*(colorW+6),top+226+(i/5)*25,colorW,22,COLORS[i].toUpperCase(),mx,my,color.equals(COLORS[i]));
        } else {
            g.drawString(font,"PRONOMES OU COMPLEMENTO (ATÉ 10 LETRAS)",left+50,top+68,WHITE,false);
            g.drawString(font,"Exemplo: ela, ele ou título curto. Pode deixar vazio.",left+50,top+122,MUTED,false);
            g.drawString(font,"Será exibido ao lado do apelido no chat e no tab.",left+50,top+148,MUTED,false);
        }
        int bottomY=top+panelHeight-36;
        dangerButton(g,left+50,bottomY,260,26,"RESTAURAR NOME REAL",mx,my);
        button(g,left+panelWidth-310,bottomY,260,26,"SALVAR",mx,my,true);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseClicked(double mx,double my,int btn) {
        if(super.mouseClicked(mx,my,btn)) return true;
        if(btn!=0) return false;
        int tabW=(panelWidth-74)/2;
        if(inside(mx,my,left+40+tabW,top+50,tabW,28)){ pronounTab=true; updateFieldVisibility(); return true; }
        if(inside(mx,my,left+34,top+50,tabW,28)){ pronounTab=false; updateFieldVisibility(); return true; }
        if(!pronounTab){
            int formatW=Math.min(150,(panelWidth-120)/3);
            for(int i=0;i<FORMATS.length;i++) if(inside(mx,my,left+50+(i%3)*(formatW+6),top+152+(i/3)*27,formatW,23)){format=FORMATS[i];return true;}
            int colorW=Math.min(110,(panelWidth-120)/5);
            for(int i=0;i<COLORS.length;i++) if(inside(mx,my,left+50+(i%5)*(colorW+6),top+226+(i/5)*25,colorW,22)){color=COLORS[i];return true;}
        }
        int bottomY=top+panelHeight-36;
        if(inside(mx,my,left+50,bottomY,260,26)){ FakeNameClientState.nickname=""; nickname.setValue(""); pronouns.setValue(""); send("fakename_reset"); onClose(); return true; }
        if(inside(mx,my,left+panelWidth-310,bottomY,260,26)){ send("fakename_save"); onClose(); return true; }
        return true;
    }
    private void send(String action){ PacketDistributor.sendToServer(new ActionPayload(action, nickname.getValue(), pronouns.getValue(), format, color, "", "")); }
    private void dangerButton(GuiGraphics g,int x,int y,int w,int h,String text,double mx,double my){
        boolean hover=inside(mx,my,x,y,w,h);
        g.fill(x,y,x+w,y+h,hover?0xFFBE3E50:0xFF8F2435);
        g.drawCenteredString(font,text,x+w/2,y+9,WHITE);
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}
