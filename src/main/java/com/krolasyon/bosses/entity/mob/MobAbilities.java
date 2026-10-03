package com.krolasyon.bosses.entity.mob;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.EruptionEntity;
import com.krolasyon.bosses.entity.GenericBoltEntity;
import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

/** Behaviour of every {@link Ab.Kind}: one {@code tick} per game tick while the ability plays. */
public final class MobAbilities {
    private MobAbilities() {}

    // ------------------------------------------------------------------ helpers
    private static boolean at(Ab ab, int t) {
        for (int h : ab.hits) if (t == h) return true;
        return false;
    }

    private static int first(Ab ab) { return ab.hits[0]; }

    private static float base(HellMob m, Ab ab) { return (float) m.getAttributeValue(Attributes.ATTACK_DAMAGE) * ab.dmg; }

    private static void snd(HellMob m, SoundEvent e, float vol, float pitch) { m.sound(e, vol, pitch); }

    public static ParticleOptions fx(Ab ab) {
        return switch (ab.particle) {
            case "flame" -> ParticleTypes.FLAME;
            case "soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "smoke" -> ParticleTypes.LARGE_SMOKE;
            case "crit" -> ParticleTypes.CRIT;
            case "spore" -> ParticleTypes.SPORE_BLOSSOM_AIR;
            case "void" -> ParticleTypes.PORTAL;
            case "ash" -> ParticleTypes.ASH;
            case "drip" -> ParticleTypes.DRIPPING_LAVA;
            case "heart" -> ParticleTypes.HEART;
            case "rod" -> ParticleTypes.END_ROD;
            case "witch" -> ParticleTypes.WITCH;
            default -> BossEntity.dust(ab.color, 1.5F);
        };
    }

    public static int style(Ab ab) {
        return switch (ab.particle) {
            case "flame" -> GenericBoltEntity.ST_FIRE;
            case "soul" -> GenericBoltEntity.ST_SOUL;
            case "smoke" -> GenericBoltEntity.ST_SMOKE;
            case "spore" -> GenericBoltEntity.ST_SPORE;
            case "void" -> GenericBoltEntity.ST_VOID;
            case "drip" -> GenericBoltEntity.ST_BLOOD;
            case "ash" -> GenericBoltEntity.ST_BONE;
            case "rod" -> GenericBoltEntity.ST_LIGHT;
            default -> GenericBoltEntity.ST_SOUL;
        };
    }

    private static void puff(ServerLevel sl, Ab ab, double x, double y, double z, int n, double spread, double speed) {
        sl.sendParticles(BossEntity.dust(ab.color, 1.5F), x, y, z, n, spread, spread, spread, speed);
        if (!ab.particle.equals("dust")) sl.sendParticles(fx(ab), x, y, z, Math.max(1, n / 3), spread, spread, spread, speed * 0.5);
    }

    private static void ring(ServerLevel sl, Ab ab, Vec3 c, double r, int n, double up) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(BossEntity.dust(ab.color, 1.7F), c.x + Math.cos(a) * r, c.y + 0.15, c.z + Math.sin(a) * r, 0, Math.cos(a), up, Math.sin(a), 0.35);
        }
    }

    private static void effect(LivingEntity e, Ab ab) {
        if (ab.effect.isEmpty()) return;
        MobEffect me = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(ab.effect));
        if (me != null) e.addEffect(new MobEffectInstance(me, ab.effTicks, ab.effLevel));
    }

    private static Vec3 aim(HellMob m, @Nullable LivingEntity t) {
        if (t == null) return m.forward();
        return t.position().add(0, t.getBbHeight() * 0.55, 0).subtract(m.getEyePosition()).normalize();
    }

    private static Vec3 flat(Vec3 v, Vec3 fallback) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1.0E-4 ? fallback : h.normalize();
    }

    private static void line(ServerLevel sl, Ab ab, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 0.01) return;
        d = d.scale(1.0 / len);
        for (double s = 0; s < len; s += step) {
            Vec3 p = a.add(d.scale(s));
            sl.sendParticles(BossEntity.dust(ab.color, 1.2F), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0);
        }
    }

    // ------------------------------------------------------------------ dispatcher
    public static void tick(HellMob m, Ab ab, int t, @Nullable LivingEntity target) {
        ServerLevel sl = m.serverLevel();
        if (t == 0) {
            m.abHits.clear();
            m.abState = 0;
            m.abVec = target != null ? flat(target.position().subtract(m.position()), m.forward()) : m.forward();
            windup(m, ab);
        }
        switch (ab.kind) {
            case MELEE -> {
                if (at(ab, t)) swing(m, ab, sl, t == first(ab) ? 1 : -1);
            }
            case SLAM -> slam(m, ab, sl, t);
            case LEAP -> leap(m, ab, sl, t, target);
            case CHARGE -> charge(m, ab, sl, t);
            case BOLT, SNIPE -> {
                if (at(ab, t) && target != null) shoot(m, ab, sl, target, 0F);
            }
            case VOLLEY -> volley(m, ab, sl, t, target);
            case BEAM -> beam(m, ab, sl, t, target);
            case CONE -> cone(m, ab, sl, t, target);
            case BURST -> burst(m, ab, sl, t);
            case SUMMON -> summon(m, ab, sl, t, target);
            case TELEPORT_STRIKE -> teleportStrike(m, ab, sl, t, target);
            case BLINK_AWAY -> blink(m, ab, sl, t);
            case HEAL_ALLIES -> healAllies(m, ab, sl, t);
            case DRAIN -> drain(m, ab, sl, t, target);
            case PULL -> pull(m, ab, sl, t, target);
            case CLOUD -> cloud(m, ab, sl, t, target);
            case ERUPT -> erupt(m, ab, sl, t, target);
            case BUFF -> buff(m, ab, sl, t);
            case METEOR -> meteor(m, ab, sl, t, target);
            case DIVE -> dive(m, ab, sl, t, target);
            case SHIELD -> shield(m, ab, sl, t);
            case RETREAT -> retreat(m, ab, sl, t, target);
        }
    }

    private static void windup(HellMob m, Ab ab) {
        float pitch = 0.85F + m.getRandom().nextFloat() * 0.3F;
        switch (ab.kind) {
            case MELEE -> snd(m, ModSounds.BLADE_SLASH.get(), 0.9F, pitch);
            case SLAM -> snd(m, ModSounds.WARDEN_SWIPE.get(), 1.4F, pitch * 0.7F);
            case LEAP -> snd(m, ModSounds.HOUND_LEAP.get(), 1.0F, pitch);
            case CHARGE, DIVE -> snd(m, ModSounds.SHADOW_DASH.get(), 1.1F, pitch);
            case BEAM -> snd(m, ModSounds.LASER_CHARGE.get(), 1.0F, pitch);
            case SUMMON -> snd(m, ModSounds.SUMMON.get(), 1.2F, pitch);
            case DRAIN, HEAL_ALLIES -> snd(m, ModSounds.DRAIN.get(), 0.8F, pitch);
            case SHIELD -> snd(m, ModSounds.PRISON_FORM.get(), 0.8F, pitch * 1.3F);
            default -> {}
        }
    }

    // ------------------------------------------------------------------ melee
    private static void swing(HellMob m, Ab ab, ServerLevel sl, int side) {
        Vec3 f = m.forward();
        double reach = m.meleeReach();
        for (int i = 0; i < 9; i++) {
            double a = (i / 8.0 - 0.5) * Math.PI * 0.9 * side;
            Vec3 d = f.yRot((float) a).scale(reach * 0.95);
            sl.sendParticles(BossEntity.dust(ab.color, 1.3F), m.getX() + d.x, m.getY() + m.getBbHeight() * 0.62 - i * 0.03, m.getZ() + d.z, 1, 0, 0, 0, 0);
        }
        Vec3 c = m.position().add(f.scale(reach * 0.6)).add(0, m.getBbHeight() * 0.5, 0);
        if (!ab.particle.equals("dust")) sl.sendParticles(fx(ab), c.x, c.y, c.z, 4, 0.3, 0.2, 0.3, 0.02);
        double r = reach * 0.7 + 0.5;
        for (LivingEntity e : m.hostilesIn(new AABB(c, c).inflate(r, m.getBbHeight() * 0.6 + 0.5, r))) {
            Vec3 to = e.position().subtract(m.position());
            if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < -0.15) continue;
            float dmg = base(m, ab);
            m.hit(e, m.damageSources().mobAttack(m), dmg, ab.a > 0 ? ab.a : 0.45, 0.2);
            effect(e, ab);
            if (ab.b > 0) m.heal((float) (dmg * ab.b));
            if (ab.particle.equals("flame")) e.setSecondsOnFire(3);
        }
    }

    private static void slam(HellMob m, Ab ab, ServerLevel sl, int t) {
        if (t < first(ab) && t % 3 == 0) puff(sl, ab, m.getX(), m.getY() + m.getBbHeight() * 0.8, m.getZ(), 2, 0.4, 0.01);
        if (!at(ab, t)) return;
        Vec3 c = m.position().add(m.forward().scale(ab.b));
        double r = ab.a > 0 ? ab.a : 4;
        snd(m, ModSounds.FISSURE.get(), 1.5F, 0.9F + m.getRandom().nextFloat() * 0.2F);
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 2, r * 0.2, 0.1, r * 0.2, 0);
        ring(sl, ab, c, r * 0.55, 24, 0.15);
        ring(sl, ab, c, r * 0.95, 36, 0.1);
        puff(sl, ab, c.x, c.y + 0.3, c.z, 24, r * 0.4, 0.1);
        for (LivingEntity e : m.hostilesAround(c, r)) {
            m.hit(e, m.damageSources().mobAttack(m), base(m, ab), 0.9, 0.55);
            effect(e, ab);
        }
    }

    private static void leap(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t == first(ab)) {
            Vec3 d = target != null ? flat(target.position().subtract(m.position()), m.forward()) : m.forward();
            double dist = target != null ? m.distanceTo(target) : 6;
            double v = Mth.clamp(dist * 0.13, 0.5, ab.a > 0 ? ab.a : 1.4);
            m.setDeltaMovement(d.x * v, ab.b > 0 ? ab.b : 0.7, d.z * v);
            m.hasImpulse = true;
            m.abState = 1;
        } else if (t > first(ab) + 3 && m.abState == 1 && m.onGround()) {
            m.abState = 2;
            snd(m, ModSounds.HOUND_LAND.get(), 1.2F, 1.0F);
            ring(sl, ab, m.position(), 2.0, 20, 0.12);
            puff(sl, ab, m.getX(), m.getY() + 0.3, m.getZ(), 14, 1.0, 0.05);
            for (LivingEntity e : m.hostilesAround(m.position(), 3.2)) {
                m.hit(e, m.damageSources().mobAttack(m), base(m, ab), 0.7, 0.4);
                effect(e, ab);
            }
        }
    }

    private static void charge(HellMob m, Ab ab, ServerLevel sl, int t) {
        int len = ab.a > 0 ? (int) ab.a : 12;
        double speed = ab.b > 0 ? ab.b : 1.0;
        if (t < first(ab)) {
            puff(sl, ab, m.getX(), m.getY() + 0.2, m.getZ(), 2, 0.3, 0.02);
            return;
        }
        if (t >= first(ab) + len) return;
        m.setDeltaMovement(m.abVec.x * speed, Math.min(0, m.getDeltaMovement().y), m.abVec.z * speed);
        m.hasImpulse = true;
        puff(sl, ab, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 3, 0.4, 0.02);
        for (LivingEntity e : m.hostilesIn(m.getBoundingBox().inflate(0.9, 0.3, 0.9))) {
            if (!m.abHits.add(e.getId())) continue;
            m.hit(e, m.damageSources().mobAttack(m), base(m, ab), 0.9, 0.35);
            effect(e, ab);
        }
    }

    // ------------------------------------------------------------------ ranged
    private static void shoot(HellMob m, Ab ab, ServerLevel sl, LivingEntity target, float spread) {
        Vec3 from = m.getEyePosition().add(m.forward().scale(0.5)).add(0, -0.15, 0);
        Vec3 dir = aim(m, target);
        if (spread != 0) dir = dir.yRot(spread).add(0, (m.getRandom().nextFloat() - 0.5F) * 0.1, 0).normalize();
        double speed = ab.a > 0 ? ab.a : (ab.kind == Ab.Kind.SNIPE ? 2.6 : 1.15);
        GenericBoltEntity b = GenericBoltEntity.create(m.level(), m, from, dir, speed, base(m, ab), (float) ab.b, ab.color, style(ab));
        if (!ab.effect.isEmpty()) b.withEffect(ab.effect, ab.effTicks, ab.effLevel);
        if (ab.kind == Ab.Kind.SNIPE) b.piercing().withLife(40);
        m.level().addFreshEntity(b);
        snd(m, ab.kind == Ab.Kind.SNIPE ? ModSounds.LASER_FIRE.get() : ModSounds.BOLT_SHOOT.get(), 1.0F, 0.9F + m.getRandom().nextFloat() * 0.3F);
        puff(sl, ab, from.x, from.y, from.z, 6, 0.15, 0.03);
    }

    private static void volley(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (!at(ab, t) || target == null) return;
        int n = ab.a > 0 ? (int) ab.a : 5;
        float spread = ab.b > 0 ? (float) ab.b : 0.5F;
        for (int i = 0; i < n; i++) {
            float s = n == 1 ? 0 : (i / (float) (n - 1) - 0.5F) * spread * 2F;
            shoot(m, ab, sl, target, s);
        }
    }

    private static void beam(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        int len = ab.a > 0 ? (int) ab.a : 20;
        Vec3 eye = m.getEyePosition();
        if (t < first(ab)) {
            sl.sendParticles(BossEntity.dust(ab.color, 1.0F + t * 0.05F), eye.x + m.forward().x * 0.5, eye.y, eye.z + m.forward().z * 0.5, 2, 0.15, 0.15, 0.15, 0.02);
            return;
        }
        if (t >= first(ab) + len || target == null) return;
        Vec3 dir = aim(m, target);
        double range = ab.maxR > 0 ? ab.maxR : 20;
        Vec3 end = eye.add(dir.scale(range));
        BlockHitResult hr = m.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, m));
        if (hr.getType() != net.minecraft.world.phys.HitResult.Type.MISS) end = hr.getLocation();
        line(sl, ab, eye.add(dir.scale(0.6)), end, 0.5);
        if (t % 4 == 0) {
            snd(m, ModSounds.LASER_FIRE.get(), 0.5F, 1.4F);
            AABB box = new AABB(eye, end).inflate(1.2);
            for (LivingEntity e : m.hostilesIn(box)) {
                Vec3 p = e.position().add(0, e.getBbHeight() * 0.5, 0);
                double along = p.subtract(eye).dot(dir);
                if (along < 0) continue;
                Vec3 closest = eye.add(dir.scale(along));
                if (closest.distanceToSqr(p) > 1.4 * 1.4) continue;
                if (e.hurt(m.damageSources().indirectMagic(m, m), base(m, ab))) effect(e, ab);
                puff(sl, ab, p.x, p.y, p.z, 5, 0.3, 0.05);
            }
        }
    }

    private static void cone(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        int len = ab.a > 0 ? (int) ab.a : 20;
        if (t < first(ab) || t >= first(ab) + len) return;
        Vec3 f = target != null ? aim(m, target) : m.forward();
        Vec3 eye = m.getEyePosition().add(m.forward().scale(0.4));
        double range = ab.maxR > 0 ? ab.maxR : 7;
        for (int i = 0; i < 7; i++) {
            double s = 1.0 + m.getRandom().nextDouble() * (range - 1.0);
            double sp = s * 0.18;
            Vec3 p = eye.add(f.scale(s)).add((m.getRandom().nextDouble() - 0.5) * sp * 2, (m.getRandom().nextDouble() - 0.5) * sp * 2, (m.getRandom().nextDouble() - 0.5) * sp * 2);
            sl.sendParticles(fx(ab), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.01);
            if (i % 2 == 0) sl.sendParticles(BossEntity.dust(ab.color, 1.6F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
        }
        if (t % 3 == 0) {
            snd(m, ModSounds.FIREBALL_SHOOT.get(), 0.4F, 1.6F);
            for (LivingEntity e : m.hostilesIn(new AABB(eye, eye.add(f.scale(range))).inflate(range * 0.35))) {
                Vec3 to = e.position().add(0, e.getBbHeight() * 0.5, 0).subtract(eye);
                if (to.length() > range + 0.5 || to.normalize().dot(f) < 0.72) continue;
                e.invulnerableTime = 0;
                if (e.hurt(m.damageSources().mobAttack(m), base(m, ab))) {
                    effect(e, ab);
                    if (ab.particle.equals("flame")) e.setSecondsOnFire(3);
                }
            }
        }
    }

    private static void burst(HellMob m, Ab ab, ServerLevel sl, int t) {
        double r = ab.a > 0 ? ab.a : 5;
        if (t < first(ab)) {
            if (t % 2 == 0) ring(sl, ab, m.position(), r * (1.0 - (first(ab) - t) / (double) first(ab) * 0.6), 18, 0.05);
            return;
        }
        if (!at(ab, t)) return;
        snd(m, ModSounds.VENGEANCE_BURST.get(), 1.1F, 1.0F + m.getRandom().nextFloat() * 0.3F);
        for (int k = 1; k <= 3; k++) ring(sl, ab, m.position().add(0, 0.5 * k, 0), r * k / 3.0, 12 + k * 10, 0.08);
        puff(sl, ab, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 30, r * 0.3, 0.12);
        for (LivingEntity e : m.hostilesAround(m.position(), r)) {
            m.hit(e, m.damageSources().mobAttack(m), base(m, ab), ab.b > 0 ? ab.b : 1.1, 0.45);
            effect(e, ab);
        }
    }

    // ------------------------------------------------------------------ support / summoning
    private static void summon(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (!at(ab, t)) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(com.krolasyon.bosses.KrolasyonBosses.MODID, ab.summon));
        if (type == null) return;
        int cap = ab.b > 0 ? (int) ab.b : 4;
        if (m.countMinions() >= cap) return;
        int n = Math.min(ab.a > 0 ? (int) ab.a : 2, cap - m.countMinions());
        for (int i = 0; i < n; i++) {
            Entity e = type.create(m.level());
            if (e == null) continue;
            double a = m.getRandom().nextDouble() * Math.PI * 2;
            double d = 1.8 + m.getRandom().nextDouble() * 1.4;
            double x = m.getX() + Math.cos(a) * d, z = m.getZ() + Math.sin(a) * d;
            e.moveTo(x, m.getY(), z, m.getRandom().nextFloat() * 360F, 0);
            if (e instanceof HellMob h) {
                h.setOwnerUUID(m.getOwnerUUID());
                h.setSummoner(m);
                if (target != null) h.setTarget(target);
            }
            if (e instanceof net.minecraft.world.entity.Mob mob) mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(e.blockPosition()), net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null, null);
            m.level().addFreshEntity(e);
            m.minions.add(e);
            puff(sl, ab, x, m.getY() + 0.5, z, 20, 0.4, 0.08);
            sl.sendParticles(ParticleTypes.POOF, x, m.getY() + 0.3, z, 8, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private static void healAllies(HellMob m, Ab ab, ServerLevel sl, int t) {
        if (!at(ab, t)) return;
        double r = ab.a > 0 ? ab.a : 9;
        float amount = ab.b > 0 ? (float) ab.b : 12F;
        for (HellMob o : m.level().getEntitiesOfClass(HellMob.class, m.getBoundingBox().inflate(r), o -> o.isAlive() && o.faction() == m.faction() && o != m)) {
            o.heal(amount);
            line(sl, ab, m.position().add(0, m.getBbHeight() * 0.7, 0), o.position().add(0, o.getBbHeight() * 0.6, 0), 0.6);
            sl.sendParticles(ParticleTypes.HEART, o.getX(), o.getY() + o.getBbHeight() + 0.2, o.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }
        m.heal(amount * 0.5F);
        ring(sl, ab, m.position(), r * 0.5, 24, 0.1);
    }

    private static void buff(HellMob m, Ab ab, ServerLevel sl, int t) {
        if (!at(ab, t)) return;
        snd(m, ModSounds.DEMON_PHASE.get(), 0.7F, 1.4F);
        effect(m, ab);
        if (ab.b > 0) m.heal((float) ab.b);
        ring(sl, ab, m.position(), 1.5, 20, 0.2);
        puff(sl, ab, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 24, m.getBbWidth() * 0.6, 0.1);
        double r = ab.a;
        if (r > 0) {
            for (HellMob o : m.level().getEntitiesOfClass(HellMob.class, m.getBoundingBox().inflate(r), o -> o.isAlive() && o.faction() == m.faction() && o != m)) effect(o, ab);
        }
    }

    private static void shield(HellMob m, Ab ab, ServerLevel sl, int t) {
        if (t == first(ab)) {
            m.setShielded(true, (float) (ab.a > 0 ? ab.a : 0.8), (float) ab.b);
        }
        if (t >= first(ab) && t % 3 == 0) {
            ring(sl, ab, m.position().add(0, m.getBbHeight() * 0.5, 0), m.getBbWidth() * 0.9, 10, 0.0);
        }
    }

    // ------------------------------------------------------------------ movement tricks
    private static void teleportStrike(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (target == null) return;
        if (t == first(ab)) {
            puff(sl, ab, m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.15);
            Vec3 look = flat(target.getLookAngle(), m.forward());
            Vec3 back = target.position().subtract(look.scale(1.7));
            if (!m.randomTeleport(back.x, target.getY(), back.z, true)) m.randomTeleport(target.getX() + 1.6, target.getY(), target.getZ(), true);
            snd(m, ModSounds.BLINK.get(), 1.0F, 1.2F);
            puff(sl, ab, m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.15);
            m.faceTowards(target);
        }
        if (ab.hits.length > 1 && t == ab.hits[1]) swing(m, ab, sl, 1);
    }

    private static void blink(HellMob m, Ab ab, ServerLevel sl, int t) {
        if (t != first(ab)) return;
        double r = ab.a > 0 ? ab.a : 10;
        puff(sl, ab, m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.15);
        for (int i = 0; i < 10; i++) {
            double a = m.getRandom().nextDouble() * Math.PI * 2;
            double d = r * (0.5 + m.getRandom().nextDouble() * 0.5);
            if (m.randomTeleport(m.getX() + Math.cos(a) * d, m.getY(), m.getZ() + Math.sin(a) * d, true)) break;
        }
        snd(m, ModSounds.BLINK.get(), 1.0F, 1.0F);
        puff(sl, ab, m.getX(), m.getY() + 1, m.getZ(), 30, 0.4, 0.15);
    }

    private static void retreat(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (t != first(ab)) return;
        Vec3 away = target != null ? flat(m.position().subtract(target.position()), m.forward().scale(-1)) : m.forward().scale(-1);
        double v = ab.a > 0 ? ab.a : 1.1;
        m.setDeltaMovement(away.x * v, ab.b > 0 ? ab.b : 0.45, away.z * v);
        m.hasImpulse = true;
        puff(sl, ab, m.getX(), m.getY() + 0.2, m.getZ(), 10, 0.4, 0.05);
    }

    private static void dive(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        int len = ab.hits.length > 1 ? Math.max(4, ab.hits[1] - ab.hits[0]) : 10;
        if (t == first(ab) && target != null) {
            Vec3 d = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(m.position()).normalize();
            m.abVec = d;
            m.setDeltaMovement(d.scale(ab.a > 0 ? ab.a : 1.3));
            m.hasImpulse = true;
        }
        if (t > first(ab) && t < first(ab) + len) {
            Vec3 d = m.abVec;
            m.setDeltaMovement(d.scale(ab.a > 0 ? ab.a : 1.3));
            m.hasImpulse = true;
            puff(sl, ab, m.getX(), m.getY() + m.getBbHeight() * 0.5, m.getZ(), 2, 0.2, 0.02);
            for (LivingEntity e : m.hostilesIn(m.getBoundingBox().inflate(0.7))) {
                if (!m.abHits.add(e.getId())) continue;
                m.hit(e, m.damageSources().mobAttack(m), base(m, ab), 0.7, 0.3);
                effect(e, ab);
            }
        }
        if (t == first(ab) + len) {
            m.setDeltaMovement(m.getDeltaMovement().x * 0.3, ab.b > 0 ? ab.b : 0.7, m.getDeltaMovement().z * 0.3);
            m.hasImpulse = true;
        }
    }

    // ------------------------------------------------------------------ control / area
    private static void drain(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (target == null || t < first(ab)) {
            if (t % 3 == 0) puff(sl, ab, m.getX(), m.getY() + m.getBbHeight() * 0.7, m.getZ(), 2, 0.3, 0.02);
            return;
        }
        if (m.distanceToSqr(target) > (ab.maxR + 4) * (ab.maxR + 4)) return;
        Vec3 a = m.position().add(0, m.getBbHeight() * 0.7, 0);
        Vec3 b = target.position().add(0, target.getBbHeight() * 0.6, 0);
        line(sl, ab, b, a, 0.6);
        if ((t - first(ab)) % 5 == 0) {
            float dmg = base(m, ab);
            if (target.hurt(m.damageSources().indirectMagic(m, m), dmg)) {
                m.heal(dmg * (ab.b > 0 ? (float) ab.b : 1F));
                effect(target, ab);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + 1, target.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    private static void pull(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (target == null || !at(ab, t)) return;
        Vec3 a = m.position().add(0, m.getBbHeight() * 0.7, 0);
        Vec3 b = target.position().add(0, target.getBbHeight() * 0.6, 0);
        line(sl, ab, a, b, 0.4);
        snd(m, ModSounds.THORN_WHIP.get(), 1.2F, 1.0F);
        if (m.hostilesIn(target.getBoundingBox().inflate(0.1)).contains(target)) {
            Vec3 d = m.position().subtract(target.position());
            Vec3 h = new Vec3(d.x, 0, d.z);
            double len = h.length();
            if (len > 1.5) {
                h = h.normalize().scale(Math.min(ab.a > 0 ? ab.a : 1.6, len * 0.25 + 0.4));
                target.setDeltaMovement(h.x, 0.3, h.z);
                target.hurtMarked = true;
            }
            target.hurt(m.damageSources().mobAttack(m), base(m, ab));
            effect(target, ab);
        }
    }

    private static void cloud(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (!at(ab, t)) return;
        Vec3 p = target != null && ab.maxR > 0 && ab.minR >= 0 && m.distanceTo(target) > 2 ? target.position() : m.position();
        MobEffect me = ab.effect.isEmpty() ? null : ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(ab.effect));
        AreaEffectCloud c = new AreaEffectCloud(m.level(), p.x, p.y + 0.1, p.z);
        c.setOwner(m);
        float r = (float) (ab.a > 0 ? ab.a : 3);
        int dur = (int) (ab.b > 0 ? ab.b : 120);
        c.setRadius(r);
        c.setDuration(dur);
        c.setWaitTime(8);
        c.setRadiusPerTick(-r / (float) dur);
        c.setFixedColor(ab.color);
        c.setParticle(BossEntity.dust(ab.color, 1.4F));
        if (me != null) c.addEffect(new MobEffectInstance(me, ab.effTicks, ab.effLevel));
        m.level().addFreshEntity(c);
        snd(m, ModSounds.THORN_ERUPT.get(), 0.9F, 1.3F);
        puff(sl, ab, p.x, p.y + 0.5, p.z, 20, r * 0.4, 0.05);
    }

    private static void erupt(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (target == null || !at(ab, t)) return;
        int kind = (int) ab.a;
        float dmg = base(m, ab);
        int idx = 0;
        for (int i = 0; i < ab.hits.length; i++) if (ab.hits[i] == t) idx = i;
        EruptionEntity.spawn(m, target.getX(), target.getZ(), target.getY(), kind, 4, dmg);
        int ringN = (int) ab.b;
        for (int i = 0; i < ringN; i++) {
            double a = i * Math.PI * 2 / ringN + idx * 0.4;
            EruptionEntity.spawn(m, target.getX() + Math.cos(a) * 2.6, target.getZ() + Math.sin(a) * 2.6, target.getY(), kind, 5 + i % 3, dmg * 0.7F);
        }
    }

    private static void meteor(HellMob m, Ab ab, ServerLevel sl, int t, @Nullable LivingEntity target) {
        if (target == null || !at(ab, t)) return;
        double r = ab.b > 0 ? ab.b : 6;
        double a = m.getRandom().nextDouble() * Math.PI * 2;
        double d = m.getRandom().nextDouble() * r;
        Vec3 ground = target.position().add(Math.cos(a) * d, 0, Math.sin(a) * d);
        Vec3 from = ground.add(0, 16, 0);
        GenericBoltEntity b = GenericBoltEntity.create(m.level(), m, from, new Vec3(0, -1, 0), 1.2, base(m, ab), (float) (ab.a > 0 ? ab.a : 2.5), ab.color, style(ab));
        if (!ab.effect.isEmpty()) b.withEffect(ab.effect, ab.effTicks, ab.effLevel);
        b.withLife(60);
        m.level().addFreshEntity(b);
        ring(sl, ab, ground, 1.8, 14, 0.02);
        snd(m, ModSounds.FIREBALL_SHOOT.get(), 0.8F, 0.7F + m.getRandom().nextFloat() * 0.3F);
    }
}
