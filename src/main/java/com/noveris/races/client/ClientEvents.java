package com.noveris.races.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.noveris.races.*;
import com.noveris.races.network.RaceNetwork.ActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final KeyMapping PANEL = key("key.noveris_races.panel", GLFW.GLFW_KEY_R);
    public static final KeyMapping PRIMARY = key("key.noveris_races.primary", GLFW.GLFW_KEY_G);
    public static final KeyMapping MOBILITY = key("key.noveris_races.mobility", GLFW.GLFW_KEY_V);
    private static int syncTimer;

    private static KeyMapping key(String name, int key) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYSYM, key, "key.categories.noveris_races");
    }

    @EventBusSubscriber(modid = NoverisRaces.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(PANEL); e.register(PRIMARY); e.register(MOBILITY); }
    }

    @EventBusSubscriber(modid = NoverisRaces.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (++syncTimer >= 40) { syncTimer = 0; PacketDistributor.sendToServer(new ActionPayload("sync", "", "", "", "", "")); }
            if (ClientRaceState.race == Race.NONE && !(mc.screen instanceof RaceSelectionScreen))
                mc.setScreen(new RaceSelectionScreen());
            else if (!ClientRaceState.confirmed && ClientRaceState.race != Race.NONE && ClientRaceState.trial <= 0 && !(mc.screen instanceof RacePanelScreen))
                mc.setScreen(new RacePanelScreen(true));
            while (PANEL.consumeClick()) mc.setScreen(ClientRaceState.race == Race.NONE ? new RaceSelectionScreen() : new RacePanelScreen(false));
            while (PRIMARY.consumeClick()) PacketDistributor.sendToServer(new ActionPayload("primary", "", "", "", "", ""));
            while (MOBILITY.consumeClick()) PacketDistributor.sendToServer(new ActionPayload("mobility", "", "", "", "", ""));
        }
    }
}
