package com.krolasyon.omnitrix;

import net.minecraft.network.chat.Component;

/** 4 abilities per alien (slot 0-3, keys Z X C V by default). duration = animation length in ticks. */
public enum Ability {
    FIREBALL(Alien.HEATBLAST, 0, 16, 10),
    FLAMETHROWER(Alien.HEATBLAST, 1, 120, 34),
    SUPERNOVA(Alien.HEATBLAST, 2, 420, 30),
    FIRE_JET(Alien.HEATBLAST, 3, 160, 16),

    WIND_GUST(Alien.TERRASPIN, 0, 40, 14),
    TORNADO(Alien.TERRASPIN, 1, 300, 30),
    SPIN_FLIGHT(Alien.TERRASPIN, 2, 220, 20),
    SHELL_SLAM(Alien.TERRASPIN, 3, 240, 24),

    CRYSTAL_SHARDS(Alien.DIAMONDHEAD, 0, 24, 12),
    CRYSTAL_BLADES(Alien.DIAMONDHEAD, 1, 360, 18),
    SPIKE_WAVE(Alien.DIAMONDHEAD, 2, 160, 20),
    CRYSTAL_PRISON(Alien.DIAMONDHEAD, 3, 400, 22),

    ACID_SPIT(Alien.GOOP, 0, 14, 10),
    STRETCH_GRAB(Alien.GOOP, 1, 120, 20),
    PUDDLE(Alien.GOOP, 2, 300, 16),
    ACID_BOMB(Alien.GOOP, 3, 200, 18),

    SPEED_DASH(Alien.XLR8, 0, 40, 10),
    BLITZ(Alien.XLR8, 1, 220, 40),
    VORTEX_RUN(Alien.XLR8, 2, 300, 40),
    OVERDRIVE(Alien.XLR8, 3, 600, 16),

    THUNDER_CLAP(Alien.FOURARMS, 0, 100, 18),
    GROUND_POUND(Alien.FOURARMS, 1, 160, 30),
    QUAD_COMBO(Alien.FOURARMS, 2, 60, 24),
    BOULDER_THROW(Alien.FOURARMS, 3, 140, 22),

    FREEZE_BREATH(Alien.BIGCHILL, 0, 100, 30),
    PHASE(Alien.BIGCHILL, 1, 400, 16),
    ICE_PRISON(Alien.BIGCHILL, 2, 300, 18),
    ICE_FLIGHT(Alien.BIGCHILL, 3, 400, 16),

    SONIC_DISCS(Alien.ULTIMATE_ECHO_ECHO, 0, 80, 14),
    SONIC_DOOM(Alien.ULTIMATE_ECHO_ECHO, 1, 400, 30),
    SONIC_SCREAM(Alien.ULTIMATE_ECHO_ECHO, 2, 120, 22),
    DISC_RIDE(Alien.ULTIMATE_ECHO_ECHO, 3, 300, 14),

    INTANGIBLE(Alien.GHOSTFREAK, 0, 300, 16),
    POSSESS(Alien.GHOSTFREAK, 1, 600, 24),
    TENTACLES(Alien.GHOSTFREAK, 2, 80, 22),
    TERROR(Alien.GHOSTFREAK, 3, 240, 26);

    public static final Ability[] VALUES = values();

    public final Alien alien;
    public final int slot, cooldown, duration;

    Ability(Alien alien, int slot, int cooldown, int duration) {
        this.alien = alien;
        this.slot = slot;
        this.cooldown = cooldown;
        this.duration = duration;
    }

    public static Ability of(Alien a, int slot) {
        for (Ability ab : VALUES) if (ab.alien == a && ab.slot == slot) return ab;
        return null;
    }

    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }

    public Component displayName() { return Component.translatable("ability.omnitrix." + key()); }

    public Component description() { return Component.translatable("ability.omnitrix." + key() + ".desc"); }

    public static Ability byId(int i) { return i >= 0 && i < VALUES.length ? VALUES[i] : null; }
}
