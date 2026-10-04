package com.rabona.arena.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /rabona komutlari. */
public final class RabonaCommands {
    private RabonaCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("rabona")
                .then(Commands.literal("takim").then(Commands.argument("takim", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"kirmizi", "mavi", "izleyici"}, b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            Match.get(p.server).join(p, Team.byKey(StringArgumentType.getString(c, "takim")));
                            return 1;
                        })))
                .then(Commands.literal("stadyum").requires(s -> s.hasPermission(2)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StadiumBuilder.build(p.serverLevel(), p.blockPosition().below(), p.getDirection().getAxis() == Direction.Axis.X, p);
                    return 1;
                }))
                .then(Commands.literal("basla").requires(s -> s.hasPermission(2))
                        .executes(c -> Match.get(c.getSource().getServer()).start() ? 1 : 0)
                        .then(Commands.argument("dakika", IntegerArgumentType.integer(1, 45)).executes(c -> {
                            Match m = Match.get(c.getSource().getServer());
                            m.durationMin = IntegerArgumentType.getInteger(c, "dakika");
                            m.saveData();
                            return m.start() ? 1 : 0;
                        })))
                .then(Commands.literal("bitir").requires(s -> s.hasPermission(2)).executes(c -> {
                    Match.get(c.getSource().getServer()).stop();
                    return 1;
                }))
                .then(Commands.literal("bot")
                        .then(Commands.literal("doldur").requires(s -> s.hasPermission(2)).executes(c -> {
                            Match.get(c.getSource().getServer()).fillBots();
                            return 1;
                        }).then(Commands.argument("kisi", IntegerArgumentType.integer(1, 11)).executes(c -> {
                            Match m = Match.get(c.getSource().getServer());
                            m.teamSize = IntegerArgumentType.getInteger(c, "kisi");
                            m.saveData();
                            m.fillBots();
                            return 1;
                        })))
                        .then(Commands.literal("temizle").requires(s -> s.hasPermission(2)).executes(c -> {
                            Match.get(c.getSource().getServer()).clearBots();
                            return 1;
                        }))
                        .then(Commands.literal("ekle").requires(s -> s.hasPermission(2))
                                .then(Commands.argument("takim", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"kirmizi", "mavi"}, b))
                                        .executes(c -> {
                                            Team t = Team.byKey(StringArgumentType.getString(c, "takim"));
                                            if (!t.playing()) return 0;
                                            Match.get(c.getSource().getServer()).spawnBot(c.getSource().getLevel(), t, c.getSource().getPosition());
                                            return 1;
                                        }))))
                .then(Commands.literal("zorluk").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("seviye", IntegerArgumentType.integer(0, 2)).executes(c -> {
                            Match m = Match.get(c.getSource().getServer());
                            m.difficulty = IntegerArgumentType.getInteger(c, "seviye");
                            m.saveData();
                            m.sync();
                            c.getSource().sendSuccess(() -> Component.translatable("msg.rabonaarena.difficulty_set", m.difficulty), true);
                            return 1;
                        })))
                .then(Commands.literal("skor").executes(c -> {
                    Match m = Match.get(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.translatable("msg.rabonaarena.score", m.scoreRed, m.scoreBlue), false);
                    return 1;
                })));
    }
}
