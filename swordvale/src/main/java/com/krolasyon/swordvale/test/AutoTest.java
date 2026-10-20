package com.krolasyon.swordvale.test;

import com.krolasyon.swordvale.SwordVale;
import com.krolasyon.swordvale.item.ValeCompassItem;
import com.krolasyon.swordvale.world.SwordShape;
import com.krolasyon.swordvale.world.ValeLayout;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only (-Dswordvale.autotest=true): finds a Sword Vale, screenshots it from the air, the ground, a giant sword and its runes. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final long SEED = 4242L;

    private record Step(int delay, String shot, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested;
    private int stepIndex;
    private int stepStart = -1;
    private boolean gui;
    private ValeLayout.Vale vale;
    private SwordShape giant;

    public static void init() {
        LOG.info("[VALETEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private AutoTest() {
        server(0, null, s -> {
            run(s, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 5000",
                    "weather clear", "gamemode spectator @a");
            ServerLevel l = s.overworld();
            vale = ValeLayout.nearest(l.getSeed(), l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), 0, 0, 4);
            LOG.info("[VALETEST] nearest vale {}", vale);
            if (vale == null) return;
            float best = 0;
            for (ValeLayout.Sword sw : ValeLayout.swords(vale)) {
                if (sw.length() <= best || !ValeLayout.biomeOk(l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), sw.x(), sw.z())) continue;
                int g = ValeLayout.ground(l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l, sw.x(), sw.z());
                if (g <= l.getSeaLevel() + 1) continue;
                best = sw.length();
                giant = new SwordShape(sw, g);
            }
            LOG.info("[VALETEST] giant sword {}", giant == null ? null : giant.s);
            tp(s, vale.cx(), groundAt(l, vale.cx(), vale.cz()) + 45, vale.cz(), 180, 25);
        });
        wait(400, "overview_n");
        server(5, null, s -> tp(s, vale.cx(), groundAt(s.overworld(), vale.cx(), vale.cz()) + 45, vale.cz(), -90, 22));
        wait(220, "overview_e");
        server(5, null, s -> tp(s, vale.cx() - vale.radius() * 0.6, groundAt(s.overworld(), vale.cx(), vale.cz()) + 70, vale.cz() - vale.radius() * 0.6, -45, 30));
        wait(250, "overview_high");
        server(5, null, s -> {
            double[] base = {giant.baseX(), giant.baseZ()};
            double d = giant.s.length() * 0.85;
            double ang = giant.s.leanDir() + Math.PI / 2;
            double x = base[0] + Math.cos(ang) * d, z = base[1] + Math.sin(ang) * d;
            float yaw = (float) Math.toDegrees(Math.atan2(base[1] - z, base[0] - x)) - 90F;
            tp(s, x, giant.groundY() + 6, z, yaw, -28);
        });
        wait(220, "giant_close");
        server(5, null, s -> {
            double[] p = giant.axisPoint(giant.s.length() * 0.35);
            double ang = giant.s.twist() + Math.PI / 2;
            double x = p[0] + Math.cos(ang) * 7, z = p[2] + Math.sin(ang) * 7;
            float yaw = (float) Math.toDegrees(Math.atan2(p[2] - z, p[0] - x)) - 90F;
            tp(s, x, p[1] - 1.5, z, yaw, 0);
        });
        wait(80, "runes_day");
        server(5, null, s -> run(s, "time set 18000"));
        wait(30, "runes_night");
        server(5, null, s -> {
            run(s, "time set 5000");
            tp(s, vale.cx() + 6, groundAt(s.overworld(), vale.cx() + 6, vale.cz() + 6) + 1.6, vale.cz() + 6, 150, -8);
        });
        wait(200, "ground_view");
        // vadiye giriş başlığı
        server(5, null, s -> tp(s, vale.cx() + vale.radius() + 60, groundAt(s.overworld(), vale.cx(), vale.cz()) + 30, vale.cz(), 90, 15));
        client(60, null, mc -> gui = true);
        server(45, null, s -> tp(s, vale.cx() + vale.radius() * 0.7, groundAt(s.overworld(), vale.cx(), vale.cz()) + 30, vale.cz(), 90, 15));
        wait(5, "entry_title");
        client(2, null, mc -> gui = false);
        server(5, null, s -> {
            ServerPlayer p = s.getPlayerList().getPlayers().get(0);
            ItemStack c = new ItemStack(SwordVale.VALE_COMPASS.get());
            ValeCompassItem.search(s.overworld(), c, p);
            LOG.info("[VALETEST] compass tag {}", c.getTag());
            int graves = 0, chests = 0;
            for (ValeLayout.Sword sw : ValeLayout.swords(vale)) {
                if (!sw.grave()) continue;
                graves++;
                int g = ValeLayout.ground(s.overworld().getChunkSource().getGenerator(), s.overworld().getChunkSource().randomState(), s.overworld(), sw.x(), sw.z());
                BlockPos gp = new SwordShape(sw, g).gravePos();
                if (!s.overworld().hasChunkAt(gp)) continue;
                for (int dy = -4; dy <= 2; dy++) {
                    if (s.overworld().getBlockState(gp.offset(0, dy, 0)).is(net.minecraft.world.level.block.Blocks.CHEST)) chests++;
                }
            }
            LOG.info("[VALETEST] graves {} loaded chests found {}", graves, chests);
            int runes = 0;
            if (giant != null) {
                int[] bb = giant.bounds();
                for (int x = bb[0]; x <= bb[3]; x++) for (int y = bb[1]; y <= bb[4]; y++) for (int z = bb[2]; z <= bb[5]; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (s.overworld().hasChunkAt(bp) && s.overworld().getBlockState(bp).is(SwordVale.RUNE_STONE.get())) runes++;
                }
            }
            LOG.info("[VALETEST] rune blocks in giant sword {}", runes);
        });
        wait(20, "END");
    }

    private static int groundAt(ServerLevel l, int x, int z) {
        return ValeLayout.ground(l.getChunkSource().getGenerator(), l.getChunkSource().randomState(), l, x, z);
    }

    private static void run(MinecraftServer s, String... cmds) {
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }

    private static void tp(MinecraftServer s, double x, double y, double z, float yaw, float pitch) {
        run(s, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
    }

    private void server(int wait, String shot, Consumer<MinecraftServer> a) { steps.add(new Step(wait, shot, a, null)); }
    private void client(int wait, String shot, Consumer<Minecraft> a) { steps.add(new Step(wait, shot, null, a)); }
    private void wait(int ticks, String shot) { steps.add(new Step(ticks, shot, null, null)); }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (mc.getOverlay() != null) return;
        if (!worldRequested && tick > 40 && mc.level == null) {
            worldRequested = true;
            LOG.info("[VALETEST] creating world");
            LevelSettings settings = new LevelSettings("valetest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("valetest", settings, new WorldOptions(SEED, true, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[VALETEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            if (st.client() != null) st.client().accept(mc);
            MinecraftServer s = mc.getSingleplayerServer();
            if (st.server() != null) s.execute(() -> {
                try {
                    if (vale == null && st != steps.get(0)) return;
                    st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[VALETEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[VALETEST] finished");
                    mc.stop();
                    return;
                }
                String name = "vale_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[VALETEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
