package com.krolasyon.storm;

import com.krolasyon.storm.net.Net;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;

/** /storm unlockall|reset|refill [players], /storm cast <skill> (ops only) */
public final class StormCommand {
    private StormCommand() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("storm").requires(s -> s.hasPermission(2))
                .then(Commands.literal("unlockall").executes(c -> unlockAll(java.util.List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(c -> unlockAll(EntityArgument.getPlayers(c, "targets")))))
                .then(Commands.literal("reset").executes(c -> reset(java.util.List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players()).executes(c -> reset(EntityArgument.getPlayers(c, "targets")))))
                .then(Commands.literal("refill").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StormData d = StormCapability.get(p);
                    d.energy = d.maxEnergy();
                    java.util.Arrays.fill(d.cooldowns, 0);
                    Net.sync(p);
                    return 1;
                }))
                .then(Commands.literal("cast").then(Commands.argument("skill", StringArgumentType.word()).executes(c -> {
                    String id = StringArgumentType.getString(c, "skill");
                    for (Skill s : Skill.VALUES) {
                        if (s.id.equals(id)) return SkillLogic.cast(c.getSource().getPlayerOrException(), s, true) ? 1 : 0;
                    }
                    c.getSource().sendFailure(Component.literal("unknown skill " + id));
                    return 0;
                }))));
    }

    private static int unlockAll(Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            StormData d = StormCapability.get(p);
            for (Skill s : Skill.VALUES) {
                d.unlocked.add(s);
                if (s.passive || java.util.Arrays.asList(d.slots).contains(s)) continue;
                for (int i = 0; i < StormData.SLOTS; i++) if (d.slots[i] == null) { d.slots[i] = s; break; }
            }
            d.energy = d.maxEnergy();
            Net.sync(p);
            p.sendSystemMessage(Component.translatable("msg.stormtree.unlocked_all"));
        }
        return players.size();
    }

    private static int reset(Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) {
            StormData d = StormCapability.get(p);
            int refund = 0;
            for (Skill s : d.unlocked) refund += s.levelCost;
            d.unlocked.clear();
            java.util.Arrays.fill(d.slots, null);
            d.domeTicks = d.ringTicks = d.avatarTicks = 0;
            d.energy = d.maxEnergy();
            p.giveExperienceLevels(refund);
            Net.sync(p);
            p.sendSystemMessage(Component.translatable("msg.stormtree.reset", refund));
        }
        return players.size();
    }
}
