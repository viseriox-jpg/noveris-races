package com.noveris.races;

import java.util.Locale;

public enum Race {
    NONE("Sem raça", 20.0, 1.0f, 0xFFB8A7BD),
    TIEFLING("Tiefling", 20.0, 1.0f, 0xFFE05252),
    LYCANTHROPE("Licantropo", 24.0, 1.05f, 0xFFC6B7A1),
    DRAGONBORN("Draconato", 26.0, 1.05f, 0xFFD18A62),
    HARPY("Harpia", 18.0, 0.95f, 0xFFDCC6E8);

    public final String title;
    public final double maxHealth;
    public final float scale;
    public final int color;

    Race(String title, double maxHealth, float scale, int color) {
        this.title = title;
        this.maxHealth = maxHealth;
        this.scale = scale;
        this.color = color;
    }

    public static Race parse(String value) {
        if (value == null) return NONE;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("licantropo")) return LYCANTHROPE;
        if (normalized.equals("draconato")) return DRAGONBORN;
        if (normalized.equals("harpia")) return HARPY;
        try { return valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return NONE; }
    }
}
