package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * CI-only visual self test (inactive unless -Dlibra.autotest=true): flat world, close-ups of the model, a rival battle,
 * every ability forced in turn, an owned Libra with the HUD, recall and defeat. Saves screenshots, then quits.
 */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private boolean showGui;

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui, Consumer<Minecraft> clientAction) {
        Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui) { this(delay, shot, action, gui, null); }
    }

    private boolean allowScreen;

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        BeybladeEntity.rivalAbilities = false;
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final String A = "summon librabeyblade:leone 0 -60 -6 {Spin:3000f,MaxSpin:3000f,Tags:[\"a\"]}";
    private static final String B = "summon librabeyblade:eagle 3.5 -60 -9 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"b\"]}";

    private static final String ST = "summon librabeyblade:stadium 0.5 -60 -14.5";

    private static final String CLEAR = "kill @e[type=!player,type=!librabeyblade:stadium]";

    /** one special-move showcase: the user beyblade tagged a plus two common targets, then force the move */
    private void showcase(String kind, int ability, String cam, int ticks, String... shots) {
        cmd(4, null, CLEAR, "weather clear", cam,
                "summon librabeyblade:" + kind + " 0.5 -58 -12 {Tags:[\"a\"]}",
                "summon librabeyblade:common -2.5 -58 -17 {Variant:5}", "summon librabeyblade:common 3.5 -58 -17.5 {Variant:0}");
        wait(18, null);
        force(ability);
        int step = ticks / (shots.length + 1);
        for (String sh : shots) wait(step, sh);
        wait(step, null);
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 5000", "weather clear", "difficulty peaceful", "gamemode spectator @a", "kill @e[type=!player]", ST);
        // ---- the seven legends spinning side by side
        cmd(10, null, "tp @a 0.5 -57.4 -9.2 180 26",
                "summon librabeyblade:leone -5.5 -58 -14.5 {Frozen:1b}", "summon librabeyblade:eagle -3.7 -58 -14.5 {Frozen:1b}",
                "summon librabeyblade:ldrago -1.8 -58 -14.5 {Frozen:1b}", "summon librabeyblade:pegasus 0 -58 -14.5 {Frozen:1b}",
                "summon librabeyblade:variares 1.8 -58 -14.5 {Frozen:1b}", "summon librabeyblade:libra 3.7 -58 -14.5 {Frozen:1b}",
                "summon librabeyblade:tempo 5.5 -58 -14.5 {Frozen:1b}");
        record(true);
        wait(50, "lineup_front");
        cmd(40, "lineup_low", "tp @a -3 -58.3 -11 205 14");
        cmd(40, "lineup_high", "tp @a 0.5 -53.5 -8.5 180 55");
        // ---- free battle, wide then close
        server(2, null, s -> BeybladeEntity.rivalAbilities = true);
        cmd(2, null, CLEAR, "tp @a 0.5 -49.5 3 180 36",
                "summon librabeyblade:pegasus -5 -57 -14.5", "summon librabeyblade:ldrago 6 -57 -14.5",
                "summon librabeyblade:leone 0.5 -57 -20", "summon librabeyblade:variares 0.5 -57 -9");
        wait(70, "battle_wide_1");
        wait(70, "battle_wide_2");
        cmd(2, null, "tp @a -1 -55.8 -6.5 170 30");
        wait(60, "battle_close_1");
        wait(60, "battle_close_2");
        server(2, null, s -> BeybladeEntity.rivalAbilities = false);
        // ---- special moves
        showcase("leone", 6, "tp @a 0.5 -45 26 180 4", 200, "leone_cyclone_1", "leone_cyclone_2", "leone_cyclone_3");
        showcase("leone", 5, "tp @a 0.5 -47 10 180 15", 110, "leone_tornadoes_1", "leone_tornadoes_2");
        showcase("ldrago", 3, "tp @a 0.5 -53 13 180 -8", 130, "ldrago_dive_1", "ldrago_dive_2", "ldrago_dive_3");
        showcase("pegasus", 5, "tp @a 0.5 -47 8 180 25", 110, "pegasus_booster_1", "pegasus_booster_2", "pegasus_booster_3");
        showcase("variares", 5, "tp @a 0.5 -50 10 180 3", 90, "variares_sword_1", "variares_sword_2");
        showcase("tempo", 5, "tp @a 0.5 -47 8 180 18", 120, "tempo_clock_1", "tempo_clock_2");
        showcase("libra", 3, "tp @a 0.5 -51 4 180 25", 100, "libra_inferno_1", "libra_inferno_2");
        showcase("eagle", 5, "tp @a 0.5 -49 6 180 15", 130, "eagle_buster_1", "eagle_buster_2");
        showcase("variares", 4, "tp @a 0.5 -54 -4 180 30", 60, "variares_blades");
        showcase("tempo", 3, "tp @a 0.5 -52 -3 180 40", 50, "tempo_hands");
        record(false);
        wait(20, "END");
    }

    private void record(boolean on) {
        client(0, null, mc -> recording = on);
    }

    private boolean recording;
    private int frameNo;
    private long lastFrameNanos;
    private final StringBuilder frameList = new StringBuilder();
    private final java.util.concurrent.ExecutorService writer = java.util.concurrent.Executors.newFixedThreadPool(3);

    /** video capture: every rendered frame while recording, with real time durations for ffmpeg's concat demuxer */
    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (!recording || mc.level == null) {
            lastFrameNanos = 0;
            return;
        }
        if (lastFrameNanos != 0) frameList.append(String.format(java.util.Locale.ROOT, "duration %.4f%n", Math.min(0.25, (now - lastFrameNanos) / 1.0E9)));
        lastFrameNanos = now;
        String name = String.format("f_%06d.jpg", frameNo++);
        frameList.append("file '").append(name).append("'\n");
        com.mojang.blaze3d.platform.NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        java.io.File dir = new java.io.File(mc.gameDirectory, "frames");
        dir.mkdirs();
        writer.submit(() -> {
            try {
                java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(img.getWidth(), img.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < img.getHeight(); y++)
                    for (int x = 0; x < img.getWidth(); x++) {
                        int abgr = img.getPixelRGBA(x, y);
                        bi.setRGB(x, y, ((abgr & 0xFF) << 16) | (abgr & 0xFF00) | ((abgr >> 16) & 0xFF));
                    }
                javax.imageio.ImageIO.write(bi, "jpg", new java.io.File(dir, name));
            } catch (Exception ex) {
                LOG.error("[AUTOTEST] frame write failed", ex);
            } finally {
                img.close();
            }
        });
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }, null));
    }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) { steps.add(new Step(wait, shot, action, null)); }

    private void client(int wait, String shot, Consumer<Minecraft> action) { steps.add(new Step(wait, shot, null, null, action)); }

    private void force(int ability) {
        server(1, null, s -> each(s, "a", b -> {
            LOG.info("[AUTOTEST] force ability {} spin={}", ability, b.getSpin());
            b.debugForceAbility(ability);
        }));
    }

    private static void each(MinecraftServer s, String tag, Consumer<BeybladeEntity> c) {
        ServerLevel l = s.overworld();
        for (BeybladeEntity e : l.getEntitiesOfClass(BeybladeEntity.class, new AABB(-80, -80, -80, 80, 40, 80))) {
            if (tag == null || e.getTags().contains(tag)) c.accept(e);
        }
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
            LevelSettings settings = new LevelSettings("ltest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ltest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !(mc.screen instanceof CommandScreen)) mc.setScreen(null);
        mc.options.hideGui = !showGui;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.gui() != null) showGui = st.gui();
            if (st.clientAction() != null) st.clientAction().accept(mc);
            if (st.action() != null) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    try {
                        st.action().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] step failed", t);
                    }
                });
            }
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                MinecraftServer srv = mc.getSingleplayerServer();
                srv.execute(() -> each(srv, null, b -> LOG.info("[AUTOTEST] state pos={} spin={} state={} ability={} rival={} shot={}",
                        b.position(), b.getSpin(), b.getState(), b.getAbility(), b.isRival(), st.shot())));
                if (st.shot().equals("END")) {
                    writer.shutdown();
                    try { writer.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
                    LOG.info("[AUTOTEST] finished, {} video frames", frameNo);
                    try {
                        java.io.File dir = new java.io.File(mc.gameDirectory, "frames");
                        dir.mkdirs();
                        java.nio.file.Files.writeString(new java.io.File(dir, "list.txt").toPath(), frameList.toString());
                    } catch (java.io.IOException ex) {
                        LOG.error("[AUTOTEST] list write failed", ex);
                    }
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
