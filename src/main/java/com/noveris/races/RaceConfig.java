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
    public static final ModConfigSpec.DoubleValue elfMaxHealth, fairyMaxHealth, satyrMaxHealth, thalassianMaxHealth;
    public static final ModConfigSpec.DoubleValue humanMaxHealth, nephilimMaxHealth, vampireMaxHealth, tieflingMaxHealth;
    public static final ModConfigSpec.DoubleValue lycanthropeMaxHealth, dragonbornMaxHealth, harpyMaxHealth;
    public static final ModConfigSpec.DoubleValue godMaxHealth, npcMaxHealth;
    public static final ModConfigSpec.DoubleValue elfProjectileDamage;
    public static final ModConfigSpec.DoubleValue fairyNatureDamage;
    public static final ModConfigSpec.DoubleValue fairyWaterDamage;
    public static final ModConfigSpec.DoubleValue fairyAirDamage;
    public static final ModConfigSpec.IntValue adminPermissionLevel;
    public static final ModConfigSpec.DoubleValue tieflingWaterDamage;
    public static final ModConfigSpec.DoubleValue fireDragonWaterDamage;
    public static final ModConfigSpec.IntValue waterDamageIntervalTicks;
    public static final ModConfigSpec.IntValue passiveRefreshTicks;
    public static final ModConfigSpec.DoubleValue fairyFallDamageMultiplier;
    public static final ModConfigSpec.DoubleValue fairyMagicDamageMultiplier;
    public static final ModConfigSpec.DoubleValue tieflingHealingMultiplier;
    public static final ModConfigSpec.DoubleValue thalassianFireWeaknessMultiplier;
    public static final ModConfigSpec.DoubleValue thalassianDryHealingMultiplier;
    public static final ModConfigSpec.DoubleValue dragonPhysicalDamageMultiplier;
    public static final ModConfigSpec.DoubleValue dragonFireDamageMultiplier;
    public static final ModConfigSpec.DoubleValue dragonFrostDamageMultiplier;
    public static final ModConfigSpec.DoubleValue harpyFallDamageMultiplier;
    public static final ModConfigSpec.IntValue hydrationMaximumTicks;
    public static final ModConfigSpec.IntValue hydrationWarningHalfTicks;
    public static final ModConfigSpec.IntValue hydrationWarningQuarterTicks;
    public static final ModConfigSpec.IntValue hydrationDamageIntervalTicks;
    public static final ModConfigSpec.DoubleValue hydrationDamage;
    public static final ModConfigSpec.DoubleValue defaultMovementSpeed;
    public static final ModConfigSpec.DoubleValue dragonMovementSpeed;
    public static final ModConfigSpec.DoubleValue harpyMovementSpeed;
    public static final ModConfigSpec.DoubleValue lycanthropeNightMovementSpeed;
    public static final ModConfigSpec.DoubleValue dragonKnockbackResistance;
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
        elfMaxHealth = b.defineInRange("elfMaxHealth", 32.0, 1.0, 1000.0);
        fairyMaxHealth = b.defineInRange("fairyMaxHealth", 30.0, 1.0, 1000.0);
        satyrMaxHealth = b.defineInRange("satyrMaxHealth", 34.0, 1.0, 1000.0);
        thalassianMaxHealth = b.defineInRange("thalassianMaxHealth", 34.0, 1.0, 1000.0);
        humanMaxHealth = b.defineInRange("humanMaxHealth", 30.0, 1.0, 1000.0);
        nephilimMaxHealth = b.defineInRange("nephilimMaxHealth", 34.0, 1.0, 1000.0);
        vampireMaxHealth = b.defineInRange("vampireMaxHealth", 34.0, 1.0, 1000.0);
        tieflingMaxHealth = b.defineInRange("tieflingMaxHealth", 34.0, 1.0, 1000.0);
        lycanthropeMaxHealth = b.defineInRange("lycanthropeMaxHealth", 36.0, 1.0, 1000.0);
        dragonbornMaxHealth = b.defineInRange("dragonbornMaxHealth", 36.0, 1.0, 1000.0);
        harpyMaxHealth = b.defineInRange("harpyMaxHealth", 32.0, 1.0, 1000.0);
        godMaxHealth = b.defineInRange("godMaxHealth", 1000.0, 1.0, 2000.0);
        npcMaxHealth = b.defineInRange("npcMaxHealth", 80.0, 1.0, 1000.0);
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

        b.comment("Passivas, atributos e multiplicadores de combate.").push("balance");
        passiveRefreshTicks = b.defineInRange("passiveRefreshTicks", 20, 1, 200);
        fairyFallDamageMultiplier = b.defineInRange("fairyFallDamageMultiplier", 0.35, 0.0, 1.0);
        fairyMagicDamageMultiplier = b.defineInRange("fairyMagicDamageMultiplier", 0.80, 0.0, 3.0);
        tieflingHealingMultiplier = b.defineInRange("tieflingHealingMultiplier", 0.75, 0.0, 3.0);
        thalassianFireWeaknessMultiplier = b.defineInRange("thalassianFireWeaknessMultiplier", 1.0, 0.0, 3.0);
        thalassianDryHealingMultiplier = b.defineInRange("thalassianDryHealingMultiplier", 0.60, 0.0, 3.0);
        dragonPhysicalDamageMultiplier = b.defineInRange("dragonPhysicalDamageMultiplier", 0.88, 0.0, 3.0);
        dragonFireDamageMultiplier = b.defineInRange("dragonFireDamageMultiplier", 0.40, 0.0, 3.0);
        dragonFrostDamageMultiplier = b.defineInRange("dragonFrostDamageMultiplier", 0.40, 0.0, 3.0);
        harpyFallDamageMultiplier = b.defineInRange("harpyFallDamageMultiplier", 0.20, 0.0, 3.0);
        hydrationMaximumTicks = b.defineInRange("hydrationMaximumTicks", 9600, 20, 24000);
        hydrationWarningHalfTicks = b.defineInRange("hydrationWarningHalfTicks", 4800, 20, 24000);
        hydrationWarningQuarterTicks = b.defineInRange("hydrationWarningQuarterTicks", 7200, 20, 24000);
        hydrationDamageIntervalTicks = b.defineInRange("hydrationDamageIntervalTicks", 200, 1, 2400);
        hydrationDamage = b.defineInRange("hydrationDamage", 2.0, 0.0, 40.0);
        defaultMovementSpeed = b.defineInRange("defaultMovementSpeed", 0.10, 0.01, 1.0);
        dragonMovementSpeed = b.defineInRange("dragonMovementSpeed", 0.092, 0.01, 1.0);
        harpyMovementSpeed = b.defineInRange("harpyMovementSpeed", 0.112, 0.01, 1.0);
        lycanthropeNightMovementSpeed = b.defineInRange("lycanthropeNightMovementSpeed", 0.11, 0.01, 1.0);
        dragonKnockbackResistance = b.defineInRange("dragonKnockbackResistance", 0.20, 0.0, 1.0);
        b.pop();

        SPEC = b.build();
    }

    public static double maxHealth(Race race) {
        return switch (race) {
            case ELF -> elfMaxHealth.get();
            case FAIRY -> fairyMaxHealth.get();
            case SATYR -> satyrMaxHealth.get();
            case THALASSIAN -> thalassianMaxHealth.get();
            case HUMAN -> humanMaxHealth.get();
            case NEPHILIM -> nephilimMaxHealth.get();
            case VAMPIRE -> vampireMaxHealth.get();
            case TIEFLING -> tieflingMaxHealth.get();
            case LYCANTHROPE -> lycanthropeMaxHealth.get();
            case DRAGONBORN -> dragonbornMaxHealth.get();
            case HARPY -> harpyMaxHealth.get();
            case GOD -> godMaxHealth.get();
            case NPC -> npcMaxHealth.get();
            default -> 20.0;
        };
    }

    private RaceConfig() {}

    public static int primaryCooldownTicks() { return primaryCooldownSeconds.get() * 20; }
    public static int primaryCooldownTicks(Race race) { return race == Race.GOD ? godPrimaryCooldownTicks() : race == Race.NPC ? npcPrimaryCooldownTicks() : primaryCooldownTicks(); }
    public static int godPrimaryCooldownTicks() { return godPrimaryCooldownSeconds.get() * 20; }
    public static int npcPrimaryCooldownTicks() { return npcPrimaryCooldownSeconds.get() * 20; }
    public static int mobilityCooldownTicks() { return mobilityCooldownSeconds.get() * 20; }
}
