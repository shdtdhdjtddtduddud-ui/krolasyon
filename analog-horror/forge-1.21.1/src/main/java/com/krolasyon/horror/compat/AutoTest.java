package com.krolasyon.horror.compat;

import com.krolasyon.horror.client.ClientFx;
import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

/**
 * CI-only (-Dkrolasyon.autotest=true): builds a fluorescent office hallway in a flat world and plays a scripted
 * cinematic with the Hatman (used for screenshots and the trailer capture), then lets the real AI loose outdoors.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick, inWorld = -1, lastT = -1;
    private boolean worldRequested;
    private final List<Cue> cues = new ArrayList<>();
    private static final int[] LIGHTS_Z = {1, -3, -7, -11, -15, -19, -23, -27, -31, -35, -39, -43};

    record Cue(int at, int until, String shot, Consumer<Ctx> fn, Runnable client) {}

    /** per-tick context */
    record Ctx(MinecraftServer server, ServerLevel level, int t, int local) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private void at(int t, Consumer<Ctx> fn) { cues.add(new Cue(t, t, null, fn, null)); }

    private void client(int t, Runnable r) { cues.add(new Cue(t, t, null, null, r)); }

    private void span(int from, int to, Consumer<Ctx> fn) { cues.add(new Cue(from, to, null, fn, null)); }

    private void shot(int t, String name) { cues.add(new Cue(t, t, name, null, null)); }

    private static void cmd(Ctx c, String... commands) {
        for (String s : commands)
            c.server().getCommands().performPrefixedCommand(c.server().createCommandSourceStack().withSuppressedOutput().withPermission(4), s);
    }

    private static void cam(Ctx c, double x, double y, double z, float yaw, float pitch) {
        cmd(c, String.format(java.util.Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", x, y, z, yaw, pitch));
    }

    private static HatmanEntity hat(Ctx c) {
        List<HatmanEntity> l = c.level().getEntitiesOfClass(HatmanEntity.class, new AABB(-200, -80, -200, 200, 40, 200), e -> e.isAlive() && !e.isRemoved());
        return l.isEmpty() ? null : l.get(0);
    }

    private static void place(Ctx c, double x, double z, float yaw) {
        HatmanEntity h = hat(c);
        if (h == null) return;
        h.moveTo(x, -60, z, yaw, 0);
        h.setYHeadRot(yaw);
        h.setYBodyRot(yaw);
    }

    private static void clear(Ctx c) {
        for (HatmanEntity h : c.level().getEntitiesOfClass(HatmanEntity.class, new AABB(-200, -80, -200, 200, 40, 200))) h.discard();
        cmd(c, "kill @e[type=item]");
    }

    private static void sound(Ctx c, SoundEvent s, double z, float vol) {
        c.level().playSound(null, 0.5, -59, z, s, SoundSource.HOSTILE, vol, 1.0F);
    }

    private static void lights(Ctx c, boolean on, int... zs) {
        for (int z : zs) cmd(c, "setblock 0 -56 " + z + " " + (on ? "minecraft:sea_lantern" : "minecraft:white_wool"));
    }

    private static void scene(String name) {
        LOG.info("[AUTOTEST] SCENE {} {}", name, System.currentTimeMillis());
    }

    private AutoTest() {
        // ------------------------------------------------ build the hallway
        at(0, c -> {
            scene("build");
            cmd(c, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "gamerule sendCommandFeedback false", "difficulty normal", "time set 6000", "weather clear", "kill @e[type=!player]",
                    "fill -14 -61 -54 14 -50 10 minecraft:air",
                    "fill -4 -61 -50 4 -61 6 minecraft:blue_wool",
                    "fill -3 -60 -48 -3 -57 4 minecraft:white_concrete", "fill 3 -60 -48 3 -57 4 minecraft:white_concrete",
                    "fill -3 -60 -48 3 -57 -48 minecraft:white_concrete", "fill -3 -60 5 3 -57 5 minecraft:white_concrete",
                    "fill -3 -56 -48 3 -56 5 minecraft:white_wool",
                    "fill -9 -61 -17 -3 -56 -8 minecraft:white_concrete hollow",
                    "setblock -3 -60 -12 minecraft:oak_door[facing=east,half=lower]", "setblock -3 -59 -12 minecraft:oak_door[facing=east,half=upper]",
                    "setblock 3 -60 -27 minecraft:oak_door[facing=west,half=lower]", "setblock 3 -59 -27 minecraft:oak_door[facing=west,half=upper]",
                    "setblock 0 -60 -47 minecraft:oak_door[facing=south,half=lower]", "setblock 0 -59 -47 minecraft:oak_door[facing=south,half=upper]",
                    "fill 3 -59 -22 3 -58 -18 minecraft:white_stained_glass",
                    "gamemode spectator @a");
            lights(c, true, LIGHTS_Z);
        });
        // ------------------------------------------------ model check in daylight (outside, flat grass)
        at(20, c -> {
            scene("daylight_model");
            cmd(c, "summon krolasyonhorror:hatman 0.5 -60 20.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
        });
        at(24, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.STALK, 0); place(c, 0.5, 20.5, 0); });
        span(25, 140, c -> cam(c, 0.5, -59.5, 27.5, 180, 8));
        shot(70, "model_front");
        span(141, 200, c -> cam(c, 7.5, -59.5, 20.5, 90, 8));
        shot(190, "model_side");
        span(201, 260, c -> cam(c, 2.5, -59.0, 24.0, 150, 25));
        at(205, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.FROZEN, 0); });
        shot(250, "model_frozen");
        at(262, c -> { HatmanEntity h = hat(c); if (h != null) h.debugAction("hat_tip"); });
        shot(282, "model_hattip");
        at(300, c -> { HatmanEntity h = hat(c); if (h != null) h.debugCrawl(true); });
        span(300, 340, c -> place(c, 0.5, 20.5 + (c.t() - 300) * 0.08, 0));
        shot(330, "model_crawl");
        at(345, c -> { HatmanEntity h = hat(c); if (h != null) { h.debugCrawl(false); h.debugState(HatmanEntity.HUNT, 100); } });
        span(345, 380, c -> place(c, 0.5, 24 + (c.t() - 345) * 0.25, 0));
        span(345, 380, c -> cam(c, 6.5, -59.5, 28.0, 120, 5));
        span(381, 440, c -> cam(c, 6.5, -59.0, 32.75, 90, 12));
        shot(368, "model_run");
        at(390, c -> cmd(c, "kill @e[type=krolasyonhorror:hatman]"));
        shot(420, "model_death");

        // ------------------------------------------------ CINEMATIC (night, hallway)
        int T = 520;
        at(T - 20, c -> cmd(c, "time set 18000", "tp @e[type=item] 0 -200 0"));
        at(T, c -> {
            scene("hall_far");
            cmd(c, "summon krolasyonhorror:hatman 0.5 -60 -36.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
        });
        at(T + 3, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.STALK, 0); place(c, 0.5, -36.5, 0); });
        span(T, T + 180, c -> cam(c, 0.5, -60, 3.0 - c.local() * 0.012, 180, 1));
        span(T + 60, T + 100, c -> { if (c.local() % 4 < 2) lights(c, false, -31, -39); else lights(c, true, -31, -39); });
        at(T + 101, c -> lights(c, true, -31, -39));
        at(T + 110, c -> { HatmanEntity h = hat(c); if (h != null) h.debugAction("look"); });
        shot(T + 90, "c_hall_far");

        T += 180;   // peeking out of the side room
        at(T, c -> {
            scene("peek");
            cmd(c, "setblock -3 -59 -12 minecraft:air", "setblock -3 -60 -12 minecraft:air", "kill @e[type=item]");
            HatmanEntity h = hat(c);
            if (h != null) h.debugState(HatmanEntity.PEEK, 0);
            place(c, -2.75, -11.5, 0);
        });
        span(T, T + 170, c -> cam(c, 1.2, -60, -2.0 - c.local() * 0.01, 165, 2));
        shot(T + 100, "c_peek");

        T += 170;   // creeping up the hallway
        at(T, c -> {
            scene("approach");
            cmd(c, "setblock -3 -60 -12 minecraft:oak_door[facing=east,half=lower]", "setblock -3 -59 -12 minecraft:oak_door[facing=east,half=upper]");
            HatmanEntity h = hat(c);
            if (h != null) h.debugState(HatmanEntity.STALK, 0);
        });
        span(T, T + 220, c -> place(c, 0.5, -36.5 + c.local() * 0.055, 0));
        span(T, T + 220, c -> cam(c, 0.5, -60, 1.0, 180, 2));
        shot(T + 150, "c_approach");

        T += 220;   // lights die - it is right there, frozen like the photograph
        at(T, c -> { scene("blackout"); lights(c, false, LIGHTS_Z); sound(c, ModSounds.STATIC.get(), 0, 0.8F); });
        at(T + 1, c -> ClientFx.debugFlash(14));
        at(T + 30, c -> {
            HatmanEntity h = hat(c);
            if (h != null) h.debugState(HatmanEntity.FROZEN, 0);
            place(c, 0.4, -6.5, 10);
            lights(c, true, LIGHTS_Z);
        });
        span(T + 30, T + 190, c -> cam(c, 0.5, -60, 1.5 - c.local() * 0.006, 180, 3));
        shot(T + 90, "c_frozen");

        T += 190;   // twitch, then the hat
        at(T, c -> { scene("twitch"); HatmanEntity h = hat(c); if (h != null) { h.debugState(HatmanEntity.STALK, 0); h.debugAction("twitch"); } sound(c, ModSounds.TWITCH.get(), -8, 1.5F); });
        shot(T + 8, "c_twitch");
        at(T + 40, c -> { HatmanEntity h = hat(c); if (h != null) h.debugAction("hat_tip"); sound(c, ModSounds.HAT_TIP.get(), -8, 1.5F); });
        shot(T + 56, "c_hattip");
        span(T, T + 100, c -> cam(c, 0.5, -60, 0.3, 180, 3));

        T += 100;   // scream and charge
        at(T, c -> { scene("scream"); HatmanEntity h = hat(c); if (h != null) { h.debugState(HatmanEntity.HUNT, 100); h.debugAction("scream"); } sound(c, ModSounds.SCREAM.get(), -8, 2F); });
        shot(T + 14, "c_scream");
        span(T + 46, T + 66, c -> place(c, 0.4, -6.5 + c.local() * 0.30, 0));
        span(T, T + 70, c -> cam(c, 0.5, -60, 0.3, 180, 3));
        at(T + 62, c -> ClientFx.debugJumpscare());
        shot(T + 64, "c_jumpscare");
        at(T + 70, c -> { lights(c, false, LIGHTS_Z); clear(c); });

        T += 110;   // it crawls
        at(T, c -> {
            scene("crawl");
            lights(c, true, LIGHTS_Z);
            cmd(c, "summon krolasyonhorror:hatman 0.5 -60 -32.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
        });
        at(T + 2, c -> { HatmanEntity h = hat(c); if (h != null) { h.debugState(HatmanEntity.HUNT, 100); h.debugCrawl(true); } });
        span(T + 2, T + 160, c -> place(c, 0.5, -32.5 + c.local() * 0.13, 0));
        span(T, T + 160, c -> cam(c, 0.5, -60, 2.0, 180, 8));
        shot(T + 110, "c_crawl");
        at(T + 160, c -> clear(c));

        T += 180;   // it rises from the floor
        at(T, c -> {
            scene("emerge");
            cmd(c, "summon krolasyonhorror:hatman 0.5 -60 -10.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}");
        });
        at(T + 2, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.EMERGE, 0); sound(c, ModSounds.EMERGE.get(), -10, 1.5F); });
        at(T + 62, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.STALK, 0); });
        span(T, T + 90, c -> cam(c, 0.5, -60, 0.0, 180, 6));
        shot(T + 30, "c_emerge");
        at(T + 90, c -> { scene("end_cinematic"); clear(c); });

        // ------------------------------------------------ live AI outdoors at night
        T += 110;
        at(T, c -> {
            scene("live_ai");
            cmd(c, "fill -14 -61 -54 14 -50 10 minecraft:air", "fill -14 -61 -54 14 -61 10 minecraft:grass_block",
                    "gamemode survival @a", "effect give @a minecraft:resistance 99999 255 true", "effect give @a minecraft:saturation 99999 10 true",
                    "tp @a 0.5 -60 0.5 180 0", "summon krolasyonhorror:hatman 0.5 -60 -22.5 {PersistenceRequired:1b}");
        });
        at(T + 2, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.STALK, 35); });
        // look at it, look away, look back...
        span(T + 70, T + 100, c -> cmd(c, "execute as @a at @s run tp @s ~ ~ ~ 0 0"));
        span(T + 170, T + 200, c -> cmd(c, "execute as @a at @s run tp @s ~ ~ ~ 180 0"));
        shot(T + 60, "ai_watched");
        shot(T + 190, "ai_after_lookaway");
        span(T + 230, T + 260, c -> cmd(c, "execute as @a at @s run tp @s ~ ~ ~ 0 0"));
        span(T + 320, T + 350, c -> cmd(c, "execute as @a at @s run tp @s ~ ~ ~ 180 0"));
        shot(T + 345, "ai_closer");
        at(T + 360, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.STALK, 99.5F); });
        shot(T + 420, "ai_hunt");
        shot(T + 470, "ai_hunt2");
        // ------------------------------------------------ the haunted house (reference photos)
        int H = T + 540;
        at(H, c -> {
            scene("house_build");
            clear(c);
            cmd(c, "gamemode spectator @a", "effect clear @a", "time set 6000", "tp @a 95 -50 30 140 8");
        });
        at(H + 20, c -> com.krolasyon.horror.house.HauntedHouse.build(c.level(), new net.minecraft.core.BlockPos(60, -61, -11)));
        span(H + 21, H + 80, c -> cam(c, 95, -50, 30, 140.2F, 8));
        shot(H + 75, "house_exterior");
        at(H + 81, c -> cmd(c, "time set 18000"));
        span(H + 82, H + 140, c -> cam(c, 70.5, -60, 10.3, 180, 2));
        shot(H + 135, "house_hall_ground");
        span(H + 141, H + 200, c -> cam(c, 66.6, -58.2, 9.6, 163.5F, 16));
        shot(H + 195, "house_living");
        span(H + 201, H + 260, c -> cam(c, 74.5, -54, 10.4, 180, 6));
        shot(H + 255, "house_landing");
        span(H + 261, H + 320, c -> cam(c, 70.5, -54, 10.4, 180, 3));
        shot(H + 315, "house_hall_upper");
        at(H + 321, c -> cmd(c, "summon krolasyonhorror:hatman 70.5 -54 -2.4 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f]}"));
        at(H + 324, c -> { HatmanEntity h = hat(c); if (h != null) h.debugState(HatmanEntity.FROZEN, 0); });
        span(H + 321, H + 380, c -> cam(c, 70.5, -54, 10.4, 180, 3));
        shot(H + 375, "house_hatman_upper");
        span(H + 381, H + 430, c -> cam(c, 70.5, -54, 2.0, 180, 3));
        shot(H + 425, "house_hatman_close");
        // ------------------------------------------------ story mode (cutscenes, tapes, notes, HUD) - English then Turkish
        int S = H + 450;
        at(S, c -> {
            scene("story_start");
            clear(c);
            cmd(c, "gamemode survival @a", "tp @a 30.5 -60 80.5 180 0");
        });
        at(S + 10, c -> com.krolasyon.horror.story.StoryDirector.start(c.server().getPlayerList().getPlayers().get(0)));
        shot(S + 110, "story_intro_a");
        shot(S + 420, "story_intro_b");
        shot(S + 700, "story_intro_c");
        shot(S + 1000, "story_intro_d");
        shot(S + 1290, "story_intro_e");
        shot(S + 1400, "story_chapter1");
        shot(S + 1560, "story_objective");
        client(S + 1600, () -> net.minecraft.client.Minecraft.getInstance().setScreen(new com.krolasyon.horror.client.story.TapeScreen("tape2")));
        shot(S + 1700, "story_tape2_a");
        shot(S + 1950, "story_tape2_b");
        client(S + 1960, () -> net.minecraft.client.Minecraft.getInstance().setScreen(null));
        client(S + 1980, () -> com.krolasyon.horror.story.StoryBridge.OPEN_NOTE.accept(19));
        shot(S + 2000, "story_note19");
        client(S + 2010, () -> net.minecraft.client.Minecraft.getInstance().setScreen(null));
        client(S + 2020, () -> com.krolasyon.horror.story.StoryBridge.TO_CLIENT.add(com.krolasyon.horror.story.StoryBridge.Msg.of("caught")));
        shot(S + 2040, "story_caught");
        client(S + 2080, () -> com.krolasyon.horror.story.StoryBridge.TO_CLIENT.add(com.krolasyon.horror.story.StoryBridge.Msg.of("fade_in")));
        client(S + 2120, () -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.options.languageCode = "tr_tr";
            mc.getLanguageManager().setSelected("tr_tr");
            mc.reloadResourcePacks();
        });
        client(S + 2320, () -> com.krolasyon.horror.story.StoryBridge.TO_CLIENT.add(com.krolasyon.horror.story.StoryBridge.Msg.of("chapter", "", 3)));
        shot(S + 2380, "story_tr_chapter3");
        client(S + 2480, () -> net.minecraft.client.Minecraft.getInstance().setScreen(new com.krolasyon.horror.client.story.TapeScreen("tape4")));
        shot(S + 2620, "story_tr_tape4");
        client(S + 2640, () -> net.minecraft.client.Minecraft.getInstance().setScreen(null));
        client(S + 2660, () -> com.krolasyon.horror.story.StoryBridge.OPEN_NOTE.accept(10));
        shot(S + 2680, "story_tr_note10");
        client(S + 2690, () -> net.minecraft.client.Minecraft.getInstance().setScreen(new com.krolasyon.horror.client.story.CreditsScreen("evict")));
        shot(S + 2780, "story_tr_ending");
        shot(S + 3150, "story_tr_credits");
        client(S + 3160, () -> net.minecraft.client.Minecraft.getInstance().setScreen(null));
        at(S + 3200, c -> com.krolasyon.horror.story.StoryDirector.stop(c.server().getPlayerList().getPlayers().get(0)));
        at(S + 3240, c -> LOG.info("[AUTOTEST] finished"));
        shot(S + 3241, "END");
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
            LevelSettings settings = new LevelSettings("htest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("htest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), null);
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !(mc.screen instanceof com.krolasyon.horror.client.story.TapeScreen
                || mc.screen instanceof com.krolasyon.horror.client.story.NoteScreen
                || mc.screen instanceof com.krolasyon.horror.client.story.CreditsScreen)) mc.setScreen(null);
        mc.options.hideGui = false;
        if (inWorld < 0) {
            inWorld = tick + 80;
            LOG.info("[AUTOTEST] in world {}", System.currentTimeMillis());
        }
        int t = tick - inWorld;
        if (t < 0) return;
        MinecraftServer srv = mc.getSingleplayerServer();
        int prevT = lastT;
        lastT = t;
        for (Cue cue : cues) {
            if (cue.at() == cue.until() ? (cue.at() <= prevT || cue.at() > t) : (t < cue.at() || t > cue.until())) continue;
            if (cue.client() != null) {
                try {
                    cue.client().run();
                } catch (Throwable ex) {
                    LOG.error("[AUTOTEST] client cue failed", ex);
                }
            }
            if (cue.fn() != null) {
                srv.execute(() -> {
                    try {
                        cue.fn().accept(new Ctx(srv, srv.overworld(), t, t - cue.at()));
                    } catch (Throwable e) {
                        LOG.error("[AUTOTEST] cue failed", e);
                    }
                });
            }
            if (cue.shot() != null) {
                if (cue.shot().equals("END")) {
                    LOG.info("[AUTOTEST] END {}", System.currentTimeMillis());
                    mc.stop();
                    return;
                }
                srv.execute(() -> {
                    HatmanEntity h = hat(new Ctx(srv, srv.overworld(), t, 0));
                    LOG.info("[AUTOTEST] shot {} hatman={} state={} aggr={} crawl={}", cue.shot(), h == null ? null : h.position(),
                            h == null ? -1 : h.getState(), h == null ? 0 : h.getAggression(), h != null && h.isCrawling());
                });
                Screenshot.grab(mc.gameDirectory, "htest_" + String.format("%04d_", t) + cue.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> LOG.info("[AUTOTEST] screenshot {}", cue.shot()));
            }
        }
    }
}
