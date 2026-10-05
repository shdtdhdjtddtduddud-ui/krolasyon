package com.krolasyon.sololeveling.client;

import com.krolasyon.sololeveling.client.screen.StatusScreen;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.system.Actions;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only visual self test (-Dsololeveling.autotest=true): walks through the mod and saves screenshots. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;

    record Step(int delay, String shot, Consumer<MinecraftServer> server, Runnable client) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000",
                "weather clear", "difficulty peaceful", "gamemode creative @a");
        server(40, null, s -> Actions.handle(player(s), "awaken", 1, 0, ""));
        cmd(10, null, "sl level 48", "sl job necromancer", "sl gold 50000", "sl rank B", "give @p sololeveling:kamish_wrath",
                "give @p sololeveling:demon_king_longsword", "give @p sololeveling:crimson_greatsword", "give @p sololeveling:healing_potion",
                "give @p sololeveling:elixir_of_life", "give @p sololeveling:orb_of_avarice", "give @p sololeveling:baruka_dagger");
        // Seoul
        cmd(60, null, "sl tp seoul_plaza");
        wait(200, "seoul_plaza_a");
        cmd(40, "seoul_plaza_b", "tp @p 40 66 30 180 5");
        cmd(40, "seoul_plaza_portals", "tp @p 40 66 34 0 0");
        cmd(80, "seoul_air_1", "tp @p 10 140 10 -45 35");
        cmd(80, "seoul_air_2", "tp @p 100 120 -60 140 30");
        cmd(80, "association", "tp @p 104 66 2 0 -12");
        cmd(60, "association_lobby", "tp @p 104 65 30 180 0");
        cmd(60, "guild_hunters", "tp @p -24 66 2 0 -12");
        cmd(60, "market", "tp @p -24 65 100 0 0");
        cmd(60, "street", "tp @p 200 65 8 90 5");
        // gates and UI
        cmd(20, null, "tp @p 40 64 52 180 0", "sl gate S", "sl gate B true");
        wait(60, "gates");
        client(20, "hud", () -> {});
        client(20, "status_window", () -> Minecraft.getInstance().setScreen(new StatusScreen()));
        client(5, null, () -> Minecraft.getInstance().setScreen(null));
        client(30, "map", () -> Net.toServer(new Net.Action("map", 0, 0, "")));
        client(5, null, () -> Minecraft.getInstance().setScreen(null));
        client(30, "news", () -> Net.toServer(new Net.Action("news", 0, 0, "")));
        client(5, null, () -> Minecraft.getInstance().setScreen(null));
        client(30, "shop", () -> Net.toServer(new Net.Action("system_shop", 0, 0, "")));
        client(5, null, () -> Minecraft.getInstance().setScreen(null));
        // regions
        for (String r : new String[]{"double_dungeon", "job_change", "demon_castle", "jeju_island"}) {
            cmd(120, r + "_a", "sl tp " + r);
            cmd(40, r + "_b", "tp @p ~ ~3 ~10 0 5");
        }
        // monster line-up in the penalty desert
        cmd(100, null, "sl tp penalty_zone", "kill @e[type=!player]");
        for (MobKind k : MobKind.values()) {
            double dist = 4 + k.height * 1.4;
            cmd(15, null, "kill @e[type=!player]", "tp @p 4000 86 -60000 180 10",
                    "summon sololeveling:" + k.id() + " 4000 86 " + (-60000 - dist) + " {NoAI:1b,Rotation:[0f,0f]}");
            wait(40, "mob_" + k.id());
            cmd(12, "mob_" + k.id() + "_side", "tp @p " + (4000 + dist) + " 86 " + (-60000 - dist) + " 90 10");
            server(2, null, s -> {
                for (SLMonster m : player(s).level().getEntitiesOfClass(SLMonster.class, new AABB(3900, 0, -60200, 4100, 255, -59800))) {
                    m.setNoAi(false);
                    if (m.abilityCount() > 0) m.forceAbility(0);
                }
            });
            wait(10, "mob_" + k.id() + "_ability");
        }
        // shadows
        cmd(10, null, "kill @e[type=!player]", "tp @p 4000 86 -60000 180 10");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            var d = com.krolasyon.sololeveling.system.HunterCapability.get(p);
            String[] src = {"sololeveling:igris", "sololeveling:high_orc", "sololeveling:ice_bear", "sololeveling:ant_soldier", "minecraft:zombie", "sololeveling:steel_fanged_lycan"};
            for (int i = 0; i < src.length; i++)
                d.shadows.add(new com.krolasyon.sololeveling.system.HunterData.ShadowRecord(java.util.UUID.randomUUID(), src[i], i == 0 ? "Igris" : "#entity." + src[i].replace(':', '.'), 10, i == 0));
            com.krolasyon.sololeveling.shadow.ShadowManager.summonAll(p);
        });
        wait(60, "shadows");
        client(10, "arise_fx", () -> ClientHooks.fx("arise", 4000, 86, -60004, 0));
        // a dungeon instance
        cmd(20, null, "kill @e[type=!player]", "sl tp seoul_plaza");
        server(60, null, s -> {
            var dm = com.krolasyon.sololeveling.world.DungeonManager.get(s);
            var in = dm.create(s, com.krolasyon.sololeveling.system.Rank.C, false, com.krolasyon.sololeveling.world.DungeonTheme.KASAKA_LAIR);
            dm.enter(player(s), in);
        });
        wait(100, "dungeon_kasaka");
        server(10, null, s -> {
            var dm = com.krolasyon.sololeveling.world.DungeonManager.get(s);
            var in = dm.create(s, com.krolasyon.sololeveling.system.Rank.B, true, com.krolasyon.sololeveling.world.DungeonTheme.ICE_FOREST);
            dm.enter(player(s), in);
        });
        wait(100, "dungeon_ice");
        // first person weapons
        cmd(20, null, "sl tp seoul_plaza", "item replace entity @p weapon.mainhand with sololeveling:kamish_wrath", "item replace entity @p weapon.offhand with sololeveling:demon_king_dagger");
        client(60, "hand_daggers", () -> Minecraft.getInstance().options.hideGui = false);
        cmd(20, "hand_longsword", "item replace entity @p weapon.mainhand with sololeveling:demon_king_longsword", "item replace entity @p weapon.offhand with air");
        cmd(20, "hand_potion", "item replace entity @p weapon.mainhand with sololeveling:elixir_of_life");
        wait(20, "END");
    }

    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            ServerPlayer p = player(s);
            for (String c : commands) s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
        }, null));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action, null)); }

    private void client(int wait, String shot, Runnable r) { steps.add(new Step(wait, shot, null, r)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[AUTOTEST] creating world");
            LevelSettings settings = new LevelSettings("sltest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("sltest", settings, new WorldOptions(42L, true, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step s = steps.get(stepIndex);
        if (tick == stepStart) {
            LOG.info("[AUTOTEST] step {} {}", stepIndex, s.shot());
            if (s.server() != null) {
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> {
                    try {
                        s.server().accept(srv);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] step failed", t);
                    }
                });
            }
            if (s.client() != null) {
                try {
                    s.client().run();
                } catch (Throwable t) {
                    LOG.error("[AUTOTEST] client step failed", t);
                }
            }
        }
        if (tick >= stepStart + s.delay()) {
            if (s.shot() != null) {
                if ("END".equals(s.shot())) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ktest_" + String.format("%03d", stepIndex) + "_" + s.shot() + ".png", mc.getMainRenderTarget(), c -> LOG.info("[AUTOTEST] shot {}", s.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
