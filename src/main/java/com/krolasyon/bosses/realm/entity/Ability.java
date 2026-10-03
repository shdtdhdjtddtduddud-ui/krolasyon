package com.krolasyon.bosses.realm.entity;

/**
 * One attack of a realm creature. {@code anim} is the model animation slot (2 attack, 3 cast, 4 special),
 * {@code hit} the tick the effect happens, {@code power} a multiplier of the creature's attack damage.
 */
public record Ability(Type type, int anim, int duration, int hit, int cooldown, float minRange, float maxRange, float power,
                      float radius, int count, Element element, String summon, int weight) {

    public enum Type {
        MELEE, LEAP, CHARGE, BOLT, BOMB, SLAM, ERUPT, SUMMON, BLINK, DRAIN, BEAM, AURA, HEAL, PULL, EXPLODE, SHIELD, WEB, STUN,
        METEOR, SPIN, VANISH, ROAR, BREATH, NOVA
    }

    public enum Element {
        FIRE(0xFF7A20, 0xFFD060), BLOOD(0xC01020, 0xFF6070), SHADOW(0x7A30E0, 0xD0A0FF), SOUL(0x30C8F0, 0xC0FFFF),
        BRIMSTONE(0xE0A020, 0xFFF080), CRYSTAL(0xE070FF, 0xFFE0FF), POISON(0x60C020, 0xD0FF80), BONE(0xE8E0D0, 0xFFFFFF);

        public final int color, light;

        Element(int color, int light) {
            this.color = color;
            this.light = light;
        }
    }

    public boolean melee() { return type == Type.MELEE; }
}
