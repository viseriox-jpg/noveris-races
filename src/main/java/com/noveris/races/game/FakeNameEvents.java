package com.noveris.races.game;

import com.noveris.races.FakeNameState;
import com.noveris.races.NoverisRaces;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = NoverisRaces.MOD_ID)
public final class FakeNameEvents {
    private FakeNameEvents() {}

    @SubscribeEvent
    public static void name(PlayerEvent.NameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p && !FakeNameState.nickname(p).isBlank())
            event.setDisplayname(FakeNameState.displayName(p));
    }

    @SubscribeEvent
    public static void tab(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer p && !FakeNameState.nickname(p).isBlank())
            event.setDisplayName(FakeNameState.displayName(p));
    }
}
