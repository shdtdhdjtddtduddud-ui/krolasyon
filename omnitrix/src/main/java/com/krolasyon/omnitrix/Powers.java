package com.krolasyon.omnitrix;

import com.krolasyon.omnitrix.entity.AlienProjectile;
import com.krolasyon.omnitrix.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Server side of all 36 alien abilities, the block stances and the passive traits. */
@Mod.EventBusSubscriber(modid = Omnitrix.MODID)
public final class Powers {
    private enum ZoneKind { TORNADO, ACID_POOL, VORTEX_RUN }

    private static final class Zone {
        final ZoneKind kind;
        final ServerPlayer owner;
        final ServerLevel level;
        final Vec3 a, b;
        final int duration;
        final double radius;
        int age;

        Zone(ZoneKind kind, ServerPlayer owner, Vec3 a, Vec3 b, int duration, double radius) {
            this.kind = kind;
            this.owner = owner;
            this.level = owner.serverLevel();
            this.a = a;
            this.b = b;
            this.duration = duration;
            this.radius = radius;
        }

        Vec3 pos() { return a.add(b.subtract(a).scale(Math.min(1.0, (double) age / duration))); }
    }

    /** an entity held in place (crystal / ice prison) */
    private record Hold(ServerPlayer owner, LivingEntity target, Vec3 at, int duration, boolean ice, int[] age) {}

    private record Possession(ServerPlayer owner, Mob mob, int duration, int[] age) {}

    private record TempBlock(ServerLevel level, BlockPos pos, BlockState state, long until) {}

    private static final List<Zone> ZONES = new ArrayList<>();
    private static final List<Hold> HOLDS = new ArrayList<>();
    private static final List<Possession> POSSESSIONS = new ArrayList<>();
    private static final List<TempBlock> TEMP_BLOCKS = new ArrayList<>();
    /** players waiting to land a slam: id -> ability */
    private static final java.util.Map<ServerPlayer, Ability> SLAMS = new java.util.HashMap<>();

    private Powers() {}

    // ------------------------------------------------------------------ entry points

    public static void use(ServerPlayer p, int slot) {
        OmniData d = OmniCapability.get(p);
        Alien a = d.current();
        if (a == null || slot < 0 || slot > 3 || !p.isAlive()) return;
        if (p.level().getGameTime() - d.transformTime < Transform.TRANSFORM_ANIM) return;
        Ability ab = a.ability(slot);
        if (ab == null || d.cooldowns[ab.ordinal()] > 0) return;
        if (!execute(p, d, ab)) return;
        d.cooldowns[ab.ordinal()] = ab.cooldown;
        d.castAbility = ab.ordinal();
        d.castTime = p.level().getGameTime();
        Net.sync(p);
    }

    public static void setBlocking(ServerPlayer p, boolean on) {
        OmniData d = OmniCapability.get(p);
        if (!d.transformed()) on = false;
        if (d.blocking == on) return;
        d.blocking = on;
        if (on) {
            Alien a = d.current();
            Util.play(p.serverLevel(), p.position(), ModRegistry.BLOCK.get(), 0.8F, a == Alien.FOURARMS ? 0.7F : 1.2F);
            if (a == Alien.BIGCHILL) iceWall(p);
        }
        Net.sync(p);
    }

    public static boolean isPossessedBy(Entity e, ServerPlayer p) {
        for (Possession ps : POSSESSIONS) if (ps.mob == e && ps.owner == p) return true;
        return false;
    }

    private static boolean execute(ServerPlayer p, OmniData d, Ability ab) {
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 hand = Util.hand(p);
        switch (ab) {
            // ================= HEATBLAST
            case FIREBALL -> shoot(p, AlienProjectile.Kind.FIREBALL, hand, look, 1.7F, ModRegistry.FIRE_SHOT.get());
            case FLAMETHROWER -> {
                Fx.follow(l, Fx.Type.FLAME_CONE, p, 30, 1F);
                Util.play(l, p.position(), ModRegistry.FLAME.get(), 1.2F, 1F);
                for (int i = 0; i < 15; i++) {
                    Util.schedule(l, i * 2, lv -> {
                        if (!p.isAlive()) return;
                        for (LivingEntity e : Util.cone(p, 8.5, 0.82, false)) {
                            Util.hit(e, 2.5F, p, p);
                            e.setSecondsOnFire(4);
                        }
                    });
                }
            }
            case SUPERNOVA -> {
                Vec3 c = p.position();
                Fx.at(l, Fx.Type.FIRE_RING, c, 22, 8F);
                Util.play(l, c, ModRegistry.SUPERNOVA.get(), 2.5F, 0.8F);
                for (LivingEntity e : Util.around(p, c, 8, true)) {
                    Util.hit(e, (float) (15 - e.distanceTo(p)), p, p);
                    e.setSecondsOnFire(7);
                    Util.push(e, c, 1.4, 0.4);
                }
                Util.particles(l, ParticleTypes.FLAME, c.add(0, 1, 0), 120, 2.5, 0.2);
                l.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
            }
            case FIRE_JET -> {
                p.setDeltaMovement(look.scale(1.5).add(0, 0.7, 0));
                p.hurtMarked = true;
                p.fallDistance = 0;
                d.flightTicks = Math.max(d.flightTicks, 1);
                Fx.follow(l, Fx.Type.FIRE_TRAIL, p, 40, 1F);
                Util.play(l, p.position(), ModRegistry.JET.get(), 1.3F, 1F);
                SLAMS.put(p, Ability.FIRE_JET);
            }
            // ================= TERRASPIN
            case WIND_GUST -> {
                Fx.follow(l, Fx.Type.WIND_CONE, p, 16, 1F);
                Util.play(l, p.position(), ModRegistry.WIND.get(), 1.4F, 1F);
                for (LivingEntity e : Util.cone(p, 11, 0.75, false)) {
                    Util.hit(e, 3F, p, p);
                    e.clearFire();
                    e.setDeltaMovement(look.scale(2.2).add(0, 0.5, 0));
                    e.hurtMarked = true;
                }
                for (Projectile pr : l.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(10), pr -> pr.getOwner() != p)) {
                    pr.setDeltaMovement(look.scale(1.5));
                    pr.setOwner(p);
                    pr.hurtMarked = true;
                }
            }
            case TORNADO -> {
                Vec3 f = Util.flat(look);
                Vec3 a = Util.ground(l, p.position().add(f.scale(2)));
                Vec3 b = a.add(f.scale(18));
                BlockHitResult bh = l.clip(new ClipContext(a.add(0, 1, 0), b.add(0, 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (bh.getType() != HitResult.Type.MISS) b = bh.getLocation().add(0, -1, 0).subtract(f.scale(0.5));
                ZONES.add(new Zone(ZoneKind.TORNADO, p, a, b, 70, 2.6));
                Fx.line(l, Fx.Type.TORNADO, a, b, 70, 2.6F);
                Util.play(l, a, ModRegistry.TORNADO.get(), 1.5F, 1F);
            }
            case SPIN_FLIGHT -> {
                d.flightTicks = 120;
                p.setDeltaMovement(p.getDeltaMovement().add(0, 1.1, 0));
                p.hurtMarked = true;
                Fx.at(l, Fx.Type.SHOCKWAVE, p.position(), 12, 3F);
                Util.play(l, p.position(), ModRegistry.TORNADO.get(), 1F, 1.5F);
            }
            case SHELL_SLAM -> {
                p.setDeltaMovement(new Vec3(look.x * 0.4, 1.1, look.z * 0.4));
                p.hurtMarked = true;
                SLAMS.put(p, Ability.SHELL_SLAM);
                Util.schedule(l, 9, lv -> {
                    if (!p.onGround()) {
                        p.setDeltaMovement(new Vec3(0, -2.2, 0));
                        p.hurtMarked = true;
                    }
                });
            }
            // ================= DIAMONDHEAD
            case CRYSTAL_SHARDS -> {
                for (int i = -2; i <= 2; i++) {
                    Vec3 dir = look.yRot(i * 0.09F).add(0, Math.abs(i) * 0.01, 0);
                    shoot(p, AlienProjectile.Kind.CRYSTAL, hand, dir, 2.2F, null);
                }
                Util.play(l, p.position(), ModRegistry.CRYSTAL.get(), 1F, 1.3F);
            }
            case CRYSTAL_BLADES -> {
                d.bladesTicks = 300;
                Util.play(l, p.position(), ModRegistry.CRYSTAL.get(), 1.2F, 0.8F);
                Util.particles(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.EMERALD_BLOCK.defaultBlockState()), p.position().add(0, 1.2, 0), 20, 0.5, 0.1);
            }
            case SPIKE_WAVE -> {
                Vec3 f = Util.flat(look);
                Vec3 a = Util.ground(l, p.position().add(f.scale(1.5))), b = Util.ground(l, p.position().add(f.scale(15)));
                Fx.line(l, Fx.Type.CRYSTAL_SPIKES, a, b, 50, 1F);
                Util.play(l, a, ModRegistry.CRYSTAL.get(), 1.5F, 0.6F);
                Set<Integer> hitIds = new HashSet<>();
                for (int i = 0; i < 14; i++) {
                    final Vec3 at = a.add(b.subtract(a).scale(i / 13.0));
                    Util.schedule(l, (int) (i * 0.7), lv -> {
                        for (LivingEntity e : Util.around(p, at, 1.6, false)) {
                            if (!hitIds.add(e.getId())) continue;
                            Util.hit(e, 9F, p, p);
                            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.9, 0));
                            e.hurtMarked = true;
                        }
                        if (lv.random.nextInt(3) == 0) Util.play(lv, at, ModRegistry.SHATTER.get(), 0.5F, 0.8F + lv.random.nextFloat() * 0.4F);
                    });
                }
            }
            case CRYSTAL_PRISON, ICE_PRISON -> {
                boolean ice = ab == Ability.ICE_PRISON;
                LivingEntity t = Util.aimEntity(p, 22);
                if (t == null) return fail(p);
                HOLDS.removeIf(h -> h.target == t);
                HOLDS.add(new Hold(p, t, t.position(), 80, ice, new int[1]));
                Fx.link(l, ice ? Fx.Type.ICE_PRISON : Fx.Type.CRYSTAL_PRISON, p, t, 80, 1F);
                Util.hit(t, 4F, p, p);
                Util.play(l, t.position(), ice ? ModRegistry.FREEZE.get() : ModRegistry.CRYSTAL.get(), 1.4F, 0.7F);
            }
            // ================= GOOP
            case ACID_SPIT -> shoot(p, AlienProjectile.Kind.ACID, hand, look, 1.6F, ModRegistry.SLIME_SHOT.get());
            case STRETCH_GRAB -> {
                LivingEntity t = Util.aimEntity(p, 18);
                if (t == null) return fail(p);
                Fx.link(l, Fx.Type.STRETCH, p, t, 14, 1F);
                Util.play(l, p.position(), ModRegistry.SLIME_SHOT.get(), 1F, 0.6F);
                Util.schedule(l, 5, lv -> {
                    if (!t.isAlive()) return;
                    Util.hit(t, 4F, p, p);
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    if (!t.getType().is(Tags.EntityTypes.BOSSES)) {
                        Vec3 dest = p.position().add(Util.flat(p.getLookAngle()).scale(1.6));
                        t.setDeltaMovement(dest.subtract(t.position()).scale(0.35).add(0, 0.35, 0));
                        t.hurtMarked = true;
                    }
                    Util.play(lv, t.position(), ModRegistry.SPLAT.get(), 1F, 1.2F);
                });
            }
            case PUDDLE -> {
                d.puddleTicks = 100;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, false, false, true));
                Fx.at(l, Fx.Type.SLIME_SPLASH, p.position(), 16, 1.5F);
                Util.play(l, p.position(), ModRegistry.SPLAT.get(), 1.2F, 0.6F);
            }
            case ACID_BOMB -> {
                shoot(p, AlienProjectile.Kind.ACID_BOMB, hand.add(0, 0.4, 0), look.add(0, 0.35, 0).normalize(), 1.15F, ModRegistry.SLIME_SHOT.get());
            }
            // ================= XLR8
            case SPEED_DASH -> {
                Vec3 f = Util.flat(look);
                Vec3 start = p.position();
                Vec3 dest = start;
                for (double dist = 14; dist >= 1; dist -= 0.5) {
                    Vec3 c = start.add(f.scale(dist));
                    BlockHitResult bh = l.clip(new ClipContext(start.add(0, 0.6, 0), c.add(0, 0.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                    if (bh.getType() != HitResult.Type.MISS) continue;
                    Vec3 g = Util.ground(l, c.add(0, 0.5, 0));
                    if (l.noCollision(p, p.getBoundingBox().move(g.subtract(start)))) { dest = g; break; }
                }
                for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(start, dest).inflate(1.5), e -> Util.canHit(p, e, false))) {
                    if (e.getBoundingBox().inflate(1).clip(start.add(0, 1, 0), dest.add(0, 1, 0)).isEmpty()) continue;
                    Util.hit(e, 6F, p, p);
                    Util.push(e, start, 1.2, 0.5);
                }
                Fx.line(l, Fx.Type.SPEED_TRAIL, start, dest, 14, 1F);
                p.teleportTo(dest.x, dest.y, dest.z);
                p.setDeltaMovement(f.scale(0.6));
                p.hurtMarked = true;
                Util.play(l, dest, ModRegistry.ZOOM.get(), 1.2F, 1F);
            }
            case BLITZ -> {
                LivingEntity t = Util.aimEntity(p, 18);
                if (t == null) return fail(p);
                Vec3 origin = p.position();
                for (int i = 0; i < 8; i++) {
                    final int k = i;
                    Util.schedule(l, i * 4, lv -> {
                        if (!t.isAlive() || !p.isAlive()) return;
                        double ang = k * 2.4;
                        Vec3 from = p.position();
                        Vec3 to = t.position().add(Math.cos(ang) * 1.4, 0, Math.sin(ang) * 1.4);
                        p.teleportTo(to.x, t.getY(), to.z);
                        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
                        Fx.line(lv, Fx.Type.SPEED_TRAIL, from, p.position(), 8, 1F);
                        Util.hit(t, k == 7 ? 7F : 3F, p, p);
                        Fx.at(lv, Fx.Type.HIT, t.position().add(0, t.getBbHeight() * 0.6, 0), 6, 4);
                        Util.play(lv, t.position(), ModRegistry.PUNCH.get(), 0.8F, 1.2F + k * 0.05F);
                        if (k == 7) Util.push(t, origin, 1.5, 0.6);
                    });
                }
            }
            case VORTEX_RUN -> {
                Vec3 c = p.position();
                ZONES.add(new Zone(ZoneKind.VORTEX_RUN, p, c, c, 40, 6));
                Fx.at(l, Fx.Type.SPEED_VORTEX, c, 40, 3.5F);
                Util.play(l, c, ModRegistry.TORNADO.get(), 1.3F, 1.6F);
            }
            case OVERDRIVE -> {
                d.overdriveTicks = 240;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 4, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 240, 2, false, false, true));
                Fx.at(l, Fx.Type.SPEED_VORTEX, p.position(), 14, 1.5F);
                Util.play(l, p.position(), ModRegistry.ZOOM.get(), 1.4F, 0.7F);
            }
            // ================= FOUR ARMS
            case THUNDER_CLAP -> Util.schedule(l, 8, lv -> {
                Fx.follow(lv, Fx.Type.CLAP, p, 14, 1F);
                Util.play(lv, p.position(), ModRegistry.CLAP.get(), 2F, 1F);
                for (LivingEntity e : Util.cone(p, 10, 0.6, false)) {
                    Util.hit(e, 9F, p, p);
                    e.setDeltaMovement(p.getLookAngle().scale(2.5).add(0, 0.6, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                }
            });
            case GROUND_POUND -> {
                p.setDeltaMovement(new Vec3(look.x * 0.6, 1.25, look.z * 0.6));
                p.hurtMarked = true;
                SLAMS.put(p, Ability.GROUND_POUND);
                Util.play(l, p.position(), ModRegistry.JET.get(), 0.8F, 0.6F);
            }
            case QUAD_COMBO -> {
                for (int i = 0; i < 4; i++) {
                    final int k = i;
                    Util.schedule(l, 3 + i * 5, lv -> {
                        if (!p.isAlive()) return;
                        for (LivingEntity e : Util.cone(p, 4.5, 0.6, false)) {
                            Util.hit(e, k == 3 ? 10F : 5F, p, p);
                            Fx.at(lv, Fx.Type.HIT, e.position().add(0, e.getBbHeight() * 0.6, 0), 6, 5);
                            if (k == 3) Util.push(e, p.position(), 2.2, 0.6);
                        }
                        Util.play(lv, p.position(), ModRegistry.PUNCH.get(), 1F, 0.8F + k * 0.1F);
                    });
                }
            }
            case BOULDER_THROW -> {
                BlockState st = ripBlock(p);
                Util.schedule(l, 11, lv -> {
                    AlienProjectile b = new AlienProjectile(lv, p, AlienProjectile.Kind.BOULDER);
                    b.setBlock(st);
                    Vec3 start = p.getEyePosition().add(0, 0.6, 0);
                    b.setPos(start.x, start.y, start.z);
                    b.shoot(p.getLookAngle().x, p.getLookAngle().y + 0.1, p.getLookAngle().z, 1.5F, 0F);
                    lv.addFreshEntity(b);
                    Util.play(lv, start, ModRegistry.WIND.get(), 1F, 0.6F);
                });
            }
            // ================= BIG CHILL
            case FREEZE_BREATH -> {
                Fx.follow(l, Fx.Type.FROST_CONE, p, 30, 1F);
                Util.play(l, p.position(), ModRegistry.FREEZE.get(), 1.2F, 1F);
                for (int i = 0; i < 15; i++) {
                    Util.schedule(l, i * 2, lv -> {
                        if (!p.isAlive()) return;
                        for (LivingEntity e : Util.cone(p, 8.5, 0.82, false)) {
                            Util.hit(e, 2F, p, p);
                            e.setTicksFrozen(Math.min(300, e.getTicksFrozen() + 25));
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                            e.clearFire();
                        }
                        freezeWater(lv, p);
                    });
                }
            }
            case PHASE, INTANGIBLE -> {
                d.phaseTicks = ab == Ability.PHASE ? 80 : 100;
                if (ab == Ability.INTANGIBLE) d.flightTicks = 100;
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, d.phaseTicks, 0, false, false, false));
                for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) m.setTarget(null);
                Util.play(l, p.position(), ModRegistry.PHASE.get(), 1F, ab == Ability.PHASE ? 1.2F : 0.8F);
                Util.particles(l, ab == Ability.PHASE ? ModRegistry.FROST.get() : ModRegistry.ECTO.get(), p.position().add(0, 1, 0), 20, 0.4, 0.02);
            }
            case ICE_FLIGHT, DISC_RIDE -> {
                d.flightTicks = ab == Ability.ICE_FLIGHT ? 240 : 200;
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.5, 0));
                p.hurtMarked = true;
                Util.play(l, p.position(), ab == Ability.ICE_FLIGHT ? ModRegistry.FREEZE.get() : ModRegistry.SONIC.get(), 1F, 1.3F);
            }
            // ================= ULTIMATE ECHO ECHO
            case SONIC_DISCS -> {
                for (int i = -1; i <= 1; i++) shoot(p, AlienProjectile.Kind.SONIC_DISC, hand, look.yRot(i * 0.25F), 1.1F, null);
                Util.play(l, p.position(), ModRegistry.SONIC.get(), 1F, 1.2F);
            }
            case SONIC_DOOM -> {
                LivingEntity t = Util.aimEntity(p, 26);
                if (t == null) return fail(p);
                Fx.link(l, Fx.Type.SONIC_DOOM, p, t, 30, 1F);
                Util.play(l, t.position(), ModRegistry.SONIC.get(), 1.5F, 0.7F);
                for (int i = 0; i < 5; i++) Util.schedule(l, i * 6, lv -> { if (t.isAlive()) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6)); });
                Util.schedule(l, 28, lv -> {
                    if (!t.isAlive()) return;
                    Util.hit(t, 22F, p, p);
                    for (LivingEntity e : Util.around(p, t.position(), 3.5, true)) if (e != t) Util.hit(e, 10F, p, p);
                    Util.push(t, p.position(), 1.5, 0.5);
                    Fx.at(lv, Fx.Type.SHOCKWAVE, t.position(), 14, 4F);
                    Util.play(lv, t.position(), ModRegistry.SCREAM.get(), 2F, 1.4F);
                });
            }
            case SONIC_SCREAM -> {
                Fx.follow(l, Fx.Type.SONIC_WAVE, p, 22, 1F);
                Util.play(l, p.position(), ModRegistry.SCREAM.get(), 2F, 1F);
                for (LivingEntity e : Util.cone(p, 13, 0.7, false)) {
                    Util.hit(e, 8F, p, p);
                    e.setDeltaMovement(p.getLookAngle().scale(2).add(0, 0.4, 0));
                    e.hurtMarked = true;
                    e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                }
                shatterGlass(l, p);
            }
            // ================= GHOSTFREAK
            case POSSESS -> {
                LivingEntity t = Util.aimEntity(p, 16);
                if (!(t instanceof Mob mob) || t.getType().is(Tags.EntityTypes.BOSSES)) return fail(p);
                POSSESSIONS.removeIf(ps -> ps.owner == p);
                POSSESSIONS.add(new Possession(p, mob, 300, new int[1]));
                d.phaseTicks = 300;
                mob.setTarget(null);
                Fx.link(l, Fx.Type.TENTACLE, p, mob, 16, 1F);
                Util.play(l, mob.position(), ModRegistry.GHOST.get(), 1.4F, 0.7F);
                p.displayClientMessage(Component.translatable("msg.omnitrix.possess", mob.getDisplayName()), true);
            }
            case TENTACLES -> {
                List<LivingEntity> ts = Util.around(p, p.position(), 9, true);
                if (ts.isEmpty()) return fail(p);
                Util.play(l, p.position(), ModRegistry.GHOST.get(), 1.2F, 1.2F);
                for (int i = 0; i < Math.min(4, ts.size()); i++) {
                    LivingEntity t = ts.get(i);
                    Fx.link(l, Fx.Type.TENTACLE, p, t, 20, 1F);
                    Util.schedule(l, 6, lv -> {
                        if (!t.isAlive()) return;
                        Util.hit(t, 6F, p, p);
                        t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                        Vec3 dest = p.position().add(Util.flat(p.getLookAngle()).scale(2));
                        t.setDeltaMovement(dest.subtract(t.position()).scale(0.3).add(0, 0.3, 0));
                        t.hurtMarked = true;
                    });
                }
            }
            case TERROR -> {
                Vec3 c = p.position();
                Fx.at(l, Fx.Type.TERROR, c, 26, 10F);
                Util.play(l, c, ModRegistry.TERROR.get(), 2F, 1F);
                for (LivingEntity e : Util.around(p, c, 10, true)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0));
                    Util.hit(e, 4F, p, p);
                    if (e instanceof PathfinderMob pm) flee(pm, p, 6);
                }
            }
            default -> { return false; }
        }
        return true;
    }

    private static boolean fail(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("msg.omnitrix.no_target"), true);
        return false;
    }

    private static void shoot(ServerPlayer p, AlienProjectile.Kind kind, Vec3 from, Vec3 dir, float speed, net.minecraft.sounds.SoundEvent sound) {
        AlienProjectile pr = new AlienProjectile(p.level(), p, kind);
        pr.setPos(from.x, from.y, from.z);
        pr.shoot(dir.x, dir.y, dir.z, speed, 0F);
        p.level().addFreshEntity(pr);
        if (sound != null) Util.play(p.serverLevel(), from, sound, 1F, 0.9F + p.getRandom().nextFloat() * 0.2F);
    }

    private static void flee(PathfinderMob m, ServerPlayer p, int times) {
        if (times <= 0 || !m.isAlive()) return;
        Vec3 away = DefaultRandomPos.getPosAway(m, 16, 7, p.position());
        if (away != null) m.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
        m.setTarget(null);
        Util.schedule((ServerLevel) m.level(), 20, lv -> flee(m, p, times - 1));
    }

    /** Four Arms tears a block out of the ground in front of him (respects protection via BreakEvent) */
    private static BlockState ripBlock(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockPos base = BlockPos.containing(p.position().add(Util.flat(p.getLookAngle()).scale(1.5))).below();
        for (BlockPos pos : new BlockPos[]{base, base.below(), p.blockPosition().below()}) {
            BlockState st = l.getBlockState(pos);
            if (st.isAir() || st.getDestroySpeed(l, pos) < 0 || st.getDestroySpeed(l, pos) > 5 || l.getBlockEntity(pos) != null || !st.getFluidState().isEmpty()) continue;
            if (!p.mayBuild() || !l.mayInteract(p, pos)) break;
            BlockEvent.BreakEvent ev = new BlockEvent.BreakEvent(l, pos, st, p);
            if (MinecraftForge.EVENT_BUS.post(ev)) break;
            l.removeBlock(pos, false);
            l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
            Util.play(l, Vec3.atCenterOf(pos), ModRegistry.SLAM.get(), 1F, 1.4F);
            return st;
        }
        return Blocks.STONE.defaultBlockState();
    }

    private static void freezeWater(ServerLevel l, ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (int d = 2; d <= 8; d += 2) {
            BlockPos c = BlockPos.containing(eye.add(look.scale(d)));
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-1, -2, -1), c.offset(1, 0, 1))) {
                if (l.getBlockState(pos).is(Blocks.WATER) && l.getFluidState(pos).isSource() && l.getBlockState(pos.above()).isAir() && p.mayBuild()) {
                    l.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    l.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + l.random.nextInt(40));
                }
            }
        }
    }

    private static void shatterGlass(ServerLevel l, ServerPlayer p) {
        if (!p.mayBuild()) return;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (int d = 2; d <= 12; d++) {
            BlockPos c = BlockPos.containing(eye.add(look.scale(d)));
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-1, -1, -1), c.offset(1, 1, 1))) {
                BlockState st = l.getBlockState(pos);
                if ((st.is(Tags.Blocks.GLASS) || st.is(Tags.Blocks.GLASS_PANES)) && !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(l, pos.immutable(), st, p))) {
                    l.destroyBlock(pos, false);
                }
            }
        }
    }

    /** Big Chill raises a wall of ice in front of him (only into air, melts after 5 s) */
    private static void iceWall(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        Direction dir = p.getDirection();
        Direction side = dir.getClockWise();
        BlockPos c = p.blockPosition().relative(dir, 2);
        long until = l.getGameTime() + 100;
        for (int s = -1; s <= 1; s++) {
            for (int y = 0; y < 3; y++) {
                BlockPos pos = c.relative(side, s).above(y);
                if (l.getBlockState(pos).isAir() && l.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) {
                    l.setBlockAndUpdate(pos, Blocks.PACKED_ICE.defaultBlockState());
                    TEMP_BLOCKS.add(new TempBlock(l, pos.immutable(), Blocks.PACKED_ICE.defaultBlockState(), until));
                }
            }
        }
        Util.play(l, Vec3.atCenterOf(c), ModRegistry.FREEZE.get(), 1.2F, 0.8F);
    }

    public static void acidPool(ServerLevel l, ServerPlayer owner, Vec3 at) {
        ZONES.add(new Zone(ZoneKind.ACID_POOL, owner, at, at, 80, 3.5));
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Util.runTasks();
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            if (!z.owner.isAlive() || z.owner.level() != z.level || ++z.age > z.duration) { it.remove(); continue; }
            tickZone(z);
        }
        for (Iterator<Hold> it = HOLDS.iterator(); it.hasNext(); ) {
            Hold h = it.next();
            LivingEntity t = h.target;
            if (!t.isAlive() || ++h.age[0] > h.duration) {
                it.remove();
                if (t.isAlive()) {
                    Util.hit(t, 6F, h.owner, h.owner);
                    ServerLevel l = (ServerLevel) t.level();
                    l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, h.ice ? Blocks.ICE.defaultBlockState() : Blocks.EMERALD_BLOCK.defaultBlockState()),
                            t.getX(), t.getY() + 1, t.getZ(), 40, 0.4, 0.6, 0.4, 0.1);
                    Util.play(l, t.position(), ModRegistry.SHATTER.get(), 1.2F, 0.9F);
                }
                continue;
            }
            boolean boss = t.getType().is(Tags.EntityTypes.BOSSES);
            if (!boss) {
                t.teleportTo(h.at.x, h.at.y, h.at.z);
                t.setDeltaMovement(Vec3.ZERO);
                if (t instanceof Mob m) { m.getNavigation().stop(); m.setTarget(null); }
            }
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, boss ? 2 : 9, false, false));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 3, false, false));
            if (h.ice) t.setTicksFrozen(200);
            if (h.age[0] % 20 == 0) Util.hit(t, 2F, h.owner, h.owner);
        }
        for (Iterator<Possession> it = POSSESSIONS.iterator(); it.hasNext(); ) {
            Possession ps = it.next();
            ServerPlayer p = ps.owner;
            OmniData d = OmniCapability.get(p);
            if (!ps.mob.isAlive() || !p.isAlive() || d.current() != Alien.GHOSTFREAK || ++ps.age[0] > ps.duration || ps.mob.level() != p.level()) {
                it.remove();
                d.phaseTicks = 0;
                p.removeEffect(MobEffects.INVISIBILITY);
                if (ps.mob.isAlive()) {
                    Util.hit(ps.mob, 8F, p, p);
                    Util.play(p.serverLevel(), ps.mob.position(), ModRegistry.GHOST.get(), 1F, 1.4F);
                }
                Net.sync(p);
                continue;
            }
            p.teleportTo(ps.mob.getX(), ps.mob.getY() + 0.2, ps.mob.getZ());
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 5, 0, false, false, false));
            if (ps.age[0] % 10 == 0) {
                LivingEntity prey = null;
                double best = 24 * 24;
                for (LivingEntity o : ps.mob.level().getEntitiesOfClass(LivingEntity.class, ps.mob.getBoundingBox().inflate(24),
                        o -> o != ps.mob && o != p && o instanceof Enemy && o.isAlive())) {
                    double dd = o.distanceToSqr(ps.mob);
                    if (dd < best) { best = dd; prey = o; }
                }
                ps.mob.setTarget(prey);
                ps.mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 15, 1, false, false));
                ((ServerLevel) ps.mob.level()).sendParticles(ModRegistry.ECTO.get(), ps.mob.getX(), ps.mob.getY() + ps.mob.getBbHeight(), ps.mob.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
            }
        }
        if (!TEMP_BLOCKS.isEmpty()) {
            for (Iterator<TempBlock> it = TEMP_BLOCKS.iterator(); it.hasNext(); ) {
                TempBlock tb = it.next();
                if (tb.level.getGameTime() < tb.until) continue;
                if (tb.level.getBlockState(tb.pos).is(tb.state.getBlock())) {
                    tb.level.removeBlock(tb.pos, false);
                    tb.level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, tb.state), tb.pos.getX() + 0.5, tb.pos.getY() + 0.5, tb.pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
                }
                it.remove();
            }
        }
    }

    private static void tickZone(Zone z) {
        ServerLevel l = z.level;
        Vec3 c = z.pos();
        switch (z.kind) {
            case TORNADO -> {
                for (LivingEntity e : Util.around(z.owner, c, z.radius, false)) {
                    if (!Util.canHit(z.owner, e, true) && e.distanceToSqr(c) > 2) continue;
                    Vec3 r = e.position().subtract(c);
                    Vec3 tangent = new Vec3(-r.z, 0, r.x).normalize().scale(0.35);
                    e.setDeltaMovement(tangent.add(c.subtract(e.position()).multiply(1, 0, 1).scale(0.15)).add(0, e.getY() - c.y < 3.5 ? 0.3 : 0.02, 0));
                    e.hurtMarked = true;
                    e.fallDistance = 0;
                    if (z.age % 10 == 0) Util.hit(e, 3F, z.owner, z.owner);
                }
                for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(z.radius, 5, z.radius), pr -> pr.getOwner() != z.owner)) {
                    pr.setDeltaMovement(pr.getDeltaMovement().scale(-0.5).add(0, 0.5, 0));
                }
                if (z.age % 20 == 0) Util.play(l, c, ModRegistry.TORNADO.get(), 0.8F, 1.2F);
            }
            case ACID_POOL -> {
                if (z.age % 3 == 0) l.sendParticles(ModRegistry.SLIME.get(), c.x, c.y + 0.1, c.z, 3, z.radius * 0.5, 0.05, z.radius * 0.5, 0.01);
                if (z.age % 10 == 0) {
                    for (LivingEntity e : Util.around(z.owner, c, z.radius, true)) {
                        Util.hit(e, 2F, z.owner, z.owner);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
                    }
                }
            }
            case VORTEX_RUN -> {
                ServerPlayer p = z.owner;
                double ang = z.age * 0.7;
                Vec3 at = Util.ground(l, c.add(Math.cos(ang) * 3.5, 0.5, Math.sin(ang) * 3.5));
                p.teleportTo(at.x, at.y, at.z);
                p.fallDistance = 0;
                for (LivingEntity e : Util.around(p, c, z.radius, false)) {
                    if (!Util.canHit(p, e, true) && e.distanceToSqr(c) > 9) continue;
                    Vec3 r = e.position().subtract(c);
                    e.setDeltaMovement(new Vec3(-r.z, 0, r.x).normalize().scale(0.45).add(c.subtract(e.position()).multiply(1, 0, 1).scale(0.12)).add(0, 0.18, 0));
                    e.hurtMarked = true;
                    if (z.age % 10 == 0) Util.hit(e, 2.5F, p, p);
                }
                if (z.age == z.duration) {
                    for (LivingEntity e : Util.around(p, c, z.radius, true)) Util.push(e, c, 1.6, 0.8);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        OmniData d = OmniCapability.get(p);
        for (int i = 0; i < d.cooldowns.length; i++) if (d.cooldowns[i] > 0) d.cooldowns[i]--;
        Alien a = d.current();
        if (a == null) return;
        ServerLevel l = p.serverLevel();
        boolean dirty = false;
        // timed states
        if (d.flightTicks > 0) {
            d.flightTicks--;
            p.fallDistance = 0;
            boolean fly = a == Alien.BIGCHILL || a == Alien.ULTIMATE_ECHO_ECHO || a == Alien.GHOSTFREAK;
            if (fly && !p.getAbilities().mayfly) {
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
            }
            if (a == Alien.TERRASPIN && !p.onGround()) {
                // spin glide
                Vec3 v = p.getDeltaMovement();
                Vec3 f = Util.flat(p.getLookAngle());
                p.setDeltaMovement(v.x * 0.9 + f.x * 0.08, Math.max(v.y, -0.08), v.z * 0.9 + f.z * 0.08);
                p.hurtMarked = true;
                if (p.tickCount % 3 == 0) l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 2, 0.4, 0.1, 0.4, 0.02);
            }
            if (d.flightTicks == 0) {
                if (fly && !p.isCreative() && !p.isSpectator()) {
                    p.getAbilities().mayfly = false;
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                }
                dirty = true;
            }
        }
        if (d.phaseTicks > 0 && --d.phaseTicks == 0) { p.removeEffect(MobEffects.INVISIBILITY); dirty = true; }
        if (d.puddleTicks > 0 && --d.puddleTicks == 0) {
            // splash back up
            for (LivingEntity t : Util.around(p, p.position(), 3, true)) {
                Util.hit(t, 6F, p, p);
                Util.push(t, p.position(), 0.8, 0.5);
            }
            Fx.at(l, Fx.Type.SLIME_SPLASH, p.position(), 16, 2.5F);
            Util.play(l, p.position(), ModRegistry.SPLAT.get(), 1.2F, 1F);
            dirty = true;
        }
        if (d.puddleTicks > 0 && p.tickCount % 10 == 0) {
            for (LivingEntity t : Util.around(p, p.position(), 1.5, true)) {
                Util.hit(t, 2F, p, p);
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
            }
        }
        if (d.overdriveTicks > 0 && --d.overdriveTicks == 0) dirty = true;
        if (d.bladesTicks > 0 && --d.bladesTicks == 0) dirty = true;
        // landing slams
        Ability slam = SLAMS.get(p);
        if (slam != null && p.onGround() && p.tickCount % 1 == 0 && l.getGameTime() - d.castTime > 4) {
            SLAMS.remove(p);
            land(p, slam);
        } else if (slam != null && l.getGameTime() - d.castTime > 80) SLAMS.remove(p);
        // passives
        switch (a) {
            case HEATBLAST -> {
                p.clearFire();
                if (p.tickCount % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false, false));
                if (p.isInWaterOrRain() && p.tickCount % 20 == 0) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false, true));
                    l.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.5, p.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                }
                if (p.tickCount % 2 == 0) l.sendParticles(ModRegistry.FIRE.get(), p.getX(), p.getY() + 2.0, p.getZ(), 1, 0.12, 0.05, 0.12, 0.01);
            }
            case TERRASPIN -> { if (p.isShiftKeyDown() && !p.onGround()) p.setDeltaMovement(p.getDeltaMovement().multiply(1, 0.7, 1)); }
            case XLR8 -> {
                if (p.tickCount % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false, false));
                // run across water while sprinting
                if (p.isSprinting() && l.getFluidState(p.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER) && !p.isUnderWater()) {
                    p.setDeltaMovement(p.getDeltaMovement().multiply(1, 0, 1));
                    p.setOnGround(true);
                    p.hurtMarked = true;
                    p.fallDistance = 0;
                }
            }
            case FOURARMS -> { if (p.tickCount % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, false, false, false)); }
            case DIAMONDHEAD -> { if (p.tickCount % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false, false)); }
            case BIGCHILL -> {
                p.setTicksFrozen(0);
                if (p.onGround() && p.tickCount % 2 == 0) frostWalk(p);
            }
            case ULTIMATE_ECHO_ECHO -> {
                if (p.tickCount % 20 == 0) {
                    for (LivingEntity t : Util.around(p, p.position(), 22, true)) t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, false, false));
                }
            }
            case GHOSTFREAK -> {
                if (p.tickCount % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false, false));
                if (!p.onGround() && p.getDeltaMovement().y < -0.1 && !p.isShiftKeyDown()) {
                    p.setDeltaMovement(p.getDeltaMovement().multiply(1, 0.75, 1));
                    p.hurtMarked = true;
                }
            }
            case GOOP -> {}
        }
        if (d.blocking && p.tickCount % 10 == 0) p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, a == Alien.TERRASPIN ? 6 : 1, false, false, false));
        if (dirty) Net.sync(p);
    }

    private static void frostWalk(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, -1, 2))) {
            if (pos.distSqr(c.below()) > 5) continue;
            if (l.getBlockState(pos).is(Blocks.WATER) && l.getFluidState(pos).isSource() && l.getBlockState(pos.above()).isAir()) {
                l.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                l.scheduleTick(pos, Blocks.FROSTED_ICE, 60 + l.random.nextInt(60));
            }
        }
    }

    private static void land(ServerPlayer p, Ability slam) {
        ServerLevel l = p.serverLevel();
        Vec3 c = p.position();
        switch (slam) {
            case GROUND_POUND, SHELL_SLAM -> {
                float radius = slam == Ability.GROUND_POUND ? 6.5F : 5F;
                Fx.at(l, Fx.Type.SHOCKWAVE, c, 22, radius);
                Util.play(l, c, ModRegistry.SLAM.get(), 2F, slam == Ability.GROUND_POUND ? 0.7F : 1F);
                BlockState below = l.getBlockState(p.blockPosition().below());
                l.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, below.isAir() ? Blocks.DIRT.defaultBlockState() : below), c.x, c.y + 0.2, c.z, 80, radius * 0.4, 0.1, radius * 0.4, 0.2);
                for (LivingEntity e : Util.around(p, c, radius, true)) {
                    Util.hit(e, (float) ((slam == Ability.GROUND_POUND ? 15 : 11) - e.distanceTo(p) * 0.8), p, p);
                    Util.push(e, c, 1.2, 0.7);
                }
            }
            case FIRE_JET -> {
                Fx.at(l, Fx.Type.FIRE_RING, c, 12, 3F);
                for (LivingEntity e : Util.around(p, c, 3, true)) {
                    Util.hit(e, 6F, p, p);
                    e.setSecondsOnFire(4);
                }
            }
            default -> {}
        }
    }

    // ------------------------------------------------------------------ combat events

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        OmniData d = OmniCapability.get(p);
        Alien a = d.current();
        if (a == null) return;
        boolean outOfWorld = e.getSource().is(DamageTypes.FELL_OUT_OF_WORLD) || e.getSource().is(DamageTypes.GENERIC_KILL);
        if (d.phaseTicks > 0 && !outOfWorld) { e.setCanceled(true); return; }
        if (a == Alien.HEATBLAST && (e.getSource().is(DamageTypes.IN_FIRE) || e.getSource().is(DamageTypes.ON_FIRE) || e.getSource().is(DamageTypes.LAVA))) {
            e.setCanceled(true);
            return;
        }
        if (a == Alien.GOOP && e.getSource().is(DamageTypes.MAGIC) && p.hasEffect(MobEffects.POISON)) { e.setCanceled(true); return; }
        if (d.puddleTicks > 0 && e.getSource().getDirectEntity() instanceof LivingEntity) { e.setCanceled(true); return; }
        if (d.blocking) {
            Entity src = e.getSource().getDirectEntity();
            switch (a) {
                case GHOSTFREAK -> { if (src instanceof LivingEntity) { e.setCanceled(true); return; } }
                case GOOP -> { if (src instanceof Projectile) { e.setCanceled(true); return; } }
                case XLR8 -> {
                    if (src != null && p.getRandom().nextFloat() < 0.75F) {
                        // sidestep
                        Vec3 side = src.position().subtract(p.position()).cross(new Vec3(0, 1, 0)).normalize().scale(p.getRandom().nextBoolean() ? 2 : -2);
                        Vec3 to = p.position().add(side);
                        if (p.level().noCollision(p, p.getBoundingBox().move(side))) {
                            Fx.line(p.serverLevel(), Fx.Type.SPEED_TRAIL, p.position(), to, 8, 1F);
                            p.teleportTo(to.x, to.y, to.z);
                        }
                        Util.play(p.serverLevel(), to, ModRegistry.ZOOM.get(), 0.6F, 1.6F);
                        e.setCanceled(true);
                        return;
                    }
                }
                default -> {}
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        // defensive side
        if (victim instanceof ServerPlayer p) {
            OmniData d = OmniCapability.get(p);
            Alien a = d.current();
            if (a != null && d.blocking) {
                Entity src = e.getSource().getEntity();
                boolean front = src == null || src.position().subtract(p.position()).normalize().dot(p.getLookAngle()) > -0.2;
                float reduce = switch (a) {
                    case TERRASPIN -> 0.85F;
                    case FOURARMS -> front ? 0.9F : 0.3F;
                    case DIAMONDHEAD -> front ? 0.8F : 0.3F;
                    case HEATBLAST -> front ? 0.7F : 0.2F;
                    case ULTIMATE_ECHO_ECHO -> 0.7F;
                    case GOOP -> 0.6F;
                    case BIGCHILL -> 0.5F;
                    default -> 0.4F;
                };
                e.setAmount(e.getAmount() * (1 - reduce));
                if (e.getSource().getDirectEntity() instanceof LivingEntity att && att.distanceTo(p) < 4) {
                    ServerLevel l = p.serverLevel();
                    switch (a) {
                        case HEATBLAST -> att.setSecondsOnFire(4);
                        case ULTIMATE_ECHO_ECHO, FOURARMS -> Util.push(att, p.position(), 1.2, 0.3);
                        case DIAMONDHEAD -> Util.schedule(l, 1, lv -> Util.hit(att, 3F, p, p));
                        default -> {}
                    }
                }
            }
        }
        // offensive side: alien melee traits
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getSource().getDirectEntity() != p || !e.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        OmniData d = OmniCapability.get(p);
        Alien a = d.current();
        if (a == null) return;
        ServerLevel l = p.serverLevel();
        switch (a) {
            case HEATBLAST -> victim.setSecondsOnFire(4);
            case BIGCHILL -> {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                victim.setTicksFrozen(Math.min(300, victim.getTicksFrozen() + 60));
            }
            case GOOP -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case GHOSTFREAK -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            case FOURARMS -> Util.schedule(l, 1, lv -> Util.push(victim, p.position(), 1.0, 0.2));
            case DIAMONDHEAD -> {
                if (d.bladesTicks > 0) {
                    e.setAmount(e.getAmount() + 6F);
                    Util.schedule(l, 1, lv -> {
                        for (LivingEntity o : Util.around(p, victim.position(), 2.5, true)) if (o != victim) Util.hit(o, 4F, p, p);
                    });
                }
            }
            default -> {}
        }
        Fx.at(l, Fx.Type.HIT, victim.position().add(0, victim.getBbHeight() * 0.6, 0), 6, a.ordinal());
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        OmniData d = OmniCapability.get(p);
        Alien a = d.current();
        if (a == null) return;
        if (a == Alien.TERRASPIN || a == Alien.GOOP || a == Alien.GHOSTFREAK || d.flightTicks > 0) e.setDistance(0);
        else if (a == Alien.FOURARMS || a == Alien.DIAMONDHEAD) e.setDamageMultiplier(0.4F);
    }

    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent e) {
        if (!(e.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult eh) || !(eh.getEntity() instanceof ServerPlayer p)) return;
        OmniData d = OmniCapability.get(p);
        Alien a = d.current();
        if (a == null || e.getProjectile().getOwner() == p) return;
        boolean reflect = (a == Alien.DIAMONDHEAD && (d.blocking || p.getRandom().nextFloat() < 0.25F))
                || (a == Alien.ULTIMATE_ECHO_ECHO && d.blocking) || (a == Alien.HEATBLAST && d.blocking);
        if (!reflect) return;
        Projectile pr = e.getProjectile();
        e.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
        if (a == Alien.HEATBLAST) {
            pr.discard();
            p.serverLevel().sendParticles(ParticleTypes.FLAME, pr.getX(), pr.getY(), pr.getZ(), 10, 0.2, 0.2, 0.2, 0.05);
            return;
        }
        pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.1));
        pr.setOwner(p);
        pr.hurtMarked = true;
        Util.play(p.serverLevel(), pr.position(), a == Alien.DIAMONDHEAD ? ModRegistry.CRYSTAL.get() : ModRegistry.SONIC.get(), 0.8F, 1.6F);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getNewTarget() instanceof ServerPlayer p && OmniCapability.get(p).phaseTicks > 0) { e.setCanceled(true); return; }
        for (Possession ps : POSSESSIONS) {
            if (ps.mob == e.getEntity() && (e.getNewTarget() == ps.owner || !(e.getNewTarget() instanceof Enemy))) {
                if (e.getNewTarget() != null) e.setCanceled(true);
            }
        }
    }

    /** crystal ore etc. are not touched by any power; tags kept for the boulder filter */
    static boolean unbreakable(BlockState s) { return s.is(BlockTags.WITHER_IMMUNE); }
}
