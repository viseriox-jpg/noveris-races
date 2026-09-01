package com.noveris.races;

public enum RaceSize {
    SMALL("Porte menor"), STANDARD("Porte padrão");
    public final String title;
    RaceSize(String title) { this.title = title; }
    public static RaceSize parse(String value) {
        try { return value == null ? STANDARD : valueOf(value); }
        catch (IllegalArgumentException ignored) { return STANDARD; }
    }
}
