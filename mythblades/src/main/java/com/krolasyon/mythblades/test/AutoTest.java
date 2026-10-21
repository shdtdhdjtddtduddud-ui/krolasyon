package com.krolasyon.mythblades.test;

import com.krolasyon.mythblades.MythBlades;
import com.krolasyon.mythblades.item.CrimsonVeinItem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** CI-only (-Dmythblades.autotest=true): üç kılıcı çerçevede, envanterde, elde ve yetenekleriyle görüntüler. */
public final class AutoTest {
    private static final Logger LOG = LogUtils.getLogger();

    private record Step(int delay, String shot, String[] commands, Consumer<MinecraftServer> server, Consumer<Minecraft> client) {}

    private final List<Step> steps = new ArrayList<>();
    private int tick;
    private boolean worldRequested, gui, allowScreen;
    private int stepIndex;
    private int stepStart = -1;

    public static void init() {
        LOG.info("[MYTHTEST] enabled");
        MinecraftForge.EVENT_BUS.register(new AutoTest());
    }

    private static ServerPlayer player(MinecraftServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static final String[] IDS = {"mythblades:rainbow_scimitar", "mythblades:crimson_vein", "mythblades:mossroot_cleaver"};

    private AutoTest() {
        cmd(0, null, "gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                "time set 6000", "weather clear", "gamemode creative @a", "difficulty normal",
                "fill -6 -61 -7 6 -54 -7 minecraft:polished_deepslate",
                "summon item_frame -3 -57 -6 {Facing:3b,Fixed:1b,Invisible:1b,Item:{id:\"mythblades:rainbow_scimitar\",Count:1b}}",
                "summon item_frame 0 -57 -6 {Facing:3b,Fixed:1b,Invisible:1b,Item:{id:\"mythblades:crimson_vein\",Count:1b}}",
                "summon item_frame 3 -57 -6 {Facing:3b,Fixed:1b,Invisible:1b,Item:{id:\"mythblades:mossroot_cleaver\",Count:1b}}");
        cmd(80, "frames", "tp @a 0.5 -57 -1.2 180 0");
        cmd(25, "frame_rainbow", "tp @a -2.5 -57 -4.4 180 0");
        cmd(25, "frame_crimson", "tp @a 0.5 -57 -4.4 180 0");
        cmd(25, "frame_mossroot", "tp @a 3.5 -57 -4.4 180 0");
        cmd(25, "frames_night", "time set 18000", "tp @a 0.5 -57 -1.2 180 0");
        cmd(5, null, "time set 6000");
        // envanter görünümü
        client(5, null, mc -> {
            allowScreen = true;
            gui = true;
            List<net.minecraft.world.item.Item> items = List.of(MythBlades.RAINBOW.get(), MythBlades.CRIMSON.get(), MythBlades.MOSSROOT.get());
            mc.setScreen(new GalleryScreen(items, 3, 6F));
        });
        wait(20, "gui_gallery");
        client(5, null, mc -> {
            allowScreen = false;
            mc.setScreen(null);
        });
        // elde: her kılıç için birinci ve üçüncü şahıs
        for (int n = 0; n < 3; n++) {
            final int nn = n;
            server(5, null, s -> {
                ServerPlayer p = player(s);
                p.getInventory().clearContent();
                p.getInventory().setItem(0, item(IDS[nn]));
                p.teleportTo(s.overworld(), 0.5, -60, 6.5, 180, 0);
            });
            client(5, null, mc -> {
                mc.player.getInventory().selected = 0;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            });
            wait(25, "hand_" + nn);
            client(5, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            wait(25, "third_front_" + nn);
            client(5, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
            wait(20, "third_back_" + nn);
            client(2, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        }

        // ---------------------------------------------------- gökkuşağı hilali
        cmd(5, null, "gamemode survival @a", "effect give @a minecraft:resistance infinite 4 true", "kill @e[type=husk]",
                "summon husk 0.5 -60 -4 {NoAI:1b,Health:60f,Attributes:[{Name:\"generic.max_health\",Base:60d}]}",
                "summon husk -1.5 -60 -9 {NoAI:1b,Health:60f,Attributes:[{Name:\"generic.max_health\",Base:60d}]}",
                "summon husk 2.5 -60 -13 {NoAI:1b,Health:60f,Attributes:[{Name:\"generic.max_health\",Base:60d}]}");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, item(IDS[0]));
            p.teleportTo(s.overworld(), 0.5, -60, 6.5, 180, 0);
        });
        client(20, null, mc -> mc.player.getInventory().selected = 0);
        client(1, null, mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        wait(3, "wave_fire");
        wait(4, "wave_fly");
        cmd(1, null, "tp @a 8.5 -59 -3 90 10");
        client(1, null, mc -> {});
        server(1, null, s -> {});
        client(26, null, mc -> {
            mc.player.setYRot(180);
            KeyMapping.click(mc.options.keyUse.getKey());
        });
        cmd(2, null, "tp @a 0.5 -60 6.5 180 0");
        client(1, null, mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        cmd(6, "wave_side", "tp @a 9.5 -58 -2 90 12");
        server(5, null, s -> {
            for (Husk h : s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -20, 20, -40, 20))) {
                LOG.info("[MYTHTEST] husk after waves hp {} effects {}", h.getHealth(), h.getActiveEffects().size());
            }
        });
        // bulut basamağı
        cmd(5, null, "tp @a 0.5 -60 6.5 180 -10", "kill @e[type=husk]");
        client(15, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        client(1, null, mc -> mc.player.jumpFromGround());
        client(6, null, mc -> {});
        client(1, null, mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        wait(4, "cloud_step");
        wait(6, "cloud_step_high");
        server(30, null, s -> LOG.info("[MYTHTEST] after cloud step y {} hp {}", player(s).getY(), player(s).getHealth()));
        client(2, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));

        // ---------------------------------------------------- kızıl damar: öz topla, atıl
        cmd(5, null, "kill @e[type=husk]",
                "summon husk 0.5 -60 3.5 {NoAI:1b,Health:200f,Attributes:[{Name:\"generic.max_health\",Base:200d}]}",
                "summon husk 0.5 -60 -6.5 {NoAI:1b,Health:200f,Attributes:[{Name:\"generic.max_health\",Base:200d}]}");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, item(IDS[1]));
            p.teleportTo(s.overworld(), 0.5, -60, 5.6, 180, 10);
        });
        client(15, null, mc -> mc.player.getInventory().selected = 0);
        for (int n = 0; n < 5; n++) {
            client(14, null, mc -> {
                mc.player.setXRot(15);
                KeyMapping.click(mc.options.keyAttack.getKey());
            });
        }
        server(2, null, s -> LOG.info("[MYTHTEST] essence after hits {}", CrimsonVeinItem.essence(player(s).getMainHandItem())));
        wait(10, "crimson_full_veins");
        client(5, null, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        wait(10, "crimson_third");
        client(2, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        cmd(5, null, "tp @a 0.5 -60 5.6 180 2");
        client(1, null, mc -> KeyMapping.click(mc.options.keyUse.getKey()));
        wait(3, "crimson_lunge");
        wait(5, "crimson_lunge_b");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            LOG.info("[MYTHTEST] after lunge pos {} essence {}", p.position(), CrimsonVeinItem.essence(p.getMainHandItem()));
            for (Husk h : s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -20, 20, -40, 20))) LOG.info("[MYTHTEST] husk hp {}", h.getHealth());
        });

        // ---------------------------------------------------- yosunkök: yer dövme ve yosunlandırma
        cmd(5, null, "kill @e[type=husk]", "fill -8 -61 -8 8 -61 8 minecraft:grass_block",
                "summon husk 3.5 -60 0.5 {NoAI:1b}", "summon husk -2.5 -60 1.5 {NoAI:1b}", "summon husk 0.5 -60 -3.5 {NoAI:1b}",
                "summon husk 1.5 -60 4.5 {NoAI:1b}");
        server(5, null, s -> {
            ServerPlayer p = player(s);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, item(IDS[2]));
            p.teleportTo(s.overworld(), 0.5, -60, 0.5, 180, 20);
        });
        client(15, null, mc -> {
            mc.player.getInventory().selected = 0;
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        });
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), true));
        wait(28, "moss_charge");
        client(1, null, mc -> KeyMapping.set(mc.options.keyUse.getKey(), false));
        wait(2, "moss_slam_a");
        wait(3, "moss_slam_b");
        cmd(4, "moss_slam_side", "tp @a 9 -57 7 135 22");
        server(10, null, s -> {
            for (Husk h : s.overworld().getEntitiesOfClass(Husk.class, new AABB(-20, -64, -20, 20, -40, 20))) {
                LOG.info("[MYTHTEST] husk after slam hp {} y {} slow {}", h.getHealth(), h.getY(), h.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN));
            }
        });
        cmd(5, null, "kill @e[type=husk]", "setblock 0 -60 -2 minecraft:cobblestone", "setblock 1 -60 -2 minecraft:stone_bricks",
                "tp @a 0.5 -60 0.5 180 30");
        client(60, null, mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        client(5, null, mc -> {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(0.5, -59.5, -1.0), Direction.SOUTH, new BlockPos(0, -60, -2), false));
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(1.5, -59.5, -1.0), Direction.SOUTH, new BlockPos(1, -60, -2), false));
        });
        wait(10, "mossify");
        server(2, null, s -> LOG.info("[MYTHTEST] mossify {} {}", s.overworld().getBlockState(new BlockPos(0, -60, -2)), s.overworld().getBlockState(new BlockPos(1, -60, -2))));
        cmd(30, "night_held", "time set 18000");
        wait(20, "END");
    }

    private static ItemStack item(String id) {
        return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id)));
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
            LOG.info("[MYTHTEST] creating world");
            LevelSettings settings = new LevelSettings("mythtest", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                    new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("mythtest", settings, new WorldOptions(9L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null && !allowScreen) mc.setScreen(null);
        mc.options.hideGui = !gui;
        if (stepStart < 0) {
            stepStart = tick + 100;
            LOG.info("[MYTHTEST] in world");
        }
        if (tick < stepStart || stepIndex >= steps.size()) return;
        Step st = steps.get(stepIndex);
        if (tick == stepStart) {
            try {
                if (st.client() != null) st.client().accept(mc);
            } catch (Throwable t) {
                LOG.error("[MYTHTEST] client step failed", t);
            }
            MinecraftServer s = mc.getSingleplayerServer();
            s.execute(() -> {
                try {
                    for (String c : st.commands()) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
                    if (st.server() != null) st.server().accept(s);
                } catch (Throwable t) {
                    LOG.error("[MYTHTEST] step failed", t);
                }
            });
        }
        if (tick - stepStart >= st.delay()) {
            if (st.shot() != null) {
                if (st.shot().equals("END")) {
                    LOG.info("[MYTHTEST] finished");
                    mc.stop();
                    return;
                }
                String name = "myth_" + String.format("%02d_", stepIndex) + st.shot() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("[MYTHTEST] screenshot {}", st.shot()));
            }
            stepIndex++;
            stepStart = tick + 1;
        }
    }
}
