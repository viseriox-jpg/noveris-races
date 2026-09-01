package com.noveris.races.client;

import com.noveris.races.DragonLineage;
import com.noveris.races.Race;
import com.noveris.races.RaceSize;
import com.noveris.races.network.RaceNetwork.StatePayload;

public final class ClientRaceState {
    public static Race race = Race.NONE;
    public static DragonLineage lineage = DragonLineage.NONE;
    public static Race ancestryA = Race.NONE;
    public static Race ancestryB = Race.NONE;
    public static RaceSize size = RaceSize.STANDARD;
    public static boolean confirmed;
    public static long trial;
    public static long primaryCooldown;
    public static long mobilityCooldown;
    public static boolean combat;
    public static boolean visionEnabled = true;

    private ClientRaceState() {}
    public static void accept(StatePayload p) {
        race = Race.parse(p.race()); lineage = DragonLineage.parse(p.lineage());
        ancestryA = Race.parse(p.ancestryA()); ancestryB = Race.parse(p.ancestryB()); size = RaceSize.parse(p.size()); confirmed = p.confirmed();
        trial = p.trial(); primaryCooldown = p.primaryCooldown(); mobilityCooldown = p.mobilityCooldown(); combat = p.combat(); visionEnabled = p.visionEnabled();
    }
}
