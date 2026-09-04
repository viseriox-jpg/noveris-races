package com.noveris.races.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.noveris.races.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Collection;

@EventBusSubscriber(modid = NoverisRaces.MOD_ID)
public final class RaceCommands {
    private RaceCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("apelido").executes(c -> openNickname(c.getSource()))
            .then(Commands.literal("reset").executes(c -> resetNickname(c.getSource()))));
        d.register(Commands.literal("noverisraces").requires(s -> s.hasPermission(2))
            .then(Commands.literal("reset").then(Commands.argument("jogador", EntityArgument.player())
                .executes(c -> reset(c.getSource(), EntityArgument.getPlayer(c, "jogador")))))
            .then(Commands.literal("consultar").then(Commands.argument("jogador", EntityArgument.player())
                .executes(c -> inspect(c.getSource(), EntityArgument.getPlayer(c, "jogador")))))
            .then(Commands.literal("definir").then(Commands.argument("jogador", EntityArgument.player())
                .then(Commands.argument("raça", StringArgumentType.word())
                    .executes(c -> set(c.getSource(), EntityArgument.getPlayer(c, "jogador"), StringArgumentType.getString(c, "raça"), "none"))
                    .then(Commands.argument("linhagem", StringArgumentType.word())
                        .executes(c -> set(c.getSource(), EntityArgument.getPlayer(c, "jogador"), StringArgumentType.getString(c, "raça"), StringArgumentType.getString(c, "linhagem")))))))
            .then(Commands.literal("listar").then(Commands.argument("raça", StringArgumentType.word())
                .executes(c -> list(c.getSource(), StringArgumentType.getString(c, "raça"))))));
    }

    private static int openNickname(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        RaceGame.syncFakeName(player, true);
        return 1;
    }
    private static int resetNickname(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        FakeNameState.reset(player); player.refreshDisplayName(); player.refreshTabListName();
        RaceGame.syncFakeName(player, false);
        source.sendSuccess(() -> Component.literal("Apelido, prefixo e complemento restaurados ao nome real."), false);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer target) {
        RaceState.reset(target); RaceGame.sync(target);
        audit(source, "resetou a raça de " + target.getGameProfile().getName());
        return 1;
    }

    private static int inspect(CommandSourceStack source, ServerPlayer target) {
        source.sendSuccess(() -> Component.literal(target.getGameProfile().getName() + ": " + RaceState.race(target).title +
                (RaceState.lineage(target) == DragonLineage.NONE ? "" : " / " + RaceState.lineage(target).title) +
                (RaceState.confirmed(target) ? " (confirmada)" : " (em teste)")), false);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer target, String raceName, String lineageName) {
        Race race = Race.parse(raceName);
        DragonLineage lineage = DragonLineage.parse(lineageName);
        if (race == Race.NONE || (race == Race.DRAGONBORN && lineage == DragonLineage.NONE)) {
            source.sendFailure(Component.literal("Raça ou linhagem inválida.")); return 0;
        }
        RaceState.beginTrial(target, race, lineage);
        target.getPersistentData().getCompound("NoverisRaces").putLong("TrialRemaining", 0);
        RaceState.confirm(target); RaceGame.sync(target);
        audit(source, "definiu " + race.title + " para " + target.getGameProfile().getName());
        return 1;
    }

    private static int list(CommandSourceStack source, String raceName) {
        Race race = Race.parse(raceName);
        Collection<ServerPlayer> matches = source.getServer().getPlayerList().getPlayers().stream().filter(p -> RaceState.race(p) == race).toList();
        source.sendSuccess(() -> Component.literal(race.title + ": " + (matches.isEmpty() ? "nenhum jogador online" :
                String.join(", ", matches.stream().map(p -> p.getGameProfile().getName()).toList()))), false);
        return matches.size();
    }

    private static void audit(CommandSourceStack source, String action) {
        String actor = source.getTextName();
        NoverisRacesLog.LOG.info("[AUDITORIA] {} {}", actor, action);
        source.sendSuccess(() -> Component.literal("Ação concluída e registrada."), true);
    }
}
