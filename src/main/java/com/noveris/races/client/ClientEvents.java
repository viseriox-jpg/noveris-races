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
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class ClientEvents {
    public static final KeyMapping PANEL = key("key.noveris_races.panel", GLFW.GLFW_KEY_R);
    public static final KeyMapping PRIMARY = key("key.noveris_races.primary", GLFW.GLFW_KEY_G);
    public static final KeyMapping MOBILITY = key("key.noveris_races.mobility", GLFW.GLFW_KEY_V);
    public static final KeyMapping VISION = key("key.noveris_races.vision", GLFW.GLFW_KEY_B);
    private static int syncTimer;

    private static KeyMapping key(String name, int key) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, KeyModifier.NONE,
                InputConstants.Type.KEYSYM, key, "key.categories.noveris_races");
    }

    @EventBusSubscriber(modid = NoverisRaces.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e) { e.register(PANEL); e.register(PRIMARY); e.register(MOBILITY); e.register(VISION); }
    }

    @EventBusSubscriber(modid = NoverisRaces.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (ClientRaceState.primaryCooldown > 0) ClientRaceState.primaryCooldown--;
            if (ClientRaceState.mobilityCooldown > 0) ClientRaceState.mobilityCooldown--;
            if (++syncTimer >= 40) { syncTimer = 0; PacketDistributor.sendToServer(new ActionPayload("sync", "", "", "", "", "", "")); }
            if (ClientRaceState.race == Race.NONE && !ClientRaceState.selectionPending && !(mc.screen instanceof RaceSelectionScreen))
                mc.setScreen(new RaceSelectionScreen());
            else if (!ClientRaceState.confirmed && ClientRaceState.race != Race.NONE && ClientRaceState.trial <= 0
                    && !(mc.screen instanceof RacePanelScreen) && !(mc.screen instanceof RaceSelectionScreen))
                mc.setScreen(new RacePanelScreen(true));
            while (PANEL.consumeClick()) {
                if (ClientRaceState.race == Race.NONE && !ClientRaceState.selectionPending) mc.setScreen(new RaceSelectionScreen());
                else if (ClientRaceState.race != Race.NONE) mc.setScreen(new RacePanelScreen(false));
            }
            while (PRIMARY.consumeClick()) PacketDistributor.sendToServer(new ActionPayload("primary", "", "", "", "", "", ""));
            while (MOBILITY.consumeClick()) PacketDistributor.sendToServer(new ActionPayload("mobility", "", "", "", "", "", ""));
            while (VISION.consumeClick()) PacketDistributor.sendToServer(new ActionPayload("vision", "", "", "", "", "", ""));
        }
    }

    @EventBusSubscriber(modid = NoverisRaces.MOD_ID, value = Dist.CLIENT)
    public static final class HudBus {
        private static final int BAR_WIDTH = 154;
        private static final int GOLD = 0xFFFFD84D;
        private static final int GOLD_DARK = 0xFF6E5700;
        private static final int PANEL = 0xCC0D0C09;

        @SubscribeEvent
        public static void render(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui || ClientRaceState.race == Race.NONE) return;
            int y = mc.getWindow().getGuiScaledHeight() / 2 - 34;
            if (ClientRaceState.primaryCooldown > 0) {
                int max = ClientRaceState.race == Race.GOD ? 200 : ClientRaceState.race == Race.NPC ? 240 : 300;
                drawCooldown(event.getGuiGraphics(), mc, 10, y, "PRIMÁRIA", ClientRaceState.primaryCooldown, max);
                y += 34;
            }
            if (ClientRaceState.mobilityCooldown > 0)
                drawCooldown(event.getGuiGraphics(), mc, 10, y, "MOBILIDADE", ClientRaceState.mobilityCooldown, 900);
        }

        private static void drawCooldown(net.minecraft.client.gui.GuiGraphics g, Minecraft mc, int x, int y,
                                         String label, long remaining, int maximum) {
            int barY = y + 17;
            double progress = Math.max(0.0, Math.min(1.0, 1.0 - (double) remaining / maximum));
            int filled = (int) Math.round(BAR_WIDTH * progress);
            g.fill(x, y, x + BAR_WIDTH + 16, y + 28, PANEL);
            g.drawString(mc.font, label + "  " + String.format(java.util.Locale.ROOT, "%.1fs", remaining / 20.0), x + 8, y + 4, 0xFFFFF1B8, false);
            g.fill(x + 8, barY, x + 8 + BAR_WIDTH, barY + 5, GOLD_DARK);
            if (filled > 0) g.fill(x + 8, barY, x + 8 + filled, barY + 5, GOLD);
        }
    }
}
