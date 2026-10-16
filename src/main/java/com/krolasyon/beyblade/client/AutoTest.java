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
        // ---- the Beyblade Workbench with a customised Pegasus (Big Bang wheel set, C145 track, F:D tip)
        cmd(4, null, "setblock 3 -60 -2 librabeyblade:bey_workbench", "tp @a 3.5 -59 0.5 180 30");
        server(6, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().invulnerable = true;
            p.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, pl) -> new com.krolasyon.beyblade.workbench.WorkbenchMenu(id, inv,
                    net.minecraft.world.inventory.ContainerLevelAccess.create(s.overworld(), new net.minecraft.core.BlockPos(3, -60, -2))),
                    net.minecraft.network.chat.Component.translatable("container.librabeyblade.workbench")));
            p.getInventory().add(new ItemStack(ModItems.part("tip_rf")));
            p.getInventory().add(new ItemStack(ModItems.part("track_230")));
            p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 8));
            if (p.containerMenu instanceof com.krolasyon.beyblade.workbench.WorkbenchMenu m) {
                m.getSlot(com.krolasyon.beyblade.workbench.WorkbenchMenu.BEY).set(custom(ModItems.PEGASUS.get(), "bolt_bigbang:1", "ring_bigbang:1", "wheel_bigbang:3", "track_c145:0", "tip_fd:1"));
                m.getSlot(com.krolasyon.beyblade.workbench.WorkbenchMenu.MATERIAL).set(new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 4));
                m.broadcastChanges();
            }
        });
        client(2, null, mc -> showGui = true);
        wait(30, "workbench");
        server(2, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            if (p.containerMenu instanceof com.krolasyon.beyblade.workbench.WorkbenchMenu m) {
                // take the track out (the beyblade becomes incomplete) and upgrade the face bolt with the gold
                m.clickMenuButton(p, 0);
                m.getSlot(com.krolasyon.beyblade.workbench.WorkbenchMenu.PART0 + 3).set(ItemStack.EMPTY);
                m.broadcastChanges();
            }
        });
        wait(20, "workbench_incomplete");
        server(2, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            if (p.containerMenu instanceof com.krolasyon.beyblade.workbench.WorkbenchMenu m) {
                m.getSlot(com.krolasyon.beyblade.workbench.WorkbenchMenu.PART0 + 3).set(new ItemStack(ModItems.part("track_230")));
                m.broadcastChanges();
            }
        });
        wait(20, "workbench_230");
        server(2, null, s -> s.getPlayerList().getPlayers().get(0).closeContainer());
        client(2, null, mc -> { mc.setScreen(null); showGui = false; });
        // ---- customised beyblades side by side: tall and low tracks, wide and sharp tips, the photo wheels
        cmd(2, null, CLEAR, "gamemode spectator @a", "tp @a 0.5 -57.6 -9.6 180 22");
        server(4, null, s -> {
            launchCustom(s, ModEntities.PEGASUS.get(), -4.5, custom(ModItems.PEGASUS.get(), "bolt_bigbang:0", "ring_bigbang:0", "wheel_bigbang:0", "track_230:0", "tip_fd:0"), true);
            launchCustom(s, ModEntities.BEFFAL.get(), -1.5, custom(ModItems.BEFFAL.get(), "bolt_phoenix:0", "ring_phoenix:0", "wheel_phoenix:0", "track_bd145:0", "tip_wb:0"), true);
            launchCustom(s, ModEntities.VARIARES.get(), 1.5, custom(ModItems.VARIARES.get(), "bolt_inferno:0", "ring_inferno:0", "wheel_inferno:0", "track_w105:0", "tip_xf:0"), true);
            launchCustom(s, ModEntities.LEONE.get(), 4.5, custom(ModItems.LEONE.get(), "bolt_leone:0", "ring_leone:0", "wheel_leone:0", "track_85:0", "tip_s:0"), true);
        });
        wait(40, "custom_lineup");
        cmd(30, "custom_low", "tp @a -2 -58.4 -11.6 200 8");
        // ---- and fighting
        server(2, null, s -> BeybladeEntity.rivalAbilities = true);
        cmd(2, null, CLEAR, "tp @a 0.5 -51 0 180 36");
        server(2, null, s -> {
            launchCustom(s, ModEntities.PEGASUS.get(), -5, custom(ModItems.PEGASUS.get(), "bolt_bigbang:3", "ring_bigbang:3", "wheel_bigbang:3", "track_c145:3", "tip_fd:3"), false);
            launchCustom(s, ModEntities.BEFFAL.get(), 5, custom(ModItems.BEFFAL.get(), "bolt_phoenix:2", "ring_phoenix:2", "wheel_phoenix:2", "track_bd145:2", "tip_wb:2"), false);
            launchCustom(s, ModEntities.VARIARES.get(), 0.5, custom(ModItems.VARIARES.get(), "bolt_inferno:1", "ring_inferno:1", "wheel_inferno:1", "track_230:1", "tip_rf:1"), false);
        });
        wait(80, "custom_battle_1");
        wait(80, "custom_battle_2");
        server(2, null, s -> BeybladeEntity.rivalAbilities = false);
        // ---- Evil Befall's five special moves
        showcase("beffal", 1, "tp @a 0.5 -53 -2 180 25", 50, "plumage");
        showcase("beffal", 2, "tp @a 0.5 -52 -3 180 30", 50, "gaze");
        showcase("beffal", 3, "tp @a 0.5 -51 -1 180 32", 40, "feathers");
        showcase("beffal", 4, "tp @a 0.5 -53 -4 180 35", 60, "mirage");
        showcase("beffal", 5, "tp @a 0.5 -48 8 180 12", 90, "requiem_1", "requiem_2");
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
