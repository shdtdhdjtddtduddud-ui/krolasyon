package com.krolasyon.skyislands.test;

import com.krolasyon.skyislands.item.IslandSeedItem;
import com.krolasyon.skyislands.world.Island;
import com.krolasyon.skyislands.world.IslandLayout;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
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
import java.util.function.Function;

/** CI-only (-Dskyislands.autotest=true): flies to the nearest big island, screenshots it from several angles, quits. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final long SEED = 20261019L;

    private record Step(int delay, String shot, Function<Island, String[]> commands) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private Island island;

    public static void init() {
        LOG.info("[SKYTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        step(0, null, is -> new String[]{"gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 5000", "weather clear", "gamemode spectator @a"});
        step(500, "wide", is -> tp(is.cx() - is.radius() - 45, is.baseY() + 14, is.cz() - 20, -65, 12));
        step(300, "front", is -> tp(is.cx() - is.radius() - 22, is.baseY() + 4, is.cz(), -90, 8));
        step(250, "below", is -> tp(is.cx() - is.radius() - 18, is.baseY() - is.depth() - 20, is.cz(), -90, -38));
        step(250, "top", is -> tp(is.cx(), is.baseY() + 45, is.cz() - is.radius() - 25, 0, 45));
        step(200, "on_island", is -> tp(is.cx() - is.radius() / 2, is.baseY() + 4, is.cz() + is.radius() / 2, -135, 0));
        step(200, "sunset", is -> new String[]{"time set 12300", tp(is.cx() - is.radius() - 45, is.baseY() + 14, is.cz() - 20, -65, 12)[0]});
        step(200, "night", is -> new String[]{"time set 18000", tp(is.cx() - is.radius() - 22, is.baseY() + 4, is.cz(), -90, 8)[0]});
        step(200, "ground_view", is -> new String[]{"time set 5000", tp(is.cx() - is.radius() - 60, 95, is.cz() - 10, -70, -35)[0]});
        step(5, null, is -> new String[]{"time set 5000", tp(is.cx() - is.radius() - 120, is.baseY(), is.cz() + 80, 180, 0)[0]});
        steps.add(new Step(200, "island_seed", null));
        step(20, "END", is -> new String[0]);
    }

    private static String[] tp(int x, int y, int z, float yaw, float pitch) {
        return new String[]{"tp @a " + x + " " + y + " " + z + " " + yaw + " " + pitch};
    }

    private void step(int delay, String shot, Function<Island, String[]> cmds) {
        steps.add(new Step(delay, shot, cmds));
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
            LOG.info("[SKYTEST] creating world");
            LevelSettings settings = new LevelSettings("skytest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("skytest", settings, new WorldOptions(SEED, true, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = true;
        MinecraftServer server = mc.getSingleplayerServer();
        if (island == null) {
            island = IslandLayout.nearestMain(server.overworld().getSeed(), 0, 0, 18, 8);
            LOG.info("[SKYTEST] target island {}", island);
            stepStart = tick + 60;
            if (island == null) {
                mc.stop();
                return;
            }
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            Island is = island;
            server.execute(() -> {
                try {
                    if (st.commands() != null) {
                        for (String c : st.commands().apply(is)) {
                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c);
                        }
                    } else {
                        var p = server.getPlayerList().getPlayers().get(0);
                        IslandSeedItem.spawn(server.overworld(), (int) p.getX(), (int) p.getY() + 2, (int) p.getZ() - 24, 9, 77L);
                    }
                } catch (Throwable t) {
                    LOG.error("[SKYTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[SKYTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "sky_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[SKYTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
