package com.noveris.races.network;

import com.noveris.races.*;
import com.noveris.races.client.ClientRaceState;
import com.noveris.races.game.RaceAbilities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RaceNetwork {
    private RaceNetwork() {}

    public record ActionPayload(String action, String race, String lineage) implements CustomPacketPayload {
        public static final Type<ActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of(
                (buf, value) -> { buf.writeUtf(value.action); buf.writeUtf(value.race); buf.writeUtf(value.lineage); },
                buf -> new ActionPayload(buf.readUtf(), buf.readUtf(), buf.readUtf()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record StatePayload(String race, String lineage, boolean confirmed, long trial,
                               long primaryCooldown, long mobilityCooldown, boolean combat) implements CustomPacketPayload {
        public static final Type<StatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NoverisRaces.MOD_ID, "state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.of(
                (buf, v) -> { buf.writeUtf(v.race); buf.writeUtf(v.lineage); buf.writeBoolean(v.confirmed); buf.writeVarLong(v.trial); buf.writeVarLong(v.primaryCooldown); buf.writeVarLong(v.mobilityCooldown); buf.writeBoolean(v.combat); },
                buf -> new StatePayload(buf.readUtf(), buf.readUtf(), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, RaceNetwork::handleAction);
        registrar.playToClient(StatePayload.TYPE, StatePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> ClientRaceState.accept(payload)));
    }

    private static void handleAction(ActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            switch (payload.action) {
                case "trial" -> {
                    if (RaceState.inCombat(player)) return;
                    Race race = Race.parse(payload.race);
                    DragonLineage lineage = DragonLineage.parse(payload.lineage);
                    if (race != Race.NONE && (race != Race.DRAGONBORN || lineage != DragonLineage.NONE)) {
                        RaceState.beginTrial(player, race, lineage);
                        RaceGame.sync(player);
                    }
                }
                case "confirm" -> { RaceState.confirm(player); RaceGame.sync(player); }
                case "primary" -> RaceAbilities.usePrimary(player);
                case "mobility" -> RaceAbilities.useMobility(player);
                case "sync" -> RaceGame.sync(player);
            }
        });
    }
}
