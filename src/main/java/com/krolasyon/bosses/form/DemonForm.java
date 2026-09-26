package com.krolasyon.bosses.form;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.entity.EruptionEntity;
import com.krolasyon.bosses.entity.HeartOrbEntity;
import com.krolasyon.bosses.network.ModNetwork;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Server side logic of the player "boss form": the Heartbreaker Demon transformation granted by the Kalp Kırıcı Kılıcı.
 * Five abilities (keys R G V B N by default), a shadow double jump, claw attacks, stat boosts and all their effects.
 */
public final class DemonForm {
    private DemonForm() {}

    public static final int WHIP = 0, HEARTS = 1, DASH = 2, TEMPEST = 3, JUDGEMENT = 4, COUNT = 5;
    public static final int ANIM_TRANSFORM = 10, ANIM_FLIP = 11, ANIM_SLAM = 12;
    public static final String[] NAMES = {"thorn_whip", "heart_volley", "shadow_dash", "scythe_tempest", "heart_judgement"};
    public static final int[] DURATION = {24, 36, 20, 32, 70};
    public static final int[] COOLDOWN = {100, 160, 110, 200, 400};
    public static final int TRANSFORM_TICKS = 36;

    public static final int CRIMSON = 0xE0405A, SHADOW = 0x1E060C, HEART_PINK = 0xFFB0B8, BRIGHT = 0xFF5A6A;
    private static final String TAG = "krolasyon_demon_form";

    /** set by the client on startup so common code (the blade item) can ask about client side players */
    public static Predicate<Player> clientDemon = p -> false;

    private static final Map<UUID, DemonState> STATES = new HashMap<>();

    private static final UUID HP = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d01");
    private static final UUID DMG = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d02");
    private static final UUID SPD = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d03");
    private static final UUID ARM = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d04");
    private static final UUID TGH = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d05");
    private static final UUID KB = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d06");
    private static final UUID ASPD = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d07");
    private static final UUID REACH = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d08");
    private static final UUID STEP = UUID.fromString("5f2a4a6e-1c1b-4d51-9a53-6b0b6f1a0d09");

    static final class DemonState {
        boolean transformed;
        int transformTick = -1;
        int ability = -1;
        int abilityTick;
        final int[] cd = new int[COUNT];
        final List<Integer> hit = new ArrayList<>();
        Vec3 dir = Vec3.ZERO;
        int slamAt = -1;
        boolean airJumped;
        int invuln;
    }

    static DemonState state(Player p) { return STATES.computeIfAbsent(p.getUUID(), k -> new DemonState()); }

    public static boolean isDemon(Player p) {
        if (p.level().isClientSide()) return clientDemon.test(p);
        DemonState s = STATES.get(p.getUUID());
        return s != null && s.transformed;
    }

    static boolean isInvulnerable(Player p) {
        DemonState s = STATES.get(p.getUUID());
        return s != null && s.transformed && s.invuln > 0;
    }

    // ------------------------------------------------------------------ transformation
    public static void transform(ServerPlayer p) {
        DemonState s = state(p);
        if (s.transformed) return;
        s.transformed = true;
        s.transformTick = 0;
        s.ability = -1;
        java.util.Arrays.fill(s.cd, 0);
        p.getPersistentData().putBoolean(TAG, true);
        applyStats(p, true);
        p.setHealth(p.getMaxHealth());
        sync(p);
        anim(p, ANIM_TRANSFORM);
        sendCooldowns(p);
    }

    /** re-apply the form silently (login) */
    static void restore(ServerPlayer p) {
        DemonState s = state(p);
        s.transformed = true;
        s.transformTick = -1;
        s.ability = -1;
        applyStats(p, true);
        sync(p);
        sendCooldowns(p);
    }

    public static void revert(ServerPlayer p, boolean fx) {
        DemonState s = state(p);
        if (!s.transformed) return;
        s.transformed = false;
        s.transformTick = -1;
        s.ability = -1;
        s.invuln = 0;
        p.getPersistentData().remove(TAG);
        applyStats(p, false);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        p.removeEffect(MobEffects.JUMP);
        sync(p);
        if (fx) {
            ServerLevel sl = p.serverLevel();
            sound(p, ModSounds.DEMON_REVERT.get(), 1.8F, 1.0F);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
            for (int i = 0; i < 60; i++) {
                double a = i * Math.PI * 2 / 60;
                sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 1.8F), p.getX(), p.getY() + 1.0, p.getZ(), 0, Math.cos(a), 0.25, Math.sin(a), 0.5);
            }
            sl.sendParticles(dust(HEART_PINK, 1.2F), p.getX(), p.getY() + 1.3, p.getZ(), 25, 0.3, 0.4, 0.3, 0);
        }
    }

    static boolean hasSavedForm(Player p) { return p.getPersistentData().getBoolean(TAG); }

    static void forget(Player p) { STATES.remove(p.getUUID()); }

    private static void applyStats(ServerPlayer p, boolean on) {
        mod(p, Attributes.MAX_HEALTH, HP, 40, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ATTACK_DAMAGE, DMG, 9, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.MOVEMENT_SPEED, SPD, 0.35, AttributeModifier.Operation.MULTIPLY_BASE, on);
        mod(p, Attributes.ARMOR, ARM, 12, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ARMOR_TOUGHNESS, TGH, 6, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.KNOCKBACK_RESISTANCE, KB, 0.6, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ATTACK_SPEED, ASPD, 1.2, AttributeModifier.Operation.ADDITION, on);
        mod(p, ForgeMod.ENTITY_REACH.get(), REACH, 1.5, AttributeModifier.Operation.ADDITION, on);
        mod(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP, 0.5, AttributeModifier.Operation.ADDITION, on);
    }

    private static void mod(ServerPlayer p, Attribute a, UUID id, double v, AttributeModifier.Operation op, boolean add) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(id);
        if (add) inst.addTransientModifier(new AttributeModifier(id, "krolasyon demon form", v, op));
    }

    // ------------------------------------------------------------------ sync
    static void sync(ServerPlayer p) {
        ModNetwork.toTrackingAndSelf(p, new ModNetwork.FormSync(p.getId(), isDemon(p)));
    }

    static void syncTo(ServerPlayer target, ServerPlayer viewer) {
        ModNetwork.toPlayer(viewer, new ModNetwork.FormSync(target.getId(), isDemon(target)));
    }

    static void anim(ServerPlayer p, int anim) {
        ModNetwork.toTrackingAndSelf(p, new ModNetwork.Anim(p.getId(), anim));
    }

    static void sendCooldowns(ServerPlayer p) {
        DemonState s = state(p);
        ModNetwork.toPlayer(p, new ModNetwork.Cooldowns(s.cd.clone(), COOLDOWN.clone()));
    }

    // ------------------------------------------------------------------ input
    public static void tryAbility(ServerPlayer p, int slot) {
        DemonState s = state(p);
        if (!s.transformed || slot < 0 || slot >= COUNT || p.isSpectator() || !p.isAlive()) return;
        if (s.transformTick >= 0 || s.ability >= 0 || s.cd[slot] > 0) return;
        s.ability = slot;
        s.abilityTick = 0;
        s.slamAt = -1;
        s.hit.clear();
        s.cd[slot] = p.isCreative() ? COOLDOWN[slot] / 4 : COOLDOWN[slot];
        anim(p, slot);
        sendCooldowns(p);
    }

    public static void airJump(ServerPlayer p) {
        DemonState s = state(p);
        if (!s.transformed || s.airJumped || p.onGround()) return;
        s.airJumped = true;
        p.fallDistance = 0;
        anim(p, ANIM_FLIP);
        sound(p, ModSounds.DOUBLE_JUMP.get(), 1.2F, 0.9F + p.getRandom().nextFloat() * 0.2F);
        ServerLevel sl = p.serverLevel();
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            sl.sendParticles(dust(i % 2 == 0 ? SHADOW : CRIMSON, 1.5F), p.getX(), p.getY() + 0.1, p.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.35);
        }
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY(), p.getZ(), 6, 0.3, 0.1, 0.3, 0.02);
    }

    // ------------------------------------------------------------------ tick
    static void tick(ServerPlayer p) {
        DemonState s = STATES.get(p.getUUID());
        if (s == null || !s.transformed) return;
        if (!p.isAlive()) return;
        ServerLevel sl = p.serverLevel();
        if (p.onGround()) s.airJumped = false;
        if (s.invuln > 0) s.invuln--;
        if (p.tickCount % 20 == 0) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 45, 1, false, false, false));
        if (p.isOnFire()) p.clearFire();
        boolean changed = false;
        for (int i = 0; i < COUNT; i++) {
            if (s.cd[i] > 0) {
                s.cd[i]--;
                if (s.cd[i] == 0) changed = true;
            }
        }
        if (changed) sendCooldowns(p);

        if (s.transformTick >= 0) {
            tickTransform(p, sl, s, s.transformTick);
            if (++s.transformTick >= TRANSFORM_TICKS) s.transformTick = -1;
        }
        if (s.ability >= 0) {
            int id = s.ability;
            switch (id) {
                case WHIP -> tickWhip(p, sl, s, s.abilityTick);
                case HEARTS -> tickHearts(p, sl, s, s.abilityTick);
                case DASH -> tickDash(p, sl, s, s.abilityTick);
                case TEMPEST -> tickTempest(p, sl, s, s.abilityTick);
                case JUDGEMENT -> tickJudgement(p, sl, s, s.abilityTick);
                default -> {}
            }
            s.abilityTick++;
            if (s.ability == id && s.abilityTick >= DURATION[id]) s.ability = -1;
        }
        if (p.getRandom().nextInt(420) == 0) sound(p, ModSounds.DEMON_AMBIENT.get(), 0.7F, 1.05F + p.getRandom().nextFloat() * 0.15F);
    }

    private static void tickTransform(ServerPlayer p, ServerLevel sl, DemonState s, int t) {
        if (t < 12) {
            float k = t / 12F;
            for (int i = 0; i < 4; i++) {
                double a = t * 0.6 + i * Math.PI / 2;
                double r = 3.4 * (1 - k) + 0.4;
                sl.sendParticles(dust(i % 2 == 0 ? SHADOW : CRIMSON, 2.0F), p.getX() + Math.cos(a) * r, p.getY() + 0.3 + k * 1.2, p.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 4, 0.4, 0.5, 0.4, 0.01);
        }
        if (t == 12) {
            sound(p, ModSounds.TRANSFORM_BURST.get(), 3.0F, 1.0F);
            sl.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1.5, p.getZ(), 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 3, 0.6, 0.4, 0.6, 0);
            for (int ring = 0; ring < 2; ring++) {
                for (int i = 0; i < 70; i++) {
                    double a = i * Math.PI * 2 / 70;
                    sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 2.4F), p.getX(), p.getY() + 0.3 + ring * 1.4, p.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 0.9 - ring * 0.3);
                }
            }
            sl.sendParticles(dust(HEART_PINK, 1.6F), p.getX(), p.getY() + 1.4, p.getZ(), 40, 0.4, 0.6, 0.4, 0);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, p.getX(), p.getY() + 1.4, p.getZ(), 20, 0.8, 0.8, 0.8, 0.3);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                EruptionEntity.spawnFor(p, p.getX() + Math.cos(a) * 2.6, p.getZ() + Math.sin(a) * 2.6, p.getY(), EruptionEntity.KIND_THORN, i % 2, 6F);
            }
            for (LivingEntity e : targetsAround(p, p.position(), 5.5)) hit(p, e, 6F, 1.6, 0.5);
        }
        if (t > 12 && t < 30 && t % 2 == 0) {
            sl.sendParticles(dust(CRIMSON, 1.2F), p.getX(), p.getY() + 2.2, p.getZ(), 3, 0.6, 0.5, 0.6, 0);
        }
    }

    // 1) Diken Kırbacı — thorn whip lash in a cone that drags victims in
    private static void tickWhip(ServerPlayer p, ServerLevel sl, DemonState s, int t) {
        if (t == 2) sound(p, ModSounds.DEMON_ROAR.get(), 0.9F, 1.4F);
        if (t == 6) sound(p, ModSounds.THORN_WHIP.get(), 2.2F, 1.0F);
        if (t >= 6 && t < 10) {
            Vec3 c = p.position().add(0, 1.4, 0).add(right(p).scale(0.8));
            sl.sendParticles(dust(SHADOW, 1.4F), c.x, c.y, c.z, 4, 0.2, 0.3, 0.2, 0);
        }
        if (t == 10) {
            Vec3 f = flatLook(p);
            for (float a : new float[]{-0.4F, -0.13F, 0.13F, 0.4F}) {
                Vec3 d = f.yRot(a);
                for (int i = 1; i <= 22; i++) {
                    double wob = Math.sin(i * 0.7 + a * 5) * 0.25;
                    Vec3 q = p.position().add(d.scale(i * 0.5)).add(0, 1.3 - i * 0.03 + wob, 0);
                    sl.sendParticles(i % 3 == 0 ? dust(BRIGHT, 1.3F) : dust(SHADOW, 1.7F), q.x, q.y, q.z, 1, 0.04, 0.04, 0.04, 0);
                    if (i % 6 == 0) sl.sendParticles(ParticleTypes.CRIT, q.x, q.y, q.z, 2, 0.1, 0.1, 0.1, 0.1);
                }
            }
            for (LivingEntity e : targetsAround(p, p.position(), 11)) {
                Vec3 to = e.position().subtract(p.position()).multiply(1, 0, 1);
                if (to.lengthSqr() > 0.01 && to.normalize().dot(f) < 0.5) continue;
                e.invulnerableTime = 0;
                if (e.hurt(p.damageSources().playerAttack(p), 14F)) {
                    Vec3 pull = p.position().add(f.scale(1.8)).subtract(e.position()).multiply(1, 0, 1);
                    double len = pull.length();
                    if (len > 1.0) {
                        pull = pull.normalize().scale(Math.min(1.8, len * 0.24));
                        e.setDeltaMovement(pull.x, 0.4, pull.z);
                        e.hurtMarked = true;
                    }
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2));
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1, e.getZ(), 5, 0.3, 0.3, 0.3, 0.1);
                    sl.sendParticles(dust(CRIMSON, 1.6F), e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.5, 0.3, 0);
                }
            }
        }
    }

    // 2) Kırık Kalp Yağmuru — homing crimson hearts from the chest sigil, each hit heals
    private static void tickHearts(ServerPlayer p, ServerLevel sl, DemonState s, int t) {
        Vec3 chest = p.position().add(0, 1.35, 0).add(flatLook(p).scale(0.5));
        if (t == 1) sound(p, ModSounds.HEART_SHOOT.get(), 1.5F, 0.6F);
        if (t < 12) sl.sendParticles(dust(HEART_PINK, 1.3F), chest.x, chest.y, chest.z, 4, 0.25, 0.25, 0.25, 0);
        if (t == 11) sl.sendParticles(ParticleTypes.HEART, chest.x, chest.y + 0.3, chest.z, 6, 0.4, 0.3, 0.4, 0);
        final int count = 7;
        if (t >= 12 && t < 12 + count * 3 && (t - 12) % 3 == 0) {
            int k = (t - 12) / 3;
            List<LivingEntity> targets = targetsAround(p, p.position(), 26);
            Vec3 look = p.getLookAngle();
            targets.removeIf(e -> e.getEyePosition().subtract(p.getEyePosition()).normalize().dot(look) < 0.4 || !p.hasLineOfSight(e));
            targets.sort(Comparator.comparingDouble((LivingEntity e) -> -e.getEyePosition().subtract(p.getEyePosition()).normalize().dot(look)));
            LivingEntity target = targets.isEmpty() ? null : targets.get(k % Math.min(3, targets.size()));
            float spread = (k - (count - 1) / 2F) * 0.3F;
            Vec3 dir = look.yRot(spread).add(0, 0.2, 0);
            HeartOrbEntity orb = new HeartOrbEntity(ModEntities.HEART_ORB.get(), sl);
            orb.setup(p, target, chest, dir, true);
            if (target == null) orb.setDeltaMovement(dir.normalize().scale(1.1));
            sl.addFreshEntity(orb);
            soundAt(p, chest, ModSounds.HEART_SHOOT.get(), 1.2F, 1.0F + k * 0.07F);
            sl.sendParticles(dust(BRIGHT, 1.5F), chest.x, chest.y, chest.z, 6, 0.1, 0.1, 0.1, 0.05);
        }
    }

    // 3) Gölge Atılışı — invulnerable shadow dash that shreds everything in the path and leaves a thorn trail
    private static void tickDash(ServerPlayer p, ServerLevel sl, DemonState s, int t) {
        if (t == 0) {
            s.dir = flatLook(p);
            s.hit.clear();
        }
        if (t < 4) sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 0.8, p.getZ(), 4, 0.4, 0.5, 0.4, 0.02);
        if (t == 4) {
            sound(p, ModSounds.SHADOW_DASH.get(), 2.2F, 1.0F);
            s.invuln = 10;
        }
        if (t >= 4 && t <= 11) {
            p.setDeltaMovement(s.dir.x * 1.85, t == 4 ? 0.12 : Math.min(0.0, p.getDeltaMovement().y), s.dir.z * 1.85);
            p.hurtMarked = true;
            p.fallDistance = 0;
            sl.sendParticles(dust(SHADOW, 2.4F), p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.35, 0.7, 0.35, 0);
            sl.sendParticles(dust(CRIMSON, 1.5F), p.getX(), p.getY() + 1.0, p.getZ(), 5, 0.35, 0.7, 0.35, 0);
            for (LivingEntity e : targetsIn(p, p.getBoundingBox().expandTowards(s.dir.scale(1.6)).inflate(1.4, 0.6, 1.4))) {
                if (s.hit.contains(e.getId())) continue;
                s.hit.add(e.getId());
                e.invulnerableTime = 0;
                hit(p, e, 15F, 0.6, 0.55);
                Vec3 side = new Vec3(-s.dir.z, 0, s.dir.x).scale(p.getRandom().nextBoolean() ? 1 : -1);
                e.setDeltaMovement(e.getDeltaMovement().add(side.scale(0.9)));
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
                soundAt(p, e.position(), ModSounds.CLAW_SWIPE.get(), 1.2F, 0.8F);
            }
            if (t % 2 == 0) EruptionEntity.spawnFor(p, p.getX() - s.dir.x * 1.5, p.getZ() - s.dir.z * 1.5, p.getY(), EruptionEntity.KIND_THORN, 5, 7F);
        }
        if (t == 12) {
            p.setDeltaMovement(p.getDeltaMovement().multiply(0.2, 1, 0.2));
            p.hurtMarked = true;
        }
    }

    // 4) Tırpan Kasırgası — the scythe tail spins into a crimson vortex that drags enemies in, then explodes
    private static void tickTempest(ServerPlayer p, ServerLevel sl, DemonState s, int t) {
        if (t == 3 || t == 15) sound(p, ModSounds.SCYTHE_SPIN.get(), 2.0F, 0.9F + t * 0.01F);
        if (t >= 6 && t <= 26) {
            double base = t * 0.9;
            for (int k = 0; k < 3; k++) {
                double a = base + k * Math.PI * 2 / 3;
                for (int j = 0; j < 5; j++) {
                    double aa = a - j * 0.16;
                    double r = 3.4 - j * 0.05;
                    sl.sendParticles(dust(j == 0 ? BRIGHT : CRIMSON, 1.9F - j * 0.28F), p.getX() + Math.cos(aa) * r, p.getY() + 0.5 + k * 0.35, p.getZ() + Math.sin(aa) * r, 1, 0, 0, 0, 0);
                }
            }
            sl.sendParticles(dust(SHADOW, 1.6F), p.getX(), p.getY() + 1.0, p.getZ(), 6, 1.6, 0.6, 1.6, 0);
            for (LivingEntity e : targetsAround(p, p.position(), 7.5)) {
                Vec3 pull = p.position().subtract(e.position()).multiply(1, 0, 1);
                if (pull.lengthSqr() > 3) {
                    e.setDeltaMovement(e.getDeltaMovement().add(pull.normalize().scale(0.2)));
                    e.hurtMarked = true;
                }
                if (t % 4 == 0 && e.distanceToSqr(p) < 4.4 * 4.4) {
                    e.invulnerableTime = 0;
                    hit(p, e, 5F, 0.2, 0.25);
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }
        if (t == 28) {
            sound(p, ModSounds.THORN_WHIP.get(), 2.5F, 0.7F);
            sound(p, ModSounds.THORN_ERUPT.get(), 2.0F, 0.8F);
            sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 1, p.getZ(), 3, 0.8, 0.3, 0.8, 0);
            for (int i = 0; i < 80; i++) {
                double a = i * Math.PI * 2 / 80;
                sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 2.4F), p.getX(), p.getY() + 0.6, p.getZ(), 0, Math.cos(a), 0.05, Math.sin(a), 1.0);
            }
            for (LivingEntity e : targetsAround(p, p.position(), 7)) {
                e.invulnerableTime = 0;
                hit(p, e, 12F, 2.0, 0.6);
            }
        }
    }

    // 5) Kalp Hükmü — leap into the sky, crash down, raise a thorn cage and tear the hearts out of every enemy around
    private static void tickJudgement(ServerPlayer p, ServerLevel sl, DemonState s, int t) {
        if (t == 0) {
            sound(p, ModSounds.DEMON_ROAR.get(), 2.5F, 0.9F);
            s.dir = flatLook(p);
        }
        if (t == 3) {
            p.setDeltaMovement(s.dir.x * 0.55, 1.35, s.dir.z * 0.55);
            p.hurtMarked = true;
            sound(p, ModSounds.DOUBLE_JUMP.get(), 1.8F, 0.7F);
            sl.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 2, 0.4, 0.1, 0.4, 0);
            for (int i = 0; i < 40; i++) {
                double a = i * Math.PI * 2 / 40;
                sl.sendParticles(dust(i % 2 == 0 ? SHADOW : CRIMSON, 2.0F), p.getX(), p.getY() + 0.1, p.getZ(), 0, Math.cos(a), 0.02, Math.sin(a), 0.6);
            }
        }
        if (s.slamAt < 0) {
            p.fallDistance = 0;
            if (t > 3) {
                sl.sendParticles(dust(CRIMSON, 1.8F), p.getX(), p.getY() + 1.2, p.getZ(), 4, 0.4, 0.6, 0.4, 0);
                sl.sendParticles(dust(HEART_PINK, 1.0F), p.getX(), p.getY() + 2.4, p.getZ(), 2, 0.4, 0.2, 0.4, 0);
            }
            if (t >= 14) {
                p.setDeltaMovement(s.dir.x * 0.4, -2.0, s.dir.z * 0.4);
                p.hurtMarked = true;
                sl.sendParticles(dust(SHADOW, 2.4F), p.getX(), p.getY() + 1.0, p.getZ(), 8, 0.3, 0.8, 0.3, 0);
            }
            if ((t >= 8 && p.onGround()) || t >= 50) {
                s.slamAt = t;
                slam(p, sl);
            }
            if (s.abilityTick >= DURATION[JUDGEMENT] - 2) s.abilityTick = DURATION[JUDGEMENT] - 3;
        } else if (t >= s.slamAt + 20) {
            s.ability = -1;
        }
    }

    private static void slam(ServerPlayer p, ServerLevel sl) {
        anim(p, ANIM_SLAM);
        sound(p, ModSounds.GROUND_SLAM.get(), 3.5F, 1.0F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.getX(), p.getY() + 0.3, p.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.FLASH, p.getX(), p.getY() + 1, p.getZ(), 1, 0, 0, 0, 0);
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < 90; i++) {
                double a = i * Math.PI * 2 / 90;
                sl.sendParticles(dust(ring == 1 ? SHADOW : CRIMSON, 2.8F - ring * 0.5F), p.getX(), p.getY() + 0.2, p.getZ(), 0, Math.cos(a), 0.02 + ring * 0.08, Math.sin(a), 0.7 + ring * 0.35);
            }
        }
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI * 2 / 10;
            EruptionEntity.spawnFor(p, p.getX() + Math.cos(a) * 3.2, p.getZ() + Math.sin(a) * 3.2, p.getY(), EruptionEntity.KIND_THORN, 1, 8F);
        }
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI * 2 / 16 + 0.2;
            EruptionEntity.spawnFor(p, p.getX() + Math.cos(a) * 6.2, p.getZ() + Math.sin(a) * 6.2, p.getY(), EruptionEntity.KIND_THORN, 6, 8F);
        }
        float stolen = 0;
        int victims = 0;
        for (LivingEntity e : targetsAround(p, p.position(), 8)) {
            double d = Math.sqrt(e.distanceToSqr(p));
            float dmg = (float) (20.0 - d * 1.2);
            e.invulnerableTime = 0;
            float before = e.getHealth();
            hit(p, e, dmg, 1.2, 0.9);
            float dealt = Math.max(0, before - e.getHealth());
            if (dealt > 0) {
                stolen += dealt * 0.3F;
                victims++;
                // crimson thread from the victim's heart to the demon
                Vec3 from = e.position().add(0, e.getBbHeight() * 0.6, 0);
                Vec3 to = p.position().add(0, 1.4, 0);
                for (int k = 0; k <= 14; k++) {
                    Vec3 q = from.lerp(to, k / 14.0).add(0, Math.sin(k / 14.0 * Math.PI) * 0.8, 0);
                    sl.sendParticles(dust(k % 2 == 0 ? HEART_PINK : CRIMSON, 1.3F), q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0);
                }
                sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, from.x, from.y, from.z, 6, 0.2, 0.2, 0.2, 0.1);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 5));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
            }
        }
        if (victims > 0) {
            p.heal(stolen);
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, Math.min(3, victims / 2)));
            sound(p, ModSounds.HEART_RIP.get(), 2.0F, 1.0F);
            sl.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 2.2, p.getZ(), 6 + victims, 0.5, 0.3, 0.5, 0);
        }
    }

    // ------------------------------------------------------------------ melee
    private static boolean cleaving;

    /** enhanced claw strikes: crimson arc, cleave around the target and a little life steal */
    static void onMelee(ServerPlayer p, LivingEntity victim, float amount) {
        if (cleaving) return;
        ServerLevel sl = p.serverLevel();
        Vec3 f = flatLook(p);
        Vec3 c = p.position().add(f.scale(1.6)).add(0, 1.2, 0);
        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 1, 0.2, 0.1, 0.2, 0);
        int side = p.getRandom().nextBoolean() ? 1 : -1;
        for (int i = 0; i < 12; i++) {
            double a = (i / 11.0 - 0.5) * Math.PI * 0.9 * side;
            Vec3 d = f.yRot((float) a).scale(2.3);
            sl.sendParticles(dust(i % 2 == 0 ? CRIMSON : SHADOW, 1.4F), p.getX() + d.x, p.getY() + 1.7 - i * 0.07, p.getZ() + d.z, 1, 0, 0, 0, 0);
        }
        sl.sendParticles(dust(BRIGHT, 1.2F), victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 8, 0.25, 0.3, 0.25, 0);
        p.heal(amount * 0.08F);
        cleaving = true;
        try {
            for (LivingEntity e : targetsIn(p, victim.getBoundingBox().inflate(2.2, 0.6, 2.2))) {
                if (e == victim) continue;
                Vec3 to = e.position().subtract(p.position());
                if (to.horizontalDistanceSqr() > 0.01 && to.normalize().dot(f) < 0.1) continue;
                hit(p, e, amount * 0.45F, 0.5, 0.15);
            }
        } finally {
            cleaving = false;
        }
    }

    // ------------------------------------------------------------------ helpers
    public static boolean canHit(Player p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !e.isAlive()) return false;
        if (e instanceof ArmorStand) return false;
        if (e instanceof Player other) return !other.isCreative() && !other.isSpectator() && p.canHarmPlayer(other);
        if (e instanceof TamableAnimal tame && tame.isOwnedBy(p)) return false;
        boolean provoked = (e instanceof Mob m && m.getTarget() == p) || p.getLastHurtMob() == e || le.getLastHurtByMob() == p;
        if (e instanceof AbstractVillager || e instanceof IronGolem || e instanceof Animal) return provoked;
        return true;
    }

    static List<LivingEntity> targetsIn(Player p, AABB box) {
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> canHit(p, e));
    }

    static List<LivingEntity> targetsAround(Player p, Vec3 c, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, r * 0.8, r),
                e -> canHit(p, e) && e.position().distanceToSqr(c) <= r * r * 1.2);
    }

    static void hit(Player p, LivingEntity e, float amount, double knock, double lift) {
        if (e.hurt(p.damageSources().playerAttack(p), amount)) {
            Vec3 d = e.position().subtract(p.position());
            d = new Vec3(d.x, 0, d.z);
            if (d.lengthSqr() < 1.0E-4) d = flatLook(p);
            d = d.normalize();
            double kr = 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.6;
            e.setDeltaMovement(e.getDeltaMovement().add(d.x * knock * kr, lift * kr, d.z * knock * kr));
            e.hurtMarked = true;
        }
    }

    static Vec3 flatLook(Player p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    static Vec3 right(Player p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
    }

    static ParticleOptions dust(int rgb, float scale) { return BossEntity.dust(rgb, scale); }

    static void sound(Player p, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    static void soundAt(Player p, Vec3 at, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }
}
