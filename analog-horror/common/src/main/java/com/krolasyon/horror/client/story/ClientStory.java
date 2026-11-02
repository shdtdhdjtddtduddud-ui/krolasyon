package com.krolasyon.horror.client.story;

import com.krolasyon.horror.client.ClientFx;
import com.krolasyon.horror.registry.ModSounds;
import com.krolasyon.horror.story.StoryBridge;
import com.krolasyon.horror.story.StoryBridge.Msg;
import com.krolasyon.horror.story.StoryData;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Client half of the story: receives cues from the integrated server and runs cutscene cameras, cards and HUD state. */
public final class ClientStory {
    private ClientStory() {}

    static boolean active;
    static StoryData.Cutscene cutscene;
    static int csTick;
    static double ox, oy, oz;
    static Marker cam;
    static int tick;
    static float letterbox, fade, fadeTarget;
    static int chapter, chapterTick = -10000;
    static String objective = "";
    static int objectiveArg = -1, objectiveTick = -10000;
    static int caughtTick = -10000;

    public static boolean hideHud() { return cutscene != null || fade > 0.05F; }

    public static boolean inCutscene() { return cutscene != null; }

    static void play(String name, float vol) {
        Minecraft mc = Minecraft.getInstance();
        SoundEvent e = ModSounds.byName(name);
        if (e == null || mc.level == null || mc.player == null) return;
        mc.level.playLocalSound(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), e, SoundSource.MASTER, vol, 1F, false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (cutscene != null) endCutscene(false);
            active = false;
            return;
        }
        tick++;
        Msg m;
        while ((m = StoryBridge.TO_CLIENT.poll()) != null) handle(mc, m);
        letterbox = Mth.approach(letterbox, cutscene != null ? 1F : 0F, 0.06F);
        fade = Mth.approach(fade, fadeTarget, 0.07F);
        if (tick - caughtTick == 14) fadeTarget = 1F;
        if (cutscene != null) tickCutscene(mc);
    }

    private static void handle(Minecraft mc, Msg m) {
        switch (m.type()) {
            case "active" -> active = m.i() == 1;
            case "cutscene" -> {
                StoryData.Cutscene c = StoryData.CUTSCENES.get(m.s());
                if (c == null) {
                    StoryBridge.TO_SERVER.add(Msg.of("cs_done", m.s()));
                    return;
                }
                cutscene = c;
                csTick = 0;
                ox = m.x(); oy = m.y(); oz = m.z();
                cam = new Marker(EntityType.MARKER, mc.level);
                applyCamera(true);
                mc.setCameraEntity(cam);
                fadeTarget = 0F;
            }
            case "tape" -> mc.setScreen(new TapeScreen(m.s()));
            case "chapter" -> {
                chapter = m.i();
                chapterTick = tick;
                play("fx.static", 0.6F);
                play("hatman.hit", 0.5F);
            }
            case "chapter_quiet" -> chapter = m.i();
            case "objective" -> {
                objective = m.s();
                objectiveArg = m.i();
                objectiveTick = tick;
                if (!objective.isEmpty()) play("story.page", 0.7F);
            }
            case "caught" -> {
                caughtTick = tick;
                ClientFx.debugJumpscare();
            }
            case "fade_in" -> fadeTarget = 0F;
            case "credits" -> mc.setScreen(new CreditsScreen(m.s()));
            case "end" -> {
                active = false;
                objective = "";
                fadeTarget = 0F;
                if (cutscene != null) endCutscene(false);
            }
            default -> { }
        }
    }

    private static void tickCutscene(Minecraft mc) {
        for (StoryData.Cue c : cutscene.cues()) if (c.tick() == csTick) play(c.sound(), c.volume());
        if (csTick >= cutscene.length()) {
            endCutscene(true);
            return;
        }
        applyCamera(false);
        csTick++;
    }

    private static void applyCamera(boolean snap) {
        StoryData.Shot s = null;
        for (StoryData.Shot sh : cutscene.shots()) if (csTick >= sh.start() && csTick < sh.end()) s = sh;
        if (s == null) s = cutscene.shots()[cutscene.shots().length - 1];
        float u = Mth.clamp((csTick - s.start()) / (float) Math.max(1, s.end() - s.start()), 0F, 1F);
        u = u * u * (3 - 2 * u);
        double x = ox + Mth.lerp(u, s.x0(), s.x1()), y = oy + Mth.lerp(u, s.y0(), s.y1()), z = oz + Mth.lerp(u, s.z0(), s.z1());
        float yaw = s.yaw0() + Mth.wrapDegrees(s.yaw1() - s.yaw0()) * u, pitch = Mth.lerp(u, s.pitch0(), s.pitch1());
        boolean cut = snap || csTick == s.start();
        cam.xo = cut ? x : cam.getX();
        cam.yo = cut ? y : cam.getY();
        cam.zo = cut ? z : cam.getZ();
        cam.yRotO = cut ? yaw : cam.getYRot();
        cam.xRotO = cut ? pitch : cam.getXRot();
        cam.setPos(x, y, z);
        cam.setYRot(yaw);
        cam.setXRot(pitch);
    }

    private static void endCutscene(boolean report) {
        Minecraft mc = Minecraft.getInstance();
        String id = cutscene.id();
        cutscene = null;
        cam = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (report) StoryBridge.TO_SERVER.add(Msg.of("cs_done", id));
    }

    static StoryData.Line currentLine() {
        if (cutscene == null) return null;
        for (StoryData.Line l : cutscene.lines()) if (csTick >= l.start() && csTick < l.start() + l.dur()) return l;
        return null;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (cutscene == null && fade < 0.5F) return;
        var in = e.getInput();
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
        in.up = in.down = in.left = in.right = in.jumping = in.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onRenderHand(net.minecraftforge.client.event.RenderHandEvent e) {
        if (cutscene != null) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre e) {
        if (cutscene != null) e.setCanceled(true);
    }
}
