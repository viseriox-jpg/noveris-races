package com.noveris.races;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class RaceState {
    private RaceState() {}
    private static final String ROOT = "NoverisRaces";
    public static final long TRIAL_TICKS = 5L * 60L * 20L;

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag persistent = player.getPersistentData();
        if (!persistent.contains(ROOT)) persistent.put(ROOT, new CompoundTag());
        return persistent.getCompound(ROOT);
    }

    public static Race race(ServerPlayer p) { return Race.parse(root(p).getString("Race")); }
    public static DragonLineage lineage(ServerPlayer p) { return DragonLineage.parse(root(p).getString("Lineage")); }
    public static FairyAffinity fairyAffinity(ServerPlayer p) {
        FairyAffinity affinity = FairyAffinity.parse(root(p).getString("FairyAffinity"));
        return affinity == FairyAffinity.NONE && race(p) == Race.FAIRY ? FairyAffinity.NATURE : affinity;
    }
    public static Race ancestryA(ServerPlayer p) { return Race.parse(root(p).getString("AncestryA")); }
    public static Race ancestryB(ServerPlayer p) { return Race.parse(root(p).getString("AncestryB")); }
    public static RaceSize size(ServerPlayer p) { return RaceSize.parse(root(p).getString("Size")); }
    public static boolean confirmed(ServerPlayer p) { return root(p).getBoolean("Confirmed"); }
    public static long trialRemaining(ServerPlayer p) { return root(p).getLong("TrialRemaining"); }
    public static long combatUntil(ServerPlayer p) { return root(p).getLong("CombatUntil"); }
    public static long primaryReady(ServerPlayer p) { return root(p).getLong("PrimaryReady"); }
    public static long mobilityReady(ServerPlayer p) { return root(p).getLong("MobilityReady"); }
    public static boolean visionEnabled(ServerPlayer p) { CompoundTag tag=root(p); return !tag.contains("VisionEnabled") || tag.getBoolean("VisionEnabled"); }

    public static void beginTrial(ServerPlayer p, Race race, DragonLineage lineage) {
        beginTrial(p, race, lineage, FairyAffinity.NATURE, Race.NONE, Race.NONE, RaceSize.MEDIUM);
    }

    public static void beginTrial(ServerPlayer p, Race race, DragonLineage lineage, FairyAffinity fairyAffinity, Race ancestryA, Race ancestryB, RaceSize size) {
        CompoundTag tag = root(p);
        tag.putString("Race", race.name());
        tag.putString("Lineage", race == Race.DRAGONBORN ? lineage.name() : DragonLineage.NONE.name());
        tag.putString("FairyAffinity", race == Race.FAIRY ? fairyAffinity.name() : FairyAffinity.NONE.name());
        tag.putBoolean("Confirmed", false);
        tag.putString("AncestryA", Race.NONE.name());
        tag.putString("AncestryB", Race.NONE.name());
        tag.putString("Size", size.name());
        tag.putLong("TrialRemaining", TRIAL_TICKS);
        tag.putLong("PrimaryReady", 0);
        tag.putLong("MobilityCharges", 3);
        tag.putLong("MobilityChargeSystem", 1);
        tag.putLong("MobilityReady", 0);
        tag.putLong("MobilityBurstReady", 0);
        tag.putLong("AegisWeaknessAt", 0);
        tag.putLong("DryTicks", 0);
        tag.putLong("HydrationWarning", 0);
        tag.putBoolean("VisionEnabled", true);
    }

    public static void confirm(ServerPlayer p) {
        if (race(p) != Race.NONE && trialRemaining(p) <= 0) root(p).putBoolean("Confirmed", true);
    }

    public static void reset(ServerPlayer p) {
        CompoundTag tag = root(p);
        tag.putString("Race", Race.NONE.name());
        tag.putString("Lineage", DragonLineage.NONE.name());
        tag.putString("FairyAffinity", FairyAffinity.NONE.name());
        tag.putBoolean("Confirmed", false);
        tag.putString("AncestryA", Race.NONE.name());
        tag.putString("AncestryB", Race.NONE.name());
        tag.putString("Size", RaceSize.MEDIUM.name());
        tag.putLong("TrialRemaining", 0);
        tag.putLong("PrimaryReady", 0);
        tag.putLong("MobilityReady", 0);
        tag.putLong("MobilityBurstReady", 0);
        tag.putBoolean("VisionEnabled", true);
    }

    public static void cancelTrialOnLogout(ServerPlayer p) {
        if (race(p) != Race.NONE && !confirmed(p)) reset(p);
    }

    public static void tickTrial(ServerPlayer p) {
        CompoundTag tag = root(p);
        long remaining = tag.getLong("TrialRemaining");
        if (race(p) != Race.NONE && !confirmed(p) && remaining > 0) tag.putLong("TrialRemaining", remaining - 1);
    }

    public static boolean inCombat(ServerPlayer p) { return p.level().getGameTime() < combatUntil(p); }
    public static void markCombat(ServerPlayer p) { root(p).putLong("CombatUntil", p.level().getGameTime() + 200); }
    public static void setPrimaryReady(ServerPlayer p, long tick) { root(p).putLong("PrimaryReady", tick); }
    public static void setMobilityReady(ServerPlayer p, long tick) { root(p).putLong("MobilityReady", tick); }
    public static void toggleVision(ServerPlayer p) { root(p).putBoolean("VisionEnabled", !visionEnabled(p)); }
    public static long customLong(ServerPlayer p, String key) { return root(p).getLong(key); }
    public static void customLong(ServerPlayer p, String key, long value) { root(p).putLong(key, value); }

    public static float effectiveScale(ServerPlayer p) {
        Race race = race(p);
        RaceSize size = size(p);
        float scale;
        scale = race.scale(size);
        if (race == Race.LYCANTHROPE && p.level().isNight()) scale = Math.min(1.10f, scale + .05f);
        return Math.min(1.15f, scale);
    }

    public static void copyOnDeath(ServerPlayer from, ServerPlayer to) {
        if (from.getPersistentData().contains(ROOT))
            to.getPersistentData().put(ROOT, from.getPersistentData().getCompound(ROOT).copy());
    }
}
