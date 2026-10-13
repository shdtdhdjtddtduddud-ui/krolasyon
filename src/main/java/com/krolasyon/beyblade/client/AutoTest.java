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

    record Step(int delay, String shot, Consumer<MinecraftServer> action, Boolean gui) {}

    public static void init() {
        LOG.info("[AUTOTEST] enabled");
        BeybladeEntity.rivalAbilities = false;
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static final String A = "summon librabeyblade:ldrago 0 -60 -6 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"a\"]}";
    private static final String B = "summon librabeyblade:libra 3.5 -60 -9 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"b\"]}";
    private static final String LA = "summon librabeyblade:libra 0 -60 -6 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"a\"]}";
    private static final String LB = "summon librabeyblade:ldrago 3.5 -60 -9 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"b\"]}";

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "difficulty peaceful", "gamemode creative @a", "kill @e[type=!player]",
                "fill -2 -60 -2 2 -60 2 minecraft:smooth_stone");
        // ---- L-Drago model close-ups on a small pedestal
        cmd(4, null, "summon librabeyblade:ldrago 0 -59 0 {Spin:3000f,MaxSpin:3000f,Frozen:1b}", "tp @a 0.5 -59 1.6 180 42");
        wait(30, "ldrago_close");
        cmd(10, "ldrago_side", "tp @a 0.5 -59 2.6 180 24");
        cmd(10, "ldrago_far", "tp @a 2.2 -59 2.2 135 30");
        // ---- L-Drago vs Libra (both rivals, free battle; left vs right spin)
        cmd(10, null, "kill @e[type=!player]", "fill -2 -60 -2 2 -60 2 minecraft:air", "tp @a 0.5 -60 1 180 28",
                "summon librabeyblade:ldrago -2.5 -60 -6 {Spin:3000f,MaxSpin:3000f}", "summon librabeyblade:libra 2.5 -60 -6 {Spin:3000f,MaxSpin:3000f}");
        wait(28, "battle_a");
        wait(14, "battle_b");
        wait(24, "battle_c");
        // ---- L-Drago abilities on a Libra target
        cmd(10, null, "kill @e[type=!player]", "tp @a 1.5 -60 2 180 18", A, B);
        wait(20, null);
        force(1);
        wait(8, "awaken_burst");
        wait(20, "awaken_aura");
        wait(10, null);
        cmd(4, null, "tp @a 1.5 -60 4 180 25", "kill @e[type=librabeyblade:ldrago]", "kill @e[type=librabeyblade:libra]", A, B,
                "summon librabeyblade:libra -3 -60 -4 {Spin:3000f,MaxSpin:3000f,Frozen:1b}");
        wait(15, null);
        force(2);
        wait(20, "vortex_a");
        wait(30, "vortex_b");
        wait(60, null);
        cmd(4, null, "tp @a 1.5 -60 6 180 -25", "kill @e[type=librabeyblade:ldrago]", "kill @e[type=librabeyblade:libra]", A, B);
        wait(15, null);
        force(3);
        wait(20, "meteor_rise");
        wait(16, "meteor_sky");
        cmd(1, null, "tp @a 1.5 -60 6 180 10");
        wait(12, "meteor_dive");
        wait(5, "meteor_impact");
        wait(8, "meteor_impact_b");
        wait(30, null);
        cmd(4, null, "tp @a 1.5 -60 2.5 180 15", "kill @e[type=librabeyblade:ldrago]", "kill @e[type=librabeyblade:libra]", A, B);
        wait(15, null);
        force(4);
        wait(7, "claw_a");
        wait(16, "claw_b");
        wait(28, "claw_upper");
        wait(30, null);
        cmd(4, null, "tp @a 1.5 -60 4 180 5", "kill @e[type=librabeyblade:ldrago]", "kill @e[type=librabeyblade:libra]", A, B);
        wait(15, null);
        force(5);
        wait(14, "roar_charge");
        wait(16, "roar_breath");
        wait(16, "roar_breath_b");
        wait(40, null);
        // ---- regression: a couple of Libra moves still work after the refactor
        cmd(4, null, "tp @a 1.5 -60 2 180 18", "kill @e[type=librabeyblade:ldrago]", "kill @e[type=librabeyblade:libra]", LA, LB);
        wait(15, null);
        force(1);
        wait(14, "libra_sonic_wave");
        wait(40, null);
        force(2);
        wait(10, "libra_shield");
        wait(20, null);
        // ---- owned L-Drago + HUD, recall
        cmd(4, null, "kill @e[type=!player]", "tp @a 0.5 -60 3 180 20");
        server(30, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            BeybladeEntity.launch(com.krolasyon.beyblade.registry.ModEntities.LDRAGO.get(), p.level(), p, new ItemStack(ModItems.LDRAGO.get()),
                    p.getEyePosition().add(0, -0.5, -1), new Vec3(0, 0.1, -0.6), 1F);
        });
        steps.add(new Step(2, "owned_hud", null, true));
        cmd(10, null, "summon librabeyblade:libra 1 -60 -7 {Spin:3000f,MaxSpin:3000f,Frozen:1b,Tags:[\"b\"]}");
        wait(40, "owned_battle");
        server(4, null, s -> each(s, null, b -> { if (!b.isRival()) b.debugForceAbility(1); }));
        wait(14, "owned_ability");
        wait(30, null);
        server(6, "recall", s -> each(s, null, b -> { if (!b.isRival()) b.recall(); }));
        wait(30, "recalled");
        cmd(4, null, "item replace entity @a weapon.mainhand with librabeyblade:ldrago_destruction", "item replace entity @a hotbar.1 with librabeyblade:ldrago_launcher",
                "item replace entity @a hotbar.2 with librabeyblade:rival_ldrago", "item replace entity @a hotbar.3 with librabeyblade:flame_libra", "tp @a 0.5 -60 3 180 20");
        wait(20, "item_hand");
        cmd(4, null, "item replace entity @a weapon.mainhand with librabeyblade:ldrago_launcher");
        wait(10, "launcher_hand");
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
        if (mc.screen != null) mc.setScreen(null);
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
