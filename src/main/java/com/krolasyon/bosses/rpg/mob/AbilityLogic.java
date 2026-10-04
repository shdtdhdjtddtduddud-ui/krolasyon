package com.krolasyon.bosses.rpg.mob;

import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs.Ability;
import com.krolasyon.bosses.rpg.entity.MagicBolt;
import com.krolasyon.bosses.rpg.entity.MagicBolt.OnHit;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.util.TempBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Behaviour of every monster / boss ability. Each ability runs for {@code duration} ticks; the main effect usually lands at {@code windup}. */
public final class AbilityLogic {
    private AbilityLogic() {}

    public enum Style { SHOOT, RAISE, LUNGE, ROAR, SPIN, SLAM, BREATH, BURROW, SELF, SWIPE }

    public record Info(int cooldown, float minRange, float maxRange, int windup, int duration, Style style, int color) {}

    public static Info info(Ability a) {
        return switch (a) {
            case FIREBALL -> new Info(70, 3, 24, 12, 20, Style.SHOOT, 0xFF6A1A);
            case ICE_SHARD -> new Info(60, 2, 20, 10, 18, Style.SHOOT, 0x90E0FF);
            case POISON_SPIT -> new Info(60, 2, 16, 10, 16, Style.SHOOT, 0x70E020);
            case ACID_SPIT -> new Info(70, 2, 16, 10, 16, Style.SHOOT, 0xC0FF30);
            case SHADOW_BOLT -> new Info(70, 3, 24, 14, 22, Style.RAISE, 0x8040C0);
            case LIGHTNING_ORB -> new Info(90, 3, 22, 16, 24, Style.RAISE, 0xA0D0FF);
            case ROCK_THROW -> new Info(80, 4, 22, 16, 24, Style.SLAM, 0x8A7A60);
            case BONE_SPEAR -> new Info(60, 3, 24, 10, 16, Style.SHOOT, 0xF0E8D0);
            case WATER_JET -> new Info(80, 2, 14, 10, 26, Style.BREATH, 0x3090FF);
            case ARCANE_MISSILES -> new Info(90, 3, 22, 8, 30, Style.RAISE, 0xC060FF);
            case BLOOD_BOLT -> new Info(70, 3, 20, 12, 20, Style.RAISE, 0xC01020);
            case SPORE_SHOT -> new Info(60, 2, 16, 10, 16, Style.SHOOT, 0xA0C040);
            case WEB_SHOT -> new Info(110, 3, 18, 10, 16, Style.SHOOT, 0xF0F0F0);
            case HOLY_BOLT -> new Info(70, 3, 24, 12, 20, Style.RAISE, 0xFFF0A0);
            case SAND_BLAST -> new Info(80, 1, 9, 12, 22, Style.BREATH, 0xE0C080);
            case FIRE_NOVA -> new Info(140, 0, 6, 18, 30, Style.ROAR, 0xFF5010);
            case FROST_NOVA -> new Info(140, 0, 6, 18, 30, Style.ROAR, 0x90E0FF);
            case SHOCKWAVE -> new Info(130, 0, 7, 16, 28, Style.SLAM, 0xD0C0A0);
            case POISON_CLOUD -> new Info(160, 0, 14, 14, 22, Style.RAISE, 0x80D020);
            case QUAKE -> new Info(150, 0, 8, 20, 34, Style.SLAM, 0x8A6A40);
            case SPORE_BURST -> new Info(150, 0, 6, 16, 26, Style.ROAR, 0xC0E060);
            case SANDSTORM -> new Info(260, 0, 10, 14, 100, Style.RAISE, 0xE0C080);
            case THUNDERSTORM -> new Info(220, 3, 20, 20, 70, Style.RAISE, 0xA0C0FF);
            case METEOR -> new Info(200, 4, 26, 24, 50, Style.RAISE, 0xFF4010);
            case ROOTS -> new Info(160, 2, 14, 14, 24, Style.SLAM, 0x60A030);
            case VORTEX_PULL -> new Info(200, 3, 12, 12, 60, Style.RAISE, 0x9080C0);
            case SONIC_SCREAM -> new Info(140, 0, 10, 14, 26, Style.ROAR, 0x60E0FF);
            case FEAR_ROAR -> new Info(200, 0, 9, 14, 30, Style.ROAR, 0xFF4040);
            case BLIND_FLASH -> new Info(200, 0, 10, 16, 24, Style.RAISE, 0xFFFFE0);
            case CURSE -> new Info(180, 2, 16, 16, 24, Style.RAISE, 0x8A2AAA);
            case DARK_PULSE -> new Info(150, 0, 7, 18, 28, Style.ROAR, 0x5A1A8A);
            case LEAP_SLAM -> new Info(110, 4, 14, 10, 34, Style.LUNGE, 0xC0A080);
            case CHARGE -> new Info(120, 5, 20, 14, 36, Style.LUNGE, 0xE0D0B0);
            case BLINK_BEHIND -> new Info(140, 3, 20, 10, 18, Style.SELF, 0xC060FF);
            case BURROW -> new Info(220, 3, 20, 16, 50, Style.BURROW, 0x8A6A40);
            case DIVE_BOMB -> new Info(120, 3, 20, 14, 40, Style.LUNGE, 0xE0E0FF);
            case SHADOW_STEP -> new Info(160, 3, 22, 10, 26, Style.SELF, 0x4A2A6A);
            case DODGE -> new Info(80, 0, 4, 2, 10, Style.SELF, 0xFFFFFF);
            case HEAL -> new Info(400, 0, 64, 20, 34, Style.SELF, 0x60FF60);
            case ENRAGE -> new Info(500, 0, 16, 16, 26, Style.ROAR, 0xFF2020);
            case STONE_SKIN -> new Info(400, 0, 16, 14, 22, Style.SELF, 0x9A9A90);
            case INVISIBILITY -> new Info(400, 0, 16, 10, 16, Style.SELF, 0x404050);
            case REGEN_AURA -> new Info(360, 0, 16, 16, 26, Style.RAISE, 0x60FF90);
            case SUMMON -> new Info(500, 0, 24, 24, 36, Style.RAISE, 0xB070FF);
            case TAIL_SWEEP -> new Info(90, 0, 4.5F, 10, 20, Style.SPIN, 0xFFFFFF);
            case SPIN_ATTACK -> new Info(110, 0, 4, 8, 30, Style.SPIN, 0xFFFFFF);
            case LIFE_DRAIN -> new Info(140, 1, 10, 10, 50, Style.RAISE, 0xC01030);
            case STEAL -> new Info(160, 0, 3, 6, 14, Style.SWIPE, 0xF0D040);
            case GRAB_THROW -> new Info(140, 0, 3.5F, 10, 24, Style.SWIPE, 0xFFFFFF);
            case BITE_BLEED -> new Info(70, 0, 3.2F, 6, 14, Style.LUNGE, 0xC01020);
            case FIRE_BREATH -> new Info(160, 1, 8, 12, 50, Style.BREATH, 0xFF6010);
            case FROST_BREATH -> new Info(160, 1, 8, 12, 50, Style.BREATH, 0xA0E8FF);
            case POISON_BREATH -> new Info(160, 1, 8, 12, 50, Style.BREATH, 0x80E020);
        };
    }

    /** is the ability something that moves the caster (so the cast goal must not freeze navigation / rotation) */
    public static boolean selfMoving(Ability a) {
        return a == Ability.LEAP_SLAM || a == Ability.CHARGE || a == Ability.DIVE_BOMB || a == Ability.DODGE || a == Ability.BURROW;
    }

    private static float pow(RpgMonster m) { return m.def().atk() * m.powerMultiplier(); }

    private static Vec3 center(LivingEntity e) { return e.position().add(0, e.getBbHeight() * 0.5, 0); }

    private static Vec3 mouth(RpgMonster m) {
        return m.position().add(0, m.getBbHeight() * 0.75, 0).add(m.forward().scale(m.getBbWidth() * 0.6));
    }

    private static MagicBolt bolt(RpgMonster m, int c1, int c2, float size, byte trail) {
        MagicBolt b = MagicBolt.create(m.level(), m, c1, c2, size * Math.max(1F, m.def().scale() * 0.6F), trail);
        Vec3 p = mouth(m);
        b.setPos(p.x, p.y, p.z);
        return b;
    }

    /** chargeup visuals before the main effect */
    private static void windupFx(RpgMonster m, Info in, int t) {
        if (!(m.level() instanceof ServerLevel)) return;
        if (t % 2 != 0) return;
        double r = m.getBbWidth() * 0.9 + 0.5;
        double a = t * 0.6;
        Vec3 c = m.position().add(0, m.getBbHeight() * 0.6, 0);
        FX.send(m.level(), FX.dust(in.color(), 1.2F), c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        FX.send(m.level(), FX.dust(in.color(), 1.2F), c.x - Math.cos(a) * r, c.y, c.z - Math.sin(a) * r, 1, 0, 0, 0, 0);
        if (in.style() == Style.RAISE) FX.send(m.level(), ParticleTypes.ENCHANT, c.x, c.y + 0.6, c.z, 4, r * 0.5, 0.4, r * 0.5, 0.4);
    }

    public static void tick(RpgMonster m, Ability a, int t, @Nullable LivingEntity target, Info in) {
        if (t < in.windup()) windupFx(m, in, t);
        Level level = m.level();
        float p = pow(m);
        float s = m.def().scale();
        boolean main = t == in.windup();
        switch (a) {
            case FIREBALL -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.BLAZE_SHOOT, 1.2F, 0.8F);
                    bolt(m, 0xFF6A1A, 0xFFD040, 0.55F, MagicBolt.FIRE).at(target, 1.1).dmg(p * 1.1F, OnHit.BURN).boom(1.8F).fire();
                }
            }
            case ICE_SHARD -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.GLASS_BREAK, 1.0F, 1.6F);
                    Vec3 dir = center(target).subtract(mouth(m));
                    for (int i = -1; i <= 1; i++) {
                        Vec3 d = dir.yRot(i * 0.18F);
                        bolt(m, 0x90E0FF, 0xFFFFFF, 0.3F, MagicBolt.ICE).aim(d, 1.4).dmg(p * 0.6F, OnHit.FREEZE).fire();
                    }
                }
            }
            case POISON_SPIT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.LLAMA_SPIT, 1.0F, 0.7F);
                    Vec3 d = center(target).subtract(mouth(m));
                    d = d.add(0, d.horizontalDistance() * 0.12, 0);
                    bolt(m, 0x70E020, 0x2A6010, 0.35F, MagicBolt.POISON).aim(d, 1.0).grav(0.03F).dmg(p * 0.7F, OnHit.POISON).fire();
                }
            }
            case ACID_SPIT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.LLAMA_SPIT, 1.0F, 0.5F);
                    Vec3 d = center(target).subtract(mouth(m));
                    d = d.add(0, d.horizontalDistance() * 0.12, 0);
                    bolt(m, 0xC0FF30, 0x608010, 0.4F, MagicBolt.POISON).aim(d, 1.0).grav(0.03F).dmg(p * 0.8F, OnHit.ACID).boom(1.2F).fire();
                }
            }
            case SHADOW_BOLT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.6F);
                    bolt(m, 0x8040C0, 0x1A0A2A, 0.5F, MagicBolt.DARK).at(target, 0.7).home(target, 0.08F).life(100).dmg(p * 1.1F, OnHit.WITHER).fire();
                }
            }
            case LIGHTNING_ORB -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
                    bolt(m, 0xA0D0FF, 0xFFFFFF, 0.8F, MagicBolt.LIGHTNING).at(target, 0.35).home(target, 0.03F).life(120).zap(8).dmg(p, OnHit.SHOCK).boom(2.2F).fire();
                }
            }
            case ROCK_THROW -> {
                if (t == 4) FX.send(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), m.position(), 20, m.getBbWidth() * 0.5, 0.1);
                if (main && target != null) {
                    FX.sound(m, SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.6F);
                    Vec3 d = center(target).subtract(mouth(m));
                    d = d.add(0, d.horizontalDistance() * 0.18, 0);
                    bolt(m, 0x8A7A60, 0x4A4038, 0.9F, MagicBolt.EARTH).aim(d, 1.0).grav(0.04F).dmg(p * 1.3F, OnHit.KNOCK).boom(1.5F).fire();
                }
            }
            case BONE_SPEAR -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.SKELETON_SHOOT, 1.0F, 0.6F);
                    bolt(m, 0xF0E8D0, 0x8A8070, 0.3F, MagicBolt.BONE).at(target, 1.8).pierce(2).dmg(p * 1.0F, OnHit.BLEED).fire();
                }
            }
            case WATER_JET -> {
                if (t >= in.windup() && t % 3 == 0 && target != null) {
                    if (t == in.windup()) FX.sound(m, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.2F, 0.8F);
                    Vec3 a0 = mouth(m), b0 = a0.add(center(target).subtract(a0).normalize().scale(in.maxRange()));
                    FX.line(level, ParticleTypes.SPLASH, a0, b0, 0.35, 0.1);
                    FX.line(level, FX.dust(0x3090FF, 1.0F), a0, b0, 0.5, 0.05);
                    for (LivingEntity e : Combat.onLine(m, a0, b0, 0.8)) {
                        Combat.magic(m, null, e, p * 0.3F);
                        Combat.knock(m.position(), e, 0.35, 0.05);
                        e.clearFire();
                    }
                }
            }
            case ARCANE_MISSILES -> {
                if (t >= in.windup() && (t - in.windup()) % 5 == 0 && target != null) {
                    FX.sound(m, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
                    int col = m.def().eyeColor();
                    Vec3 side = m.right().scale((m.getRandom().nextBoolean() ? 1 : -1) * 0.8);
                    MagicBolt b = bolt(m, col, 0xFFFFFF, 0.25F, MagicBolt.ARCANE);
                    b.setPos(b.getX() + side.x, b.getY() + 0.3, b.getZ() + side.z);
                    b.aim(new Vec3(side.x, 0.6, side.z).add(m.forward()), 0.6).home(target, 0.15F).life(70).dmg(p * 0.45F, OnHit.NONE).fire();
                }
            }
            case BLOOD_BOLT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.WITCH_THROW, 1.0F, 0.6F);
                    bolt(m, 0xC01020, 0x400008, 0.45F, MagicBolt.BLOOD).at(target, 1.2).dmg(p * 1.1F, OnHit.LIFESTEAL).fire();
                }
            }
            case SPORE_SHOT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 1.0F, 0.7F);
                    for (int i = 0; i < 2; i++)
                        bolt(m, 0xA0C040, 0xE0FF80, 0.3F, MagicBolt.NATURE).at(target, 0.9 + i * 0.2).dmg(p * 0.5F, OnHit.NAUSEA).fire();
                }
            }
            case WEB_SHOT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.SPIDER_AMBIENT, 1.0F, 1.5F);
                    bolt(m, 0xF0F0F0, 0xC0C0C0, 0.4F, MagicBolt.WEB).at(target, 1.2).dmg(p * 0.3F, OnHit.WEB).fire();
                }
            }
            case HOLY_BOLT -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
                    bolt(m, 0xFFF0A0, 0xFFFFFF, 0.6F, MagicBolt.LIGHT).at(target, 1.3).dmg(p * 1.2F, OnHit.HOLY).boom(2.0F).fire();
                }
            }
            case SAND_BLAST -> {
                if (t >= in.windup() && t % 2 == 0) {
                    Vec3 o = mouth(m), d = target != null ? center(target).subtract(o) : m.forward();
                    FX.cone(level, FX.dust(0xE0C080, 1.4F), o, d, in.maxRange(), 0.5, 12);
                    if (t % 6 == 0) for (LivingEntity e : Combat.inCone(m, o, d, in.maxRange(), 0.82)) {
                        Combat.magic(m, null, e, p * 0.35F);
                        Combat.effect(e, MobEffects.BLINDNESS, 50, 0);
                    }
                }
            }
            case FIRE_NOVA -> nova(m, t, in, p, 6, ParticleTypes.FLAME, 0xFF5010, OnHit.BURN, 0.8);
            case FROST_NOVA -> nova(m, t, in, p, 6, ParticleTypes.SNOWFLAKE, 0x90E0FF, OnHit.FREEZE, 0.4);
            case DARK_PULSE -> nova(m, t, in, p, 7, ParticleTypes.SQUID_INK, 0x5A1A8A, OnHit.WITHER, 0.6);
            case SHOCKWAVE -> {
                if (t >= in.windup() && t < in.windup() + 8) {
                    double r = 1 + (t - in.windup()) * 0.9;
                    BlockState below = m.level().getBlockState(m.blockPosition().below());
                    if (below.isAir()) below = Blocks.DIRT.defaultBlockState();
                    FX.ring(level, new BlockParticleOption(ParticleTypes.BLOCK, below), m.position().add(0, 0.2, 0), r, 28, 0.2);
                    FX.ring(level, ParticleTypes.CLOUD, m.position().add(0, 0.3, 0), r, 16, 0.0);
                    if (t == in.windup()) FX.sound(m, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.6F);
                    for (LivingEntity e : Combat.victims(m, m.position(), r + 0.6)) {
                        if (e.position().distanceTo(m.position()) < r - 1.5 || !e.onGround()) continue;
                        if (Combat.magic(m, null, e, p * 0.9F)) Combat.knock(m.position(), e, 1.0, 0.6);
                    }
                }
            }
            case POISON_CLOUD -> {
                if (main) {
                    Vec3 at = target != null ? target.position() : m.position();
                    AreaEffectCloud c = new AreaEffectCloud(level, at.x, at.y, at.z);
                    c.setOwner(m);
                    c.setRadius(3.0F + s * 0.5F);
                    c.setDuration(140);
                    c.setRadiusPerTick(-0.01F);
                    c.setParticle(FX.dust(0x80D020, 1.5F));
                    c.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 80, 1));
                    level.addFreshEntity(c);
                    FX.sound(level, at, SoundEvents.BREWING_STAND_BREW, 1.2F, 0.6F);
                }
            }
            case QUAKE -> {
                if (t >= in.windup() && (t - in.windup()) % 4 == 0) {
                    double r = 3 + s * 2;
                    FX.sound(m, SoundEvents.GENERIC_EXPLODE, 0.7F, 0.4F);
                    for (int i = 0; i < 30; i++) {
                        double ang = m.getRandom().nextDouble() * Math.PI * 2, rr = m.getRandom().nextDouble() * r;
                        BlockPos bp = BlockPos.containing(m.getX() + Math.cos(ang) * rr, m.getY() - 0.5, m.getZ() + Math.sin(ang) * rr);
                        BlockState st = level.getBlockState(bp);
                        if (!st.isAir()) FX.send(level, new BlockParticleOption(ParticleTypes.BLOCK, st), bp.getX() + 0.5, bp.getY() + 1.1, bp.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.15);
                    }
                    for (LivingEntity e : Combat.victims(m, m.position(), r)) {
                        if (!e.onGround()) continue;
                        Combat.magic(m, null, e, p * 0.45F);
                        Combat.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 40, 2);
                        e.push(0, 0.35, 0);
                        e.hurtMarked = true;
                    }
                }
            }
            case SPORE_BURST -> {
                if (main) {
                    FX.sound(m, SoundEvents.SPORE_BLOSSOM_BREAK, 1.5F, 0.6F);
                    FX.sphere(level, ParticleTypes.SPORE_BLOSSOM_AIR, m.position().add(0, 1, 0), 5, 120);
                    FX.sphere(level, FX.dust(0xC0E060, 1.5F), m.position().add(0, 1, 0), 4, 60);
                    for (LivingEntity e : Combat.victims(m, m.position(), 6)) {
                        Combat.magic(m, null, e, p * 0.5F);
                        Combat.effect(e, MobEffects.CONFUSION, 160, 0);
                        Combat.effect(e, MobEffects.POISON, 80, 0);
                        Combat.effect(e, MobEffects.BLINDNESS, 30, 0);
                    }
                }
            }
            case SANDSTORM -> {
                if (t >= in.windup()) {
                    double r = 7 + s;
                    Vec3 c = m.position();
                    for (int i = 0; i < 3; i++) FX.spiral(level, FX.dust(0xE0C080, 1.8F), c.add(0, (t * 0.3) % 3, 0), 4, r * (0.4 + i * 0.3), 1, 16);
                    if (t % 10 == 0) {
                        FX.sound(m, SoundEvents.SAND_BREAK, 1.0F, 0.5F);
                        for (LivingEntity e : Combat.victims(m, c, r)) {
                            Combat.magic(m, null, e, p * 0.25F);
                            Combat.effect(e, MobEffects.BLINDNESS, 40, 0);
                            Vec3 tang = new Vec3(-(e.getZ() - c.z), 0, e.getX() - c.x).normalize().scale(0.4);
                            e.push(tang.x, 0.05, tang.z);
                            e.hurtMarked = true;
                        }
                    }
                }
            }
            case THUNDERSTORM -> {
                if (t >= in.windup() && (t - in.windup()) % 10 == 0 && target != null) {
                    Vec3 at = target.position().add((m.getRandom().nextDouble() - 0.5) * 5, 0, (m.getRandom().nextDouble() - 0.5) * 5);
                    if (m.getRandom().nextInt(3) == 0) at = target.position();
                    strike(m, at, p * 0.9F);
                }
            }
            case METEOR -> {
                if (main && target != null) {
                    int n = m.isBoss() ? 5 : 1;
                    for (int i = 0; i < n; i++) {
                        Vec3 at = target.position().add(i == 0 ? 0 : (m.getRandom().nextDouble() - 0.5) * 10, 0, i == 0 ? 0 : (m.getRandom().nextDouble() - 0.5) * 10);
                        MagicBolt b = MagicBolt.create(level, m, 0xFF4010, 0xFFE060, 1.2F, MagicBolt.FIRE);
                        b.setPos(at.x + 4, at.y + 22 + i * 3, at.z + 2);
                        b.aim(at.subtract(b.position()), 0.9).dmg(p * 1.6F, OnHit.BURN).boom(3.0F).life(100).fire();
                    }
                    FX.sound(m, SoundEvents.BLAZE_SHOOT, 1.5F, 0.5F);
                }
            }
            case ROOTS -> {
                if (main && target != null) {
                    FX.sound(level, target.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.6F);
                    Vec3 c = target.position();
                    for (LivingEntity e : Combat.victims(m, c, 2.5)) {
                        Combat.magic(m, null, e, p * 0.6F);
                        Combat.applyOnHit(m, e, OnHit.ROOT, 0);
                    }
                    for (int i = 0; i < 5; i++) FX.spiral(level, FX.dust(0x4A7A20, 1.2F), c.add(Math.cos(i * 1.256) * 0.6, 0, Math.sin(i * 1.256) * 0.6), 2.2, 0.35, 2, 18);
                    FX.send(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), c, 30, 0.6, 0.1);
                }
            }
            case VORTEX_PULL -> {
                if (t >= in.windup()) {
                    Vec3 c = m.position().add(0, 1, 0);
                    double r = 10 + s;
                    FX.spiral(level, FX.dust(0x9080C0, 1.2F), m.position(), 3, 2 + (t % 10) * 0.6, 2, 30);
                    if (t % 2 == 0) for (LivingEntity e : Combat.victims(m, c, r)) {
                        Vec3 d = c.subtract(e.position()).normalize().scale(0.12);
                        e.setDeltaMovement(e.getDeltaMovement().add(d.x, 0.02, d.z));
                        e.hurtMarked = true;
                        if (e.distanceToSqr(m) < 6 && t % 10 == 0) Combat.magic(m, null, e, p * 0.4F);
                    }
                }
            }
            case SONIC_SCREAM -> {
                if (main) {
                    FX.sound(m, SoundEvents.WARDEN_SONIC_BOOM, 1.5F, 1.4F);
                    Vec3 o = mouth(m), d = target != null ? center(target).subtract(o) : m.forward();
                    for (int i = 1; i < 8; i++) FX.send(level, ParticleTypes.SONIC_BOOM, o.add(d.normalize().scale(i * 1.2)), 1, 0, 0);
                    for (LivingEntity e : Combat.inCone(m, o, d, in.maxRange(), 0.75)) {
                        Combat.magic(m, null, e, p * 0.9F);
                        Combat.effect(e, MobEffects.CONFUSION, 100, 0);
                        Combat.knock(m.position(), e, 0.8, 0.2);
                    }
                }
            }
            case FEAR_ROAR -> {
                if (main) {
                    FX.sound(m, SoundEvents.RAVAGER_ROAR, 2.0F, 0.8F + m.getRandom().nextFloat() * 0.3F);
                    FX.burstRing(level, FX.dust(0xFF4040, 2.0F), m.position().add(0, 1, 0), 40, 0.6);
                    for (LivingEntity e : Combat.victims(m, m.position(), 9)) {
                        Combat.effect(e, MobEffects.WEAKNESS, 120, 1);
                        Combat.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                        Combat.knock(m.position(), e, 0.9, 0.2);
                    }
                }
            }
            case BLIND_FLASH -> {
                if (main) {
                    FX.sound(m, SoundEvents.BEACON_DEACTIVATE, 1.5F, 1.8F);
                    FX.sphere(level, ParticleTypes.END_ROD, m.position().add(0, 1.2, 0), 2, 60);
                    FX.send(level, ParticleTypes.FLASH, m.position().add(0, 1.2, 0), 1, 0, 0);
                    for (LivingEntity e : Combat.victims(m, m.position(), 10)) {
                        Combat.effect(e, MobEffects.BLINDNESS, 80, 0);
                        Combat.effect(e, MobEffects.DARKNESS, 60, 0);
                    }
                }
            }
            case CURSE -> {
                if (main && target != null) {
                    FX.sound(m, SoundEvents.EVOKER_PREPARE_WOLOLO, 1.2F, 0.6F);
                    FX.zigzag(level, FX.dust(0x8A2AAA, 1.2F), mouth(m), center(target), 6, 0.5);
                    FX.spiral(level, ParticleTypes.WITCH, target.position(), 2.4, 0.8, 3, 40);
                    Combat.effect(target, MobEffects.WEAKNESS, 160, 1);
                    Combat.effect(target, MobEffects.MOVEMENT_SLOWDOWN, 120, 2);
                    Combat.effect(target, MobEffects.DIG_SLOWDOWN, 160, 1);
                    Combat.effect(target, MobEffects.GLOWING, 160, 0);
                }
            }
            case LEAP_SLAM -> {
                if (main && target != null) {
                    Vec3 d = target.position().subtract(m.position());
                    double h = d.horizontalDistance();
                    m.setDeltaMovement(d.x / Math.max(h, 1) * Math.min(h * 0.11, 1.4), 0.55 + Math.min(h * 0.03, 0.35), d.z / Math.max(h, 1) * Math.min(h * 0.11, 1.4));
                    m.hasImpulse = true;
                    FX.sound(m, SoundEvents.RAVAGER_STEP, 1.2F, 0.7F);
                }
                if (t > in.windup() + 3 && m.onGround() && !m.landed) {
                    m.landed = true;
                    FX.sound(m, SoundEvents.GENERIC_EXPLODE, 0.9F, 0.8F);
                    FX.burstRing(level, ParticleTypes.CLOUD, m.position().add(0, 0.2, 0), 30, 0.4);
                    for (LivingEntity e : Combat.victims(m, m.position(), 2.5 + s * 0.8)) Combat.melee(m, e, p * 1.2F, 0.9, 0.5);
                }
            }
            case CHARGE -> {
                if (main) { FX.sound(m, SoundEvents.RAVAGER_ROAR, 1.0F, 1.2F); m.landed = false; }
                if (t >= in.windup() && t < in.windup() + 16) {
                    Vec3 f = m.forward().scale(0.55 + m.def().speed());
                    m.setDeltaMovement(f.x, m.getDeltaMovement().y, f.z);
                    m.hasImpulse = true;
                    FX.send(level, ParticleTypes.CLOUD, m.position().add(0, 0.2, 0), 2, 0.3, 0.02);
                    for (LivingEntity e : Combat.victims(m, m.position().add(m.forward()).add(0, m.getBbHeight() * 0.4, 0), m.getBbWidth() * 0.7 + 1)) {
                        if (m.hitThisCast.add(e.getId())) Combat.melee(m, e, p * 1.4F, 1.4, 0.45);
                    }
                    if (m.horizontalCollision) m.castTick = in.duration();
                }
            }
            case BLINK_BEHIND -> {
                if (main && target != null) {
                    Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.2));
                    teleportFx(m);
                    if (m.randomTeleport(behind.x, target.getY(), behind.z, false)) {
                        teleportFx(m);
                        m.faceTowards(target);
                        Combat.melee(m, target, p, 0.4, 0.1);
                    }
                }
            }
            case BURROW -> {
                if (t == 2) { FX.sound(m, SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.5F); m.setInvisible(true); }
                if (t > 2 && t < in.windup() + 16) {
                    BlockState below = level.getBlockState(m.blockPosition().below());
                    if (!below.isAir()) FX.send(level, new BlockParticleOption(ParticleTypes.BLOCK, below), m.position(), 6, 0.5, 0.1);
                }
                if (t == in.windup() + 16 && target != null) {
                    m.teleportTo(target.getX(), target.getY(), target.getZ());
                }
                if (t == in.windup() + 18) {
                    m.setInvisible(false);
                    BlockState below = level.getBlockState(m.blockPosition().below());
                    if (below.isAir()) below = Blocks.DIRT.defaultBlockState();
                    FX.send(level, new BlockParticleOption(ParticleTypes.BLOCK, below), m.position(), 60, 1.0, 0.3);
                    FX.sound(m, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.7F);
                    for (LivingEntity e : Combat.victims(m, m.position(), 2.5 + s * 0.5)) Combat.melee(m, e, p * 1.3F, 0.3, 1.0);
                }
            }
            case DIVE_BOMB -> {
                if (t == 1) m.setDeltaMovement(m.getDeltaMovement().add(0, 0.9, 0));
                if (main && target != null) {
                    Vec3 d = center(target).subtract(m.position()).normalize().scale(1.3);
                    m.setDeltaMovement(d);
                    FX.sound(m, SoundEvents.PHANTOM_SWOOP, 1.5F, 0.8F);
                }
                if (t > in.windup() && t < in.windup() + 20) {
                    FX.send(level, ParticleTypes.CLOUD, m.position(), 2, 0.2, 0.02);
                    for (LivingEntity e : Combat.victims(m, m.position(), m.getBbWidth() + 1)) {
                        if (m.hitThisCast.add(e.getId())) Combat.melee(m, e, p * 1.3F, 0.8, 0.3);
                    }
                    if (m.onGround() || m.horizontalCollision) m.castTick = in.duration();
                }
            }
            case SHADOW_STEP -> {
                if (t == 1) { m.setInvisible(true); FX.send(level, ParticleTypes.SQUID_INK, m.position().add(0, 1, 0), 30, 0.4, 0.05); }
                if (main && target != null) {
                    Vec3 side = target.position().add(target.getLookAngle().yRot((float) Math.PI / 2).multiply(1, 0, 1).normalize().scale(2));
                    m.randomTeleport(side.x, target.getY(), side.z, false);
                }
                if (t == in.windup() + 6 && target != null) {
                    m.setInvisible(false);
                    m.faceTowards(target);
                    FX.send(level, ParticleTypes.SQUID_INK, m.position().add(0, 1, 0), 30, 0.4, 0.05);
                    Combat.melee(m, target, p * 1.4F, 0.5, 0.1);
                    Combat.effect(target, MobEffects.DARKNESS, 60, 0);
                }
            }
            case DODGE -> {
                if (main) {
                    Vec3 side = m.right().scale(m.getRandom().nextBoolean() ? 0.9 : -0.9).add(m.forward().scale(-0.4));
                    m.setDeltaMovement(side.x, 0.35, side.z);
                    m.hasImpulse = true;
                    FX.send(level, ParticleTypes.POOF, m.position(), 6, 0.3, 0.02);
                }
            }
            case HEAL -> {
                if (t >= in.windup() && t % 3 == 0) FX.spiral(level, ParticleTypes.HAPPY_VILLAGER, m.position(), m.getBbHeight(), m.getBbWidth(), 1, 12);
                if (main) {
                    m.heal(m.getMaxHealth() * (m.isBoss() ? 0.08F : 0.25F));
                    FX.sound(m, SoundEvents.PLAYER_LEVELUP, 0.8F, 0.6F);
                }
            }
            case ENRAGE -> {
                if (main) {
                    FX.sound(m, SoundEvents.RAVAGER_ROAR, 1.6F, 0.6F);
                    Combat.effect(m, MobEffects.DAMAGE_BOOST, 200, 1);
                    Combat.effect(m, MobEffects.MOVEMENT_SPEED, 200, 1);
                    FX.send(level, ParticleTypes.ANGRY_VILLAGER, m.position().add(0, m.getBbHeight(), 0), 8, 0.5, 0.1);
                    FX.column(level, FX.dust(0xFF2020, 1.6F), m.position(), m.getBbHeight() + 1, m.getBbWidth(), 60);
                }
            }
            case STONE_SKIN -> {
                if (main) {
                    FX.sound(m, SoundEvents.STONE_PLACE, 1.6F, 0.5F);
                    Combat.effect(m, MobEffects.DAMAGE_RESISTANCE, 160, 2);
                    FX.sphere(level, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), m.position().add(0, m.getBbHeight() * 0.5, 0), m.getBbWidth() + 0.5, 50);
                }
            }
            case INVISIBILITY -> {
                if (main) {
                    Combat.effect(m, MobEffects.INVISIBILITY, 120, 0);
                    Combat.effect(m, MobEffects.MOVEMENT_SPEED, 120, 1);
                    FX.send(level, ParticleTypes.LARGE_SMOKE, m.position().add(0, 1, 0), 30, 0.5, 0.02);
                    FX.sound(m, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
                }
            }
            case REGEN_AURA -> {
                if (main) {
                    FX.sound(m, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.2F);
                    FX.ring(level, FX.dust(0x60FF90, 1.5F), m.position().add(0, 0.2, 0), 8, 60, 0.3);
                    for (Mob ally : level.getEntitiesOfClass(Mob.class, m.getBoundingBox().inflate(8), e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive())) {
                        Combat.effect(ally, MobEffects.REGENERATION, 120, 1);
                        FX.send(level, ParticleTypes.HEART, ally.position().add(0, ally.getBbHeight() + 0.3, 0), 2, 0.3, 0);
                    }
                }
            }
            case SUMMON -> {
                if (t < in.windup()) FX.ring(level, FX.dust(0xB070FF, 1.4F), m.position().add(0, 0.1, 0), 2.5, 20, 0.4);
                if (main) summon(m, target);
            }
            case TAIL_SWEEP -> {
                if (main) {
                    FX.sound(m, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.6F);
                    double r = 3 + s * 0.8;
                    FX.arc(level, ParticleTypes.SWEEP_ATTACK, m.position(), m.yBodyRot + 180, r * 0.8, 300, 0.5, 12);
                    for (LivingEntity e : Combat.victims(m, m.position(), r)) Combat.melee(m, e, p * 1.1F, 1.2, 0.4);
                }
            }
            case SPIN_ATTACK -> {
                if (t >= in.windup() && (t - in.windup()) % 6 == 0) {
                    FX.sound(m, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.0F);
                    double r = 2.5 + s * 0.6;
                    FX.ring(level, ParticleTypes.SWEEP_ATTACK, m.position().add(0, m.getBbHeight() * 0.5, 0), r * 0.8, 8, 0);
                    for (LivingEntity e : Combat.victims(m, m.position(), r)) Combat.melee(m, e, p * 0.6F, 0.5, 0.1);
                }
            }
            case LIFE_DRAIN -> {
                if (t >= in.windup() && target != null && m.distanceToSqr(target) < 14 * 14) {
                    FX.line(level, FX.dust(0xC01030, 0.9F), center(target), mouth(m), 0.4, 0.08);
                    if (t % 8 == 0) {
                        if (Combat.magic(m, null, target, p * 0.35F)) m.heal(p * 0.35F);
                        FX.send(level, ParticleTypes.DAMAGE_INDICATOR, center(target), 3, 0.2, 0.05);
                    }
                }
            }
            case STEAL -> {
                if (main && target != null && m.distanceToSqr(target) < 16) {
                    Combat.melee(m, target, p * 0.6F, 0.2, 0.1);
                    if (target instanceof Player pl) stealCoins(m, pl);
                    Vec3 back = m.forward().scale(-0.9);
                    m.setDeltaMovement(back.x, 0.3, back.z);
                    FX.send(level, ParticleTypes.POOF, m.position(), 8, 0.3, 0.02);
                }
            }
            case GRAB_THROW -> {
                if (main && target != null && m.distanceToSqr(target) < (3.5 + s) * (3.5 + s)) {
                    FX.sound(m, SoundEvents.IRON_GOLEM_ATTACK, 1.4F, 0.6F);
                    if (Combat.melee(m, target, p * 1.2F, 0, 0)) {
                        Vec3 d = m.forward().scale(1.8);
                        target.setDeltaMovement(d.x, 0.9, d.z);
                        target.hurtMarked = true;
                    }
                }
            }
            case BITE_BLEED -> {
                if (main && target != null && m.distanceToSqr(target) < (3.2 + s) * (3.2 + s)) {
                    FX.sound(m, SoundEvents.EVOKER_FANGS_ATTACK, 1.0F, 1.0F);
                    if (Combat.melee(m, target, p * 1.0F, 0.3, 0.1)) Combat.applyOnHit(m, target, OnHit.BLEED, p);
                    FX.send(level, ParticleTypes.DAMAGE_INDICATOR, center(target), 6, 0.3, 0.1);
                }
            }
            case FIRE_BREATH -> breath(m, t, in, target, p, ParticleTypes.FLAME, 0xFF6010, OnHit.BURN, SoundEvents.FIRECHARGE_USE);
            case FROST_BREATH -> breath(m, t, in, target, p, ParticleTypes.SNOWFLAKE, 0xA0E8FF, OnHit.FREEZE, SoundEvents.POWDER_SNOW_STEP);
            case POISON_BREATH -> breath(m, t, in, target, p, ParticleTypes.ITEM_SLIME, 0x80E020, OnHit.POISON, SoundEvents.BREWING_STAND_BREW);
        }
    }

    private static void nova(RpgMonster m, int t, Info in, float p, double radius, ParticleOptions pt, int col, OnHit h, double knock) {
        Level level = m.level();
        int k = t - in.windup();
        double r = radius + m.def().scale();
        if (k >= 0 && k < 8) {
            double cur = 1 + k * (r / 7.0);
            FX.ring(level, pt, m.position().add(0, 0.3, 0), cur, 36, 0.05);
            FX.ring(level, FX.dust(col, 2.0F), m.position().add(0, 0.6, 0), cur * 0.95, 24, 0.1);
            if (k == 0) {
                FX.sound(m, SoundEvents.GENERIC_EXPLODE, 1.0F, 1.3F);
                FX.sphere(level, pt, m.position().add(0, 1, 0), 1.5, 40);
                for (LivingEntity e : Combat.victims(m, m.position().add(0, 0.5, 0), r)) {
                    if (Combat.magic(m, null, e, p * 1.0F)) {
                        Combat.applyOnHit(m, e, h, p);
                        Combat.knock(m.position(), e, knock, 0.3);
                    }
                }
            }
        }
    }

    private static void breath(RpgMonster m, int t, Info in, @Nullable LivingEntity target, float p, ParticleOptions pt, int col, OnHit h, net.minecraft.sounds.SoundEvent snd) {
        if (t < in.windup()) return;
        Level level = m.level();
        Vec3 o = mouth(m);
        Vec3 d = target != null ? center(target).subtract(o) : m.forward();
        double range = in.maxRange() + m.def().scale() * 1.5;
        if (t % 8 == in.windup() % 8) FX.sound(m, snd, 1.2F, 0.8F);
        FX.cone(level, pt, o, d, range, 0.35, 14);
        FX.cone(level, FX.dust(col, 1.6F), o, d, range, 0.3, 8);
        if (t % 5 == 0) for (LivingEntity e : Combat.inCone(m, o, d, range, 0.85)) {
            Combat.magic(m, null, e, p * 0.35F);
            Combat.applyOnHit(m, e, h, p * 0.3F);
        }
    }

    public static void strike(LivingEntity caster, Vec3 at, float dmg) {
        Level level = caster.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        FX.send(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.5, 0), 40, 0.8, 0.3);
        for (LivingEntity e : Combat.victims(caster, at, 2.5)) {
            if (Combat.magic(caster, null, e, dmg)) Combat.effect(e, RpgEffects.STUN.get(), 20, 0);
        }
    }

    private static void teleportFx(RpgMonster m) {
        FX.send(m.level(), ParticleTypes.PORTAL, m.position().add(0, m.getBbHeight() * 0.5, 0), 40, 0.5, 0.3);
        FX.send(m.level(), FX.dust(m.def().eyeColor(), 1.5F), m.position().add(0, m.getBbHeight() * 0.5, 0), 20, 0.4, 0.05);
        FX.sound(m, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
    }

    private static void summon(RpgMonster m, @Nullable LivingEntity target) {
        MonsterDef d = m.def();
        if (d.summon() == null || !(m.level() instanceof ServerLevel sl)) return;
        EntityType<?> type = RpgEntities.typeOf(d.summon());
        if (type == null) return;
        int existing = sl.getEntitiesOfClass(RpgMonster.class, m.getBoundingBox().inflate(24), e -> e.getType() == type).size();
        int n = Math.min(m.isBoss() ? 4 : 2, (m.isBoss() ? 8 : 4) - existing);
        FX.sound(m, SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.8F);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / Math.max(1, n) + m.getRandom().nextDouble();
            Vec3 p = m.position().add(Math.cos(a) * (2 + m.getBbWidth()), 0.5, Math.sin(a) * (2 + m.getBbWidth()));
            Entity e = type.create(sl);
            if (!(e instanceof Mob mob)) continue;
            mob.moveTo(p.x, p.y, p.z, m.getRandom().nextFloat() * 360, 0);
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            if (target != null) mob.setTarget(target);
            sl.addFreshEntity(mob);
            FX.column(sl, FX.dust(0xB070FF, 1.5F), p, 2.5, 0.6, 30);
            FX.send(sl, ParticleTypes.SOUL, p.add(0, 1, 0), 10, 0.3, 0.05);
        }
    }

    private static void stealCoins(RpgMonster m, Player pl) {
        for (int i = 0; i < pl.getInventory().getContainerSize(); i++) {
            ItemStack st = pl.getInventory().getItem(i);
            if (st.getItem() instanceof com.krolasyon.bosses.rpg.item.CoinItem) {
                int n = Math.min(st.getCount(), 1 + m.getRandom().nextInt(4));
                ItemStack taken = st.split(n);
                m.stolen.add(taken);
                pl.displayClientMessage(net.minecraft.network.chat.Component.literal("§e" + m.getName().getString() + " kesenden " + n + " sikke çaldı!"), true);
                return;
            }
        }
    }

    public static float dist(Entity a, Entity b) { return Mth.sqrt((float) a.distanceToSqr(b)); }

}
