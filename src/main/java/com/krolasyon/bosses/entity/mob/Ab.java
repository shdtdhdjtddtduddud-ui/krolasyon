package com.krolasyon.bosses.entity.mob;

/** One ability of a {@link MobSpec}: a kind of behaviour plus tuning numbers. Index 0 of every spec is the basic melee. */
public final class Ab {
    public enum Kind {
        MELEE, SLAM, LEAP, CHARGE, BOLT, VOLLEY, BEAM, CONE, BURST, SUMMON, TELEPORT_STRIKE, BLINK_AWAY,
        HEAL_ALLIES, DRAIN, PULL, CLOUD, ERUPT, BUFF, METEOR, DIVE, SHIELD, RETREAT, SNIPE
    }

    public final Kind kind;
    public final String anim;
    public int dur = 20, cd = 100, weight = 3;
    public int[] hits = {8};
    public double minR = 0, maxR = 8;
    public float dmg = 1.0F;
    public double a, b;
    public int color = 0xFFFFFF;
    public String particle = "dust";
    public String effect = "";
    public int effTicks = 60, effLevel = 0;
    public String summon = "";
    public boolean needLos = false;
    public boolean airborne = false;
    public boolean phase2Only = false;
    public String sound = "";

    private Ab(Kind kind, String anim) {
        this.kind = kind;
        this.anim = anim;
    }

    public static Ab of(Kind kind, String anim) { return new Ab(kind, anim); }

    public Ab time(int dur, int... hits) { this.dur = dur; if (hits.length > 0) this.hits = hits; return this; }
    public Ab cd(int cd) { this.cd = cd; return this; }
    public Ab w(int weight) { this.weight = weight; return this; }
    public Ab range(double min, double max) { this.minR = min; this.maxR = max; return this; }
    public Ab dmg(float d) { this.dmg = d; return this; }
    public Ab p(double a, double b) { this.a = a; this.b = b; return this; }
    public Ab color(int c) { this.color = c; return this; }
    public Ab fx(String particle) { this.particle = particle; return this; }
    public Ab eff(String id, int ticks, int level) { this.effect = id; this.effTicks = ticks; this.effLevel = level; return this; }
    public Ab summon(String id) { this.summon = id; return this; }
    public Ab los() { this.needLos = true; return this; }
    public Ab air() { this.airborne = true; return this; }
    public Ab p2() { this.phase2Only = true; return this; }
    public Ab snd(String s) { this.sound = s; return this; }
}
