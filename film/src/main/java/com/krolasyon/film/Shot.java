package com.krolasyon.film;

import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Videodaki tek bir çekim: kayıtsız hazırlık, ardından kaydedilen {@link #len} tick. */
public final class Shot {
    public interface CamFn { Cam at(Director d, float t, float u); }
    public interface AimFn { float[] at(Director d, float t, float u); }
    public interface TickFn { void run(Director d, int t); }

    public final String name;
    public int len;
    public final List<Consumer<Director>> setup = new ArrayList<>();
    public int settle = 40, maxWait = 2400;
    public final Map<Integer, List<Consumer<Director>>> at = new HashMap<>();
    public TickFn every;
    public CamFn cam;
    public AimFn aim;
    /** 0: arayüz yok, 1: yalnızca boss çubuğu, 2: tam oyun arayüzü. */
    public int hud;
    public boolean hand, allowScreen;
    public float letterbox = 1F;
    public int fadeIn, fadeOut;
    public final List<Title> titles = new ArrayList<>();
    public String[] music;
    public float musicVol = 0.5F;
    public CameraType view = CameraType.FIRST_PERSON;
    public final List<Vec3> warm = new ArrayList<>();
    public final List<Object[]> accents = new ArrayList<>();

    public record Title(String text, String sub, int start, int dur, int style, int color) {}

    public Shot(String name, int len) {
        this.name = name;
        this.len = len;
    }

    public Shot setup(Consumer<Director> c) { setup.add(c); return this; }
    public Shot at(int t, Consumer<Director> c) { at.computeIfAbsent(t, k -> new ArrayList<>()).add(c); return this; }
    public Shot every(TickFn f) { every = f; return this; }
    public Shot cam(CamFn f) { cam = f; return this; }
    public Shot aim(AimFn f) { aim = f; return this; }
    public Shot play() { hud = 2; hand = true; letterbox = 0F; return this; }
    public Shot hud(int h) { hud = h; return this; }
    public Shot letterbox(float l) { letterbox = l; return this; }
    public Shot fade(int in, int out) { fadeIn = in; fadeOut = out; return this; }
    public Shot view(CameraType v) { view = v; return this; }
    public Shot screen() { allowScreen = true; return this; }
    public Shot settle(int s) { settle = s; return this; }
    public Shot music(float vol, String... tracks) { music = tracks; musicVol = vol; return this; }
    public Shot warm(Vec3... p) { warm.addAll(List.of(p)); return this; }
    /** Büyük bölüm başlığı. */
    public Shot chapter(String text, String sub, int start, int dur) { titles.add(new Title(text, sub, start, dur, 0, 0xF2C14E)); return this; }
    public Shot title(String text, String sub, int start, int dur, int style, int color) { titles.add(new Title(text, sub, start, dur, style, color)); return this; }
    /** Sonradan karıştırılacak vurgu sesi (konumsuz). */
    public Shot accent(int t, String sound, float vol, float pitch) { accents.add(new Object[]{t, sound, vol, pitch}); return this; }

    void scale(double k) {
        if (k == 1.0) return;
        len = (int) Math.round(len * k);
        Map<Integer, List<Consumer<Director>>> m = new HashMap<>();
        at.forEach((t, l) -> m.computeIfAbsent((int) Math.round(t * k), x -> new ArrayList<>()).addAll(l));
        at.clear();
        at.putAll(m);
        List<Title> tt = new ArrayList<>();
        for (Title t : titles) tt.add(new Title(t.text, t.sub, (int) Math.round(t.start * k), (int) Math.round(t.dur * k), t.style, t.color));
        titles.clear();
        titles.addAll(tt);
        for (Object[] a : accents) a[0] = (int) Math.round((Integer) a[0] * k);
        fadeIn = (int) Math.round(fadeIn * k);
        fadeOut = (int) Math.round(fadeOut * k);
    }
}
