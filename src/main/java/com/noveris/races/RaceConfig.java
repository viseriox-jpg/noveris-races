package com.noveris.races;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Valores de balanceamento editáveis no arquivo de configuração SERVER. */
public final class RaceConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue primaryCooldownSeconds;
    public static final ModConfigSpec.IntValue godPrimaryCooldownSeconds;
    public static final ModConfigSpec.IntValue npcPrimaryCooldownSeconds;
    public static final ModConfigSpec.IntValue mobilityCooldownSeconds;
    public static final ModConfigSpec.DoubleValue elfProjectileRange;
    public static final ModConfigSpec.DoubleValue elfProjectileDamage;
    public static final ModConfigSpec.DoubleValue fairyNatureDamage;
    public static final ModConfigSpec.DoubleValue fairyWaterDamage;
    public static final ModConfigSpec.DoubleValue fairyAirDamage;
    public static final ModConfigSpec.IntValue adminPermissionLevel;
    public static final ModConfigSpec.DoubleValue tieflingWaterDamage;
    public static final ModConfigSpec.DoubleValue fireDragonWaterDamage;
    public static final ModConfigSpec.IntValue waterDamageIntervalTicks;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Recargas das habilidades, em segundos.").push("cooldowns");
        primaryCooldownSeconds = b.defineInRange("primaryCooldownSeconds", 15, 1, 300);
        godPrimaryCooldownSeconds = b.defineInRange("godPrimaryCooldownSeconds", 10, 1, 300);
        npcPrimaryCooldownSeconds = b.defineInRange("npcPrimaryCooldownSeconds", 12, 1, 300);
        mobilityCooldownSeconds = b.defineInRange("mobilityCooldownSeconds", 45, 1, 600);
        b.pop();

        b.comment("Danos e alcance das habilidades.").push("abilities");
        elfProjectileRange = b.defineInRange("elfProjectileRange", 18.0, 1.0, 64.0);
        elfProjectileDamage = b.defineInRange("elfProjectileDamage", 5.0, 0.1, 40.0);
        fairyNatureDamage = b.defineInRange("fairyNatureDamage", 2.0, 0.1, 40.0);
        fairyWaterDamage = b.defineInRange("fairyWaterDamage", 4.0, 0.1, 40.0);
        fairyAirDamage = b.defineInRange("fairyAirDamage", 3.0, 0.1, 40.0);
        b.pop();

        b.comment("Permissão mínima para usar as raças administrativas.").push("permissions");
        adminPermissionLevel = b.defineInRange("adminPermissionLevel", 2, 0, 4);
        b.pop();

        b.comment("Dano contínuo sofrido na água ou chuva. O intervalo é medido em ticks.");
        tieflingWaterDamage = b.defineInRange("tieflingWaterDamage", 2.0, 0.0, 40.0);
        fireDragonWaterDamage = b.defineInRange("fireDragonWaterDamage", 2.0, 0.0, 40.0);
        waterDamageIntervalTicks = b.defineInRange("waterDamageIntervalTicks", 20, 1, 200);

        SPEC = b.build();
    }

    private RaceConfig() {}

    public static int primaryCooldownTicks() { return primaryCooldownSeconds.get() * 20; }
    public static int primaryCooldownTicks(Race race) { return race == Race.GOD ? godPrimaryCooldownTicks() : race == Race.NPC ? npcPrimaryCooldownTicks() : primaryCooldownTicks(); }
    public static int godPrimaryCooldownTicks() { return godPrimaryCooldownSeconds.get() * 20; }
    public static int npcPrimaryCooldownTicks() { return npcPrimaryCooldownSeconds.get() * 20; }
    public static int mobilityCooldownTicks() { return mobilityCooldownSeconds.get() * 20; }
}
