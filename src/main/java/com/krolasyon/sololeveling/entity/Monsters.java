package com.krolasyon.sololeveling.entity;

import com.krolasyon.sololeveling.registry.ModEntities;
import com.krolasyon.sololeveling.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static com.krolasyon.sololeveling.entity.SLMonster.*;

/** Builds every monster and wires its special moves. */
public final class Monsters {
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.1F), 1.8F);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1F, 0.85F, 0.3F), 2F);
    private static final DustParticleOptions ICE = new DustParticleOptions(new Vector3f(0.6F, 0.9F, 1F), 1.6F);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.4F, 0.9F, 0.2F), 1.6F);

    private Monsters() {}

    public static SLMonster create(MobKind k, EntityType<SLMonster> type, Level level) {
        SLMonster m = new SLMonster(type, level);
        switch (k) {
            case GOBLIN -> m.ability("lunge", A_LEAP, 12, 80, 3, 8, Monsters::lunge);
            case HOBGOBLIN -> m.ability("smash", A_SLAM, 22, 100, 0, 3.5, (s, t, i) -> slam(s, i, 12, 3.5F, 1.2F, 1.0));
            case STEEL_FANGED_LYCAN -> {
                m.ability("pounce", A_LEAP, 14, 70, 3, 10, Monsters::lunge);
                m.ability("howl", A_ROAR, 30, 300, 0, 16, (s, t, i) -> {
                    if (i == 5) for (Mob o : s.level().getEntitiesOfClass(SLMonster.class, s.getBoundingBox().inflate(14)))
                        o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 1));
                }).sound(ModSounds.HOWL.get());
            }
            case GIANT_CENTIPEDE -> {
                m.hitEffect = e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                m.ability("charge", A_LEAP, 18, 100, 4, 12, Monsters::lunge);
            }
            case KASAKA -> {
                m.hitEffect = e -> {
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                    if (s(e).nextFloat() < 0.3F) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5));
                };
                m.ability("venom_spit", A_SHOOT, 24, 70, 4, 24, (s, t, i) -> {
                    if (t != null && (i == 8 || i == 13 || i == 18)) shootAt(s, t, SLProjectile.Kind.VENOM, 1.3, 0.75F, 0.12);
                });
                m.ability("tail_sweep", A_SPIN, 24, 90, 0, 5, (s, t, i) -> {
                    if (i == 12) {
                        s.hurtAround(5.5, (float) s.kind.damage * 1.1F, 1.6);
                        ring(s, ParticleTypes.SWEEP_ATTACK, 4.5, 12);
                    }
                });
                m.ability("paralyze_bite", A_BITE, 16, 120, 0, 4.5, (s, t, i) -> {
                    if (i == 9 && t != null && s.distanceTo(t) < 5) {
                        t.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * 1.4F);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                        t.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
                    }
                });
                m.ability("shed_skin", A_ROAR, 30, 600, 0, 30, (s, t, i) -> {
                    if (i == 15) {
                        s.heal(s.getMaxHealth() * 0.15F);
                        dust(s, GREEN, 40);
                    }
                }).phase(1).noSight();
            }
            case STONE_STATUE -> {
                m.ability("smash", A_SLAM, 24, 90, 0, 3.5, (s, t, i) -> slam(s, i, 14, 3.5F, 1.4F, 1.0));
                m.ability("eye_bolt", A_SHOOT, 16, 120, 5, 20, (s, t, i) -> {
                    if (i == 10 && t != null) shootAt(s, t, SLProjectile.Kind.MANA_BOLT, 1.4, 0.8F, 0);
                });
            }
            case STATUE_OF_GOD -> {
                m.ability("eye_laser", A_CAST, 70, 120, 0, 40, Monsters::eyeLaser).sound(ModSounds.LASER.get()).noSight();
                m.ability("divine_palm", A_SLAM, 40, 140, 0, 14, (s, t, i) -> {
                    if (i == 26) {
                        Vec3 at = s.position().add(s.getLookAngle().multiply(6, 0, 6));
                        quake(s, at, 6, (float) s.kind.damage * 1.5F);
                    }
                });
                m.ability("awaken_statues", A_SUMMON, 40, 500, 0, 40, (s, t, i) -> {
                    if (i == 20) summon(s, MobKind.STONE_STATUE, 2, 6);
                }).noSight();
            }
            case CASTLE_KNIGHT -> m.ability("charge", A_LEAP, 16, 110, 4, 10, Monsters::lunge);
            case IGRIS -> {
                m.ability("crimson_dash", A_LEAP, 18, 80, 4, 16, (s, t, i) -> dashSlash(s, t, i, 2.0, 1.2F));
                m.ability("whirlwind", A_SPIN, 30, 110, 0, 5, (s, t, i) -> {
                    if (i % 6 == 0 && i > 0) {
                        s.hurtAround(4.5, (float) s.kind.damage * 0.55F, 0.9);
                        ring(s, ParticleTypes.SWEEP_ATTACK, 3.5, 8);
                        ring(s, RED, 3, 16);
                    }
                }).sound(ModSounds.SLASH.get());
                m.ability("cleave", A_SLAM, 26, 90, 0, 5, (s, t, i) -> slam(s, i, 16, 4.5F, 1.8F, 1.4));
                m.ability("sword_wave", A_SHOOT, 22, 100, 6, 24, (s, t, i) -> {
                    if (i == 12 && t != null) {
                        SLProjectile p = shootAt(s, t, SLProjectile.Kind.FLAME_SLASH, 1.6, 1.1F, 0);
                        p.pierce = 3;
                    }
                }).sound(ModSounds.SLASH.get());
                m.ability("knights_rally", A_SUMMON, 30, 600, 0, 40, (s, t, i) -> {
                    if (i == 15) summon(s, MobKind.CASTLE_KNIGHT, 2, 4);
                }).phase(1).noSight();
                m.onPhase2 = () -> {
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 0));
                    dust(m, RED, 80);
                };
            }
            case ICE_ELF -> {
                m.ranged = true;
                m.ability("ice_shard", A_SHOOT, 18, 40, 0, 22, (s, t, i) -> {
                    if (i == 10 && t != null) shootAt(s, t, SLProjectile.Kind.ICE_SHARD, 1.6, 0.9F, 0);
                });
                m.ability("frost_nova", A_CAST, 20, 160, 0, 5, (s, t, i) -> {
                    if (i == 12) nova(s, 5, ICE, MobEffects.MOVEMENT_SLOWDOWN, 0.5F);
                });
            }
            case ICE_BEAR -> {
                m.ability("maul", A_SLAM, 22, 80, 0, 4, (s, t, i) -> slam(s, i, 13, 4F, 1.5F, 1.5));
                m.ability("roar", A_ROAR, 30, 260, 0, 12, (s, t, i) -> {
                    if (i == 8) nova(s, 10, ICE, MobEffects.MOVEMENT_SLOWDOWN, 0.2F);
                }).sound(ModSounds.ROAR.get());
            }
            case BARUKA -> {
                m.ability("blink_strike", A_LEAP, 14, 70, 3, 20, (s, t, i) -> {
                    if (i == 4 && t != null) {
                        Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1.6, 0, 1.6));
                        dust(s, ICE, 20);
                        s.teleportTo(behind.x, t.getY(), behind.z);
                        dust(s, ICE, 20);
                        s.playSound(ModSounds.DASH.get(), 1.5F, 1.4F);
                    }
                    if (i == 8 && t != null && s.distanceTo(t) < 3.5) {
                        t.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * 1.3F);
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    }
                });
                m.ability("ice_storm", A_CAST, 34, 140, 0, 24, (s, t, i) -> {
                    if (t != null && i >= 10 && i % 3 == 0) {
                        Vec3 sky = t.position().add((s.getRandom().nextDouble() - 0.5) * 6, 7, (s.getRandom().nextDouble() - 0.5) * 6);
                        SLProjectile p = SLProjectile.shoot(s, SLProjectile.Kind.ICE_SHARD, t.position().subtract(sky), 1.2, (float) s.kind.damage * 0.6F);
                        p.setPos(sky.x, sky.y, sky.z);
                    }
                });
                m.ability("frost_nova", A_SPIN, 22, 120, 0, 6, (s, t, i) -> {
                    if (i == 12) nova(s, 6.5, ICE, MobEffects.MOVEMENT_SLOWDOWN, 0.9F);
                });
                m.ability("flurry", A_SPIN, 24, 90, 0, 4, (s, t, i) -> {
                    if (i % 4 == 0 && i > 0 && t != null && s.distanceTo(t) < 4) {
                        t.invulnerableTime = 0;
                        t.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * 0.4F);
                    }
                }).sound(ModSounds.SLASH.get());
                m.ability("elf_call", A_SUMMON, 30, 600, 0, 40, (s, t, i) -> {
                    if (i == 15) summon(s, MobKind.ICE_ELF, 3, 5);
                }).phase(1).noSight();
            }
            case HIGH_ORC -> {
                m.ability("cleave", A_SLAM, 22, 70, 0, 4, (s, t, i) -> slam(s, i, 13, 4F, 1.4F, 1.2));
                m.ability("war_cry", A_ROAR, 30, 300, 0, 16, (s, t, i) -> {
                    if (i == 8) for (Mob o : s.level().getEntitiesOfClass(SLMonster.class, s.getBoundingBox().inflate(12)))
                        o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
                }).sound(ModSounds.ROAR.get());
                m.ability("charge", A_LEAP, 16, 120, 4, 12, Monsters::lunge);
            }
            case KARGALGAN -> {
                m.ranged = true;
                m.ability("fire_volley", A_SHOOT, 30, 70, 0, 28, (s, t, i) -> {
                    if (t != null && i >= 10 && i % 5 == 0) shootAt(s, t, SLProjectile.Kind.FIREBALL, 1.1, 0.7F, 0.15).explode = 2.5F;
                }).sound(ModSounds.FIRE.get());
                m.ability("lightning_orb", A_CAST, 24, 100, 0, 28, (s, t, i) -> {
                    if (t != null && i == 14) shootAt(s, t, SLProjectile.Kind.LIGHTNING_ORB, 0.8, 1.2F, 0).explode = 3.5F;
                });
                m.ability("bloodlust_blessing", A_CAST, 30, 300, 0, 30, (s, t, i) -> {
                    if (i == 15) {
                        for (Mob o : s.level().getEntitiesOfClass(SLMonster.class, s.getBoundingBox().inflate(20))) {
                            o.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
                            o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));
                            dust(o, RED, 15);
                        }
                    }
                }).noSight();
                m.ability("summon_orcs", A_SUMMON, 34, 500, 0, 40, (s, t, i) -> {
                    if (i == 17) summon(s, MobKind.HIGH_ORC, 2, 5);
                }).noSight();
                m.ability("mana_shield", A_CAST, 20, 400, 0, 40, (s, t, i) -> {
                    if (i == 10) s.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 2));
                }).phase(1).noSight();
            }
            case DEMON -> {
                m.ability("hellfire", A_SHOOT, 20, 80, 4, 20, (s, t, i) -> {
                    if (t != null && i == 10) shootAt(s, t, SLProjectile.Kind.FIREBALL, 1.3, 0.8F, 0).explode = 2F;
                }).sound(ModSounds.FIRE.get());
                m.ability("claw_combo", A_SPIN, 20, 80, 0, 3.5, (s, t, i) -> {
                    if (i % 5 == 0 && i > 0 && t != null && s.distanceTo(t) < 3.5) {
                        t.invulnerableTime = 0;
                        t.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * 0.5F);
                    }
                });
            }
            case CERBERUS -> {
                m.ability("triple_bite", A_LEAP, 24, 70, 3, 14, (s, t, i) -> {
                    lunge(s, t, i);
                    if ((i == 10 || i == 15 || i == 20) && t != null && s.distanceTo(t) < 4.5) {
                        t.invulnerableTime = 0;
                        t.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * 0.6F);
                    }
                });
                m.ability("hellfire_breath", A_CAST, 40, 110, 0, 10, Monsters::breath).sound(ModSounds.FIRE.get());
                m.ability("hound_call", A_ROAR, 30, 500, 0, 40, (s, t, i) -> {
                    if (i == 10) summon(s, MobKind.STEEL_FANGED_LYCAN, 3, 5);
                }).sound(ModSounds.HOWL.get()).noSight();
                m.ability("stomp", A_SLAM, 24, 90, 0, 5, (s, t, i) -> slam(s, i, 14, 5.5F, 1.4F, 1.6));
                m.onPhase2 = () -> m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
            }
            case BARAN -> {
                m.ability("thunder_strike", A_CAST, 40, 90, 0, 30, (s, t, i) -> {
                    if (t != null && (i == 14 || i == 22 || i == 30)) lightning(s, t.position().add((s.getRandom().nextDouble() - 0.5) * 3, 0, (s.getRandom().nextDouble() - 0.5) * 3), (float) s.kind.damage * 0.8F);
                }).sound(ModSounds.THUNDER.get());
                m.ability("lightning_orbs", A_SHOOT, 30, 90, 0, 30, (s, t, i) -> {
                    if (t != null && i >= 10 && i % 6 == 0) shootAt(s, t, SLProjectile.Kind.LIGHTNING_ORB, 1.0, 0.9F, 0.1).explode = 3F;
                });
                m.ability("storm_field", A_SPIN, 50, 160, 0, 8, (s, t, i) -> {
                    if (i > 10 && i % 8 == 0) {
                        s.hurtAround(7, (float) s.kind.damage * 0.45F, 0.6);
                        ring(s, ParticleTypes.ELECTRIC_SPARK, 6, 30);
                    }
                }).sound(ModSounds.THUNDER.get());
                m.ability("demon_cleave", A_SLAM, 26, 80, 0, 5, (s, t, i) -> slam(s, i, 16, 5F, 1.6F, 1.5));
                m.ability("demon_legion", A_SUMMON, 30, 600, 0, 40, (s, t, i) -> {
                    if (i == 15) summon(s, MobKind.DEMON, 3, 5);
                }).phase(1).noSight();
                m.onPhase2 = () -> {
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 1));
                    lightning(m, m.position(), 0);
                };
            }
            case ANT_SOLDIER -> {
                m.hitEffect = e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                m.ability("acid", A_SHOOT, 16, 90, 4, 18, (s, t, i) -> {
                    if (t != null && i == 9) shootAt(s, t, SLProjectile.Kind.VENOM, 1.4, 0.7F, 0.05);
                });
                m.ability("leap", A_LEAP, 14, 80, 4, 12, Monsters::lunge);
            }
            case BERU -> {
                m.hitEffect = e -> m.heal((float) m.kind.damage * 0.3F);
                m.ability("claw_rush", A_LEAP, 18, 60, 3, 18, (s, t, i) -> dashSlash(s, t, i, 2.4, 1.3F));
                m.ability("sonic_roar", A_ROAR, 30, 220, 0, 14, (s, t, i) -> {
                    if (i == 10) {
                        nova(s, 12, RED, MobEffects.MOVEMENT_SLOWDOWN, 0.6F);
                        for (LivingEntity e : s.enemiesAround(12)) e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    }
                }).sound(ModSounds.ROAR.get());
                m.ability("sky_slam", A_SLAM, 36, 140, 3, 20, (s, t, i) -> {
                    if (i == 6) {
                        s.setDeltaMovement(0, 1.4, 0);
                        s.hurtMarked = true;
                    }
                    if (i == 16 && t != null) {
                        Vec3 d = t.position().subtract(s.position());
                        s.setDeltaMovement(d.x * 0.25, -1.6, d.z * 0.25);
                        s.hurtMarked = true;
                    }
                    if (i == 24) quake(s, s.position(), 6, (float) s.kind.damage * 1.2F);
                });
                m.ability("acid_barrage", A_SHOOT, 26, 100, 0, 24, (s, t, i) -> {
                    if (t != null && i >= 8 && i % 3 == 0) shootAt(s, t, SLProjectile.Kind.VENOM, 1.6, 0.5F, 0.2);
                });
                m.onPhase2 = () -> {
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 2));
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 1));
                    m.playSound(ModSounds.ROAR.get(), 3F, 0.6F);
                };
            }
            default -> {}
        }
        return m;
    }

    private static net.minecraft.util.RandomSource s(LivingEntity e) { return e.getRandom(); }

    // ---------------------------------------------------------------- reusable moves

    static void lunge(SLMonster s, LivingEntity t, int i) {
        if (t == null) return;
        if (i == 3) {
            Vec3 d = t.position().subtract(s.position());
            s.setDeltaMovement(d.x * 0.22, 0.42, d.z * 0.22);
            s.hurtMarked = true;
        }
        if (i > 4 && i < 12 && !s.abilityHit && s.distanceTo(t) < s.getBbWidth() + 1.5) {
            s.doHurtTarget(t);
            s.abilityHit = true;
        }
    }

    static void dashSlash(SLMonster s, LivingEntity t, int i, double speed, float mult) {
        if (t == null) return;
        if (i == 4) {
            s.dashTowards(t.position().subtract(s.position()), speed);
            s.playSound(ModSounds.DASH.get(), 1.5F, 1F);
        }
        if (i > 4 && i < 12) {
            if (s.level() instanceof ServerLevel sl) sl.sendParticles(RED, s.getX(), s.getY() + 1, s.getZ(), 4, 0.3, 0.6, 0.3, 0);
            for (LivingEntity e : s.enemiesAround(1.5)) {
                if (e.invulnerableTime > 0) continue;
                e.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * mult);
            }
        }
    }

    static void slam(SLMonster s, int i, int hitTick, float radius, float mult, double knock) {
        if (i != hitTick) return;
        Vec3 c = s.position().add(s.getLookAngle().multiply(radius * 0.5, 0, radius * 0.5));
        quake(s, c, radius, (float) s.kind.damage * mult);
    }

    static void quake(SLMonster s, Vec3 c, double r, float dmg) {
        if (!(s.level() instanceof ServerLevel sl)) return;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 2, r), s::isEnemy)) {
            e.hurt(s.damageSources().mobAttack(s), dmg);
            Vec3 d = e.position().subtract(c).normalize();
            e.push(d.x * 0.9, 0.5, d.z * 0.9);
            e.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.2, c.z, 3, r * 0.3, 0.1, r * 0.3, 0);
        for (int k = 0; k < 40; k++) {
            double a = k * Math.PI * 2 / 40;
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x + Math.cos(a) * r * 0.8, c.y + 0.1, c.z + Math.sin(a) * r * 0.8, 1, 0, 0.02, 0, 0.01);
        }
        sl.playSound(null, c.x, c.y, c.z, ModSounds.SLAM.get(), SoundSource.HOSTILE, 2F, 0.8F);
    }

    static SLProjectile shootAt(SLMonster s, LivingEntity t, SLProjectile.Kind k, double speed, float dmgMult, double spread) {
        Vec3 dir = t.getEyePosition().subtract(0, t.getBbHeight() * 0.25, 0).subtract(s.getEyePosition());
        if (spread > 0) dir = dir.normalize().add((s.getRandom().nextDouble() - 0.5) * spread, (s.getRandom().nextDouble() - 0.5) * spread * 0.5, (s.getRandom().nextDouble() - 0.5) * spread);
        return SLProjectile.shoot(s, k, dir, speed, (float) s.kind.damage * dmgMult);
    }

    static void summon(SLMonster s, MobKind k, int n, double r) {
        if (!(s.level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < n; i++) {
            SLMonster m = ModEntities.mob(k).create(sl);
            if (m == null) continue;
            double a = s.getRandom().nextDouble() * Math.PI * 2;
            m.moveTo(s.getX() + Math.cos(a) * r, s.getY(), s.getZ() + Math.sin(a) * r, s.getYRot(), 0);
            m.instance = s.instance;
            m.setTarget(s.getTarget());
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.PORTAL, m.getX(), m.getY() + 1, m.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        }
    }

    static void nova(SLMonster s, double r, DustParticleOptions dust, net.minecraft.world.effect.MobEffect effect, float dmgMult) {
        for (LivingEntity e : s.enemiesAround(r)) {
            if (dmgMult > 0) e.hurt(s.damageSources().indirectMagic(s, s), (float) s.kind.damage * dmgMult);
            e.addEffect(new MobEffectInstance(effect, 80, 2));
        }
        ring(s, dust, r * 0.7, 40);
        ring(s, dust, r, 50);
    }

    static void ring(SLMonster s, net.minecraft.core.particles.ParticleOptions p, double r, int n) {
        if (!(s.level() instanceof ServerLevel sl)) return;
        for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2 / n;
            sl.sendParticles(p, s.getX() + Math.cos(a) * r, s.getY() + 0.8, s.getZ() + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    static void dust(LivingEntity s, DustParticleOptions p, int n) {
        if (s.level() instanceof ServerLevel sl) sl.sendParticles(p, s.getX(), s.getY() + s.getBbHeight() * 0.5, s.getZ(), n, s.getBbWidth() * 0.6, s.getBbHeight() * 0.4, s.getBbWidth() * 0.6, 0);
    }

    static void lightning(SLMonster s, Vec3 at, float dmg) {
        if (!(s.level() instanceof ServerLevel sl)) return;
        LightningBolt b = EntityType.LIGHTNING_BOLT.create(sl);
        if (b != null) {
            b.moveTo(at.x, at.y, at.z);
            b.setVisualOnly(true);
            sl.addFreshEntity(b);
        }
        if (dmg > 0) for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.5, 3, 2.5), s::isEnemy)) {
            e.hurt(s.damageSources().lightningBolt(), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
        }
    }

    /** Statue of God: a sweeping beam from the eyes. Players kneeling (sneaking) towards it are spared — "Worship God". */
    static void eyeLaser(SLMonster s, LivingEntity t, int i) {
        if (!(s.level() instanceof ServerLevel sl) || t == null) return;
        Vec3 eye = s.getEyePosition();
        if (i < 20) {
            if (i % 4 == 0) sl.sendParticles(GOLD, eye.x, eye.y, eye.z, 10, 0.6, 0.2, 0.6, 0);
            return;
        }
        Vec3 aim = s.getPersistentData().contains("laserX") ? new Vec3(s.getPersistentData().getDouble("laserX"), s.getPersistentData().getDouble("laserY"), s.getPersistentData().getDouble("laserZ")) : t.position();
        Vec3 goal = t.position().add(0, t.getBbHeight() * 0.5, 0);
        aim = aim.lerp(goal, 0.12);
        s.getPersistentData().putDouble("laserX", aim.x);
        s.getPersistentData().putDouble("laserY", aim.y);
        s.getPersistentData().putDouble("laserZ", aim.z);
        Vec3 dir = aim.subtract(eye).normalize();
        double len = Math.min(45, aim.distanceTo(eye) + 6);
        for (double d = 1; d < len; d += 0.8) {
            Vec3 q = eye.add(dir.scale(d));
            sl.sendParticles(GOLD, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
            if (((int) (d * 10)) % 4 == 0) sl.sendParticles(ParticleTypes.END_ROD, q.x, q.y, q.z, 1, 0, 0, 0, 0);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(q, q).inflate(0.7), s::isEnemy)) {
                if (e instanceof Player p && p.isShiftKeyDown() && p.getLookAngle().dot(s.position().subtract(p.position()).normalize()) > 0.6) continue;
                if (e.invulnerableTime > 0) continue;
                e.hurt(s.damageSources().indirectMagic(s, s), (float) s.kind.damage * 0.9F);
                e.setSecondsOnFire(3);
            }
        }
        if (i == s.current.duration - 1) s.getPersistentData().remove("laserX");
    }

    static void breath(SLMonster s, LivingEntity t, int i) {
        if (!(s.level() instanceof ServerLevel sl) || i < 10) return;
        Vec3 eye = s.getEyePosition();
        Vec3 dir = t != null ? t.getEyePosition().subtract(eye).normalize() : s.getLookAngle();
        for (int k = 0; k < 6; k++) {
            Vec3 v = dir.add((s.getRandom().nextDouble() - 0.5) * 0.4, (s.getRandom().nextDouble() - 0.5) * 0.3, (s.getRandom().nextDouble() - 0.5) * 0.4).scale(0.9);
            sl.sendParticles(ParticleTypes.FLAME, eye.x + dir.x, eye.y + dir.y, eye.z + dir.z, 0, v.x, v.y, v.z, 1);
        }
        if (i % 5 == 0) for (LivingEntity e : s.enemiesAround(10)) {
            Vec3 to = e.position().subtract(s.position()).normalize();
            if (to.dot(dir) > 0.75) {
                e.hurt(s.damageSources().mobAttack(s), (float) s.kind.damage * 0.35F);
                e.setSecondsOnFire(4);
            }
        }
    }
}
