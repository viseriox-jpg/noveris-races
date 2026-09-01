package com.noveris.races;

import com.noveris.races.network.RaceNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(NoverisRaces.MOD_ID)
public final class NoverisRaces {
    public static final String MOD_ID = "noveris_races";
    public NoverisRaces(IEventBus modBus) {
        modBus.addListener(RaceNetwork::register);
    }
}
