package com.krolasyon.aethercore.test;

import com.krolasyon.aethercore.AetherCore;
import com.krolasyon.aethercore.block.AetherCoreBlockEntity;
import com.krolasyon.aethercore.world.ShrineFeature;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
import java.util.function.Consumer;

/** CI-only (-Daethercore.autotest=true): builds a shrine, activates the core, screenshots the animation and shield. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, String[] commands, Consumer<MinecraftServer> action) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private boolean showGui;

    public static void init() {
        LOG.info("[CORETEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final BlockPos SHRINE = new BlockPos(0, -60, -10);
    private static final BlockPos CORE = SHRINE.above();

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "difficulty normal");
        act(10, null, s -> ShrineFeature.build(s.overworld(), SHRINE, s.overworld().random, false));
        cmd(80, "dormant_close", "tp @a 0.5 -58.6 -7.2 180 12");
        cmd(40, "shrine_wide", "tp @a 7 -56.5 -2 135 25");
        act(5, null, s -> core(s).addEnergy(20 * 60 * 10));
        act(5, null, s -> core(s).setActive(true));
        cmd(30, "opening", "tp @a 0.5 -58.6 -7.4 180 10");
        wait(60, "active_close");
        wait(13, "active_close_b");
        cmd(40, "active_side", "tp @a -2.2 -58.9 -9.2 -110 5");
        cmd(60, "dome_outside", "tp @a 24 -52 14 135 15");
        cmd(20, null, "summon minecraft:zombie 6 -60 -10", "summon minecraft:skeleton -12 -60 -16", "summon minecraft:zombie -5 -60 -4",
                "summon minecraft:skeleton 20 -60 -10", "summon minecraft:creeper 4 -60 -16");
        cmd(80, "mobs_pushed", "tp @a 0 -55 8 180 25");
        wait(60, "mobs_pushed_b");
        cmd(40, "dome_inside", "tp @a 3 -59 -6 150 -25");
        cmd(40, "night", "time set 18000", "tp @a 0.5 -58.5 -6.8 180 10");
        act(5, null, s -> {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), "give @a aethercore:aether_core 1");
            showGui = true;
        });
        cmd(40, "item_in_hand", "time set 6000", "tp @a 0.5 -58 4 180 0");
        act(5, null, s -> { showGui = false; core(s).setActive(false); });
        cmd(90, "closing", "tp @a 0.5 -58.6 -7.4 180 10");
        wait(20, "END");
    }

    private static AetherCoreBlockEntity core(MinecraftServer s) {
        ServerLevel l = s.overworld();
        if (l.getBlockEntity(CORE) instanceof AetherCoreBlockEntity be) return be;
        l.setBlock(CORE, AetherCore.AETHER_CORE.get().defaultBlockState(), 3);
        return (AetherCoreBlockEntity) l.getBlockEntity(CORE);
    }

    private void cmd(int wait, String shot, String... commands) { steps.add(new Step(wait, shot, commands, null)); }

    private void act(int wait, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(wait, shot, new String[0], a)); }

    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, new String[0], null)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[CORETEST] creating world");
            LevelSettings settings = new LevelSettings("coretest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("coretest", settings, new WorldOptions(5L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !showGui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[CORETEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    for (String c : st.commands()) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
                    if (st.action() != null) st.action().accept(s);
                } catch (Throwable t) {
                    LOG.error("[CORETEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[CORETEST] finished");
                    mc.stop();
                    return;
                }
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    if (s.overworld().getBlockEntity(CORE) instanceof AetherCoreBlockEntity be) {
                        LOG.info("[CORETEST] state {} active={} energy={} mobs={}", st.shot(), be.isActive(), be.getEnergy(),
                                s.overworld().getEntities(net.minecraft.world.entity.EntityType.ZOMBIE, e -> true).size());
                    }
                });
                String name = "core_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[CORETEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
