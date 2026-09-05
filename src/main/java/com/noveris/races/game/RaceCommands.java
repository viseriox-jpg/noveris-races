package com.noveris.races.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.noveris.races.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.noveris.races.network.RaceNetwork.AdminPanelPayload;

import java.util.Collection;
import java.util.Arrays;
import java.util.Locale;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

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
                .then(Commands.argument("raça", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(raceSuggestions(), b))
                    .executes(c -> set(c.getSource(), EntityArgument.getPlayer(c, "jogador"), StringArgumentType.getString(c, "raça"), "none"))
                    .then(Commands.argument("linhagem", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(lineageSuggestions(), b))
                        .executes(c -> set(c.getSource(), EntityArgument.getPlayer(c, "jogador"), StringArgumentType.getString(c, "raça"), StringArgumentType.getString(c, "linhagem")))))))
            .then(Commands.literal("listar").then(Commands.argument("raça", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(raceSuggestions(), b))
                .executes(c -> list(c.getSource(), StringArgumentType.getString(c, "raça"), 1))
                .then(Commands.argument("página", IntegerArgumentType.integer(1))
                    .executes(c -> list(c.getSource(), StringArgumentType.getString(c, "raça"), IntegerArgumentType.getInteger(c, "página"))))))
            .then(Commands.literal("painel").executes(c -> openAdminPanel(c.getSource()))));
    }

    private static java.util.List<String> raceSuggestions() {
        return Arrays.stream(Race.values())
                .filter(r -> r != Race.NONE)
                .map(r -> r.title.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static java.util.List<String> lineageSuggestions() {
        return Arrays.stream(DragonLineage.values())
                .filter(l -> l != DragonLineage.NONE)
                .map(l -> l.title.toLowerCase(Locale.ROOT))
                .toList();
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

    private static int openAdminPanel(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PacketDistributor.sendToPlayer(player, new AdminPanelPayload(true));
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

    private static final int LIST_PAGE_SIZE = 8;

    private record ListedPlayer(String name, boolean online) {}

    private static int list(CommandSourceStack source, String raceName, int requestedPage) {
        Race race = Race.parse(raceName);
        if (race == Race.NONE) {
            source.sendFailure(Component.literal("Raça inválida: " + raceName));
            return 0;
        }

        List<ListedPlayer> matches = playersWithRace(source, race);
        int totalPages = Math.max(1, (matches.size() + LIST_PAGE_SIZE - 1) / LIST_PAGE_SIZE);
        if (requestedPage > totalPages) {
            source.sendFailure(Component.literal("A página " + requestedPage + " não existe. A raça " + race.title
                    + " possui " + totalPages + " página(s)."));
            return 0;
        }

        int page = Math.max(1, requestedPage);
        int from = (page - 1) * LIST_PAGE_SIZE;
        int to = Math.min(from + LIST_PAGE_SIZE, matches.size());
        Component header = Component.literal("Noveris Races » " + race.title
                + "  •  " + matches.size() + " jogador(es)  •  página " + page + "/" + totalPages)
                .withStyle(ChatFormatting.GOLD);
        source.sendSuccess(() -> header, false);

        if (matches.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Nenhum jogador com essa raça foi encontrado."), false);
        } else {
            for (int i = from; i < to; i++) {
                ListedPlayer player = matches.get(i);
                String status = player.online ? "online" : "offline";
                ChatFormatting color = player.online ? ChatFormatting.GREEN : ChatFormatting.GRAY;
                source.sendSuccess(() -> Component.literal("• " + player.name + " (" + status + ")").withStyle(color), false);
            }
        }

        if (totalPages > 1) {
            Component navigation = Component.literal("Páginas: ");
            if (page > 1) navigation = navigation.append(pageLink("« anterior", race, page - 1));
            if (page > 1 && page < totalPages) navigation = navigation.append(Component.literal("  "));
            if (page < totalPages) navigation = navigation.append(pageLink("próxima »", race, page + 1));
            Component finalNavigation = navigation;
            source.sendSuccess(() -> finalNavigation, false);
        }
        return matches.size();
    }

    private static Component pageLink(String label, Race race, int page) {
        String command = "/noverisraces listar " + race.title.toLowerCase(Locale.ROOT) + " " + page;
        return Component.literal("[" + label + "]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal("Abrir página " + page))));
    }

    private static List<ListedPlayer> playersWithRace(CommandSourceStack source, Race race) {
        Map<UUID, ListedPlayer> matches = new HashMap<>();
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            if (RaceState.race(player) == race)
                matches.put(player.getUUID(), new ListedPlayer(player.getGameProfile().getName(), true));
        }

        Path playerData = source.getServer().getWorldPath(LevelResource.PLAYER_DATA_DIR);
        try (var files = Files.list(playerData)) {
            files.filter(path -> path.getFileName().toString().endsWith(".dat")).forEach(path -> {
                String fileName = path.getFileName().toString();
                try {
                    UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 4));
                    if (matches.containsKey(uuid)) return;
                    CompoundTag data = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
                    CompoundTag state = data.getCompound("NoverisRaces");
                    if (Race.parse(state.getString("Race")) != race) return;
                    String name = source.getServer().getProfileCache().get(uuid)
                            .map(GameProfile::getName).orElse(uuid.toString());
                    matches.put(uuid, new ListedPlayer(name, false));
                } catch (Exception ignored) {
                    // Arquivos antigos ou incompletos não devem interromper a listagem.
                }
            });
        } catch (Exception ignored) {
            // Se o diretório não estiver disponível, a lista online continua funcionando.
        }

        return matches.values().stream()
                .sorted(Comparator.comparing(ListedPlayer::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static void audit(CommandSourceStack source, String action) {
        String actor = source.getTextName();
        NoverisRacesLog.LOG.info("[AUDITORIA] {} {}", actor, action);
        source.sendSuccess(() -> Component.literal("Ação concluída e registrada."), true);
    }
}
