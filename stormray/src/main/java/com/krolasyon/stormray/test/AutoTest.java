package com.krolasyon.stormray.test;

import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import com.krolasyon.stormray.entity.StormRayEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Husk;
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

/** CI-only (-Dstormray.autotest=true): shows the blade in a frame, in hand, charging, the spike wave and a root snare. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, String[] commands, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private boolean gui;

    public static void init() {
        LOG.info("[RAYTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static StormRayEntity ray(MinecraftServer s, String tag) {
        for (StormRayEntity r : s.overworld().getEntitiesOfClass(StormRayEntity.class, new AABB(-200, -64, -200, 200, 200, 200))) {
            if (r.getTags().contains(tag)) return r;
        }
        return null;
    }

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "difficulty normal",
                "summon stormray:storm_ray 0.5 -55 -12 {NoAI:1b,Rotation:[0f,0f],Tags:[\"show\"],PersistenceRequired:1b}");
        cmd(60, "front", "tp @a 0.5 -55.3 -6.5 180 8");
        wait(3, "front_b");
        wait(3, "front_c");
        wait(3, "front_d");
        cmd(20, "side", "tp @a 7.5 -55 -12.5 90 5");
        cmd(20, "three_quarter", "tp @a 5 -53 -7 135 22");
        wait(4, "three_quarter_b");
        cmd(20, "top", "tp @a 0.5 -49 -12 0 85");
        cmd(20, "below", "tp @a 3 -59 -9 150 -40");
        cmd(30, "night", "time set 18000", "tp @a 5 -53 -7 135 22");
        wait(5, "night_b");
        // evcilleştir, eyerle, bin, uç
        server(5, null, s -> {
            StormRayEntity r = ray(s, "show");
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            r.tame(p);
            r.setSaddled(true);
            r.setNoAi(false);
            p.teleportTo(r.getX(), r.getY() + 1, r.getZ());
            p.startRiding(r, true);
        });
        cmd(20, "saddled", "time set 6000");
        client(5, null, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            mc.player.setXRot(-10);
            net.minecraft.client.KeyMapping.set(mc.options.keyUp.getKey(), true);
        });
        wait(30, "riding_back");
        wait(20, "riding_back_b");
        client(3, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(5, "riding_front");
        client(3, null, mc -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            net.minecraft.client.KeyMapping.set(mc.options.keyUp.getKey(), false);
        });
        server(5, null, s -> {
            StormRayEntity r = ray(s, "show");
            LOG.info("[RAYTEST] ridden ray pos {} passengers {}", r.position(), r.getPassengers().size());
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
                    String.format("summon husk %.1f %.1f %.1f {NoAI:1b,Health:60f,Attributes:[{Name:\"generic.max_health\",Base:60d}]}", r.getX() + 4, r.getY() - 2, r.getZ() + 6));
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
                    String.format("summon husk %.1f %.1f %.1f {NoAI:1b,NoGravity:1b}", r.getX() - 5, r.getY() - 1, r.getZ() + 7));
        });
        wait(15, "zap_a");
        wait(6, "zap_b");
        wait(40, "zap_c");
        server(5, null, s -> {
            for (Husk h : s.overworld().getEntitiesOfClass(Husk.class, new AABB(-200, -64, -200, 200, 200, 200))) {
                LOG.info("[RAYTEST] husk health {}", h.getHealth());
            }
            StormRayEntity r = ray(s, "show");
            r.ejectPassengers();
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(),
                    "summon stormray:storm_ray 10 -50 10 {Tags:[\"wild\"]}");
        });
        cmd(10, null, "tp @a 0.5 -55 25 180 -10");
        server(100, null, s -> LOG.info("[RAYTEST] wild ray pos {}", ray(s, "wild").position()));
        server(100, null, s -> LOG.info("[RAYTEST] wild ray pos {}", ray(s, "wild").position()));
        wait(5, "wild_flying");
        wait(20, "END");
    }

    private void cmd(int wait, String shot, String... c) { steps.add(new Step(wait, shot, c, null, null)); }
    private void server(int wait, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(wait, shot, new String[0], a, null)); }
    private void client(int wait, String shot, Consumer<Minecraft> a) { steps.add(new Step(wait, shot, new String[0], null, a)); }
    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, new String[0], null, null)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[RAYTEST] creating world");
            LevelSettings settings = new LevelSettings("raytest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("raytest", settings, new WorldOptions(9L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[RAYTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[RAYTEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    for (String c : st.commands()) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
                    if (st.server() != null) st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[RAYTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[RAYTEST] finished");
                    mc.stop();
                    return;
                }
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    int spikes = s.overworld().getEntitiesOfClass(StormRayEntity.class, new AABB(-200, -64, -200, 200, 200, 200)).size();
                    int snares = s.getPlayerList().getPlayers().get(0).isPassenger() ? 1 : 0;
                    List<LivingEntity> husks = new ArrayList<>(s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -20, 20, -40, 20)));
                    LOG.info("[RAYTEST] state {} rays={} riding={} husks={} playerHp={}", st.shot(), spikes, snares, husks.size(),
                            s.getPlayerList().getPlayers().get(0).getHealth());
                });
                String name = "ray_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[RAYTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
