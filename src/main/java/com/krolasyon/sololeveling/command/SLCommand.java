package com.krolasyon.sololeveling.command;

import com.krolasyon.sololeveling.system.*;
import com.krolasyon.sololeveling.world.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

/** /sl — admin and testing commands. */
public final class SLCommand {
    private SLCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sl").requires(s -> s.hasPermission(2))
                .then(Commands.literal("awaken").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Actions.handle(p, "awaken", 1, 0, "");
                    return 1;
                }))
                .then(Commands.literal("level").then(Commands.argument("n", IntegerArgumentType.integer(1, 150)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    HunterData h = HunterCapability.get(p);
                    if (!h.awakened) Actions.handle(p, "awaken", 1, 0, "");
                    int n = IntegerArgumentType.getInteger(c, "n");
                    if (n > h.level) {
                        h.statPoints += (n - h.level) * 5;
                        h.level = n;
                    } else h.level = n;
                    h.exp = 0;
                    Progression.applyAttributes(p, h);
                    Progression.autoSlot(h);
                    Progression.checkQuest(p);
                    h.markDirty();
                    return ok(c, "level " + n);
                })))
                .then(Commands.literal("points").then(Commands.argument("n", IntegerArgumentType.integer(0, 10000)).executes(c -> {
                    HunterData h = HunterCapability.get(c.getSource().getPlayerOrException());
                    h.statPoints = IntegerArgumentType.getInteger(c, "n");
                    h.markDirty();
                    return ok(c, "points " + h.statPoints);
                })))
                .then(Commands.literal("gold").then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(c -> {
                    HunterData h = HunterCapability.get(c.getSource().getPlayerOrException());
                    h.gold = IntegerArgumentType.getInteger(c, "n");
                    h.markDirty();
                    return ok(c, "gold " + h.gold);
                })))
                .then(Commands.literal("job").then(Commands.argument("job", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Job.values()).map(j -> j.name().toLowerCase(Locale.ROOT)), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            HunterData h = HunterCapability.get(p);
                            h.job = Job.valueOf(StringArgumentType.getString(c, "job").toUpperCase(Locale.ROOT));
                            Progression.applyAttributes(p, h);
                            Progression.autoSlot(h);
                            h.markDirty();
                            return ok(c, "job " + h.job);
                        })))
                .then(Commands.literal("rank").then(Commands.argument("rank", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Rank.values()).map(r -> r.label), b))
                        .executes(c -> {
                            HunterData h = HunterCapability.get(c.getSource().getPlayerOrException());
                            String s = StringArgumentType.getString(c, "rank").toUpperCase(Locale.ROOT);
                            for (Rank r : Rank.values()) if (r.label.equals(s)) h.rank = r;
                            h.licensed = true;
                            h.markDirty();
                            return ok(c, "rank " + h.rank);
                        })))
                .then(Commands.literal("tp").then(Commands.argument("where", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Regions.WAYPOINTS.keySet(), b))
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String w = StringArgumentType.getString(c, "where");
                            HunterData h = HunterCapability.get(p);
                            h.waypoints.add(w);
                            Regions.teleport(p, w);
                            return 1;
                        })))
                .then(Commands.literal("gate").then(Commands.argument("rank", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"E", "D", "C", "B", "A", "S"}, b))
                        .executes(c -> gate(c, false))
                        .then(Commands.argument("red", BoolArgumentType.bool()).executes(c -> gate(c, BoolArgumentType.getBool(c, "red"))))))
                .then(Commands.literal("penalty").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                    WorldState ws = WorldState.get(c.getSource().getServer());
                    ws.penaltyEnabled = BoolArgumentType.getBool(c, "on");
                    ws.setDirty();
                    return ok(c, "penalty " + ws.penaltyEnabled);
                })))
                .then(Commands.literal("dailydone").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (int i = 0; i < 4; i++) DailyQuest.progress(p, i, 10000);
                    return ok(c, "daily done");
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    HunterCapability.get(p).load(new HunterData().save());
                    Progression.applyAttributes(p, HunterCapability.get(p));
                    return ok(c, "reset");
                }))
                .then(Commands.literal("news").executes(c -> {
                    var s = c.getSource().getServer();
                    NewsManager.get(s).post(s, 5, Sys.t("news.flavor." + s.overworld().random.nextInt(24)), true);
                    return 1;
                })));
    }

    private static int gate(CommandContext<CommandSourceStack> c, boolean red) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String s = StringArgumentType.getString(c, "rank").toUpperCase(Locale.ROOT);
        Rank rank = Rank.E;
        for (Rank r : Rank.values()) if (r.label.equals(s)) rank = r;
        BlockPos at = BlockPos.containing(p.position().add(p.getLookAngle().multiply(6, 0, 6)));
        GateManager.spawnGate(p.server, p.serverLevel(), at, rank, red, p.level().dimension() == Regions.SEOUL ? GateManager.district(at.getX(), at.getZ()) : "wilderness", p.getRandom());
        return ok(c, "gate " + rank + (red ? " red" : ""));
    }

    private static int ok(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal("[System] " + msg), false);
        return 1;
    }
}
