package com.krolasyon.bosses.rpg.magic;

import com.krolasyon.bosses.rpg.def.RpgDefs.School;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.entity.MagicBolt;
import com.krolasyon.bosses.rpg.entity.MagicBolt.OnHit;
import com.krolasyon.bosses.rpg.entity.Summon;
import com.krolasyon.bosses.rpg.mob.AbilityLogic;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.util.Tasks;
import com.krolasyon.bosses.rpg.util.TempBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import javax.annotation.Nullable;
import java.util.List;

/** Executes a spell for any caster. Mana, cooldowns and learning are handled by the caller (PlayerMagic / NPC AI). */
public final class SpellCaster {
    private SpellCaster() {}

    public static byte trail(School s) {
        return switch (s) {
            case FIRE -> MagicBolt.FIRE; case ICE -> MagicBolt.ICE; case LIGHTNING -> MagicBolt.LIGHTNING; case EARTH -> MagicBolt.EARTH;
            case WIND -> MagicBolt.WIND; case WATER -> MagicBolt.WATER; case LIGHT -> MagicBolt.LIGHT; case DARK -> MagicBolt.DARK;
            case NATURE -> MagicBolt.NATURE; case ARCANE -> MagicBolt.ARCANE; case BLOOD -> MagicBolt.BLOOD; case SPIRIT -> MagicBolt.SPIRIT;
        };
    }

    public static OnHit onHit(School s) {
        return switch (s) {
            case FIRE -> OnHit.BURN; case ICE -> OnHit.FREEZE; case LIGHTNING -> OnHit.SHOCK; case EARTH, WIND -> OnHit.KNOCK;
            case WATER -> OnHit.SLOW; case LIGHT -> OnHit.HOLY; case DARK -> OnHit.WITHER; case NATURE -> OnHit.POISON;
            case ARCANE, SPIRIT -> OnHit.NONE; case BLOOD -> OnHit.BLEED;
        };
    }

    public static ParticleOptions particle(School s) {
        return switch (s) {
            case FIRE -> ParticleTypes.FLAME; case ICE -> ParticleTypes.SNOWFLAKE; case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()); case WIND -> ParticleTypes.CLOUD;
            case WATER -> ParticleTypes.SPLASH; case LIGHT -> ParticleTypes.END_ROD; case DARK -> ParticleTypes.SQUID_INK;
            case NATURE -> ParticleTypes.HAPPY_VILLAGER; case ARCANE -> ParticleTypes.WITCH; case BLOOD -> ParticleTypes.DAMAGE_INDICATOR;
            case SPIRIT -> ParticleTypes.SOUL_FIRE_FLAME;
        };
    }

    public static SoundEvent sound(School s) {
        return switch (s) {
            case FIRE -> SoundEvents.BLAZE_SHOOT; case ICE -> SoundEvents.GLASS_BREAK; case LIGHTNING -> SoundEvents.LIGHTNING_BOLT_IMPACT;
            case EARTH -> SoundEvents.STONE_BREAK; case WIND -> SoundEvents.PHANTOM_FLAP; case WATER -> SoundEvents.PLAYER_SPLASH;
            case LIGHT -> SoundEvents.BEACON_POWER_SELECT; case DARK -> SoundEvents.WITHER_SHOOT; case NATURE -> SoundEvents.AZALEA_LEAVES_BREAK;
            case ARCANE -> SoundEvents.ILLUSIONER_CAST_SPELL; case BLOOD -> SoundEvents.WITCH_DRINK; case SPIRIT -> SoundEvents.SOUL_ESCAPE;
        };
    }

    private static Vec3 eye(LivingEntity c) { return c.getEyePosition(); }

    /** point the caster is aiming at (entity, block or max range) */
    public static Vec3 aimPoint(LivingEntity c, double range) {
        LivingEntity t = aimEntity(c, range);
        if (t != null) return t.position();
        Vec3 from = eye(c), to = from.add(c.getLookAngle().scale(range));
        BlockHitResult hit = c.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c));
        Vec3 p = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        // drop to ground
        BlockPos bp = BlockPos.containing(p);
        for (int i = 0; i < 12 && c.level().getBlockState(bp.below()).isAir(); i++) bp = bp.below();
        return new Vec3(p.x, bp.getY(), p.z);
    }

    @Nullable
    public static LivingEntity aimEntity(LivingEntity c, double range) {
        Vec3 from = eye(c), to = from.add(c.getLookAngle().scale(range));
        BlockHitResult block = c.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c));
        if (block.getType() != HitResult.Type.MISS) to = block.getLocation();
        AABB box = c.getBoundingBox().expandTowards(to.subtract(from)).inflate(1.5);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(c.level(), c, from, to, box,
                e -> e instanceof LivingEntity le && e != c && le.isAlive() && Combat.canHarm(c, le));
        return r != null && r.getEntity() instanceof LivingEntity le ? le : null;
    }

    /** main entry; power multiplies the spell's base power */
    public static void cast(LivingEntity c, SpellDef s, float power) {
        if (!(c.level() instanceof ServerLevel level)) return;
        float dmg = s.power() * power;
        School sc = s.school();
        int col = s.color(), col2 = s.color2();
        FX.sound(level, c.position(), sound(sc), 1.0F, 0.9F + c.getRandom().nextFloat() * 0.3F);
        FX.spiral(level, FX.dust(col, 1.0F), c.position(), c.getBbHeight(), 0.8, 2, 20);
        switch (s.shape()) {
            case BOLT -> bolt(c, s, dmg, 0.35F, 1.6, 0, sc == School.ARCANE ? 0.12F : 0);
            case BALL -> {
                if (s.id().equals("poison_cloud")) { cloud(c, s, aimPoint(c, 24), dmg); return; }
                bolt(c, s, dmg, 0.65F, 1.0, 2.6F, 0);
            }
            case BEAM -> beam(c, s, dmg, level);
            case CONE -> cone(c, s, dmg, level);
            case NOVA -> nova(c, s, dmg, level);
            case RAIN -> rain(c, s, dmg, level);
            case WALL -> wall(c, s, dmg, level);
            case BUFF -> buff(c, s, power);
            case HEAL -> heal(c, s, dmg, level);
            case SUMMON -> summon(c, s, power);
            case TELEPORT -> teleport(c, s, dmg, level);
            case CHAIN -> chain(c, s, dmg, level);
            case AURA -> aura(c, s, dmg, level);
            case TRAP -> trap(c, s, dmg, level);
            case PULL -> pull(c, s, dmg, level);
            case STRIKE -> strike(c, s, dmg, level);
        }
    }

    // ------------------------------------------------------------------ shapes
    private static void bolt(LivingEntity c, SpellDef s, float dmg, float size, double speed, float boom, float homing) {
        School sc = s.school();
        int n = s.id().equals("magic_missile") ? 3 : 1;
        LivingEntity target = homing > 0 || n > 1 ? aimEntity(c, 30) : null;
        for (int i = 0; i < n; i++) {
            MagicBolt b = MagicBolt.create(c.level(), c, s.color(), s.color2(), size, trail(sc));
            Vec3 dir = c.getLookAngle();
            if (n > 1) dir = dir.yRot((i - 1) * 0.15F).add(0, 0.1, 0);
            b.aim(dir, speed).dmg(n > 1 ? dmg * 0.6F : dmg, onHit(sc)).life(60);
            if (boom > 0) b.boom(boom);
            if (sc == School.EARTH) b.grav(0.02F);
            if (sc == School.BLOOD) b.dmg(dmg, OnHit.LIFESTEAL);
            if (sc == School.WIND) b.pierce(3);
            if (target != null) b.home(target, n > 1 ? 0.12F : homing);
            b.fire();
        }
        if (sc == School.BLOOD) c.hurt(c.damageSources().magic(), 1.0F);
    }

    private static void cloud(LivingEntity c, SpellDef s, Vec3 at, float dmg) {
        net.minecraft.world.entity.AreaEffectCloud cl = new net.minecraft.world.entity.AreaEffectCloud(c.level(), at.x, at.y, at.z);
        cl.setOwner(c);
        cl.setRadius(3.5F);
        cl.setDuration(160);
        cl.setParticle(FX.dust(s.color(), 1.5F));
        cl.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        c.level().addFreshEntity(cl);
    }

    private static void beam(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        School sc = s.school();
        Tasks.add(level, 24, t -> {
            if (!c.isAlive()) return false;
            Vec3 a = eye(c).subtract(0, 0.3, 0).add(c.getLookAngle().scale(0.6));
            Vec3 b = a.add(c.getLookAngle().scale(22));
            BlockHitResult hit = level.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c));
            if (hit.getType() != HitResult.Type.MISS) b = hit.getLocation();
            FX.line(level, FX.dust(s.color(), 1.3F), a, b, 0.3, 0.02);
            if (t % 2 == 0) FX.line(level, particle(sc), a, b, 0.9, 0.08);
            FX.send(level, FX.dust(s.color2(), 2.0F), b, 4, 0.2, 0.02);
            if (t % 4 == 0) {
                if (t % 8 == 0) FX.sound(level, a, sound(sc), 0.6F, 1.4F);
                for (LivingEntity e : Combat.onLine(c, a, b, 0.7)) {
                    if (Combat.magic(c, null, e, dmg)) {
                        Combat.applyOnHit(c, e, onHit(sc), dmg);
                        if (s.id().equals("life_drain")) c.heal(dmg * 0.7F);
                        if (sc == School.WATER || s.id().equals("vacuum_blade")) Combat.knock(c.position(), e, 0.3, 0.05);
                    }
                }
            }
            return true;
        });
    }

    private static void cone(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        School sc = s.school();
        int dur = s.cooldown() <= 4 ? 20 : 6;
        Tasks.add(level, dur, t -> {
            if (!c.isAlive()) return false;
            Vec3 o = eye(c).subtract(0, 0.3, 0);
            Vec3 d = c.getLookAngle();
            double range = sc == School.WIND || s.id().equals("tidal_wave") ? 9 : 7;
            FX.cone(level, particle(sc), o, d, range, 0.4, 16);
            FX.cone(level, FX.dust(s.color(), 1.4F), o, d, range, 0.35, 10);
            if (t % 4 == 0) {
                for (LivingEntity e : Combat.inCone(c, o, d, range, 0.8)) {
                    if (s.id().equals("gust") || s.id().equals("tidal_wave")) Combat.knock(c.position(), e, 1.6, 0.4);
                    if (s.id().equals("blinding_light")) { Combat.effect(e, MobEffects.BLINDNESS, 100, 0); Combat.effect(e, MobEffects.GLOWING, 160, 0); }
                    if (s.id().equals("curse_of_weakness")) { Combat.effect(e, MobEffects.WEAKNESS, 200, 1); Combat.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 120, 1); }
                    if (dmg > 0 && Combat.magic(c, null, e, dmg)) Combat.applyOnHit(c, e, onHit(sc), dmg);
                }
            }
            return true;
        });
    }

    private static void nova(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        School sc = s.school();
        double r = 6.5;
        Tasks.add(level, 10, t -> {
            double cur = 1 + t * (r / 9.0);
            Vec3 p = c.position().add(0, 0.3, 0);
            FX.ring(level, particle(sc), p, cur, 40, 0.05);
            FX.ring(level, FX.dust(s.color(), 2.0F), p.add(0, 0.4, 0), cur * 0.95, 30, 0.1);
            if (t == 0) {
                FX.sphere(level, particle(sc), c.position().add(0, 1, 0), 1.5, 40);
                FX.send(level, ParticleTypes.FLASH, c.position().add(0, 1, 0), 1, 0, 0);
                for (LivingEntity e : Combat.victims(c, c.position(), r)) {
                    if (Combat.magic(c, null, e, dmg)) {
                        Combat.applyOnHit(c, e, onHit(sc), dmg);
                        Combat.knock(c.position(), e, sc == School.EARTH || sc == School.WIND ? 1.5 : 0.7, sc == School.EARTH ? 0.7 : 0.3);
                        if (s.id().equals("wail_of_souls")) Combat.effect(e, MobEffects.WEAKNESS, 120, 1);
                        if (s.id().equals("blood_boil")) c.heal(dmg * 0.25F);
                    }
                }
                if (s.id().equals("blood_boil")) c.hurt(c.damageSources().magic(), 3.0F);
            }
            return true;
        });
    }

    private static void rain(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        School sc = s.school();
        Vec3 center = aimPoint(c, 32);
        Tasks.add(level, 60, t -> {
            FX.ring(level, FX.dust(s.color(), 1.5F), center.add(0, 0.2, 0), 5.5, 30, 0);
            if (t % 4 == 0) {
                double a = c.getRandom().nextDouble() * Math.PI * 2, rr = c.getRandom().nextDouble() * 5;
                Vec3 at = center.add(Math.cos(a) * rr, 0, Math.sin(a) * rr);
                if (sc == School.LIGHTNING) {
                    AbilityLogic.strike(c, at, dmg);
                } else {
                    MagicBolt b = MagicBolt.create(level, c, s.color(), s.color2(), sc == School.EARTH || sc == School.FIRE ? 0.9F : 0.5F, trail(sc));
                    b.setPos(at.x + 2, at.y + 18, at.z + 1);
                    b.aim(at.subtract(b.position()), 1.0).dmg(dmg, onHit(sc)).boom(1.8F).life(60).fire();
                }
            }
            return true;
        });
    }

    private static void wall(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        School sc = s.school();
        Vec3 look = c.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 side = new Vec3(-look.z, 0, look.x);
        Vec3 base = c.position().add(look.scale(3.5));
        BlockState st = switch (sc) {
            case ICE -> Blocks.PACKED_ICE.defaultBlockState();
            case EARTH -> Blocks.COBBLESTONE.defaultBlockState();
            case FIRE -> Blocks.FIRE.defaultBlockState();
            default -> Blocks.GLASS.defaultBlockState();
        };
        int height = sc == School.FIRE ? 1 : 3;
        for (int i = -3; i <= 3; i++) {
            Vec3 p = base.add(side.scale(i));
            BlockPos bp = BlockPos.containing(p.x, c.getY(), p.z);
            for (int k = 0; k < 4 && level.getBlockState(bp.below()).isAir(); k++) bp = bp.below();
            for (int h = 0; h < height; h++) {
                BlockPos q = bp.above(h);
                if (TempBlocks.place(level, q, st, sc == School.FIRE ? 120 : 200)) {
                    FX.send(level, particle(sc), Vec3.atCenterOf(q), 6, 0.4, 0.02);
                }
            }
        }
        if (sc == School.FIRE) {
            Vec3 b = base;
            Tasks.add(level, 120, t -> {
                if (t % 10 == 0) for (LivingEntity e : Combat.onLine(c, b.add(side.scale(-3.5)), b.add(side.scale(3.5)), 1.0)) {
                    Combat.magic(c, null, e, dmg);
                    e.setSecondsOnFire(4);
                }
                return true;
            });
        }
    }

    private static void effect(LivingEntity e, MobEffect eff, int ticks, int amp) { e.addEffect(new MobEffectInstance(eff, ticks, amp)); }

    private static void buff(LivingEntity c, SpellDef s, float power) {
        int dur = (int) (600 * Math.min(2.0F, power));
        switch (s.id()) {
            case "phoenix_blessing" -> { effect(c, MobEffects.FIRE_RESISTANCE, dur, 0); effect(c, MobEffects.DAMAGE_BOOST, dur / 2, 1); effect(c, MobEffects.REGENERATION, 200, 1); }
            case "ice_armor" -> { effect(c, MobEffects.DAMAGE_RESISTANCE, dur, 1); effect(c, RpgEffects.THORN_AURA.get(), dur, 0); }
            case "stone_skin" -> { effect(c, MobEffects.DAMAGE_RESISTANCE, dur, 2); effect(c, MobEffects.MOVEMENT_SLOWDOWN, dur, 0); }
            case "wind_walk" -> { effect(c, MobEffects.MOVEMENT_SPEED, dur, 2); effect(c, MobEffects.JUMP, dur, 1); effect(c, MobEffects.SLOW_FALLING, dur, 0); }
            case "water_breath" -> { effect(c, MobEffects.WATER_BREATHING, dur * 3, 0); effect(c, MobEffects.DOLPHINS_GRACE, dur, 0); effect(c, MobEffects.CONDUIT_POWER, dur, 0); }
            case "divine_shield" -> { effect(c, RpgEffects.DIVINE_SHIELD.get(), 160, 0); effect(c, MobEffects.GLOWING, 160, 0); }
            case "cloak_of_shadows" -> { effect(c, MobEffects.INVISIBILITY, dur / 2, 0); effect(c, MobEffects.MOVEMENT_SPEED, dur / 2, 1); effect(c, MobEffects.NIGHT_VISION, dur, 0); }
            case "barkskin" -> { effect(c, MobEffects.DAMAGE_RESISTANCE, dur, 1); effect(c, MobEffects.REGENERATION, dur / 3, 0); effect(c, RpgEffects.THORN_AURA.get(), dur, 0); }
            case "mana_shield" -> { effect(c, MobEffects.ABSORPTION, dur, 3); effect(c, RpgEffects.MANA_FLOW.get(), dur, 0); }
            case "sanguine_pact" -> { effect(c, RpgEffects.BLOOD_PACT.get(), dur, 0); effect(c, MobEffects.DAMAGE_BOOST, dur, 0); c.hurt(c.damageSources().magic(), 4.0F); }
            case "ancestral_guard" -> {
                effect(c, MobEffects.DAMAGE_RESISTANCE, dur, 1);
                for (int i = 0; i < 2; i++) Summon.spawn(c, Summon.Kind.SPIRIT_WARRIOR, c.getX() + (i == 0 ? 1.5 : -1.5), c.getY(), c.getZ(), 600, power);
            }
            default -> effect(c, MobEffects.DAMAGE_BOOST, dur, 0);
        }
        FX.column(c.level(), FX.dust(s.color(), 1.5F), c.position(), c.getBbHeight() + 0.5, 0.8, 60);
        FX.burstRing(c.level(), particle(s.school()), c.position().add(0, 0.2, 0), 24, 0.25);
    }

    private static void heal(LivingEntity c, SpellDef s, float amount, ServerLevel level) {
        List<LivingEntity> allies = level.getEntitiesOfClass(LivingEntity.class, c.getBoundingBox().inflate(s.id().equals("healing_rain") || s.id().equals("greater_heal") ? 8 : 4),
                e -> e == c || Combat.sameSide(c, e) && !(e instanceof net.minecraft.world.entity.monster.Enemy));
        for (LivingEntity e : allies) {
            if (s.id().equals("purify")) {
                e.getActiveEffects().stream().filter(x -> !x.getEffect().isBeneficial()).map(MobEffectInstance::getEffect).toList().forEach(e::removeEffect);
                e.clearFire();
            }
            if (s.id().equals("blood_ritual") && e == c) {
                // converts nearby enemies' blood into life
                for (LivingEntity v : Combat.victims(c, c.position(), 7)) {
                    if (Combat.magic(c, null, v, amount * 0.4F)) FX.line(level, FX.dust(0xC01020, 1.0F), v.position().add(0, 1, 0), c.position().add(0, 1, 0), 0.4, 0.05);
                }
            }
            e.heal(amount);
            if (s.id().equals("regrowth") || s.id().equals("healing_rain")) effect(e, MobEffects.REGENERATION, 160, 1);
            FX.spiral(level, s.school() == com.krolasyon.bosses.rpg.def.RpgDefs.School.WATER ? ParticleTypes.SPLASH : ParticleTypes.HAPPY_VILLAGER, e.position(), 2.2, 0.7, 2, 24);
            FX.send(level, ParticleTypes.HEART, e.position().add(0, e.getBbHeight() + 0.3, 0), 3, 0.3, 0.02);
        }
        if (s.id().equals("healing_rain")) {
            Vec3 p = c.position();
            Tasks.add(level, 80, t -> {
                FX.column(level, ParticleTypes.FALLING_WATER, p.add(0, 3, 0), 1.5, 5, 20);
                return true;
            });
        }
        FX.sound(level, c.position(), SoundEvents.PLAYER_LEVELUP, 0.6F, 1.6F);
    }

    private static void summon(LivingEntity c, SpellDef s, float power) {
        Summon.Kind kind;
        int n;
        switch (s.id()) {
            case "summon_wolves" -> { kind = Summon.Kind.WOLF; n = 3; }
            case "raise_dead" -> { kind = Summon.Kind.SKELETON; n = 3; }
            case "angel_summon" -> { kind = Summon.Kind.ANGEL; n = 1; }
            case "treant_call" -> { kind = Summon.Kind.TREANT; n = 1; }
            case "blood_golem" -> { kind = Summon.Kind.BLOOD_GOLEM; n = 1; }
            default -> { kind = Summon.Kind.SPIRIT_WOLF; n = 2; }
        }
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + c.getYRot() * Math.PI / 180;
            Summon.spawn(c, kind, c.getX() + Math.cos(a) * 2, c.getY() + 0.2, c.getZ() + Math.sin(a) * 2, 900, power);
        }
        FX.sound(c.level(), c.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 1.0F);
    }

    private static void teleport(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        Vec3 from = c.position();
        if (s.id().equals("air_dash")) {
            Vec3 d = c.getLookAngle().scale(2.2);
            c.setDeltaMovement(d.x, Math.max(0.3, d.y), d.z);
            c.hurtMarked = true;
            effect(c, MobEffects.SLOW_FALLING, 40, 0);
            FX.cone(level, ParticleTypes.CLOUD, c.position(), d.scale(-1), 3, 0.5, 30);
            return;
        }
        Vec3 eye = eye(c), to = eye.add(c.getLookAngle().scale(14));
        BlockHitResult hit = level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, c));
        Vec3 dest = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation().subtract(c.getLookAngle().scale(0.8));
        BlockPos bp = BlockPos.containing(dest);
        for (int i = 0; i < 10 && level.getBlockState(bp.below()).isAir(); i++) bp = bp.below();
        FX.send(level, particle(s.school()), from.add(0, 1, 0), 40, 0.4, 0.1);
        FX.send(level, ParticleTypes.PORTAL, from.add(0, 1, 0), 40, 0.4, 0.4);
        c.teleportTo(dest.x, bp.getY(), dest.z);
        c.fallDistance = 0;
        FX.send(level, particle(s.school()), c.position().add(0, 1, 0), 40, 0.4, 0.1);
        FX.sound(level, c.position(), SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.2F);
        switch (s.id()) {
            case "lightning_step" -> {
                FX.zigzag(level, FX.dust(0xA0C0FF, 1.2F), from.add(0, 1, 0), c.position().add(0, 1, 0), 8, 0.8);
                for (LivingEntity e : Combat.onLine(c, from.add(0, 1, 0), c.position().add(0, 1, 0), 1.2)) {
                    Combat.magic(c, null, e, dmg * 1.5F);
                    Combat.applyOnHit(c, e, OnHit.SHOCK, dmg);
                }
            }
            case "shadow_step" -> effect(c, MobEffects.INVISIBILITY, 60, 0);
            case "ghost_walk" -> { effect(c, MobEffects.INVISIBILITY, 100, 0); effect(c, MobEffects.MOVEMENT_SPEED, 100, 2); }
            default -> {}
        }
    }

    private static void chain(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        LivingEntity first = aimEntity(c, 24);
        if (first == null) {
            List<LivingEntity> near = Combat.victims(c, c.position().add(c.getLookAngle().scale(6)), 7);
            if (near.isEmpty()) return;
            first = near.get(0);
        }
        java.util.Set<Integer> hit = new java.util.HashSet<>();
        LivingEntity cur = first;
        Vec3 prev = eye(c).subtract(0, 0.3, 0);
        for (int jump = 0; jump < 5 && cur != null; jump++) {
            Vec3 p = cur.position().add(0, cur.getBbHeight() * 0.5, 0);
            FX.zigzag(level, FX.dust(s.color(), 1.0F), prev, p, 6, 0.6);
            FX.zigzag(level, particle(s.school()), prev, p, 4, 0.4);
            if (Combat.magic(c, null, cur, dmg * (1 - jump * 0.12F))) {
                Combat.applyOnHit(c, cur, onHit(s.school()), dmg);
                if (s.school() == School.BLOOD) c.heal(dmg * 0.25F);
            }
            hit.add(cur.getId());
            prev = p;
            LivingEntity next = null;
            double best = 49;
            for (LivingEntity e : Combat.victims(c, cur.position(), 7)) {
                if (hit.contains(e.getId())) continue;
                double d = e.distanceToSqr(cur);
                if (d < best) { best = d; next = e; }
            }
            cur = next;
        }
    }

    private static void aura(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        Tasks.add(level, 200, t -> {
            if (!c.isAlive()) return false;
            Vec3 p = c.position();
            if (t % 3 == 0) FX.ring(level, FX.dust(s.color(), 1.0F), p.add(0, 0.15, 0), 5, 30, 0.02);
            if (t % 10 != 0) return true;
            switch (s.id()) {
                case "static_field" -> {
                    for (LivingEntity e : Combat.victims(c, p, 5)) {
                        FX.zigzag(level, FX.dust(0xA0C0FF, 0.8F), p.add(0, 1, 0), e.position().add(0, 1, 0), 4, 0.5);
                        Combat.magic(c, null, e, dmg);
                    }
                }
                case "feather_fall" -> level.getEntitiesOfClass(LivingEntity.class, c.getBoundingBox().inflate(6), e -> Combat.sameSide(c, e))
                        .forEach(e -> effect(e, MobEffects.SLOW_FALLING, 40, 0));
                case "sacred_ground" -> {
                    level.getEntitiesOfClass(LivingEntity.class, c.getBoundingBox().inflate(5), e -> Combat.sameSide(c, e)).forEach(e -> e.heal(1.5F));
                    for (LivingEntity e : Combat.victims(c, p, 5)) if (e.getMobType() == MobType.UNDEAD) { Combat.magic(c, null, e, dmg * 2); e.setSecondsOnFire(2); }
                    FX.column(level, ParticleTypes.END_ROD, p, 2, 5, 10);
                }
                case "time_slow" -> {
                    for (LivingEntity e : Combat.victims(c, p, 7)) { effect(e, MobEffects.MOVEMENT_SLOWDOWN, 30, 3); effect(e, MobEffects.DIG_SLOWDOWN, 30, 2); }
                    FX.sphere(level, ParticleTypes.REVERSE_PORTAL, p.add(0, 1, 0), 6, 30);
                }
                default -> {}
            }
            return true;
        });
    }

    private static void trap(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        Vec3 at = aimPoint(c, 24);
        Tasks.add(level, 100, t -> {
            if (t % 2 == 0) FX.ring(level, FX.dust(s.color(), 1.3F), at.add(0, 0.15, 0), 3.5, 24, 0.02);
            if (t % 10 != 0) return true;
            for (LivingEntity e : Combat.victims(c, at, 3.8)) {
                switch (s.id()) {
                    case "absolute_zero" -> { Combat.applyOnHit(c, e, OnHit.FREEZE, dmg); effect(e, MobEffects.MOVEMENT_SLOWDOWN, 30, 6); e.setTicksFrozen(300); }
                    case "quicksand" -> { effect(e, MobEffects.MOVEMENT_SLOWDOWN, 30, 4); e.setDeltaMovement(e.getDeltaMovement().add(0, -0.2, 0)); }
                    case "entangle" -> Combat.applyOnHit(c, e, OnHit.ROOT, dmg);
                    case "drowning_prison" -> { effect(e, MobEffects.LEVITATION, 15, 0); e.setAirSupply(Math.max(-20, e.getAirSupply() - 60)); }
                    default -> {}
                }
                Combat.magic(c, null, e, dmg * 0.5F);
                FX.send(level, particle(s.school()), e.position().add(0, 0.5, 0), 10, 0.4, 0.05);
            }
            return true;
        });
    }

    private static void pull(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        if (s.id().equals("telekinesis")) {
            LivingEntity t = aimEntity(c, 24);
            if (t != null) {
                Vec3 d = c.position().subtract(t.position()).normalize().scale(1.4);
                t.setDeltaMovement(d.x, 0.5, d.z);
                t.hurtMarked = true;
                FX.line(level, FX.dust(s.color(), 1.0F), eye(c), t.position().add(0, 1, 0), 0.4, 0.05);
                Combat.magic(c, null, t, dmg);
            }
            return;
        }
        Vec3 at = aimPoint(c, 24).add(0, 1, 0);
        boolean hole = s.id().equals("black_hole");
        double r = hole ? 10 : 7;
        Tasks.add(level, hole ? 80 : 60, t -> {
            FX.spiral(level, FX.dust(s.color(), 1.2F), at.subtract(0, 1, 0), hole ? 2 : 4, 1 + (t % 12) * 0.4, 2, 30);
            if (hole) FX.sphere(level, ParticleTypes.SQUID_INK, at, 1.2, 10);
            if (s.id().equals("tornado")) FX.spiral(level, ParticleTypes.CLOUD, at.subtract(0, 1, 0), 6, 2.5, 3, 30);
            for (LivingEntity e : Combat.victims(c, at, r)) {
                Vec3 d = at.subtract(e.position()).normalize().scale(hole ? 0.16 : 0.1);
                e.setDeltaMovement(e.getDeltaMovement().add(d.x, s.id().equals("tornado") ? 0.08 : d.y * 0.5, d.z));
                e.hurtMarked = true;
                if (t % 10 == 0) Combat.magic(c, null, e, dmg);
            }
            return true;
        });
    }

    private static void strike(LivingEntity c, SpellDef s, float dmg, ServerLevel level) {
        Vec3 at = aimPoint(c, 32);
        School sc = s.school();
        Tasks.add(level, 30, t -> {
            if (t < 15) {
                FX.ring(level, FX.dust(s.color(), 1.5F), at.add(0, 0.1, 0), 2.5 - t * 0.12, 24, 0);
                FX.send(level, particle(sc), at.add(0, 6 - t * 0.4, 0), 4, 0.3, 0.02);
                return true;
            }
            if (t == 15) {
                switch (s.id()) {
                    case "wrath_of_sky" -> { for (int i = 0; i < 3; i++) AbilityLogic.strike(c, at.add((i - 1) * 1.2, 0, 0), dmg * 0.6F); }
                    case "starfall" -> {
                        MagicBolt b = MagicBolt.create(level, c, s.color(), s.color2(), 1.6F, MagicBolt.ARCANE);
                        b.setPos(at.x, at.y + 20, at.z);
                        b.aim(new Vec3(0, -1, 0), 1.6).dmg(dmg, OnHit.NONE).boom(4.0F).life(40).fire();
                    }
                    case "earth_spikes", "glacial_spike" -> {
                        BlockState st = s.id().equals("earth_spikes") ? Blocks.POINTED_DRIPSTONE.defaultBlockState() : Blocks.BLUE_ICE.defaultBlockState();
                        for (int i = 0; i < 6; i++) {
                            double a = i * Math.PI / 3;
                            TempBlocks.place(level, BlockPos.containing(at.x + Math.cos(a) * 1.5, at.y, at.z + Math.sin(a) * 1.5), st, 60);
                        }
                    }
                    default -> {}
                }
                FX.column(level, particle(sc), at, 8, 1.2, 120);
                FX.column(level, FX.dust(s.color(), 2.5F), at, 8, 1.0, 80);
                FX.send(level, ParticleTypes.EXPLOSION, at.add(0, 1, 0), 3, 0.8, 0);
                FX.sound(level, at, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.8F);
                if (!s.id().equals("starfall")) {
                    for (LivingEntity e : Combat.victims(c, at.add(0, 1, 0), 3)) {
                        float m = s.id().equals("banish") && (e.getMobType() == MobType.UNDEAD) ? 2.0F : 1.0F;
                        if (Combat.magic(c, null, e, dmg * m)) {
                            Combat.applyOnHit(c, e, onHit(sc), dmg);
                            Combat.knock(at, e, 0.4, sc == School.EARTH ? 1.0 : 0.5);
                        }
                    }
                }
            }
            return true;
        });
    }

    /** id helper for UI */
    @Nullable
    public static Entity unused() { return null; }
}
