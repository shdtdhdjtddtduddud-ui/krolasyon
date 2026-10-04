package com.krolasyon.futbol.client;

import com.krolasyon.futbol.entity.FootballEntity;
import com.krolasyon.futbol.entity.FootballerEntity;
import com.krolasyon.futbol.game.Move;
import com.krolasyon.futbol.game.Role;
import com.krolasyon.futbol.game.Team;
import com.krolasyon.futbol.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Goal replays. The client keeps a rolling recording of every player, bot and ball near the pitch. After a goal the
 * last seconds are played back with client-only stand-in entities (the real ones are hidden) and a cinematic camera.
 */
public final class Replay {
    private Replay() {}

    private static final int CAP = 240;
    private static final int LENGTH = 150;

    record Snap(int kind, double x, double y, double z, float body, float head, float pitch, int team, int number, int skin,
                boolean keeper, int role, UUID uuid, String name, int trail) {}

    record Frame(long tick, Map<Integer, Snap> ents) {}

    record AnimEv(long tick, int id, Move move, int variant) {}

    private static final ArrayDeque<Frame> FRAMES = new ArrayDeque<>();
    private static final ArrayDeque<AnimEv> ANIMS = new ArrayDeque<>();
    private static List<Frame> stored;
    private static List<AnimEv> storedAnims;
    private static long captureAt = -1;
    private static boolean autoPlay;

    public static boolean active;
    private static int index;
    private static final Map<Integer, Entity> ACTORS = new HashMap<>();
    private static CameraType savedCamera;
    public static Vec3 focus = Vec3.ZERO;

    public static boolean hasStored() { return stored != null && !stored.isEmpty(); }

    public static int progress() { return index; }

    public static int length() { return stored == null ? 0 : stored.size(); }

    public static boolean isActor(Entity e) {
        if (e instanceof ReplayPlayer) return true;
        if (e instanceof FootballerEntity f) return f.replayDriver != null;
        if (e instanceof FootballEntity b) return b.replayDriver != null;
        return false;
    }

    // ------------------------------------------------------------------ recording

    public static void onAnim(LivingEntity e, Move m, int variant) {
        if (active || isActor(e)) return;
        ANIMS.addLast(new AnimEv(ClientState.clientTicks, e.getId(), m, variant));
        while (ANIMS.size() > 400) ANIMS.removeFirst();
    }

    public static void onGoal() {
        captureAt = ClientState.clientTicks + 28;
        autoPlay = true;
    }

    private static void record(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        Vec3 center = ClientState.hasPitch ? new Vec3(ClientState.px + 0.5, ClientState.py + 1, ClientState.pz + 0.5) : mc.player.position();
        double range = ClientState.hasPitch ? 60 : 40;
        Map<Integer, Snap> ents = new HashMap<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (isActor(e)) continue;
            if (Math.abs(e.getX() - center.x) > range || Math.abs(e.getZ() - center.z) > range) continue;
            if (e instanceof FootballEntity b) {
                ents.put(e.getId(), new Snap(0, e.getX(), e.getY(), e.getZ(), 0, 0, 0, 0, 0, 0, false, 0, null, "", b.getTrail()));
            } else if (e instanceof FootballerEntity f) {
                ents.put(e.getId(), new Snap(1, e.getX(), e.getY(), e.getZ(), f.yBodyRot, f.yHeadRot, f.getXRot(), f.getFootTeam().ordinal(),
                        f.getNumber(), f.getSkin(), f.isKeeper(), f.getRole().ordinal(), null, f.baseName, 0));
            } else if (e instanceof AbstractClientPlayer p && !p.isSpectator()) {
                ents.put(e.getId(), new Snap(2, e.getX(), e.getY(), e.getZ(), p.yBodyRot, p.yHeadRot, p.getXRot(), 0, 0, 0, false, 0,
                        p.getUUID(), p.getGameProfile().getName(), 0));
            }
        }
        FRAMES.addLast(new Frame(ClientState.clientTicks, ents));
        while (FRAMES.size() > CAP) FRAMES.removeFirst();
    }

    private static void capture() {
        List<Frame> all = new ArrayList<>(FRAMES);
        int from = Math.max(0, all.size() - LENGTH);
        stored = new ArrayList<>(all.subList(from, all.size()));
        long t0 = stored.isEmpty() ? 0 : stored.get(0).tick();
        storedAnims = new ArrayList<>();
        for (AnimEv a : ANIMS) if (a.tick() >= t0) storedAnims.add(a);
    }

    // ------------------------------------------------------------------ playback

    public static void toggle() {
        if (active) stop();
        else if (hasStored()) start();
    }

    public static void tick(Minecraft mc) {
        if (mc.level == null) {
            if (active) stop();
            FRAMES.clear();
            return;
        }
        if (captureAt >= 0 && ClientState.clientTicks >= captureAt) {
            captureAt = -1;
            capture();
            if (autoPlay && !active) start();
        }
        if (!active) {
            record(mc);
            return;
        }
        index++;
        if (index >= stored.size() || ClientState.state == 1) {
            stop();
            return;
        }
        long t = stored.get(index).tick();
        for (AnimEv a : storedAnims) {
            if (a.tick() != t) continue;
            Entity actor = ACTORS.get(a.id());
            if (actor instanceof LivingEntity le) ClientAnims.start(le, a.move(), a.variant());
        }
        Frame f = stored.get(index);
        for (Snap s : f.ents().values()) if (s.kind() == 0) focus = new Vec3(s.x(), s.y(), s.z());
    }

    private static void start() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || stored == null || stored.isEmpty()) return;
        active = true;
        index = 0;
        int next = -500000;
        Map<Integer, Snap> firstSeen = new LinkedHashMap<>();
        for (Frame f : stored) for (Map.Entry<Integer, Snap> e : f.ents().entrySet()) firstSeen.putIfAbsent(e.getKey(), e.getValue());
        for (Map.Entry<Integer, Snap> en : firstSeen.entrySet()) {
            int src = en.getKey();
            Snap s = en.getValue();
            Consumer<Entity> drv = e -> drive(e, src);
            Entity actor;
            if (s.kind() == 0) {
                FootballEntity b = ModEntities.BALL.get().create(level);
                if (b == null) continue;
                b.replayDriver = drv;
                actor = b;
            } else if (s.kind() == 1) {
                FootballerEntity f = ModEntities.FOOTBALLER.get().create(level);
                if (f == null) continue;
                f.setFootTeam(Team.byId(s.team()));
                f.setNumber(s.number());
                f.setSkin(s.skin());
                f.baseName = s.name();
                if (s.keeper()) f.setKeeper(true);
                f.setRole(Role.byId(s.role()));
                f.setNoGravity(true);
                f.replayDriver = drv;
                actor = f;
            } else {
                ReplayPlayer rp = new ReplayPlayer(level, new GameProfile(UUID.randomUUID(), s.name()), s.uuid());
                rp.driver = drv;
                actor = rp;
            }
            actor.setId(next--);
            actor.moveTo(s.x(), s.y(), s.z(), s.body(), s.pitch());
            if (actor instanceof AbstractClientPlayer p) level.addPlayer(actor.getId(), p);
            else level.putNonPlayerEntity(actor.getId(), actor);
            ACTORS.put(src, actor);
        }
        savedCamera = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        Frame f0 = stored.get(0);
        for (Snap s : f0.ents().values()) if (s.kind() == 0) focus = new Vec3(s.x(), s.y(), s.z());
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        active = false;
        autoPlay = false;
        if (mc.level != null) for (Entity e : ACTORS.values()) mc.level.removeEntity(e.getId(), Entity.RemovalReason.DISCARDED);
        ACTORS.clear();
        if (savedCamera != null) mc.options.setCameraType(savedCamera);
        savedCamera = null;
    }

    private static void drive(Entity e, int src) {
        if (!active || stored == null || index >= stored.size()) return;
        Snap s = stored.get(index).ents().get(src);
        if (s == null) {
            e.setInvisible(true);
            return;
        }
        e.setInvisible(false);
        e.setPos(s.x(), s.y(), s.z());
        e.setDeltaMovement(Vec3.ZERO);
        e.setYRot(s.body());
        e.setXRot(s.pitch());
        if (e instanceof LivingEntity le) {
            le.yBodyRot = s.body();
            le.yHeadRot = s.head();
        }
        if (e instanceof FootballEntity b && b.getTrail() != s.trail()) b.setTrail(s.trail());
    }

    /** stand-in for a real player; borrows the skin of the original */
    public static class ReplayPlayer extends RemotePlayer {
        public final UUID source;
        public Consumer<Entity> driver;

        public ReplayPlayer(ClientLevel level, GameProfile profile, UUID source) {
            super(level, profile);
            this.source = source;
        }

        @Nullable
        @Override
        protected PlayerInfo getPlayerInfo() {
            var conn = Minecraft.getInstance().getConnection();
            return conn == null || source == null ? null : conn.getPlayerInfo(source);
        }

        @Override
        public void tick() {
            if (driver != null) driver.accept(this);
            super.tick();
        }

        @Override
        public boolean shouldShowName() { return false; }
    }
}
