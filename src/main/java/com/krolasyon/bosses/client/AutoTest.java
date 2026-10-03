package com.krolasyon.bosses.client;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.CrimsonHoundEntity;
import com.krolasyon.bosses.entity.SealWardenEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
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
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI-only visual self test (inactive unless -Dkrolasyon.autotest=true): creates a flat world, spawns each boss,
 * forces every ability and saves screenshots, then quits.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;

    record Step(int delay, String shot, Consumer<MinecraftServer> action) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final String TARGET = "summon minecraft:husk 4 -60 -13 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:1000f,Attributes:[{Name:\"generic.max_health\",Base:1000d}]}";

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty normal", "gamemode creative @a", "tp @a 1 -60 2 180 -6",
                "kill @e[type=!player]");
        // every realm creature, five at a time
        java.util.List<String> ids = new ArrayList<>(com.krolasyon.bosses.realm.entity.RealmEntities.TYPES.keySet());
        for (int i = 0; i < ids.size(); i += 5) {
            java.util.List<String> group = ids.subList(i, Math.min(ids.size(), i + 5));
            boolean big = group.stream().anyMatch(id -> com.krolasyon.bosses.realm.entity.RealmEntities.spec(id).boss());
            double gap = big ? 6.5 : 4.0, z = big ? -16 : -11;
            java.util.List<String> c = new ArrayList<>();
            c.add("kill @e[type=!player]");
            c.add("tp @a 0.5 -59 " + (big ? 4 : 2) + " 180 " + (big ? -4 : 4));
            for (int k = 0; k < group.size(); k++) {
                double x = (k - (group.size() - 1) / 2.0) * gap + 0.5;
                c.add("summon krolasyonbosses:" + group.get(k) + " " + x + " -60 " + z + " {NoAI:1b,PersistenceRequired:1b,Rotation:[" + (k % 2 == 0 ? 15 : -15) + "f,0f]}");
            }
            cmd(60, "mobs_" + i, c.toArray(new String[0]));
        }
        // a few lords casting
        cmd(20, null, "kill @e[type=!player]", "tp @a 1 -60 2 180 -6", TARGET, "summon krolasyonbosses:varkhas 0 -60 -16 {Rotation:[0f,0f]}");
        server(30, null, s -> forceRealm(s, 3));
        wait(12, "varkhas_erupt");
        server(40, null, s -> forceRealm(s, 2));
        wait(14, "varkhas_meteor");
        cmd(20, null, "kill @e[type=!player]", TARGET, "summon krolasyonbosses:azgaroth 0 -60 -18 {Rotation:[0f,0f]}");
        server(30, null, s -> forceRealm(s, 3));
        wait(16, "azgaroth_breath");
        server(40, null, s -> forceRealm(s, 6));
        wait(16, "azgaroth_nova");
        cmd(20, null, "kill @e[type=!player]", TARGET, "summon krolasyonbosses:nyxar 0 -60 -16 {Rotation:[0f,0f]}");
        server(30, null, s -> forceRealm(s, 4));
        wait(20, "nyxar_beam");
        // portal ruin in the overworld
        cmd(10, null, "kill @e[type=!player]", "tp @a 8 -58 14 160 8");
        server(60, "portal_ruin", s -> com.krolasyon.bosses.realm.world.Builders.portalRuin(s.overworld(), new net.minecraft.core.BlockPos(4, -60, 0),
                s.overworld().random, true));
        // the six biomes of the realm
        for (String b : new String[]{"ash_wastes", "blood_marsh", "obsidian_forest", "basalt_warfields", "soul_valley", "throne_wastes"}) {
            server(220, "biome_" + b, s -> realmBiome(s, b));
            cmd(80, "biome_" + b + "_b", "execute as @a at @s run tp @s ~ ~ ~ ~150 12");
        }
        // the chronicle
        server(10, null, s -> {
            for (net.minecraft.server.level.ServerPlayer p : s.getPlayerList().getPlayers()) {
                com.krolasyon.bosses.realm.data.RealmData d = com.krolasyon.bosses.realm.data.RealmData.get(p);
                d.rep[0] = 64; d.rep[1] = -55; d.rep[2] = 12; d.rep[3] = 35; d.rep[4] = -10;
                d.allied[0] = true; d.conquered[3] = true; d.chapter = 3; d.mana = 72;
                d.save(p);
                com.krolasyon.bosses.realm.net.RealmNet.sync(p);
            }
        });
        steps.add(new Step(20, null, s -> showGui = 1));
        wait(20, "journal_story");
        steps.add(new Step(20, null, s -> showGui = 2));
        wait(20, "journal_kingdoms");
        steps.add(new Step(20, null, s -> showGui = 3));
        wait(20, "journal_blessings");
        steps.add(new Step(5, null, s -> showGui = 0));
        wait(20, "END");
    }

    private int showGui;

    private static void forceRealm(MinecraftServer s, int ability) {
        ServerLevel l = s.overworld();
        for (com.krolasyon.bosses.realm.entity.RealmBoss b : l.getEntitiesOfClass(com.krolasyon.bosses.realm.entity.RealmBoss.class, new AABB(-80, -80, -80, 80, 40, 80))) {
            List<net.minecraft.world.entity.monster.Husk> z = l.getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, b.getBoundingBox().inflate(40));
            LivingEntity t = z.isEmpty() ? null : z.get(0);
            LOG.info("[AUTOTEST] force {} ability {}", b.getName().getString(), ability);
            if (t != null && ability < b.spec.abilities().length) b.debugForceAbility(ability, t);
        }
    }

    private static void realmBiome(MinecraftServer s, String biome) {
        ServerLevel realm = s.getLevel(com.krolasyon.bosses.realm.Realm.REALM);
        if (realm == null) {
            LOG.error("[AUTOTEST] realm dimension missing");
            return;
        }
        var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, com.krolasyon.bosses.realm.Realm.rl(biome));
        var found = realm.findClosestBiome3d(h -> h.is(key), new net.minecraft.core.BlockPos(0, 64, 0), 4000, 32, 64);
        if (found == null) {
            LOG.error("[AUTOTEST] biome {} not found", biome);
            return;
        }
        var pos = found.getFirst();
        realm.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        int y = realm.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        LOG.info("[AUTOTEST] biome {} at {} surface {}", biome, pos, y);
        for (net.minecraft.server.level.ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.teleportTo(realm, pos.getX() + 0.5, Math.max(y, 42) + 6, pos.getZ() + 0.5, 30F, 15F);
        }
    }

    private <T extends BossEntity> void bossRun(String id, Class<T> cls, int[][] abilities) {
        cmd(10, null, "kill @e[type=!player]", "tp @a 1 -60 2 180 -6", TARGET,
                "summon krolasyonbosses:" + id + " 0 -60 -30 {Rotation:[0f,0f]}");
        wait(18, id + "_run");
        wait(70, id + "_idle_far");
        cmd(20, id + "_front", "tp @a 1 -60 -5 180 -8");
        cmd(20, id + "_side", "tp @a -9 -60 -16 -90 -6");
        cmd(20, id + "_back", "tp @a 2 -60 -27 0 -6");
        cmd(2, null, "tp @a 1 -60 -5 180 -8");
        int last = -1;
        for (int[] a : abilities) {
            if (a[0] != last) force(cls, a[0]);
            wait(a[1], id + "_ability" + a[0] + (a[0] == last ? "b" : ""));
            last = a[0];
            wait(40, null);
        }
        server(10, null, s -> each(s, cls, e -> e.hurt(e.damageSources().generic(), e.getMaxHealth() * 0.55F)));
        wait(30, id + "_phase2");
        server(20, null, s -> each(s, cls, LivingEntity::kill));
        wait(28, id + "_death");
        wait(70, null);
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action)); }

    private <T extends BossEntity> void force(Class<T> cls, int ability) {
        steps.add(new Step(1, null, s -> each(s, cls, b -> {
            LivingEntity target = b.getTarget();
            if (target == null) {
                List<Zombie> z = b.level().getEntitiesOfClass(Zombie.class, b.getBoundingBox().inflate(40));
                if (!z.isEmpty()) target = z.get(0);
            }
            if (target == null) {
                List<net.minecraft.world.entity.monster.Husk> z = b.level().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, b.getBoundingBox().inflate(40));
                if (!z.isEmpty()) target = z.get(0);
            }
            LOG.info("[AUTOTEST] force {} ability {} target {}", b.getName().getString(), ability, target);
            if (target != null) b.debugForceAbility(ability, target);
        })));
    }

    private static <T extends BossEntity> void each(MinecraftServer s, Class<T> cls, Consumer<T> c) {
        ServerLevel l = s.overworld();
        for (T e : l.getEntitiesOfClass(cls, new AABB(-80, -80, -80, 80, 40, 80))) c.accept(e);
    }

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
            LevelSettings settings = new LevelSettings("ktest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ktest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (showGui > 0) {
            if (!(mc.screen instanceof com.krolasyon.bosses.realm.client.RealmScreens.Journal j) || j.page() != showGui - 1) {
                com.krolasyon.bosses.realm.client.RealmScreens.openJournal();
                if (mc.screen instanceof com.krolasyon.bosses.realm.client.RealmScreens.Journal j2) j2.setPage(showGui - 1);
            }
            mc.options.hideGui = false;
        } else {
            if (mc.screen != null) mc.setScreen(null);
            mc.options.hideGui = true;
        }
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart && st.action() != null) {
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    st.action().accept(s);
                } catch (Throwable t) {
                    LOG.error("[AUTOTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> each(srv, BossEntity.class, b -> LOG.info("[AUTOTEST] state {} pos={} hp={} target={} ability={} t={} anim={}",
                        b.getName().getString(), b.position(), b.getHealth(), b.getTarget() == null ? null : b.getTarget().getName().getString(),
                        b.debugAbility(), b.debugAbilityTick(), st.shot())));
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                Screenshot.grab(mc.gameDirectory, "ktest_" + String.format("%02d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
