package com.noveris.races.client;

import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** Editor visual do apelido; o nome real nunca é editado. */
public final class FakeNameScreen extends NoverisScreen {
    private EditBox nickname, pronouns;
    private boolean pronounTab;
    private String format, color, prefix;
    private static final String[] FORMATS = {"normal", "bold", "italic", "underlined", "strikethrough", "uniform"};
    private static final String[] COLORS = {"white", "yellow", "gold", "red", "dark_red", "green", "blue", "purple", "dark_purple", "aqua"};

    public FakeNameScreen() { super("APELIDO DE NOVERIS"); format=FakeNameClientState.format; color=FakeNameClientState.color; prefix=FakeNameClientState.prefix; }
    /** A tela já desenha o painel opaco; o background padrão do Screen adicionaria blur por cima dele. */
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) { }
    @Override protected void init() {
        panelWidth = Math.min(width - 32, 1080); panelHeight = Math.min(height - 30, 650);
        left = (width - panelWidth) / 2; top = (height - panelHeight) / 2;
        int x=left+50;
        int fieldW = Math.min(420, panelWidth / 2 - 80);
        nickname = new EditBox(font, x, top+100, fieldW, 22, Component.literal("Apelido"));
        nickname.setMaxLength(20); nickname.setValue(FakeNameClientState.nickname); addRenderableWidget(nickname);
        pronouns = new EditBox(font, x, top+100, fieldW, 22, Component.literal("Complemento"));
        pronouns.setMaxLength(10); pronouns.setValue(FakeNameClientState.pronouns); addRenderableWidget(pronouns);
        updateFieldVisibility();
    }
    private void updateFieldVisibility() { nickname.visible = !pronounTab; nickname.active = !pronounTab; pronouns.visible = pronounTab; pronouns.active = pronounTab; }
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        frame(g, "APELIDO DE NOVERIS");
        int tabW=(panelWidth-74)/2;
        button(g,left+34,top+50,tabW,28,"APELIDO",mx,my,!pronounTab); button(g,left+40+tabW,top+50,tabW,28,"PRONOMES / COMPLEMENTO",mx,my,pronounTab);
        if (!pronounTab) {
            g.drawString(font,"APELIDO (ATÉ 20 CARACTERES)",left+50,top+88,WHITE,false);
            g.drawString(font,"O nome real continua visível para a administração.",left+50,top+130,MUTED,false);
            g.drawString(font,"FORMATAÇÃO",left+50,top+154,WHITE,false);
            int formatW=Math.min(150,(panelWidth-120)/3);
            for(int i=0;i<FORMATS.length;i++) button(g,left+50+(i%3)*(formatW+6),top+166+(i/3)*25,formatW,21,FORMATS[i].toUpperCase(),mx,my,hasFormat(FORMATS[i]));
            g.drawString(font,"COR",left+50,top+220,WHITE,false);
            int colorW=Math.min(110,(panelWidth-120)/6);
            for(int i=0;i<COLORS.length;i++) coloredButton(g,left+50+(i%6)*(colorW+6),top+232+(i/6)*24,colorW,21,COLORS[i],mx,my,color.equals(COLORS[i]));
        } else {
            g.drawString(font,"PRONOMES OU COMPLEMENTO (ATÉ 10 LETRAS)",left+50,top+88,WHITE,false);
            g.drawString(font,"Exemplo: ela, ele ou título curto. Pode deixar vazio.",left+50,top+132,MUTED,false);
            g.drawString(font,"Será exibido ao lado do apelido no chat e no tab.",left+50,top+150,MUTED,false);
            g.drawString(font,"PREFIXO",left+50,top+174,WHITE,false);
            button(g,left+50,top+186,150,24,"NENHUM",mx,my,prefix.equals("none"));
            button(g,left+206,top+186,150,24,"AVARION",mx,my,prefix.equals("avarion"));
            button(g,left+362,top+186,150,24,"ORVANNIS",mx,my,prefix.equals("orvannis"));
            g.drawString(font,"Avarion: verde escuro   •   Orvannis: roxo escuro",left+50,top+222,MUTED,false);
        }
        int bottomY=top+panelHeight-34;
        button(g,left+panelWidth-310,bottomY,260,26,"SALVAR",mx,my,true);
        super.render(g,mx,my,partial);
        drawPreview(g);
    }
    @Override public boolean mouseClicked(double mx,double my,int btn) {
        if(super.mouseClicked(mx,my,btn)) return true;
        if(btn!=0) return false;
        int tabW=(panelWidth-74)/2;
        if(inside(mx,my,left+40+tabW,top+50,tabW,28)){ pronounTab=true; updateFieldVisibility(); return true; }
        if(inside(mx,my,left+34,top+50,tabW,28)){ pronounTab=false; updateFieldVisibility(); return true; }
        if(!pronounTab){
            int formatW=Math.min(150,(panelWidth-120)/3);
            for(int i=0;i<FORMATS.length;i++) if(inside(mx,my,left+50+(i%3)*(formatW+6),top+166+(i/3)*25,formatW,21)){toggleFormat(FORMATS[i]);return true;}
            int colorW=Math.min(110,(panelWidth-120)/6);
            for(int i=0;i<COLORS.length;i++) if(inside(mx,my,left+50+(i%6)*(colorW+6),top+232+(i/6)*24,colorW,21)){color=COLORS[i];return true;}
        } else {
            if(inside(mx,my,left+50,top+186,150,24)){prefix="none";return true;}
            if(inside(mx,my,left+206,top+186,150,24)){prefix="avarion";return true;}
            if(inside(mx,my,left+362,top+186,150,24)){prefix="orvannis";return true;}
        }
        int bottomY=top+panelHeight-34;
        if(inside(mx,my,left+panelWidth-310,bottomY,260,26)){ send("fakename_save"); onClose(); return true; }
        return true;
    }
    private void send(String action){ PacketDistributor.sendToServer(new ActionPayload(action, nickname.getValue(), pronouns.getValue(), format, color, prefix, "")); }
    private boolean hasFormat(String f){ return format.equals(f) || format.contains(","+f) || format.contains(f+","); }
    private void toggleFormat(String f){
        if(f.equals("normal")){ format="normal"; return; }
        java.util.LinkedHashSet<String> set=new java.util.LinkedHashSet<>();
        for(String s:format.split(",")) if(!s.isBlank()&&!s.equals("normal")) set.add(s);
        if(!set.add(f)) set.remove(f);
        format=set.isEmpty()?"normal":String.join(",",set);
    }
    private void coloredButton(GuiGraphics g,int x,int y,int w,int h,String value,double mx,double my,boolean active){
        button(g,x,y,w,h,colorLabel(value),mx,my,active);
        int swatch=colorValue(value); g.fill(x+5,y+5,x+15,y+h-5,swatch);
    }
    private int colorValue(String value){ return switch(value){case "yellow"->0xFFFFE83B;case "gold"->0xFFFFB300;case "red"->0xFFFF3B30;case "dark_red"->0xFF8F2435;case "green"->0xFF35C759;case "blue"->0xFF368AFF;case "purple"->0xFFFF69B4;case "dark_purple"->0xFF5B2C83;case "aqua"->0xFF32D7E8;default->0xFFFFFFFF;}; }
    private ChatFormatting colorFormatting(String value){
        if ("purple".equals(value)) return ChatFormatting.LIGHT_PURPLE;
        if ("dark_red".equals(value)) return ChatFormatting.DARK_RED;
        if ("dark_purple".equals(value)) return ChatFormatting.DARK_PURPLE;
        ChatFormatting parsed=ChatFormatting.getByName(value);
        return parsed==null?ChatFormatting.WHITE:parsed;
    }
    private String colorLabel(String value){ return switch(value){case "purple"->"PINK";case "dark_red"->"DARK RED";case "dark_purple"->"DARK PURPLE";default->value.toUpperCase();}; }
    private Component preview(){
        String name=nickname.getValue().isBlank()?"SeuNome":nickname.getValue();
        Style style=Style.EMPTY.withColor(colorFormatting(color));
        for(String f:format.split(",")) switch(f){case "bold"->style=style.withBold(true);case "italic"->style=style.withItalic(true);case "underlined"->style=style.withUnderlined(true);case "strikethrough"->style=style.withStrikethrough(true);case "uniform"->style=style.withFont(ResourceLocation.withDefaultNamespace("uniform"));default->{}};
        MutableComponent c=switch(prefix){case "avarion"->Component.literal("Avarion ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(true));case "orvannis"->Component.literal("Orvannis ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withBold(true));default->Component.empty();};
        return c.append(Component.literal(name).withStyle(style)).append(pronouns.getValue().isBlank()?Component.empty():Component.literal(" ["+pronouns.getValue()+"]").withStyle(ChatFormatting.GRAY));
    }
    private void drawPreview(GuiGraphics g){
        int x=left+panelWidth/2+20; int y=top+88;
        g.drawString(font,"PRÉVIA",x,y,WHITE,false);
        g.enableScissor(x,y+14,left+panelWidth-34,y+42);
        g.drawString(font,preview(),x,y+20,WHITE,false);
        g.disableScissor();
    }
    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
}
