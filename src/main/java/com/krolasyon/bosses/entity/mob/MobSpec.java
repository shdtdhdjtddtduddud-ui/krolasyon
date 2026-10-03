package com.krolasyon.bosses.entity.mob;

import com.krolasyon.bosses.faction.Faction;

import java.util.ArrayList;
import java.util.List;

/** Data description of one ordinary Azrakor creature (everything the generic {@code HellMob} needs). */
public final class MobSpec {
    public final String id;
    public final Faction faction;
    public double hp = 30, armor = 0, attack = 4, speed = 0.25, follow = 32, kbResist = 0;
    public float width = 0.8F, height = 1.9F;
    public boolean flying = false, fireImmune = true;
    public double hoverHeight = 2.0;
    public float modelScale = 1.0F;
    public float shadow = 0.5F;
    public double reach = 2.0, walkAnim = 1.0, runAnim = 1.0;
    public int xp = 10, deathTicks = 24;
    public String head = "head";
    public String voice = "demon";
    public float pitch = 1.0F, volume = 1.0F;
    public int eggA = 0x333333, eggB = 0xCC3333;
    public int spawnWeight = 6, groupMin = 1, groupMax = 2;
    public String[] biomes = {};
    public final List<Ab> abilities = new ArrayList<>();
    public MobSpec(String id, Faction faction) {
        this.id = id;
        this.faction = faction;
    }

    public MobSpec stats(double hp, double armor, double attack, double speed) {
        this.hp = hp; this.armor = armor; this.attack = attack; this.speed = speed;
        return this;
    }

    public MobSpec size(float w, float h, float scale) { this.width = w; this.height = h; this.modelScale = scale; return this; }
    public MobSpec fly(double hover) { this.flying = true; this.hoverHeight = hover; return this; }
    public MobSpec reach(double r) { this.reach = r; return this; }
    public MobSpec kb(double k) { this.kbResist = k; return this; }
    public MobSpec follow(double f) { this.follow = f; return this; }
    public MobSpec anim(double walk, double run) { this.walkAnim = walk; this.runAnim = run; return this; }
    public MobSpec voice(String v, float pitch) { this.voice = v; this.pitch = pitch; return this; }
    public MobSpec egg(int a, int b) { this.eggA = a; this.eggB = b; return this; }
    public MobSpec spawn(int weight, int min, int max, String... biomes) { this.spawnWeight = weight; this.groupMin = min; this.groupMax = max; this.biomes = biomes; return this; }
    public MobSpec xp(int xp, int deathTicks) { this.xp = xp; this.deathTicks = deathTicks; return this; }
    public MobSpec head(String h) { this.head = h; return this; }
    public MobSpec shadow(float s) { this.shadow = s; return this; }

    public MobSpec ab(Ab ab) { this.abilities.add(ab); return this; }
}
