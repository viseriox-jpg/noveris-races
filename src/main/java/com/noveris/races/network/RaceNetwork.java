package com.noveris.races.network;

import com.noveris.races.*;
import com.noveris.races.client.ClientRaceState;
import com.noveris.races.game.RaceAbilities;
import com.noveris.races.client.FakeNameClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RaceNetwork {
    private RaceNetwork() {}

    public record ActionPayload(String action, String race, String lineage, String fairyAffinity, String ancestryA, String ancestryB, String size) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUtf(value.action); buf.writeUtf(value.race); buf.writeUtf(value.lineage); buf.writeUtf(value.fairyAffinity); buf.writeUtf(value.ancestryA); buf.writeUtf(value.ancestryB); buf.writeUtf(value.size); },
                buf -> new ActionPayload(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StatePayload(String race, String lineage, String fairyAffinity, String ancestryA, String ancestryB, String size, boolean confirmed, long trial,
                               long primaryCooldown, long mobilityCooldown, int mobilityCharges,
                               boolean combat, boolean visionEnabled, int hydration) implements CustomPacketPayload {
        public static final Type<StatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeUtf(v.race); buf.writeUtf(v.lineage); buf.writeUtf(v.fairyAffinity); buf.writeUtf(v.ancestryA); buf.writeUtf(v.ancestryB); buf.writeUtf(v.size); buf.writeBoolean(v.confirmed); buf.writeVarLong(v.trial); buf.writeVarLong(v.primaryCooldown); buf.writeVarLong(v.mobilityCooldown); buf.writeVarInt(v.mobilityCharges); buf.writeBoolean(v.combat); buf.writeBoolean(v.visionEnabled); buf.writeVarInt(v.hydration); },
                buf -> new StatePayload(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record FakeNamePayload(String nickname, String pronouns, String format, String color, boolean open) implements CustomPacketPayload {
        public static final Type<FakeNamePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "fake_name"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FakeNamePayload> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeUtf(v.nickname); buf.writeUtf(v.pronouns); buf.writeUtf(v.format); buf.writeUtf(v.color); buf.writeBoolean(v.open); },
                buf -> new FakeNamePayload(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, RaceNetwork::handleAction);
        registrar.playToClient(StatePayload.TYPE, StatePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientRaceState.accept(payload)));
        registrar.playToClient(FakeNamePayload.TYPE, FakeNamePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    FakeNameClientState.accept(payload.nickname(), payload.pronouns(), payload.format(), payload.color(), payload.open());
                    if (payload.open() && net.minecraft.client.Minecraft.getInstance().screen == null)
                        net.minecraft.client.Minecraft.getInstance().setScreen(new com.noveris.races.client.FakeNameScreen());
                }));
    }

    private static void handleAction(ActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case "trial" -> {
                    if (RaceState.inCombat(player)) return;
                    Race race = Race.parse(payload.race);
                    DragonLineage lineage = DragonLineage.parse(payload.lineage);
                    FairyAffinity fairyAffinity = FairyAffinity.parse(payload.fairyAffinity);
                    Race ancestryA = Race.parse(payload.ancestryA);
                    Race ancestryB = Race.parse(payload.ancestryB);
                    RaceSize size = RaceSize.parse(payload.size);
                    boolean validHybrid = race != Race.HALF_BLOOD || (ancestryA == Race.HUMAN
                            && ancestryB.validAncestry() && ancestryB != Race.HUMAN && ancestryB != Race.NEPHILIM);
                    boolean validFairy = race != Race.FAIRY || fairyAffinity != FairyAffinity.NONE;
                    if (race != Race.NONE && (race != Race.DRAGONBORN || lineage != DragonLineage.NONE) && validHybrid && validFairy) {
                        RaceState.beginTrial(player, race, lineage, fairyAffinity, ancestryA, ancestryB, size);
                        RaceGame.sync(player);
                    }
                }
                case "confirm" -> { RaceState.confirm(player); RaceGame.sync(player); }
                case "primary" -> RaceAbilities.usePrimary(player);
                case "mobility" -> RaceAbilities.useMobility(player);
                case "vision" -> { RaceState.toggleVision(player); RaceGame.sync(player); }
                case "sync" -> RaceGame.sync(player);
                case "fakename_save" -> {
                    FakeNameState.save(player, payload.race(), payload.lineage(), payload.fairyAffinity(), payload.ancestryA());
                    RaceGame.syncFakeName(player, false);
                }
                case "fakename_reset" -> {
                    FakeNameState.reset(player);
                    RaceGame.syncFakeName(player, false);
                }
            }
        });
    }
}
