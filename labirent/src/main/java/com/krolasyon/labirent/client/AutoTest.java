package com.krolasyon.labirent.client;

import com.krolasyon.labirent.client.screen.CaptainScreen;
import com.krolasyon.labirent.client.screen.JournalScreen;
import com.krolasyon.labirent.client.screen.MapScreen;
import com.krolasyon.labirent.maze.Dims;
import com.krolasyon.labirent.maze.MazeManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** CI-only visual self test (inactive unless -Dlabirent.autotest=true): plays through the opening and takes screenshots. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private final List<Step> steps = new ArrayList<>();
    private int index;
    private int stepStart = -1;

    record Step(int delay, String shot, String[] commands, BooleanSupplier until, Supplier<Screen> screen, boolean hud) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        // the Box ride
        until(() -> inMaze() && Minecraft.getInstance().player.isPassenger(), 3600);
        step(60, "box_ride", false);
        until(() -> inMaze() && !Minecraft.getInstance().player.isPassenger(), 900);
        step(30, "box_arrived", true);
        cmd(40, "glade_hud", true, "time set 2000", "gamerule doDaylightCycle false", "weather clear");
        until(() -> MazeManager.get() != null && MazeManager.get().state.built, 6000);
        cmd(40, null, false, "gamemode spectator @a");
        cmd(60, "glade_overview", false, "tp @a 30 110 30 135 35");
        cmd(60, "glade_homestead", false, "tp @a 2 72 -2 -120 10");
        cmd(60, "glade_npcs", false, "tp @a -1 68 12 180 15");
        cmd(40, "glade_npcs_close", true, "tp @a -1 66 10 180 10");
        cmd(9, "npcs_stumble", false, "labirent tokez");
        cmd(60, "maze_from_above", false, "tp @a -60 120 -60 -45 40");
        cmd(60, "north_door", true, "tp @a 0 70 -18 180 -5");
        cmd(60, "corridor", false, "tp @a -3 66 -33 180 0");
        cmd(40, null, false, "gamemode creative @a", "tp @a -4 65 -33 -90 0",
                "summon labirent:griever 4 65 -33 {Rotation:[90f,0f],NoAI:1b}", "summon labirent:beetle_blade 0 65 -34");
        cmd(60, "griever_front", false, "tp @a -3 66 -33 -90 8");
        cmd(40, "griever_side", false, "tp @a 4 67 -29 180 25");
        cmd(5, null, false, "kill @e[type=labirent:griever]", "time set 12300");
        cmd(200, "doors_closing", true, "tp @a 0 70 -15 180 -5");
        cmd(180, "doors_closed", false, "tp @a 0 70 -15 180 -5");
        screen(40, "map_screen", () -> new MapScreen(false));
        screen(40, "captain_screen", CaptainScreen::new);
        screen(40, "journal_screen", JournalScreen::create);
        cmd(20, null, false, "labirent bolum 6", "labirent gunes", "time set 6000");
        cmd(200, "sun_gone", true, "tp @a 0 66 0 0 -10");
        cmd(60, "cliff", false, "tp @a -116 67 -3 90 15");
        cmd(20, null, false, "tp @a 403 61 0 -90 0");
        step(220, "chamber", false);
        step(20, "END", false);
    }

    private static boolean inMaze() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.level.dimension() == Dims.MAZE;
    }

    private void until(BooleanSupplier c, int max) { steps.add(new Step(max, null, new String[0], c, null, false)); }

    private void step(int d, String shot, boolean hud) { steps.add(new Step(d, shot, new String[0], null, null, hud)); }

    private void cmd(int d, String shot, boolean hud, String... c) { steps.add(new Step(d, shot, c, null, null, hud)); }

    private void screen(int d, String shot, Supplier<Screen> s) { steps.add(new Step(d, shot, new String[0], null, s, true)); }

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
            LevelSettings settings = new LevelSettings("ltest", GameType.SURVIVAL, false, Difficulty.EASY, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("ltest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null || index >= steps.size()) return;
        Step st = steps.get(index);
        if (mc.screen != null && st.screen() == null && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) mc.setScreen(null);
        if (stepStart < 0) {
            stepStart = tick;
            LOG.info("[AUTOTEST] step {} {}", index, st.shot());
            MinecraftServer s = mc.getSingleplayerServer();
            if (st.commands().length > 0) s.execute(() -> {
                for (String c : st.commands()) {
                    try {
                        s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput()
                                .withLevel(s.getLevel(Dims.MAZE) == null ? s.overworld() : s.getLevel(Dims.MAZE)), c);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] command failed {}", c, t);
                    }
                }
            });
            if (st.screen() != null) mc.setScreen(st.screen().get());
        }
        mc.options.hideGui = !st.hud();
        int elapsed = tick - stepStart;
        boolean done = st.until() != null ? (st.until().getAsBoolean() || elapsed >= st.delay()) : elapsed >= st.delay();
        if (!done) return;
        if (st.until() != null && !st.until().getAsBoolean()) LOG.warn("[AUTOTEST] condition timed out at step {}", index);
        if (st.shot() != null) {
            if (st.shot().equals("END")) {
                LOG.info("[AUTOTEST] finished");
                mc.stop();
                return;
            }
            String name = "ltest_" + String.format("%02d_", index) + st.shot() + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[AUTOTEST] screenshot {}", name));
        }
        if (st.screen() != null) mc.setScreen(null);
        index++;
        stepStart = -1;
    }
}
