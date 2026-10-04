package com.rabona.arena.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.rabona.arena.client.anim.Anim;
import com.rabona.arena.entity.BallEntity;
import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.game.Move;
import com.rabona.arena.game.Pitch;
import com.rabona.arena.game.Pos;
import com.rabona.arena.game.Team;
import com.rabona.arena.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Gol tekrari. Istemci son ~10 saniyeyi kaydeder; gol olunca sahne kopya varliklarla (kukla) yeniden
 * oynatilir. Gercek oyuncular tekrar sirasinda gizlenir, kamera topun etrafinda doner, son anlar agir cekim.
 */
public final class Replay {
    private record Snap(int id, int kind, UUID uuid, String name, double x, double y, double z, float yRot, float yHead,
                        float yBody, float xRot, int anim, float animT, boolean mirror, int team, int num, int skin, int pos,
                        Quaternionf rot, int fx, float walkPos, float walkSpeed) {}

    private static final int KEEP = 200, LENGTH = 150;
    private static final ArrayDeque<List<Snap>> BUFFER = new ArrayDeque<>();
    private static List<List<Snap>> clip;
    private static boolean playing;
    private static float cursor;
    private static int startAt = -1;
    private static final Map<Integer, Entity> PUPPETS = new HashMap<>();
    private static final Set<Entity> PUPPET_SET = Collections.newSetFromMap(new IdentityHashMap<>());
    private static Vec3 focus;
    private static Field[] walkFields;

    private Replay() {}

    public static boolean playing() { return playing; }

    public static float cursor() { return cursor; }

    public static Vec3 focus() { return focus; }

    public static boolean hasClip() { return clip != null && !clip.isEmpty(); }

    public static boolean isPuppet(Entity e) { return PUPPET_SET.contains(e); }

    public static boolean slowMo() { return playing && clip != null && cursor > clip.size() - 45; }

    // ================================================================ kayit
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            BUFFER.clear();
            stop();
            return;
        }
        if (startAt >= 0 && ClientState.clientTicks >= startAt) {
            startAt = -1;
            start();
        }
        if (playing) {
            cursor += slowMo() ? 0.4f : 1f;
            if (cursor >= clip.size() - 1) stop();
            return;
        }
        Pitch pitch = ClientState.pitch;
        if (pitch == null || ClientState.match == null || ClientState.match.phase() != 2) return;
        AABB area = new AABB(pitch.world(-Pitch.HALF_LEN - 8, -Pitch.HALF_WID - 8, pitch.surfaceY() - 4),
                pitch.world(Pitch.HALF_LEN + 8, Pitch.HALF_WID + 8, pitch.surfaceY() + 20)).inflate(1);
        List<Snap> frame = new ArrayList<>();
        for (Entity e : mc.level.getEntities((Entity) null, area, en -> en instanceof BallEntity || en instanceof FootballerEntity || en instanceof AbstractClientPlayer)) {
            if (e.isInvisible() && !(e instanceof BallEntity)) continue;
            frame.add(snap(e));
        }
        BUFFER.addLast(frame);
        while (BUFFER.size() > KEEP) BUFFER.removeFirst();
    }

    private static Snap snap(Entity e) {
        int anim = -1;
        float animT = 0;
        boolean mir = false;
        int kind, team = 0, num = 0, skin = 0, pos = 0, fx = 0;
        float yHead = e.getYHeadRot(), yBody = e.getYRot(), wp = 0, ws = 0;
        Quaternionf rot = null;
        String name = e.getName().getString();
        if (e instanceof LivingEntity le) {
            ClientAnims.Active a = ClientAnims.get(le, le.tickCount);
            if (a != null) {
                anim = a.move().ordinal();
                animT = le.tickCount - a.start();
                mir = a.mirror();
            }
            yBody = le.yBodyRot;
            wp = le.walkAnimation.position();
            ws = le.walkAnimation.speed();
        }
        if (e instanceof BallEntity b) {
            kind = 0;
            rot = new Quaternionf(b.rot);
            skin = b.getSkin();
            fx = b.getEffect();
        } else if (e instanceof FootballerEntity f) {
            kind = 1;
            team = f.getSquad().ordinal();
            num = f.getNumber();
            skin = f.getSkinId();
            pos = f.getFieldPos().ordinal();
            name = f.getBaseName();
        } else {
            kind = 2;
        }
        return new Snap(e.getId(), kind, e.getUUID(), name, e.getX(), e.getY(), e.getZ(), e.getYRot(), yHead, yBody, e.getXRot(),
                anim, animT, mir, team, num, skin, pos, rot, fx, wp, ws);
    }

    /** Gol oldu: son anlari klibe al, otomatik oynat. */
    public static void onGoal() {
        if (BUFFER.size() < 20) return;
        List<List<Snap>> all = new ArrayList<>(BUFFER);
        clip = new ArrayList<>(all.subList(Math.max(0, all.size() - LENGTH), all.size()));
        if (ClientState.autoReplay) startAt = ClientState.clientTicks + 45;
    }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        if (playing) {
            stop();
        } else if (hasClip()) {
            start();
        } else if (mc.player != null) {
            mc.player.displayClientMessage(Component.translatable("msg.rabonaarena.no_replay"), true);
        }
    }

    private static void start() {
        if (!hasClip()) return;
        playing = true;
        cursor = 0;
    }

    public static void stop() {
        playing = false;
        startAt = -1;
        PUPPETS.clear();
        PUPPET_SET.clear();
        focus = null;
    }

    // ================================================================ oynatma
    public static void render(PoseStack ps, float partial) {
        if (!playing || clip == null) return;
        Minecraft mc = Minecraft.getInstance();
        int i = Math.min(clip.size() - 1, (int) cursor);
        int j = Math.min(clip.size() - 1, i + 1);
        float f = (cursor - (int) cursor) + (slowMo() ? partial * 0.4f : partial);
        f = Math.min(1, f);
        List<Snap> a = clip.get(i);
        Map<Integer, Snap> next = new HashMap<>();
        for (Snap s : clip.get(j)) next.put(s.id(), s);
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        EntityRenderDispatcher disp = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        int tickBase = 100000 + i;
        for (Snap s : a) {
            Snap n = next.getOrDefault(s.id(), s);
            Entity e = puppet(s);
            if (e == null) continue;
            double x = s.x() + (n.x() - s.x()) * f, y = s.y() + (n.y() - s.y()) * f, z = s.z() + (n.z() - s.z()) * f;
            e.setPos(x, y, z);
            e.xo = x;
            e.yo = y;
            e.zo = z;
            e.xOld = x;
            e.yOld = y;
            e.zOld = z;
            e.tickCount = tickBase;
            float yaw = lerpDeg(s.yRot(), n.yRot(), f);
            e.setYRot(yaw);
            e.yRotO = yaw;
            e.setXRot(s.xRot());
            e.xRotO = s.xRot();
            if (e instanceof LivingEntity le) {
                le.yBodyRot = le.yBodyRotO = lerpDeg(s.yBody(), n.yBody(), f);
                le.yHeadRot = le.yHeadRotO = lerpDeg(s.yHead(), n.yHead(), f);
                setWalk(le.walkAnimation, s.walkSpeed() + (n.walkSpeed() - s.walkSpeed()) * f, s.walkPos() + (n.walkPos() - s.walkPos()) * f);
                if (s.anim() >= 0) {
                    Move m = Move.byId(s.anim());
                    ClientAnims.playAt(le, m, tickBase - s.animT(), s.mirror());
                } else {
                    ClientAnims.clearFor(le);
                }
            }
            if (e instanceof BallEntity b) {
                if (s.rot() != null) {
                    b.rotO.set(s.rot());
                    b.rot.set(n.rot() != null ? n.rot() : s.rot());
                }
                focus = new Vec3(x, y, z);
                b.setEffect(s.fx(), 0);
            }
            ps.pushPose();
            int light = disp.getPackedLightCoords(e, f);
            disp.render(e, x - cam.x, y - cam.y, z - cam.z, yaw, f, ps, buf, light);
            ps.popPose();
        }
        buf.endBatch();
    }

    private static Entity puppet(Snap s) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = PUPPETS.get(s.id());
        if (e != null) return e;
        switch (s.kind()) {
            case 0 -> {
                BallEntity b = new BallEntity(ModEntities.BALL.get(), mc.level);
                b.setSkin(s.skin());
                e = b;
            }
            case 1 -> {
                FootballerEntity f = new FootballerEntity(ModEntities.FOOTBALLER.get(), mc.level);
                f.setup(Team.byId(s.team()), s.num(), s.skin(), s.name(), 80);
                f.setFieldPos(Pos.byId(s.pos()));
                f.setCustomNameVisible(false);
                e = f;
            }
            default -> {
                RemotePlayer p = new RemotePlayer(mc.level, new GameProfile(s.uuid(), s.name()));
                e = p;
            }
        }
        PUPPETS.put(s.id(), e);
        PUPPET_SET.add(e);
        return e;
    }

    private static void setWalk(WalkAnimationState w, float speed, float pos) {
        try {
            if (walkFields == null) {
                List<Field> fs = new ArrayList<>();
                for (Field fl : WalkAnimationState.class.getDeclaredFields()) {
                    if (fl.getType() == float.class) {
                        fl.setAccessible(true);
                        fs.add(fl);
                    }
                }
                walkFields = fs.toArray(new Field[0]);
            }
            if (walkFields.length == 3) {
                walkFields[0].setFloat(w, speed);
                walkFields[1].setFloat(w, speed);
                walkFields[2].setFloat(w, pos);
            }
        } catch (Throwable ignored) {
        }
    }

    private static float lerpDeg(float a, float b, float t) {
        float d = ((b - a) % 360 + 540) % 360 - 180;
        return a + d * t;
    }

    static float animLength(Move m) {
        Anim a = com.rabona.arena.client.anim.AnimLibrary.get(m);
        return a == null ? 0 : a.length;
    }
}
