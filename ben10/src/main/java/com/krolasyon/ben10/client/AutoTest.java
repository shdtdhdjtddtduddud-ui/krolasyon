package com.krolasyon.ben10.client;

import com.krolasyon.ben10.Alien;
import com.krolasyon.ben10.OmniData;
import com.krolasyon.ben10.Transform;
import com.krolasyon.ben10.ability.Powers;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
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

/** CI-only visual self test (-Dkrolben10.autotest=true): flat world, every alien from the front, every ability from the side, screenshots. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private int tick;
    private boolean worldRequested;
    private int inWorld = -1;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;
    private int stepStart;
    private static int camMode; // 0 front third person, 1 side camera entity
    private static Entity sideCam;

    record Step(int delay, String shot, Consumer<MinecraftServer> server, Runnable client) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static ServerPlayer sp(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static final String TARGETS = "summon minecraft:husk 0 -60 7 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Health:1000f,Attributes:[{Name:\"generic.max_health\",Base:1000d}],Rotation:[180f,0f]}";

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty normal", "gamemode creative @a", "tp @a 0 -60 0 0 0",
                "kill @e[type=!player]", "give @a krolben10:omnitrix", "summon minecraft:armor_stand -8 -59.4 4 {Invisible:1b,NoGravity:1b,Marker:1b,Tags:[\"cam\"],Rotation:[-90f,8f]}",
                "fill -6 -61 10 6 -61 14 minecraft:sand", "fill 3 -60 4 5 -58 4 minecraft:glass", "fill -5 -60 9 -2 -59 9 minecraft:water");
        client(10, "human_front", () -> cam(0));
        for (Alien a : Alien.values()) {
            server(4, null, s -> {
                ServerPlayer p = sp(s);
                p.teleportTo(0.5, -60, 0.5);
                p.setYRot(0);
                p.setXRot(0);
                p.getAbilities().flying = false;
                Transform.transform(p, a);
            });
            client(2, null, () -> cam(0));
            wait(14, a.id + "_front");
            server(1, null, s -> sp(s).setYRot(150));
            wait(6, a.id + "_back34");
            server(1, null, s -> sp(s).setYRot(0));
            client(2, null, () -> cam(1));
            cmd(2, null, "kill @e[type=minecraft:husk]", TARGETS);
            wait(4, a.id + "_side");
            for (int i = 0; i < 4; i++) {
                final int slot = i;
                int dur = a.durations[i];
                server(1, null, s -> {
                    ServerPlayer p = sp(s);
                    OmniData d = OmniData.get(p);
                    d.cooldown[slot] = 0;
                    d.sinceTransform = 100;
                    p.setYRot(0);
                    p.setXRot(5);
                    Powers.use(p, slot);
                    LOG.info("[AUTOTEST] {} ability {} started={}", a.id, slot, d.ability);
                });
                int mid = Math.max(3, Math.min(dur - 1, dur / 2));
                wait(mid, a.id + "_ab" + (i + 1) + "a");
                wait(Math.max(2, Math.min(12, dur - mid)), a.id + "_ab" + (i + 1) + "b");
                server(Math.max(2, dur - mid - 10) + 6, null, s -> {});
                server(1, null, s -> {
                    ServerPlayer p = sp(s);
                    OmniData d = OmniData.get(p);
                    if (d.ability >= 0) Powers.finish(p, d);
                    p.teleportTo(0.5, -60, 0.5);
                    p.setDeltaMovement(0, 0, 0);
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                });
                wait(6, null);
            }
            server(1, null, s -> Powers.guard(sp(s), true));
            wait(10, a.id + "_guard");
            server(1, null, s -> Powers.guard(sp(s), false));
            client(1, null, () -> cam(0));
            server(1, null, s -> Powers.guard(sp(s), true));
            wait(10, a.id + "_guardfront");
            server(1, null, s -> Powers.guard(sp(s), false));
            wait(4, null);
        }
        server(2, null, s -> Transform.detransform(sp(s), true));
        wait(6, "timeout");
        client(2, "dial", () -> {
            cam(0);
            Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().setScreen(new OmnitrixScreen());
        });
        client(10, "dial2", () -> {});
        server(2, null, s -> Transform.transform(sp(s), Alien.FOURARMS));
        client(10, "hud", () -> {
            Minecraft.getInstance().setScreen(null);
            Minecraft.getInstance().options.hideGui = false;
            Minecraft.getInstance().options.setCameraType(CameraType.FIRST_PERSON);
        });
        wait(10, "END");
    }

    private static void cam(int mode) {
        Minecraft mc = Minecraft.getInstance();
        camMode = mode;
        if (mode == 0) {
            mc.setCameraEntity(mc.player);
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        } else {
            if (mc.level != null) {
                for (Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) sideCam = e;
                }
            }
            if (sideCam != null) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.setCameraEntity(sideCam);
            }
        }
    }

    private void cmd(int wait, String shot, String... commands) {
        steps.add(new Step(wait, shot, s -> {
            for (String c : commands) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
        }, null));
    }

    private void wait(int ticks, String shot) {
        steps.add(new Step(ticks, shot, null, null));
    }

    private void server(int wait, String shot, Consumer<MinecraftServer> action) {
        steps.add(new Step(wait, shot, action, null));
    }

    private void client(int wait, String shot, Runnable r) {
        steps.add(new Step(wait, shot, null, r));
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
            LevelSettings settings = new LevelSettings("otest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("otest", settings, new WorldOptions(42L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !(mc.screen instanceof OmnitrixScreen)) mc.setScreen(null);
        if (!(mc.screen instanceof OmnitrixScreen) && stepIndex < steps.size() - 2) mc.options.hideGui = true;
        if (inWorld < 0) {
            inWorld = tick;
            stepStart = tick + 100;
            LOG.info("[AUTOTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.server() != null) {
                MinecraftServer s = mc.getSingleplayerServer();
                s.execute(() -> {
                    try {
                        st.server().accept(s);
                    } catch (Throwable t) {
                        LOG.error("[AUTOTEST] step failed", t);
                    }
                });
            }
            if (st.client() != null) {
                try {
                    st.client().run();
                } catch (Throwable t) {
                    LOG.error("[AUTOTEST] client step failed", t);
                }
            }
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[AUTOTEST] finished");
                    mc.stop();
                    return;
                }
                OmniData d = OmniData.get(mc.player);
                LOG.info("[AUTOTEST] shot {} alien={} ability={} t={} pos={}", st.shot(), d.alien, d.ability, d.abilityTick, mc.player.position());
                Screenshot.grab(mc.gameDirectory, "otest_" + String.format("%03d_", stepIndex) + st.shot() + ".png", mc.getMainRenderTarget(),
                        msg -> {});
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
