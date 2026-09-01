package com.noveris.races;

import java.util.Locale;

public enum DragonLineage {
    NONE("Nenhuma"), FIRE("Fogo"), FROST("Gelo"), VENOM("Veneno");
    public final String title;
    DragonLineage(String title) { this.title = title; }
    public static DragonLineage parse(String value) {
        if (value != null) {
            if (value.equalsIgnoreCase("fogo")) return FIRE;
            if (value.equalsIgnoreCase("gelo")) return FROST;
            if (value.equalsIgnoreCase("veneno")) return VENOM;
        }
        try { return value == null ? NONE : valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return NONE; }
    }
}
