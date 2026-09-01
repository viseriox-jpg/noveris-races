package com.noveris.races;

public enum RaceSize {
    SMALL("Porte menor"), MEDIUM("Porte médio"), LARGE("Porte maior");
    public final String title;
    RaceSize(String title) { this.title = title; }
    public static RaceSize parse(String value) {
        if ("STANDARD".equals(value)) return MEDIUM;
        try { return value == null ? MEDIUM : valueOf(value); }
        catch (IllegalArgumentException ignored) { return MEDIUM; }
    }
}
