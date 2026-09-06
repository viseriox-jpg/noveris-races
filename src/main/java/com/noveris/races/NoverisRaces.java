package com.noveris.races;

import com.noveris.races.network.RaceNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

@Mod(NoverisRaces.MOD_ID)
public final class NoverisRaces {
    public static final String MOD_ID = "noveris_races";
    public NoverisRaces(IEventBus modBus, ModContainer container) {
        modBus.addListener(RaceNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, RaceConfig.SPEC);
    }
}
