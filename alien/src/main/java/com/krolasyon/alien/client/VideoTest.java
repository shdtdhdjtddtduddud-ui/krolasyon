package com.krolasyon.alien.client;

import com.krolasyon.alien.ai.HidingManager;
import com.krolasyon.alien.ai.Objectives;
import com.krolasyon.alien.ai.VentNetwork;
import com.krolasyon.alien.block.CeilingLightBlock;
import com.krolasyon.alien.block.LockerBlock;
import com.krolasyon.alien.block.SealedDoorBlock;
import com.krolasyon.alien.block.VentBlock;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.registry.ModBlocks;
import com.krolasyon.alien.registry.ModEntities;
import com.krolasyon.alien.registry.ModItems;
import com.krolasyon.alien.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.util.function.Consumer;

/**
 * CI-only scripted gameplay recording (inactive unless -Dalien.video=true). Builds a station and plays a 90 second
 * sequence of real gameplay moments while the CI job records the screen and audio.
 */
public final class VideoTest {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int CELL = 14;

    private int tick;
    private boolean worldRequested;
    private int phase = -1;      // -1 waiting, 0 setup, 1 recording
    private int t;               // ticks since recording start
    private volatile boolean ready;

    // scene anchors, filled on the server thread
    private volatile Vec3 start, ventExit, ventCam, lockerIn, lockerFront, genCam, gen, sealCam, seal, console, consoleCam, podInside;
    private volatile BlockPos lockerPos, ventPos;
    private volatile float lockerYaw;
    private int x0, z0;

    // camera
    private Vec3 camFrom, camTo;
    private float yawFrom, yawTo, pitchFrom, pitchTo;
    private int camStart, camLen;
    private Vec3 lookAt;
    private float fade;

    public static void init() {
        LOG.info("[VIDEO] enabled");
        MinecraftForge.EVENT_BUS.register(new VideoTest());
    }

    private static ServerPlayer player(MinecraftServer s) { return s.getPlayerList().getPlayers().get(0); }

    private static void cmd(MinecraftServer s, String c) {
        s.getCommands().performPrefixedCommand(player(s).createCommandSourceStack().withSuppressedOutput().withPermission(4), c);
    }

    private void server(Consumer<MinecraftServer> a) {
        MinecraftServer s = Minecraft.getInstance().getSingleplayerServer();
        if (s == null) return;
        s.execute(() -> {
            try { a.accept(s); } catch (Throwable e) { LOG.error("[VIDEO] server action failed", e); }
        });
    }

    // ------------------------------------------------------------------ camera helpers
    private void cam(Vec3 from, Vec3 to, float yaw0, float yaw1, float pitch0, float pitch1, int len) {
        camFrom = from; camTo = to; yawFrom = yaw0; yawTo = yaw1; pitchFrom = pitch0; pitchTo = pitch1; camStart = t; camLen = Math.max(1, len);
        lookAt = null;
    }

    private static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90F;
    }

    private static float pitchTo(Vec3 eye, Vec3 to) {
        Vec3 d = to.subtract(eye);
        return (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
    }

    private void applyCamera(LocalPlayer p) {
        if (camFrom == null) return;
        float k = Mth.clamp((t - camStart) / (float) camLen, 0F, 1F);
        float e = k * k * (3 - 2 * k);
        Vec3 pos = camFrom.lerp(camTo, e);
        float yaw = Mth.rotLerp(e, yawFrom, yawTo);
        float pitch = Mth.lerp(e, pitchFrom, pitchTo);
        if (lookAt != null) {
            Vec3 eye = pos.add(0, p.getEyeHeight(), 0);
            yaw = yawTo(eye, lookAt);
            pitch = pitchTo(eye, lookAt);
        }
        p.setPos(pos.x, pos.y, pos.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
        p.setXRot(pitch);
    }

    private Vec3 roomCenter(Vec3 p) {
        int i = Math.floorDiv((int) Math.floor(p.x) - x0, CELL), j = Math.floorDiv((int) Math.floor(p.z) - z0, CELL);
        return new Vec3(x0 + i * CELL + CELL / 2 + 0.5, p.y, z0 + j * CELL + CELL / 2 + 0.5);
    }

    // ------------------------------------------------------------------ scene anchors (server thread)
    private void locate(MinecraftServer s) {
        ServerLevel l = s.overworld();
        Objectives o = Objectives.get(l);
        start = new Vec3(0.5, -60, 0.5);
        x0 = -CELL / 2;
        z0 = -CELL / 2;
        // nearest ceiling vent to the start
        double best = 1e9;
        for (BlockPos v : VentNetwork.get(l).within(l, start, 0, 40)) {
            BlockPos ex = VentNetwork.exitFor(l, v);
            if (ex == null || !l.getBlockState(v.below()).isAir()) continue;
            double d = v.distSqr(BlockPos.containing(start));
            if (d < best) { best = d; ventPos = v; ventExit = Vec3.atBottomCenterOf(ex); }
        }
        if (ventExit != null) {
            Vec3 c = roomCenter(ventExit);
            Vec3 dir = c.subtract(ventExit).multiply(1, 0, 1);
            if (dir.length() < 2) dir = new Vec3(1, 0, 0);
            ventCam = ventExit.add(dir.normalize().scale(5.5));
            ventCam = new Vec3(ventCam.x, ventExit.y, ventCam.z);
        }
        // nearest locker
        best = 1e9;
        for (BlockPos b : BlockPos.betweenClosed(BlockPos.containing(start).offset(-30, -2, -30), BlockPos.containing(start).offset(30, 2, 30))) {
            BlockState st = l.getBlockState(b);
            if (st.getBlock() instanceof LockerBlock && st.getValue(LockerBlock.HALF) == DoubleBlockHalf.LOWER) {
                double d = b.distSqr(BlockPos.containing(start));
                if (d < best) {
                    best = d;
                    lockerPos = b.immutable();
                    Direction f = st.getValue(LockerBlock.FACING);
                    lockerYaw = f.toYRot();
                    lockerIn = Vec3.atBottomCenterOf(b);
                    lockerFront = Vec3.atBottomCenterOf(b.relative(f, 2));
                }
            }
        }
        BlockPos g = o.generatorPos();
        if (g != null) {
            gen = Vec3.atCenterOf(g);
            Direction f = l.getBlockState(g).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            genCam = Vec3.atBottomCenterOf(g.relative(f, 3));
        }
        BlockPos c = o.consolePos();
        if (c != null) {
            console = Vec3.atCenterOf(c);
            Direction f = l.getBlockState(c).getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            consoleCam = Vec3.atBottomCenterOf(c.relative(f, 2));
            podInside = roomCenter(Vec3.atBottomCenterOf(c));
            // the keycard seal of the pod bay
            best = 1e9;
            for (BlockPos b : BlockPos.betweenClosed(c.offset(-16, -2, -16), c.offset(16, 3, 16))) {
                if (!l.getBlockState(b).is(ModBlocks.KEYCARD_SEAL.get())) continue;
                double d = b.distSqr(c);
                if (d < best) { best = d; seal = Vec3.atBottomCenterOf(b.immutable()); }
            }
            if (seal != null) {
                Vec3 pod = roomCenter(seal.add(0, 0, 0));
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    Vec3 cand = seal.add(d.getStepX() * 4, 0, d.getStepZ() * 4);
                    Vec3 rc = roomCenter(cand);
                    if (!rc.equals(roomCenter(Vec3.atBottomCenterOf(c)))) { sealCam = cand; break; }
                }
            }
        }
        LOG.info("[VIDEO] anchors vent={} locker={} gen={} console={} seal={} sealCam={}", ventExit, lockerIn, gen, console, seal, sealCam);
        ready = true;
    }

    private XenomorphEntity spawnStatic(ServerLevel l, Vec3 at, float yaw) {
        XenomorphEntity x = ModEntities.XENOMORPH.get().create(l);
        if (x == null) return null;
        x.moveTo(at.x, at.y, at.z, yaw, 0);
        x.setYHeadRot(yaw);
        x.setYBodyRot(yaw);
        x.setNoAi(true);
        l.addFreshEntity(x);
        return x;
    }

    private void clearAliens(MinecraftServer s) {
        for (XenomorphEntity x : s.overworld().getEntitiesOfClass(XenomorphEntity.class, new AABB(-200, -80, -200, 200, 0, 200))) x.discard();
    }

    private void each(MinecraftServer s, Consumer<XenomorphEntity> c) {
        for (XenomorphEntity x : s.overworld().getEntitiesOfClass(XenomorphEntity.class, new AABB(-200, -80, -200, 200, 0, 200))) c.accept(x);
    }

    private void slot(MinecraftServer s, ItemStack st) {
        ServerPlayer p = player(s);
        p.getInventory().setItem(0, st);
        p.getInventory().selected = 0;
    }

    private static void title(MinecraftServer s, String title, String sub, int stay) {
        ServerPlayer p = player(s);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(15, stay, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
    }

    // ------------------------------------------------------------------ timeline
    private static final int S0 = 0, S1 = 100, S2 = 400, S3 = 640, S4 = 940, S5 = 1200, S6 = 1360, S7 = 1680, END = 1800;

    private void scene(Minecraft mc, LocalPlayer p) {
        // fades between scenes
        int[] cuts = {S1, S2, S3, S4, S5, S6, S7, END};
        fade = 0F;
        for (int c : cuts) {
            if (t > c - 10 && t <= c) fade = Math.max(fade, (t - (c - 10)) / 10F);
            if (t >= c && t < c + 10) fade = Math.max(fade, 1F - (t - c) / 10F);
        }

        // S0: title card in the dark start room
        if (t == S0) {
            server(s -> title(s, "ALIEN: ISOLATION", "Sevastopol — Minecraft 1.20.1 Forge modu", 70));
            cam(start, start, 225, 250, 5, 5, 100);
        }
        // S1: sneaking through the start room with the motion tracker; the alien prowls next door
        if (t == S1) {
            mc.options.keyShift.setDown(true);
            server(s -> {
                slot(s, new ItemStack(ModItems.MOTION_TRACKER.get()));
                XenomorphEntity x = ModEntities.XENOMORPH.get().create(s.overworld());
                if (x != null) {
                    x.moveTo(14.5, -60, 0.5, 90, 0);
                    s.overworld().addFreshEntity(x);
                }
            });
            cam(start.add(-3, 0, -3), start.add(3, 0, 3), 300, 330, 10, 0, 300);
        }
        // S2: it drops out of the ceiling vent
        if (t == S2) {
            mc.options.keyShift.setDown(false);
            server(s -> { clearAliens(s); slot(s, new ItemStack(ModItems.FLASHLIGHT.get())); });
            if (ventCam != null) {
                Vec3 v = ventExit;
                float yaw = yawTo(ventCam.add(0, 1.6, 0), v.add(0, 1.8, 0));
                cam(ventCam, ventCam.add(ventCam.subtract(v).normalize().scale(0.8)), yaw, yaw, -25, -8, 240);
            }
        }
        if (t == S2 + 40 && ventExit != null) server(s -> {
            ServerLevel l = s.overworld();
            l.playSound(null, ventPos, ModSounds.XENO_VENT.get(), SoundSource.HOSTILE, 1.6F, 0.9F);
        });
        if (t == S2 + 70 && ventExit != null) server(s -> {
            ServerLevel l = s.overworld();
            float yaw = yawTo(ventExit, ventCam);
            XenomorphEntity x = spawnStatic(l, ventExit, yaw);
            if (x != null) x.debugAnim(XenomorphEntity.ANIM_VENT_EXIT);
            l.playSound(null, BlockPos.containing(ventExit), ModSounds.XENO_DROP.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
            BlockPos o = BlockPos.containing(ventExit);
            for (BlockPos lp : BlockPos.betweenClosed(o.offset(-5, 0, -5), o.offset(5, 7, 5))) {
                BlockState st = l.getBlockState(lp);
                if (st.getBlock() instanceof CeilingLightBlock) CeilingLightBlock.blackout(l, lp.immutable(), st, 6 + l.random.nextInt(30));
            }
        });
        if (t == S2 + 100) server(s -> { each(s, x -> x.debugAnim(XenomorphEntity.ANIM_HISS)); s.overworld().playSound(null, BlockPos.containing(ventExit == null ? start : ventExit), ModSounds.XENO_SCREECH.get(), SoundSource.HOSTILE, 1.6F, 1.0F); });
        if (t == S2 + 170) server(s -> each(s, x -> x.debugAnim(XenomorphEntity.ANIM_SEARCH)));

        // S3: hiding in a locker while it sniffs right outside
        if (t == S3 && lockerPos != null) {
            server(s -> {
                clearAliens(s);
                slot(s, ItemStack.EMPTY);
                HidingManager.enter(player(s), lockerPos);
            });
            cam(lockerIn, lockerIn, lockerYaw, lockerYaw, 4, 4, 300);
        }
        if (t == S3 + 50 && lockerFront != null) server(s -> {
            XenomorphEntity x = spawnStatic(s.overworld(), lockerFront, lockerYaw + 180F);
            if (x != null) x.debugAnim(XenomorphEntity.ANIM_LOCKER);
            s.overworld().playSound(null, BlockPos.containing(lockerFront), ModSounds.XENO_SNIFF.get(), SoundSource.HOSTILE, 1.4F, 1.0F);
        });
        if (t == S3 + 60) mc.options.keyShift.setDown(true);
        if (t == S3 + 125) server(s -> {
            each(s, x -> x.debugAnim(XenomorphEntity.ANIM_LOCKER));
            s.overworld().playSound(null, BlockPos.containing(lockerFront), ModSounds.XENO_SNIFF.get(), SoundSource.HOSTILE, 1.4F, 1.1F);
        });
        if (t == S3 + 160) mc.options.keyShift.setDown(false);
        if (t == S3 + 200) server(s -> { each(s, x -> x.debugAnim(XenomorphEntity.ANIM_HISS)); s.overworld().playSound(null, BlockPos.containing(lockerFront), ModSounds.XENO_GROWL.get(), SoundSource.HOSTILE, 1.4F, 1.0F); });
        if (t == S3 + 250) server(this::clearAliens);

        // S4: fuses into the generator, power comes back
        if (t == S4) {
            server(s -> {
                HidingManager.exit(player(s));
                clearAliens(s);
                slot(s, new ItemStack(ModItems.FUSE.get(), 3));
            });
            if (genCam != null) {
                float yaw = yawTo(genCam.add(0, 1.6, 0), gen);
                cam(genCam, genCam.add(gen.subtract(genCam).normalize().scale(1.2)).multiply(1, 0, 1).add(0, genCam.y, 0), yaw, yaw, 20, 30, 120);
            }
        }
        for (int k = 0; k < 3; k++) {
            if (t == S4 + 50 + k * 30 && gen != null) server(s -> {
                ServerPlayer sp = player(s);
                Objectives.onGeneratorUsed(s.overworld(), sp, BlockPos.containing(gen), InteractionHand.MAIN_HAND);
                sp.swing(InteractionHand.MAIN_HAND, true);
            });
        }
        if (t == S4 + 150 && genCam != null) {
            float yaw = yawTo(genCam.add(0, 1.6, 0), gen);
            Vec3 here = p.position();
            cam(here, here, yaw, yaw + 160, 25, -15, 110);
        }

        // S5: keycard in hand, the pod bay door slides open
        if (t == S5) {
            server(s -> slot(s, new ItemStack(ModItems.KEYCARD.get())));
            if (sealCam != null) {
                float yaw = yawTo(sealCam.add(0, 1.6, 0), seal.add(0, 1, 0));
                cam(sealCam, sealCam.add(seal.subtract(sealCam).normalize().scale(1.5)), yaw, yaw, 5, 5, 140);
            }
        }
        if (t == S5 + 70 && seal != null) server(s -> {
            player(s).swing(InteractionHand.MAIN_HAND, true);
            SealedDoorBlock.open(s.overworld(), BlockPos.containing(seal));
        });

        // S6: launch countdown; it comes for us; flamethrower keeps it back
        if (t == S6 && console != null) {
            server(s -> {
                ServerPlayer sp = player(s);
                sp.setGameMode(GameType.SURVIVAL);
                sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60, 4, false, false));
                sp.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20 * 60, 1, false, false));
                slot(s, new ItemStack(ModItems.FLAMETHROWER.get()));
                Objectives.onConsoleUsed(s.overworld(), sp, BlockPos.containing(console));
            });
            float yaw = yawTo(consoleCam.add(0, 1.6, 0), console);
            cam(consoleCam, consoleCam, yaw, yaw, 25, 25, 40);
        }
        if (t == S6 + 45 && consoleCam != null && seal != null) {
            float yaw0 = yawTo(consoleCam.add(0, 1.6, 0), console);
            float yaw1 = yawTo(consoleCam.add(0, 1.6, 0), seal.add(0, 1, 0));
            cam(consoleCam, consoleCam, yaw0, yaw1, 25, 0, 40);
        }
        if (t == S6 + 70 && seal != null && consoleCam != null) server(s -> {
            Vec3 at = seal.add(seal.subtract(consoleCam).normalize().multiply(1, 0, 1).scale(1.5));
            XenomorphEntity x = ModEntities.XENOMORPH.get().create(s.overworld());
            if (x != null) {
                x.moveTo(at.x, consoleCam.y, at.z, yawTo(at, consoleCam), 0);
                s.overworld().addFreshEntity(x);
            }
        });
        if (t > S6 + 70 && t < S7) {
            XenomorphEntity near = null;
            for (XenomorphEntity x : mc.level.getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(30))) near = x;
            if (near != null && !near.isHidden()) {
                lookAt = near.position().add(0, 1.4, 0);
                boolean fire = near.distanceTo(p) < 4.2F && near.getClientState() != XenomorphEntity.State.FLEE;
                mc.options.keyUse.setDown(fire);
            } else {
                mc.options.keyUse.setDown(false);
                if (console != null && t > S6 + 120) lookAt = console.add(0, 0.6, 0);
            }
            if (t == S6 + 180) server(s -> each(s, x -> {
                ServerPlayer sp = player(s);
                if (x.distanceTo(sp) > 8 && !x.isHidden()) {
                    Vec3 front = sp.position().add(sp.getLookAngle().multiply(1, 0, 1).normalize().scale(6));
                    x.teleportTo(front.x, sp.getY(), front.z);
                }
            }));
        }
        // S7: the pod launches
        if (t == S7) {
            mc.options.keyUse.setDown(false);
            Vec3 here = p.position();
            cam(here, here.add(0, 0.4, 0), p.getYRot(), p.getYRot() + 40, p.getXRot(), -20, 120);
        }
        if (t >= END) {
            LOG.info("[VIDEO] finished");
            try { Files.writeString(new File(mc.gameDirectory, "REC_DONE").toPath(), "1"); } catch (Exception ignored) {}
            mc.stop();
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
            LOG.info("[VIDEO] creating world");
            LevelSettings settings = new LevelSettings("avideo", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("avideo", settings, new WorldOptions(1979L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        LocalPlayer p = mc.player;
        if (mc.level == null || p == null || mc.getSingleplayerServer() == null) return;
        if (mc.screen != null) mc.setScreen(null);
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        if (phase == -1) {
            phase = 0;
            t = -160;
            server(s -> {
                for (String c : new String[]{"gamerule doDaylightCycle false", "gamerule doMobSpawning false", "time set 18000",
                        "difficulty normal", "gamemode creative @a", "tp @a 0 -60 0 225 0"}) cmd(s, c);
            });
        }
        if (phase == 0) {
            t++;
            if (t == -140) server(s -> cmd(s, "sevastopol olustur"));
            if (t == -100) server(s -> { clearAliens(s); locate(s); });
            if (t >= -100 && p.getAbilities().mayfly) p.getAbilities().flying = true;
            if (t >= 0 && ready) {
                phase = 1;
                t = 0;
                try { Files.writeString(new File(mc.gameDirectory, "REC_START").toPath(), "1"); } catch (Exception ignored) {}
                LOG.info("[VIDEO] recording");
            } else if (t >= 0) t = -1;
            return;
        }
        if (p.getAbilities().mayfly) p.getAbilities().flying = true;
        scene(mc, p);
        applyCamera(p);
        t++;
    }

    @SubscribeEvent
    public void onGui(RenderGuiEvent.Post e) {
        if (fade <= 0.001F && phase == 1) return;
        int a = phase == 1 ? (int) (fade * 255) : 255;
        e.getGuiGraphics().fill(0, 0, e.getWindow().getGuiScaledWidth(), e.getWindow().getGuiScaledHeight(), a << 24);
    }
}
