package com.sololeveling.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.GateEntity;
import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModDimensions;
import com.sololeveling.system.PlayerSync;
import com.sololeveling.system.Stats;
import com.sololeveling.system.Travel;
import com.sololeveling.util.Ranks;
import com.sololeveling.world.GateManager;
import com.sololeveling.world.Regions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** /sl ... admin & debug commands (operator level 2). */
@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class SLCommands {
    private SLCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("sl").requires(s -> s.hasPermission(2))
                .then(Commands.literal("gate")
                        .then(Commands.argument("rank", StringArgumentType.word())
                                .executes(c -> gate(c.getSource(), StringArgumentType.getString(c, "rank"), null, false))
                                .then(Commands.argument("theme", StringArgumentType.word())
                                        .executes(c -> gate(c.getSource(), StringArgumentType.getString(c, "rank"), StringArgumentType.getString(c, "theme"), false))
                                        .then(Commands.literal("red").executes(c -> gate(c.getSource(), StringArgumentType.getString(c, "rank"), StringArgumentType.getString(c, "theme"), true))))))
                .then(Commands.literal("level").then(Commands.argument("n", IntegerArgumentType.integer(1, 200)).executes(c -> level(c.getSource(), IntegerArgumentType.getInteger(c, "n")))))
                .then(Commands.literal("skills").executes(c -> skills(c.getSource())))
                .then(Commands.literal("region").then(Commands.argument("id", StringArgumentType.word()).executes(c -> region(c.getSource(), StringArgumentType.getString(c, "id")))))
                .then(Commands.literal("gold").then(Commands.argument("n", IntegerArgumentType.integer(0)).executes(c -> gold(c.getSource(), IntegerArgumentType.getInteger(c, "n"))))));
    }

    private static int gate(CommandSourceStack src, String rank, String theme, boolean red) {
        ServerLevel lvl = src.getLevel();
        if (Ranks.index(rank.toUpperCase()) == 0 && !rank.equalsIgnoreCase("E")) { src.sendFailure(Component.literal("rank: E D C B A S N")); return 0; }
        rank = rank.toUpperCase();
        if (theme == null) theme = GateManager.randomTheme(rank, lvl.random);
        boolean ok = false;
        for (Content.ThemeDef t : Content.THEMES) if (t.id().equals(theme)) ok = true;
        if (!ok) { src.sendFailure(Component.literal("unknown theme " + theme)); return 0; }
        Vec3 p = src.getPosition();
        Vec3 look = src.getRotation().y == 0 && src.getRotation().x == 0 ? new Vec3(0, 0, 1) : Vec3.directionFromRotation(0, src.getRotation().y);
        GateManager.spawnGate(lvl, p.x + look.x * 5, p.y, p.z + look.z * 5, rank, theme, red, true);
        return 1;
    }

    private static int level(CommandSourceStack src, int n) {
        ServerPlayer sp = src.getPlayer();
        if (sp == null) return 0;
        SLPlayer d = ModCaps.get(sp);
        d.level = n;
        d.xp = 0;
        d.points = Math.max(d.points, 5 * n);
        for (Content.SkillDef s : Content.SKILLS) if (n >= s.level()) d.skills.add(s.id());
        d.mana = d.maxMana();
        Stats.apply(sp);
        sp.setHealth(sp.getMaxHealth());
        PlayerSync.sync(sp);
        return 1;
    }

    private static int skills(CommandSourceStack src) {
        ServerPlayer sp = src.getPlayer();
        if (sp == null) return 0;
        for (Content.SkillDef s : Content.SKILLS) ModCaps.get(sp).skills.add(s.id());
        PlayerSync.sync(sp);
        return 1;
    }

    private static int gold(CommandSourceStack src, int n) {
        ServerPlayer sp = src.getPlayer();
        if (sp == null) return 0;
        ModCaps.get(sp).gold += n;
        PlayerSync.sync(sp);
        return 1;
    }

    private static int region(CommandSourceStack src, String id) {
        ServerPlayer sp = src.getPlayer();
        Content.RegionDef r = Regions.find(id);
        if (sp == null || r == null) { src.sendFailure(Component.literal("seoul gangnam busan incheon jeju")); return 0; }
        ServerLevel hw = src.getServer().getLevel(ModDimensions.HUNTER_WORLD);
        if (hw == null) return 0;
        double[] a = Regions.arrival(r);
        Travel.teleport(sp, hw, a[0], a[1], a[2], 180F);
        ModCaps.get(sp).discovered.add(id);
        PlayerSync.sync(sp);
        return 1;
    }
}
