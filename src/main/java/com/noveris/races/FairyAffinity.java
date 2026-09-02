package com.noveris.races;

public enum FairyAffinity {
    NONE("Nenhuma"),
    NATURE("Natureza"),
    WATER("Água"),
    AIR("Ar");

    public final String title;
    FairyAffinity(String title) { this.title = title; }

    public static FairyAffinity parse(String value) {
        if (value == null || value.isBlank()) return NONE;
        try { return valueOf(value.toUpperCase()); }
        catch (IllegalArgumentException ignored) { return NONE; }
    }
}
