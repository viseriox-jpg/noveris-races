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
    public static boolean confirmed(ServerPlayer p) { return root(p).getBoolean("Confirmed"); }
    public static long trialRemaining(ServerPlayer p) { return root(p).getLong("TrialRemaining"); }
    public static long combatUntil(ServerPlayer p) { return root(p).getLong("CombatUntil"); }
    public static long primaryReady(ServerPlayer p) { return root(p).getLong("PrimaryReady"); }
    public static long mobilityReady(ServerPlayer p) { return root(p).getLong("MobilityReady"); }

    public static void beginTrial(ServerPlayer p, Race race, DragonLineage lineage) {
        CompoundTag tag = root(p);
        tag.putString("Race", race.name());
        tag.putString("Lineage", race == Race.DRAGONBORN ? lineage.name() : DragonLineage.NONE.name());
        tag.putBoolean("Confirmed", false);
        tag.putLong("TrialRemaining", TRIAL_TICKS);
        tag.putLong("PrimaryReady", 0);
        tag.putLong("MobilityReady", 0);
    }

    public static void confirm(ServerPlayer p) {
        if (race(p) != Race.NONE && trialRemaining(p) <= 0) root(p).putBoolean("Confirmed", true);
    }

    public static void reset(ServerPlayer p) {
        CompoundTag tag = root(p);
        tag.putString("Race", Race.NONE.name());
        tag.putString("Lineage", DragonLineage.NONE.name());
        tag.putBoolean("Confirmed", false);
        tag.putLong("TrialRemaining", 0);
        tag.putLong("PrimaryReady", 0);
        tag.putLong("MobilityReady", 0);
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

    public static void copyOnDeath(ServerPlayer from, ServerPlayer to) {
        if (from.getPersistentData().contains(ROOT))
            to.getPersistentData().put(ROOT, from.getPersistentData().getCompound(ROOT).copy());
    }
}
