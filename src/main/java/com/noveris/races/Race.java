package com.noveris.races;

import java.util.Locale;

public enum Race {
    NONE("Sem raça", RaceRealm.NEUTRAL, 20.0, 1f, 1f, 0xFFC8BFA6),
    ELF("Elfo", RaceRealm.ORVANNIS, 20.0, .98f, 1.05f, 0xFFA8D49B),
    FAIRY("Feérico", RaceRealm.ORVANNIS, 16.0, .85f, .92f, 0xFFE8B8DE),
    SATYR("Sátiro", RaceRealm.ORVANNIS, 22.0, .98f, 1.05f, 0xFFC69B62),
    THALASSIAN("Thalassiano", RaceRealm.ORVANNIS, 22.0, 1f, 1.07f, 0xFF68C9D0),
    HUMAN("Humano", RaceRealm.NEUTRAL, 20.0, .95f, 1f, 0xFFD8CBB1),
    NEPHILIM("Nephilin", RaceRealm.NEUTRAL, 22.0, 1.02f, 1.10f, 0xFFE4D49A),
    VAMPIRE("Vampiro", RaceRealm.NEUTRAL, 24.0, .98f, 1.05f, 0xFFBFA3A3),
    REVENANT("Revenante", RaceRealm.NEUTRAL, 26.0, 1.05f, 1.15f, 0xFF9DA69B),
    HALF_BLOOD("Meio-Sangue", RaceRealm.NEUTRAL, 20.0, .95f, 1f, 0xFFD1B98A),
    TIEFLING("Tiefling", RaceRealm.AVARION, 20.0, .98f, 1.05f, 0xFFE58B32),
    LYCANTHROPE("Licantropo", RaceRealm.AVARION, 24.0, .95f, 1f, 0xFFD7CDB4),
    DRAGONBORN("Draconato", RaceRealm.AVARION, 26.0, 1.05f, 1.15f, 0xFFD6A13A),
    HARPY("Harpia", RaceRealm.AVARION, 18.0, .88f, .95f, 0xFFE9DDBB);

    public final String title;
    public final RaceRealm realm;
    public final double maxHealth;
    public final float smallScale;
    public final float maxScale;
    public final int color;

    Race(String title, RaceRealm realm, double maxHealth, float smallScale, float maxScale, int color) {
        this.title = title;
        this.realm = realm;
        this.maxHealth = maxHealth;
        this.smallScale = smallScale;
        this.maxScale = Math.min(1.15f, maxScale);
        this.color = color;
    }

    public static Race parse(String value) {
        if (value == null) return NONE;
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("licantropo")) return LYCANTHROPE;
        if (normalized.equals("draconato")) return DRAGONBORN;
        if (normalized.equals("harpia")) return HARPY;
        if (normalized.equals("elfo")) return ELF;
        if (normalized.equals("feérico") || normalized.equals("feerico")) return FAIRY;
        if (normalized.equals("sátiro") || normalized.equals("satiro")) return SATYR;
        if (normalized.equals("thalassiano")) return THALASSIAN;
        if (normalized.equals("humano")) return HUMAN;
        if (normalized.equals("nephilin")) return NEPHILIM;
        if (normalized.equals("vampiro")) return VAMPIRE;
        if (normalized.equals("revenante")) return REVENANT;
        if (normalized.equals("meio-sangue") || normalized.equals("meio_sangue")) return HALF_BLOOD;
        try { return valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return NONE; }
    }

    public boolean validAncestry() { return this != NONE && this != HALF_BLOOD; }
    public float scale(RaceSize size) { return size == RaceSize.SMALL ? smallScale : maxScale; }
}
