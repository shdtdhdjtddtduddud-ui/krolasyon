package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModEntities;
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
        // ---- the eight legends spinning side by side
        cmd(10, null, "tp @a 0.5 -57.4 -9.2 180 26",
                "summon librabeyblade:leone -6.2 -58 -14.5 {Frozen:1b}", "summon librabeyblade:eagle -4.4 -58 -14.5 {Frozen:1b}",
                "summon librabeyblade:ldrago -2.6 -58 -14.5 {Frozen:1b}", "summon librabeyblade:pegasus -0.8 -58 -14.5 {Frozen:1b}",
                "summon librabeyblade:beffal 1.0 -58 -14.5 {Frozen:1b}", "summon librabeyblade:variares 2.8 -58 -14.5 {Frozen:1b}",
                "summon librabeyblade:libra 4.6 -58 -14.5 {Frozen:1b}", "summon librabeyblade:tempo 6.4 -58 -14.5 {Frozen:1b}");
        record(true);
        wait(50, "lineup_front");
        // ---- launches by the player: a clean one, then a sloppy one (too steep, shaky aim, weak pull)
        cmd(2, null, CLEAR, "gamemode survival @a", "effect give @a minecraft:resistance 600 9 true", "tp @a 0.5 -60 5 180 30");
        for (int k = 0; k < 2; k++) {
            final boolean good = k == 0;
            server(4, null, s -> {
                ServerPlayer p = s.getPlayerList().getPlayers().get(0);
                p.getInventory().setItem(0, new ItemStack(ModItems.STANDARD_LAUNCHER.get()));
                p.getInventory().setItem(1, new ItemStack(ModItems.PEGASUS.get()));
                p.getInventory().selected = 0;
            });
            client(2, null, mc -> {
                mc.player.getInventory().selected = 0;
                mc.player.setYRot(180F);
                mc.player.setXRot(good ? 30F : 80F);
                // hold right click like a player would
                net.minecraft.client.KeyMapping.click(mc.options.keyUse.getKey());
                mc.options.keyUse.setDown(true);
            });
            client(0, null, mc -> showGui = true);
            if (good) {
                wait(30, "launch_gauge_good");
                wait(9, null);
            } else {
                // shaking the aim around while winding
                for (int j2 = 0; j2 < 4; j2++) {
                    final int jj = j2;
                    client(2, null, mc -> mc.player.setYRot(180F + (jj % 2 == 0 ? 9F : -9F)));
                }
                wait(1, "launch_gauge_bad");
            }
            client(0, null, mc -> {
                mc.options.keyUse.setDown(false);
                mc.player.setXRot(12F);
                mc.player.setYRot(180F);
            });
            wait(30, good ? "launch_good_flying" : "launch_bad_flying");
            wait(30, good ? "launch_good_spinning" : "launch_bad_wobbling");
            client(0, null, mc -> showGui = false);
            cmd(30, null, CLEAR);
        }
        cmd(2, null, "gamemode spectator @a");
        record(false);
        wait(20, "END");
    }

    /** a beyblade item with the given parts ("part_id:level") */
    private static ItemStack custom(net.minecraft.world.item.Item bey, String... parts) {
        ItemStack st = new ItemStack(bey);
        for (String p : parts) {
            String[] a = p.split(":");
            com.krolasyon.beyblade.parts.BeyPart part = com.krolasyon.beyblade.parts.BeyPart.get(a[0]);
            com.krolasyon.beyblade.parts.Loadout.write(st, part.type(), part, Integer.parseInt(a[1]));
        }
        return st;
    }

    private static void launchCustom(MinecraftServer s, net.minecraft.world.entity.EntityType<? extends BeybladeEntity> type, double x, ItemStack st, boolean frozen) {
        BeybladeEntity b = BeybladeEntity.launch(type, s.overworld(), null, st, new Vec3(x, -57.5, frozen ? -14.5 : -14.5 + (x > 0 ? -2 : 2)), Vec3.ZERO, 1F);
        b.setFrozen(frozen);
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
        if (mc.screen != null && !(mc.screen instanceof CommandScreen) && !(mc.screen instanceof WorkbenchScreen)) mc.setScreen(null);
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
                srv.execute(() -> each(srv, null, b -> LOG.info("[AUTOTEST] state pos={} spin={} state={} ability={} rival={} sclash={} shot={}",
                        b.position(), b.getSpin(), b.getState(), b.getAbility(), b.isRival(), b.getSpecialClashPartner(), st.shot())));
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
