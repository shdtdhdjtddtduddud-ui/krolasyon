package com.krolasyon.labirent;

import com.krolasyon.labirent.maze.ExploreTracker;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.maze.MazeState;
import com.krolasyon.labirent.net.ServerHooks;
import com.krolasyon.labirent.story.Objective;
import com.krolasyon.labirent.team.Role;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;

/** /labirent ... */
public final class LabCommands {
    private LabCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("labirent")
                .then(Commands.literal("durum").executes(LabCommands::status))
                .then(Commands.literal("kaptan").then(Commands.argument("oyuncu", EntityArgument.player()).executes(c -> {
                    ServerPlayer src = c.getSource().getPlayerOrException();
                    ServerPlayer t = EntityArgument.getPlayer(c, "oyuncu");
                    ServerHooks.transfer(new com.krolasyon.labirent.net.Net.Transfer(t.getUUID()), src);
                    return 1;
                })))
                .then(Commands.literal("rol").then(Commands.argument("oyuncu", EntityArgument.player())
                        .then(Commands.argument("rol", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Role.values()).map(r -> r.id), b))
                                .executes(c -> {
                                    ServerPlayer src = c.getSource().getPlayerOrException();
                                    ServerPlayer t = EntityArgument.getPlayer(c, "oyuncu");
                                    Role r = Role.byName(StringArgumentType.getString(c, "rol"));
                                    if (r == null) {
                                        c.getSource().sendFailure(Component.translatable("msg.labirent.bad_role"));
                                        return 0;
                                    }
                                    ServerHooks.setRole(new com.krolasyon.labirent.net.Net.SetRole(t.getUUID(), r.ordinal()), src);
                                    return 1;
                                }))))
                .then(Commands.literal("emir").then(Commands.argument("metin", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer src = c.getSource().getPlayerOrException();
                    ServerHooks.order(new com.krolasyon.labirent.net.Net.Order(ServerHooks.ORDER_SAY, StringArgumentType.getString(c, "metin")), src);
                    return 1;
                })))
                .then(Commands.literal("git").requires(s -> s.hasPermission(2)).executes(c -> {
                    MazeManager m = MazeManager.get();
                    if (m != null) m.sendToGlade(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("kutu").requires(s -> s.hasPermission(2)).executes(c -> {
                    MazeManager m = MazeManager.get();
                    if (m != null) m.box.queueNpc(com.krolasyon.labirent.maze.BoxDirector.NEWCOMER_NAMES[m.state.arrivals % 24], true);
                    return 1;
                }))
                .then(Commands.literal("bolum").requires(s -> s.hasPermission(2)).then(Commands.argument("n", IntegerArgumentType.integer(1, 9)).executes(c -> {
                    MazeManager m = MazeManager.get();
                    if (m != null) m.story.forceChapter(IntegerArgumentType.getInteger(c, "n") - 1);
                    return 1;
                })))
                .then(Commands.literal("sifirla").requires(s -> s.hasPermission(2)).executes(c -> {
                    MazeManager m = MazeManager.get();
                    if (m == null) return 0;
                    MazeState s = m.state;
                    s.built = false;
                    s.layout = null;
                    s.seed = s.seed * 31 + 7;
                    s.chapter = 0;
                    Arrays.fill(s.progress, 0);
                    s.events = 0;
                    s.stonesFound = s.wordsFound = 0;
                    s.lastShiftNight = -1;
                    s.lastArrivalDay = -1;
                    s.memoriesUnlocked = 0;
                    s.known = new byte[s.known.length];
                    s.setDirty();
                    c.getSource().sendSuccess(() -> Component.translatable("msg.labirent.reset"), true);
                    return 1;
                })));
    }

    private static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        MazeManager m = MazeManager.get();
        if (m == null) return 0;
        MazeState s = m.state;
        CommandSourceStack src = c.getSource();
        src.sendSuccess(() -> Component.translatable("chapter.labirent.num", s.chapter + 1).withStyle(ChatFormatting.GOLD)
                .append(" - ").append(Component.translatable("chapter.labirent." + s.chapter)), false);
        for (Objective o : Objective.values()) {
            if (o.chapter != s.chapter) continue;
            boolean done = s.progress(o) >= o.target;
            MutableComponent line = Component.literal(done ? " ✔ " : " • ").withStyle(done ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .append(o.text()).append(Component.literal(" (" + s.progress(o) + "/" + o.target + ")").withStyle(ChatFormatting.DARK_GRAY));
            src.sendSuccess(() -> line, false);
        }
        src.sendSuccess(() -> Component.translatable("msg.labirent.status", s.captainName, ExploreTracker.percent(s),
                Integer.bitCount(s.wordsFound), s.arrivals), false);
        return 1;
    }
}
