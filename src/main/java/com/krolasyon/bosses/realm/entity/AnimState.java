package com.krolasyon.bosses.realm.entity;

/** client-side animation state of a keyframe animated realm creature */
public class AnimState {
    public int id = -1;
    public float start;
    public float runBlend, prevRunBlend;

    public void play(int id, float tick) {
        this.id = id;
        this.start = tick;
    }

    public void tick(boolean running) {
        prevRunBlend = runBlend;
        runBlend += ((running ? 1F : 0F) - runBlend) * 0.18F;
    }

    public interface Holder {
        AnimState anim();
    }
}
