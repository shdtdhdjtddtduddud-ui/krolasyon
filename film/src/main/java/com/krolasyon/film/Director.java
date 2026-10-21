package com.krolasyon.film;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Senaryoyu çalıştıran durum makinesi: hazırlık (kayıtsız), bekleme (yükleme), çekim (sabit adımlı kayıt). */
public final class Director {
    public static final Logger LOG = LogUtils.getLogger();
    public static final boolean QUICK = Boolean.getBoolean("film.quick");
    public static final long SEED = 4242L;
    /** Hedef video süresi (tick): 10 dakika. */
    private static final int TARGET_TICKS = 20 * 600;
    private static final Pattern STATS = Pattern.compile("C: (\\d+)/(\\d+).*?pC: (\\d+), pU: (\\d+)");

    enum State { BOOT, LOADING, INIT, WARM, SETUP, WAIT, ROLL, DONE }

    public final int fps = QUICK ? 10 : 30;
    public final W w = new W();
    public final Map<String, UUID> ids = new ConcurrentHashMap<>();
    private final List<Shot> shots;
    private final Recorder rec = new Recorder();
    private final File out = new File("film");
    private BufferedWriter log;
    private State state = State.BOOT;
    private int idx = -1, tick, stateTick, shotTick, warmIdx, lastRendered = -1, stableTicks;
    public Shot cur;
    private float partial;
    private long frames;
    private double lx, ly, lz;
    private float lyaw;
    private Cam camNow;
    private Field nextTick;
    private Method camPos;
    private long realStart;

    public Director(List<Shot> shots) {
        this.shots = shots;
        int total = 0;
        for (Shot s : shots) total += s.len;
        double k = Mth.clamp(TARGET_TICKS / (double) total, 0.75, 1.6);
        LOG.info("[FILM] {} shots, raw {} s, scale {}", shots.size(), total / 20.0, k);
        for (Shot s : shots) s.scale(k);
    }

    // ------------------------------------------------------------------ yardımcılar (senaryo için)

    public Minecraft mc() { return Minecraft.getInstance(); }
    public MinecraftServer srv() { return Minecraft.getInstance().getSingleplayerServer(); }
    public ServerLevel lvl() { return srv().overworld(); }
    public ServerPlayer sp() { return srv().getPlayerList().getPlayers().get(0); }
    public LocalPlayer lp() { return Minecraft.getInstance().player; }
    public float partial() { return partial; }
    public int shotTick() { return shotTick; }

    public static String f(String fmt, Object... a) { return String.format(Locale.ROOT, fmt, a); }

    /** Sunucu iş parçacığında çalıştırır. */
    public void server(Consumer<ServerLevel> c) {
        MinecraftServer s = srv();
        s.execute(() -> safe("server", () -> c.accept(s.overworld())));
    }

    /** Sunucu iş parçacığından çağrılmalı. */
    public void run(String... cmds) {
        MinecraftServer s = srv();
        for (String c : cmds) s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c);
    }

    public void cmd(String... cmds) { server(l -> run(cmds)); }

    public void key(KeyMapping k, boolean down) { KeyMapping.set(k.getKey(), down); }

    public void click(KeyMapping k) { KeyMapping.click(k.getKey()); }

    public void releaseKeys() {
        var o = mc().options;
        for (KeyMapping k : new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight, o.keyJump, o.keyShift, o.keySprint, o.keyUse, o.keyAttack}) {
            KeyMapping.set(k.getKey(), false);
        }
    }

    public static ItemStack item(String id, int n) {
        Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return it == null ? ItemStack.EMPTY : new ItemStack(it, n);
    }

    /** Sunucu tarafı: oyuncunun envanter yuvasına eşya koyar. */
    public void give(int slot, String id) { sp().getInventory().setItem(slot, item(id, id.endsWith("arrow") ? 64 : 1)); }

    public void select(int slot) { if (lp() != null) lp().getInventory().selected = slot; }

    /** Sunucu tarafı: varlık yaratır ve anahtarla hatırlar. */
    public Entity spawn(String key, String typeId, double x, double y, double z, float yaw, Consumer<Entity> cfg) {
        EntityType<?> t = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(typeId));
        if (t == null) {
            LOG.warn("[FILM] unknown entity {}", typeId);
            return null;
        }
        ServerLevel l = lvl();
        Entity e = t.create(l);
        if (e == null) return null;
        e.moveTo(x, y, z, yaw, 0F);
        e.setYHeadRot(yaw);
        if (e instanceof Mob m) {
            m.yBodyRot = yaw;
            m.finalizeSpawn(l, l.getCurrentDifficultyAt(e.blockPosition()), MobSpawnType.COMMAND, null, null);
            m.setPersistenceRequired();
        }
        e.addTag("film");
        if (cfg != null) cfg.accept(e);
        l.addFreshEntity(e);
        if (key != null) ids.put(key, e.getUUID());
        return e;
    }

    /** Sunucu tarafı varlık (sunucu iş parçacığında). */
    public Entity se(String key) {
        UUID u = ids.get(key);
        return u == null ? null : lvl().getEntity(u);
    }

    /** İstemci tarafı varlık. */
    public Entity ce(String key) {
        UUID u = ids.get(key);
        if (u == null || mc().level == null) return null;
        for (Entity e : mc().level.entitiesForRendering()) if (e.getUUID().equals(u)) return e;
        return null;
    }

    public static Vec3 at(Entity e, float p) {
        return new Vec3(Mth.lerp(p, e.xo, e.getX()), Mth.lerp(p, e.yo, e.getY()), Mth.lerp(p, e.zo, e.getZ()));
    }

    /** İstemci varlığının bu karedeki konumu (yoksa yedek). */
    public Vec3 pos(String key, Vec3 fallback) {
        Entity e = ce(key);
        return e == null ? fallback : at(e, partial);
    }

    public Vec3 me() {
        LocalPlayer p = lp();
        return p == null ? Vec3.ZERO : at(p, partial);
    }

    /** Sunucu tarafı zemin (yaprak hariç) yüksekliği: üstündeki ilk boş blok. */
    public int ground(int x, int z) {
        return lvl().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    /** Oyuncu gözünden hedefe bakış açıları; yumuşak = kare başına yaklaşma oranı. */
    public float[] aimAt(Vec3 target, float soft) {
        LocalPlayer p = lp();
        Vec3 eye = me().add(0, p.getEyeHeight(), 0);
        double dx = target.x - eye.x, dy = target.y - eye.y, dz = target.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        if (soft >= 1F) return new float[]{yaw, pitch};
        float cy = p.getYRot(), cp = p.getXRot();
        return new float[]{cy + Mth.wrapDegrees(yaw - cy) * soft, cp + (pitch - cp) * soft};
    }

    // ------------------------------------------------------------------ akış

    private void go(State s) {
        state = s;
        stateTick = 0;
    }

    static void safe(String what, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            LOG.error("[FILM] {} failed", what, t);
        }
    }

    private void nextShot() {
        releaseKeys();
        idx++;
        if (idx >= shots.size()) {
            finish();
            return;
        }
        cur = shots.get(idx);
        warmIdx = 0;
        LOG.info("[FILM] shot {}/{} {} ({} ticks) real {} s", idx + 1, shots.size(), cur.name, cur.len, (System.currentTimeMillis() - realStart) / 1000);
        go(cur.warm.isEmpty() ? State.SETUP : State.WARM);
    }

    private void startRoll() {
        Minecraft mc = mc();
        mc.options.setCameraType(cur.view);
        VClock.fix();
        if (!rec.started()) {
            try {
                var rt = mc.getMainRenderTarget();
                rec.start(new File(out, "video.mp4"), rt.width, rt.height, fps);
            } catch (IOException e) {
                LOG.error("[FILM] ffmpeg start failed", e);
            }
        }
        double t0 = frames / (double) fps;
        line(f("{\"shot\":\"%s\",\"t\":%.4f,\"len\":%.3f}", cur.name, t0, cur.len / 20.0));
        if (cur.music != null) {
            StringBuilder sb = new StringBuilder();
            for (String m : cur.music) sb.append(sb.length() == 0 ? "" : ",").append('"').append(m).append('"');
            line(f("{\"music\":[%s],\"t\":%.4f,\"v\":%.3f}", sb, t0, cur.musicVol));
        }
        for (Object[] a : cur.accents) {
            line(f("{\"t\":%.4f,\"p\":\"%s\",\"v\":%.3f,\"pi\":%.3f,\"rel\":true,\"lin\":false}", t0 + (Integer) a[0] / 20.0, a[1], (Float) a[2], (Float) a[3]));
        }
        shotTick = 0;
        stableTicks = 0;
        go(State.ROLL);
        events(0);
    }

    private void events(int t) {
        List<Consumer<Director>> l = cur.at.get(t);
        if (l != null) for (Consumer<Director> c : l) safe(cur.name + "@" + t, () -> c.accept(this));
        if (cur.every != null) safe(cur.name + " every", () -> cur.every.run(this, t));
    }

    private void endRoll() {
        VClock.free();
        nextShot();
    }

    private void finish() {
        VClock.free();
        rec.finish();
        line(f("{\"end\":%.4f}", frames / (double) fps));
        try {
            if (log != null) log.close();
            new File(out, "DONE").createNewFile();
        } catch (IOException ignored) {
        }
        LOG.info("[FILM] END frames {} real {} s", frames, (System.currentTimeMillis() - realStart) / 1000);
        go(State.DONE);
    }

    private synchronized void line(String s) {
        try {
            if (log == null) {
                out.mkdirs();
                log = new BufferedWriter(new FileWriter(new File(out, "events.jsonl")));
            }
            log.write(s);
            log.write('\n');
        } catch (IOException e) {
            LOG.error("[FILM] log", e);
        }
    }

    private boolean ready() {
        Minecraft mc = mc();
        Matcher m = STATS.matcher(mc.levelRenderer.getChunkStatistics());
        if (!m.find()) return stateTick > 200;
        int rendered = Integer.parseInt(m.group(1));
        int pending = Integer.parseInt(m.group(3)) + Integer.parseInt(m.group(4));
        if (rendered == lastRendered && pending == 0) stableTicks++;
        else stableTicks = 0;
        lastRendered = rendered;
        return stableTicks >= 30;
    }

    private void placeSpectator(Cam c) {
        LocalPlayer p = lp();
        if (c == null || p == null || !p.isSpectator()) return;
        p.setDeltaMovement(Vec3.ZERO);
        p.setPos(c.pos().x, c.pos().y - p.getEyeHeight(), c.pos().z);
        p.setYRot(c.yaw());
        p.setXRot(c.pitch());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        tick++;
        stateTick++;
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = false;
        mc.getToasts().clear();
        if (mc.getOverlay() != null) return;
        if (mc.screen != null && mc.level != null && (cur == null || !cur.allowScreen || state != State.ROLL)) mc.setScreen(null);
        switch (state) {
            case BOOT -> {
                if (tick > 40 && mc.level == null) {
                    LOG.info("[FILM] creating world (quick={})", QUICK);
                    realStart = System.currentTimeMillis();
                    LevelSettings settings = new LevelSettings("film", GameType.CREATIVE, false, Difficulty.NORMAL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT);
                    mc.createWorldOpenFlows().createFreshLevel("film", settings, new WorldOptions(SEED, true, false),
                            reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.NORMAL).value().createWorldDimensions());
                    go(State.LOADING);
                }
            }
            case LOADING -> {
                if (mc.level != null && mc.player != null && srv() != null && stateTick > 200) {
                    VClock.install(fps);
                    try {
                        nextTick = MinecraftServer.class.getDeclaredField("nextTickTime");
                        nextTick.setAccessible(true);
                    } catch (Exception e) {
                        LOG.warn("[FILM] no nextTickTime field; no lockstep", e);
                    }
                    try {
                        camPos = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                        camPos.setAccessible(true);
                    } catch (Exception e) {
                        LOG.error("[FILM] no Camera.setPosition", e);
                    }
                    server(l -> {
                        Scenes.init(this);
                        w.ready = true;
                    });
                    go(State.INIT);
                }
            }
            case INIT -> {
                if (w.ready) nextShot();
                else if (stateTick > 12000) {
                    LOG.error("[FILM] init timeout");
                    nextShot();
                }
            }
            case WARM -> {
                if (stateTick == 1) {
                    Vec3 p = cur.warm.get(warmIdx);
                    cmd("gamemode spectator @a", f("tp @a %.1f %.1f %.1f", p.x, p.y, p.z));
                } else if ((stateTick > 60 && ready()) || stateTick > 1200) {
                    warmIdx++;
                    if (warmIdx >= cur.warm.size()) go(State.SETUP);
                    else go(State.WARM);
                }
            }
            case SETUP -> {
                for (Consumer<Director> c : cur.setup) safe(cur.name + " setup", () -> c.accept(this));
                lastRendered = -1;
                stableTicks = 0;
                go(State.WAIT);
            }
            case WAIT -> {
                mc.options.setCameraType(cur.view);
                if (cur.cam != null) safe("precam", () -> placeSpectator(cur.cam.at(this, 0, 0)));
                boolean ok = ready();
                if ((stateTick >= cur.settle && ok) || stateTick > cur.maxWait) {
                    if (!ok) LOG.warn("[FILM] {} started before chunks settled", cur.name);
                    startRoll();
                }
            }
            case ROLL -> {
                shotTick++;
                if (shotTick >= cur.len) {
                    endRoll();
                    return;
                }
                if (cur.cam != null) safe("cam", () -> placeSpectator(cur.cam.at(this, shotTick, shotTick / (float) cur.len)));
                events(shotTick);
            }
            case DONE -> {
                if (stateTick == 40) mc.stop();
            }
        }
    }

    @SubscribeEvent
    public void onRender(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.phase == TickEvent.Phase.START) {
            partial = event.renderTickTime;
            if (state == State.ROLL && cur.aim != null && mc.player != null) {
                float t = shotTick + partial;
                safe("aim", () -> {
                    float[] a = cur.aim.at(this, t, t / cur.len);
                    if (a != null) {
                        mc.player.setYRot(a[0]);
                        mc.player.setXRot(Mth.clamp(a[1], -90F, 90F));
                    }
                });
            }
            return;
        }
        if (state != State.ROLL) return;
        rec.frame(mc.getMainRenderTarget());
        frames++;
        VClock.step();
        if (frames % (fps * 10L) == 0) LOG.info("[FILM] frame {} ({} s video, {} s real)", frames, frames / fps, (System.currentTimeMillis() - realStart) / 1000);
        lockstep();
    }

    private void lockstep() {
        MinecraftServer s = srv();
        if (s == null || nextTick == null) return;
        long until = System.currentTimeMillis() + 30000;
        try {
            while (System.currentTimeMillis() < until && s.isRunning()) {
                if (VClock.millis() - nextTick.getLong(s) <= 120) return;
                Thread.sleep(1);
            }
        } catch (Exception e) {
            nextTick = null;
        }
    }

    @SubscribeEvent
    public void onCamera(ViewportEvent.ComputeCameraAngles event) {
        camNow = null;
        Camera c = event.getCamera();
        if ((state == State.ROLL || state == State.WAIT) && cur != null && cur.cam != null && camPos != null) {
            float t = (state == State.ROLL ? shotTick : 0) + (float) event.getPartialTick();
            try {
                Cam k = cur.cam.at(this, t, t / cur.len);
                if (k != null) {
                    camPos.invoke(c, k.pos());
                    event.setYaw(k.yaw());
                    event.setPitch(k.pitch());
                    event.setRoll(k.roll());
                    camNow = k;
                }
            } catch (Throwable e) {
                LOG.error("[FILM] camera", e);
            }
        }
        lx = c.getPosition().x;
        ly = c.getPosition().y;
        lz = c.getPosition().z;
        lyaw = camNow != null ? camNow.yaw() : event.getYaw();
    }

    @SubscribeEvent
    public void onFov(ViewportEvent.ComputeFov event) {
        if (camNow != null && camNow.fov() > 0 && event.usedConfiguredFov()) event.setFOV(camNow.fov());
    }

    @SubscribeEvent
    public void onHand(RenderHandEvent event) {
        if (cur == null || !cur.hand || camNow != null) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onOverlay(RenderGuiOverlayEvent.Pre event) {
        ResourceLocation id = event.getOverlay().id();
        if (id.equals(VanillaGuiOverlay.CHAT_PANEL.id())) {
            event.setCanceled(true);
            return;
        }
        int hud = cur == null ? 0 : cur.hud;
        if (hud == 2) return;
        if (hud == 1 && id.equals(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onGui(RenderGuiEvent.Post event) {
        if (cur == null || (state != State.ROLL && state != State.WAIT)) return;
        GuiGraphics g = event.getGuiGraphics();
        int gw = g.guiWidth(), gh = g.guiHeight();
        float t = (state == State.ROLL ? shotTick : 0) + partial;
        if (cur.letterbox > 0) {
            int bar = (int) (gh * 0.105F * cur.letterbox);
            g.fill(0, 0, gw, bar, 0xFF000000);
            g.fill(0, gh - bar, gw, gh, 0xFF000000);
        }
        for (Shot.Title tt : cur.titles) title(g, tt, t, gw, gh);
        float a = 0;
        if (cur.fadeIn > 0 && t < cur.fadeIn) a = 1F - t / cur.fadeIn;
        if (cur.fadeOut > 0 && t > cur.len - cur.fadeOut) a = Math.max(a, (t - (cur.len - cur.fadeOut)) / cur.fadeOut);
        if (state == State.WAIT && cur.fadeIn > 0) a = 1;
        a = Mth.clamp(a, 0, 1);
        if (a > 0.002F) g.fill(0, 0, gw, gh, ((int) (a * 255) << 24));
    }

    private static String spaced(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0) b.append(' ');
            b.append(s.charAt(i));
        }
        return b.toString();
    }

    private void title(GuiGraphics g, Shot.Title tt, float t, int gw, int gh) {
        float lt = t - tt.start();
        if (lt < 0 || lt > tt.dur()) return;
        float in = Math.min(1F, lt / 12F), outA = Math.min(1F, (tt.dur() - lt) / 14F);
        float a = Math.max(0, Math.min(in, outA));
        int alpha = (int) (a * 255);
        if (alpha < 8) return;
        Font font = mc().font;
        float grow = 1F + 0.05F * (lt / tt.dur());
        float scale;
        float cy;
        String text = tt.text();
        switch (tt.style()) {
            case 0 -> { scale = 3.0F; cy = gh * 0.40F; text = spaced(text); }
            case 1 -> { scale = 2.6F; cy = gh * 0.66F; }
            case 2 -> { scale = 1.35F; cy = gh * 0.76F; grow = 1F; }
            default -> { scale = 4.6F; cy = gh * 0.42F; text = spaced(text); }
        }
        scale *= grow;
        Component c = Component.literal(text).withStyle(ChatFormatting.BOLD);
        int tw = font.width(c);
        g.pose().pushPose();
        g.pose().translate(gw / 2F, cy, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, c, -tw / 2, -4, (alpha << 24) | tt.color(), true);
        g.pose().popPose();
        if (tt.style() == 0 || tt.style() == 3) {
            int lw = (int) (tw * scale * 0.55F * Math.min(1F, lt / 20F));
            int ly2 = (int) (cy + 6 * scale);
            g.fill(gw / 2 - lw, ly2, gw / 2 + lw, ly2 + 1, ((int) (alpha * 0.8F) << 24) | tt.color());
        }
        if (tt.sub() != null && !tt.sub().isEmpty()) {
            float ss = tt.style() == 2 ? 0.9F : 1.25F;
            Component s = Component.literal(tt.sub()).withStyle(ChatFormatting.ITALIC);
            g.pose().pushPose();
            g.pose().translate(gw / 2F, cy + (tt.style() == 2 ? 9 : 6 * scale + 7), 0);
            g.pose().scale(ss, ss, 1);
            g.drawString(font, s, -font.width(s) / 2, 0, (alpha << 24) | 0xE6E0D2, true);
            g.pose().popPose();
        }
    }

    // ------------------------------------------------------------------ ses günlüğü

    @SubscribeEvent
    public void onSound(PlaySoundEvent event) {
        if (state != State.ROLL) return;
        SoundInstance s = event.getSound();
        if (s == null || s.getSource() == SoundSource.MUSIC) return;
        try {
            WeighedSoundEvents ev = s.resolve(mc().getSoundManager());
            if (ev == null || s.getSound() == null) return;
            ResourceLocation path = s.getSound().getPath();
            if (path.getPath().contains("empty")) return;
            double vol = s.getVolume() * mc().options.getSoundSourceVolume(s.getSource());
            if (vol <= 0.001) return;
            boolean lin = s.getAttenuation() == SoundInstance.Attenuation.LINEAR;
            line(f("{\"t\":%.4f,\"p\":\"%s\",\"v\":%.3f,\"pi\":%.3f,\"x\":%.2f,\"y\":%.2f,\"z\":%.2f,\"rel\":%s,\"lin\":%s,\"d\":%d,"
                            + "\"lx\":%.2f,\"ly\":%.2f,\"lz\":%.2f,\"yaw\":%.1f}",
                    frames / (double) fps, path, vol, s.getPitch(), s.getX(), s.getY(), s.getZ(), s.isRelative(), lin,
                    s.getSound().getAttenuationDistance(), lx, ly, lz, lyaw));
        } catch (Throwable e) {
            LOG.debug("[FILM] sound log", e);
        }
    }

    // ------------------------------------------------------------------ oyuncu ve dostlar ölmesin

    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        var e = event.getEntity();
        float keep = e instanceof ServerPlayer ? 8F : (e instanceof com.krolasyon.elvenrealm.entity.ElfEntity
                || e instanceof com.krolasyon.stormray.entity.StormRayEntity) ? 12F : -1F;
        if (keep < 0) return;
        if (e.getHealth() - event.getAmount() < keep) event.setAmount(Math.max(0, e.getHealth() - keep));
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            event.setCanceled(true);
            p.setHealth(10F);
        }
    }
}
