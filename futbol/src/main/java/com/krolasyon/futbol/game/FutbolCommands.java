package com.krolasyon.futbol.game;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.registry.ModEntities;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public final class FutbolCommands {
    private FutbolCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("futbol")
                .then(Commands.literal("saha").requires(s -> s.hasPermission(2)).executes(FutbolCommands::build))
                .then(Commands.literal("baslat")
                        .executes(c -> start(c, MatchManager.minutes, MatchManager.playersPerTeam))
                        .then(Commands.argument("dakika", IntegerArgumentType.integer(1, 90))
                                .executes(c -> start(c, IntegerArgumentType.getInteger(c, "dakika"), MatchManager.playersPerTeam))
                                .then(Commands.argument("oyuncu", IntegerArgumentType.integer(1, 11))
                                        .executes(c -> start(c, IntegerArgumentType.getInteger(c, "dakika"), IntegerArgumentType.getInteger(c, "oyuncu"))))))
                .then(Commands.literal("bitir").executes(c -> {
                    MatchManager.stop();
                    return 1;
                }))
                .then(Commands.literal("takim")
                        .then(Commands.literal("kirmizi").executes(c -> team(c, Team.RED)))
                        .then(Commands.literal("mavi").executes(c -> team(c, Team.BLUE)))
                        .then(Commands.literal("izleyici").executes(c -> team(c, Team.NONE))))
                .then(Commands.literal("top").executes(FutbolCommands::ball))
                .then(Commands.literal("ortala").executes(c -> {
                    MatchManager.centerBall();
                    return 1;
                }))
                .then(Commands.literal("hareket").then(Commands.argument("isim", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Move.values()).map(m -> m.name().toLowerCase()), b))
                        .executes(FutbolCommands::move)))
                .then(Commands.literal("bot").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("takim", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"kirmizi", "mavi"}, b))
                                .then(Commands.argument("adet", IntegerArgumentType.integer(1, 11)).executes(FutbolCommands::bots))))
                .then(Commands.literal("botlarisil").requires(s -> s.hasPermission(2)).executes(c -> {
                    MatchManager.removeBots();
                    return 1;
                })));
    }

    private static int build(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        MatchManager.stop();
        PitchBuilder.build(p.serverLevel(), p.blockPosition());
        MatchManager.centerBall();
        MatchManager.markDirty();
        c.getSource().sendSuccess(() -> Component.literal("⚽ Stadyum kuruldu! Takım seçmek ve maç başlatmak için [J].").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int start(CommandContext<CommandSourceStack> c, int min, int players) {
        MatchManager.minutes = min;
        MatchManager.playersPerTeam = players;
        return MatchManager.start(c.getSource().getPlayer()) ? 1 : 0;
    }

    private static int team(CommandContext<CommandSourceStack> c, Team t) throws CommandSyntaxException {
        MatchManager.setTeam(c.getSource().getPlayerOrException(), t);
        return 1;
    }

    private static int ball(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel l = p.serverLevel();
        FootballEntity b = ModEntities.BALL.get().create(l);
        if (b == null) return 0;
        Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(1.5));
        b.moveTo(at.x, p.getY() + 0.5, at.z);
        l.addFreshEntity(b);
        return 1;
    }

    private static int move(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Move m = Move.parse(StringArgumentType.getString(c, "isim"));
        if (m == null) {
            c.getSource().sendFailure(Component.literal("Böyle bir hareket yok."));
            return 0;
        }
        return MoveExecutor.perform(c.getSource().getPlayerOrException(), m, 0.8F) ? 1 : 0;
    }

    private static int bots(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Team t = Team.parse(StringArgumentType.getString(c, "takim"));
        if (t == Team.NONE) t = Team.RED;
        int n = IntegerArgumentType.getInteger(c, "adet");
        ServerLevel l = p.serverLevel();
        for (int i = 0; i < n; i++) {
            FootballerEntity b = ModEntities.FOOTBALLER.get().create(l);
            if (b == null) continue;
            b.setTeam(t);
            b.setNumber(2 + l.random.nextInt(20));
            b.slot = 1 + i;
            b.moveTo(p.getX() + (l.random.nextDouble() - 0.5) * 6, p.getY(), p.getZ() + (l.random.nextDouble() - 0.5) * 6, l.random.nextFloat() * 360F, 0);
            l.addFreshEntity(b);
        }
        return n;
    }
}
