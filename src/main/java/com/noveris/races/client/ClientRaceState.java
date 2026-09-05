package com.noveris.races.client;

import com.noveris.races.DragonLineage;
import com.noveris.races.FairyAffinity;
import com.noveris.races.Race;
import com.noveris.races.RaceSize;
import com.noveris.races.network.RaceNetwork.StatePayload;

public final class ClientRaceState {
    public static Race race = Race.NONE;
    public static DragonLineage lineage = DragonLineage.NONE;
    public static FairyAffinity fairyAffinity = FairyAffinity.NONE;
    public static Race ancestryA = Race.NONE;
    public static Race ancestryB = Race.NONE;
    public static RaceSize size = RaceSize.MEDIUM;
    public static boolean confirmed;
    public static long trial;
    public static long primaryCooldown;
    public static long mobilityCooldown;
    public static int mobilityCharges = 3;
    public static boolean combat;
    public static boolean visionEnabled = true;
    public static int hydration = 100;
    public static boolean selectionPending;

    private ClientRaceState() {}
    public static void accept(StatePayload p) {
        race = Race.parse(p.race()); lineage = DragonLineage.parse(p.lineage()); fairyAffinity = FairyAffinity.parse(p.fairyAffinity());
        ancestryA = Race.parse(p.ancestryA()); ancestryB = Race.parse(p.ancestryB()); size = RaceSize.parse(p.size()); confirmed = p.confirmed();
        trial = p.trial(); primaryCooldown = p.primaryCooldown(); mobilityCooldown = p.mobilityCooldown(); mobilityCharges = p.mobilityCharges(); combat = p.combat(); visionEnabled = p.visionEnabled(); hydration = p.hydration();
        if (race != Race.NONE && confirmed) selectionPending = false;
    }
}
