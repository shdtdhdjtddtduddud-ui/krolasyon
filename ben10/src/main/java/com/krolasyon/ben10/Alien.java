package com.krolasyon.ben10;

import net.minecraft.network.chat.Component;

import java.util.Locale;

/** The nine Omnitrix forms with their physical stats and four ability slots (cooldown / duration in ticks). */
public enum Alien {
    //          colour     height width hp  speed  dmg armor kb    flags                                   block  cooldowns                durations
    HEATBLAST(0xFFB020, 1.95F, 0.7F, 6, 1.05F, 4, 4, 0.1F, Flag.FIRE_IMMUNE | Flag.FLY, 0.65F, new int[]{16, 120, 400, 200}, new int[]{12, 44, 32, 20}),
    FOURARMS(0xD02030, 2.6F, 0.9F, 20, 1.0F, 9, 8, 0.8F, Flag.HALF_FALL | Flag.BIG_JUMP, 0.8F, new int[]{160, 120, 200, 160}, new int[]{24, 18, 60, 40}),
    DIAMONDHEAD(0x7CF0B0, 2.3F, 0.8F, 14, 0.95F, 6, 14, 0.6F, 0, 0.85F, new int[]{24, 140, 260, 400}, new int[]{12, 24, 16, 50}),
    XLR8(0x2EA8C8, 1.8F, 0.7F, 4, 2.6F, 4, 4, 0.2F, Flag.WATER_RUN | Flag.HALF_FALL, 0.5F, new int[]{60, 260, 200, 600}, new int[]{8, 60, 36, 120}),
    GOOP(0x7EE030, 2.0F, 0.7F, 10, 1.0F, 3, 2, 0.5F, Flag.NO_FALL | Flag.POISON_IMMUNE, 0.7F, new int[]{20, 80, 260, 300}, new int[]{12, 14, 60, 30}),
    CANNONBOLT(0x9AAE4E, 1.9F, 0.9F, 16, 0.9F, 5, 12, 0.9F, Flag.NO_FALL, 0.9F, new int[]{200, 160, 220, 300}, new int[]{100, 30, 60, 50}),
    BIGCHILL(0x2878B8, 2.2F, 0.8F, 8, 1.1F, 4, 4, 0.2F, Flag.FLY | Flag.FREEZE_IMMUNE | Flag.NO_FALL, 0.7F, new int[]{140, 240, 40, 420}, new int[]{50, 20, 14, 100}),
    ECHOECHO(0x2E80E0, 2.3F, 0.8F, 10, 1.05F, 4, 6, 0.4F, Flag.FLY | Flag.NO_FALL, 0.75F, new int[]{60, 160, 260, 500}, new int[]{14, 50, 30, 50}),
    GHOSTFREAK(0xB0B0B0, 2.0F, 0.7F, 4, 1.1F, 5, 2, 0.3F, Flag.FLY | Flag.NO_FALL | Flag.NIGHT_VISION, 0.6F, new int[]{40, 260, 400, 300}, new int[]{14, 30, 120, 100});

    public static final class Flag {
        public static final int FIRE_IMMUNE = 1, FLY = 2, HALF_FALL = 4, NO_FALL = 8, WATER_RUN = 16, POISON_IMMUNE = 32,
                FREEZE_IMMUNE = 64, NIGHT_VISION = 128, BIG_JUMP = 256;

        private Flag() {}
    }

    public final int color;
    public final float height, width;
    public final int bonusHealth;
    public final float speed;
    public final int damage, armor;
    public final float knockbackRes;
    public final int flags;
    public final float blockReduction;
    public final int[] cooldowns, durations;
    public final String id;

    Alien(int color, float height, float width, int hp, float speed, int dmg, int armor, float kb, int flags, float block, int[] cd, int[] dur) {
        this.color = color;
        this.height = height;
        this.width = width;
        this.bonusHealth = hp;
        this.speed = speed;
        this.damage = dmg;
        this.armor = armor;
        this.knockbackRes = kb;
        this.flags = flags;
        this.blockReduction = block;
        this.cooldowns = cd;
        this.durations = dur;
        this.id = name().toLowerCase(Locale.ROOT);
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public Component displayName() {
        return Component.translatable("alien.krolben10." + id);
    }

    public Component abilityName(int slot) {
        return Component.translatable("ability.krolben10." + id + "." + (slot + 1));
    }

    public static Alien byId(int i) {
        return i < 0 || i >= values().length ? null : values()[i];
    }
}
