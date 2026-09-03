package com.noveris.races;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Dados do apelido exibido; o GameProfile/nome real nunca é alterado. */
public final class FakeNameState {
    private static final String ROOT = "NoverisFakeName";
    private FakeNameState() {}

    private static net.minecraft.nbt.CompoundTag root(ServerPlayer p) {
        var data = p.getPersistentData();
        if (!data.contains(ROOT)) data.put(ROOT, new net.minecraft.nbt.CompoundTag());
        return data.getCompound(ROOT);
    }
    public static String nickname(ServerPlayer p) { return root(p).getString("Nickname"); }
    public static String pronouns(ServerPlayer p) { return root(p).getString("Pronouns"); }
    public static String format(ServerPlayer p) { return root(p).getString("Format"); }
    public static String color(ServerPlayer p) { return root(p).getString("Color"); }
    public static void save(ServerPlayer p, String nickname, String pronouns, String format, String color) {
        var tag = root(p);
        tag.putString("Nickname", clean(nickname, 20));
        tag.putString("Pronouns", clean(pronouns, 10));
        tag.putString("Format", switch (format.toLowerCase()) {
            case "bold", "italic", "underlined", "strikethrough", "uniform" -> format.toLowerCase();
            default -> "normal";
        });
        tag.putString("Color", validColor(color) ? color.toLowerCase() : "white");
    }
    public static void reset(ServerPlayer p) { p.getPersistentData().remove(ROOT); }
    private static String clean(String value, int max) {
        if (value == null) return "";
        String cleaned = value.replaceAll("[\\n\\r\\t]", "").trim();
        return cleaned.substring(0, Math.min(max, cleaned.length()));
    }
    private static boolean validColor(String c) {
        return c != null && switch (c.toLowerCase()) {
            case "white", "yellow", "gold", "red", "green", "blue", "purple", "aqua", "gray" -> true;
            default -> false;
        };
    }
    public static Component displayName(ServerPlayer p) {
        String name = nickname(p);
        if (name.isBlank()) name = p.getGameProfile().getName();
        String suffix = pronouns(p).isBlank() ? "" : " §7[" + pronouns(p) + "]";
        Style style = Style.EMPTY.withColor(ChatFormatting.getByName(color(p).isBlank() ? "white" : color(p)));
        switch (format(p)) {
            case "bold" -> style = style.withBold(true);
            case "italic" -> style = style.withItalic(true);
            case "underlined" -> style = style.withUnderlined(true);
            case "strikethrough" -> style = style.withStrikethrough(true);
            case "uniform" -> style = style.withFont(ResourceLocation.withDefaultNamespace("uniform"));
            default -> { }
        }
        return Component.literal(name).withStyle(style).append(Component.literal(suffix));
    }
}
