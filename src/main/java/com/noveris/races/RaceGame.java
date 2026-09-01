package com.noveris.races;

import com.noveris.races.network.RaceNetwork.StatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class RaceGame {
    private RaceGame() {}
    public static void sync(ServerPlayer p) {
        long now = p.level().getGameTime();
        PacketDistributor.sendToPlayer(p, new StatePayload(
                RaceState.race(p).name(), RaceState.lineage(p).name(), RaceState.ancestryA(p).name(), RaceState.ancestryB(p).name(), RaceState.size(p).name(), RaceState.confirmed(p),
                RaceState.trialRemaining(p), Math.max(0, RaceState.primaryReady(p) - now),
                Math.max(0, RaceState.mobilityReady(p) - now), RaceState.inCombat(p)));
    }
}
