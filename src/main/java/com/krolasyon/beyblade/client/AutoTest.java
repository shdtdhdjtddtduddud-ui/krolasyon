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

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty peaceful", "gamemode spectator @a", "kill @e[type=!player]");
        // ---- Leone's tornadoes circling Leone itself
        cmd(10, null, "tp @a 1.5 -52 12 180 18", A, B, "summon librabeyblade:common -3 -60 -8 {Variant:2,Spin:3000f,MaxSpin:3000f,Frozen:1b}");
        wait(15, null);
        force(5);
        wait(25, "tornado_a");
        wait(25, "tornado_b");
        wait(30, "tornado_c");
        wait(50, null);
        // ---- stadium: shock wave, Blader Spirit, fast circling
        cmd(10, null, "kill @e[type=!player]", "summon librabeyblade:stadium 0.5 -60 -14.5", "tp @a 0.5 -55.5 -6 180 35",
                "summon librabeyblade:pegasus -1 -59 -13.5 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"s1\"]}",
                "summon librabeyblade:common 0.2 -59 -13.5 {Variant:0,Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"s2\"]}");
        wait(30, null);
        server(1, null, s -> each(s, "s2", b -> each(s, "s1", o -> b.shockwave(o, 1.0))));
        wait(3, "shock_a");
        wait(4, "shock_b");
        server(1, null, s -> each(s, "s1", b -> { b.addSpirit(100); b.releaseSpirit(); }));
        wait(6, "spirit_a");
        wait(30, "spirit_b");
        cmd(4, null, "kill @e[type=librabeyblade:pegasus]", "kill @e[type=librabeyblade:common]", "gamemode creative @a", "tp @a 0.5 -60 2.7 180 25");
        server(4, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            BeybladeEntity b = BeybladeEntity.launch(com.krolasyon.beyblade.registry.ModEntities.PEGASUS.get(), p.level(), p, new ItemStack(ModItems.PEGASUS.get()),
                    p.getEyePosition().add(0, 0.5, -1), new Vec3(0, 0.45, -0.5), 1F);
            b.setOrder(BeybladeEntity.OR_CIRCLE);
        });
        steps.add(new Step(60, null, null, true));
        cmd(4, null, "tp @a 0.5 -44 -2 180 60");
        wait(20, "circle_a");
        wait(5, "circle_b");
        wait(5, "circle_c");
        steps.add(new Step(2, null, null, false));
        wait(20, "END");
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
