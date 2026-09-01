package com.noveris.races;

import java.util.Locale;

public enum Race {
    NONE("Sem raça", 20.0, 1.0f, 0xFFC8BFA6),
    TIEFLING("Tiefling", 20.0, 1.0f, 0xFFE58B32),
    LYCANTHROPE("Licantropo", 24.0, 1.05f, 0xFFD7CDB4),
    DRAGONBORN("Draconato", 26.0, 1.05f, 0xFFD6A13A),
    HARPY("Harpia", 18.0, 0.95f, 0xFFE9DDBB);

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
