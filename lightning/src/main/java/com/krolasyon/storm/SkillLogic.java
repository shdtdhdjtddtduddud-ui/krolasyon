package com.krolasyon.storm;

import com.krolasyon.storm.entity.BallLightningEntity;
import com.krolasyon.storm.entity.BoltProjectile;
import com.krolasyon.storm.net.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** All server side gameplay of the skill tree: unlocking, casting, passives, timed skills. */
@Mod.EventBusSubscriber(modid = StormTree.MODID)
public final class SkillLogic {
    private record Task(ServerLevel level, long due, Consumer<ServerLevel> run) {}

    private static final class Grasp {
        final ServerPlayer caster;
        final LivingEntity target;
        int age;

        Grasp(ServerPlayer caster, LivingEntity target) { this.caster = caster; this.target = target; }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Grasp> GRASPS = new ArrayList<>();
    public static final int GRASP_PULL = 10, GRASP_TOTAL = 60;

    private SkillLogic() {}

    // ------------------------------------------------------------------ unlocking / slots

    public static void unlock(ServerPlayer p, int id) {
        Skill s = Skill.byId(id);
        StormData d = StormCapability.get(p);
        if (s == null || d.has(s)) return;
        if (!s.requirementsMet(d.unlocked)) {
            p.displayClientMessage(Component.translatable("msg.stormtree.locked"), true);
            return;
        }
        if (!p.getAbilities().instabuild) {
            if (p.experienceLevel < s.levelCost) {
                p.displayClientMessage(Component.translatable("msg.stormtree.no_xp", s.levelCost), true);
                return;
            }
            p.giveExperienceLevels(-s.levelCost);
        }
        d.unlocked.add(s);
        if (!s.passive) {
            for (int i = 0; i < StormData.SLOTS; i++) {
                if (d.slots[i] == null) { d.slots[i] = s; break; }
            }
        }
        ServerLevel l = p.serverLevel();
        play(l, p.position(), ModRegistry.UNLOCK.get(), 1F, 1F);
        Fx.at(l, Fx.Type.BURST, p.position().add(0, 1, 0), 16, 2.5F);
        Fx.follow(l, Fx.Type.AURA, p, 30, 1F);
        sparks(l, p.position().add(0, 1, 0), 30, 0.6);
        Net.sync(p);
    }

    public static void assign(ServerPlayer p, int slot, int id) {
        if (slot < 0 || slot >= StormData.SLOTS) return;
        StormData d = StormCapability.get(p);
        Skill s = Skill.byId(id);
        if (s != null && (!d.has(s) || s.passive)) return;
        for (int i = 0; i < StormData.SLOTS; i++) if (s != null && d.slots[i] == s) d.slots[i] = d.slots[slot];
        d.slots[slot] = s;
        Net.sync(p);
    }

    // ------------------------------------------------------------------ casting

    public static void castSlot(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= StormData.SLOTS) return;
        Skill s = StormCapability.get(p).slots[slot];
        if (s != null) cast(p, s, false);
    }

    public static float energyCost(Player p, Skill s) {
        return StormTomeItem.carries(p) ? s.energy * 0.8F : s.energy;
    }

    /** @param force ignore cooldown/energy/unlock (commands and the CI self test) */
    public static boolean cast(ServerPlayer p, Skill s, boolean force) {
        StormData d = StormCapability.get(p);
        if (s.passive || p.isSpectator() || !p.isAlive()) return false;
        float cost = energyCost(p, s);
        if (!force) {
            if (!d.has(s)) return false;
            if (d.cooldowns[s.ordinal()] > 0) return false;
            if (d.energy < cost) {
                p.displayClientMessage(Component.translatable("msg.stormtree.no_energy"), true);
                p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.6F);
                return false;
            }
        }
        if (!execute(p, s, d)) return false;
        if (!force || d.has(s)) {
            if (!force) d.energy -= cost;
            d.cooldowns[s.ordinal()] = s.cooldown;
        }
        Net.anim(p, s.ordinal());
        Net.sync(p);
        return true;
    }

    private static boolean execute(ServerPlayer p, Skill s, StormData d) {
        ServerLevel l = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        switch (s) {
            case THUNDER_STRIKE -> {
                Vec3 at = aimPoint(p, 40);
                strike(l, p, at, 8F, 2.5F, true);
            }
            case LIGHTNING_BOLT -> {
                BoltProjectile b = new BoltProjectile(l, p);
                Vec3 start = hand(p);
                b.setPos(start.x, start.y, start.z);
                b.shoot(look.x, look.y, look.z, 3.2F, 0F);
                l.addFreshEntity(b);
                play(l, start, ModRegistry.ZAP.get(), 1F, 1.1F + l.random.nextFloat() * 0.2F);
            }
            case SPARK_BURST -> {
                Vec3 c = p.position().add(0, 1, 0);
                Fx.at(l, Fx.Type.BURST, c, 14, 6F);
                play(l, c, ModRegistry.BURST.get(), 1.4F, 1F);
                for (LivingEntity e : enemiesAround(p, c, 6, true)) {
                    hit(e, 6F, p, p);
                    push(e, c, 1.1);
                    Fx.arc(l, p, e, 8, 1F);
                    sparks(l, e.position().add(0, e.getBbHeight() / 2, 0), 8, 0.3);
                }
                sparks(l, c, 40, 0.8);
            }
            case STORM_DOME -> {
                d.domeTicks = 160;
                Fx.follow(l, Fx.Type.DOME, p, 160, 2.6F);
                play(l, p.position(), ModRegistry.DOME.get(), 1.2F, 1F);
            }
            case STATIC_RING -> {
                d.ringTicks = 200;
                Fx.follow(l, Fx.Type.RING, p, 200, 3F);
                play(l, p.position(), ModRegistry.RING.get(), 1.2F, 1F);
            }
            case THUNDERSTORM -> {
                Vec3 c = aimPoint(p, 40);
                play(l, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 2F, 0.8F);
                Fx.at(l, Fx.Type.NOVA, c.add(0, 0.1, 0), 50, 6F);
                Set<Integer> struck = new HashSet<>();
                for (int i = 0; i < 10; i++) {
                    schedule(l, 4 + i * 6, lv -> {
                        if (!p.isAlive()) return;
                        Vec3 at = null;
                        if (lv.random.nextFloat() < 0.65F) {
                            for (LivingEntity e : enemiesAround(p, c, 7, true)) {
                                if (struck.add(e.getId())) { at = e.position(); break; }
                            }
                        }
                        if (at == null) at = ground(lv, c.add((lv.random.nextDouble() - 0.5) * 12, 0, (lv.random.nextDouble() - 0.5) * 12));
                        strike(lv, p, at, 7F, 2.2F, false);
                    });
                }
            }
            case STORM_AVATAR -> {
                d.avatarTicks = 300;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 0, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 1, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 0, false, false, true));
                Fx.follow(l, Fx.Type.AURA, p, 300, 1F);
                Fx.at(l, Fx.Type.BURST, p.position().add(0, 1, 0), 14, 4F);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
                if (bolt != null) {
                    bolt.moveTo(p.getX(), p.getY(), p.getZ());
                    bolt.setVisualOnly(true);
                    l.addFreshEntity(bolt);
                }
                play(l, p.position(), ModRegistry.CHARGE.get(), 1.2F, 1.3F);
            }
            case SHOCK_GRASP -> {
                LivingEntity t = aimEntity(p, 22, false);
                if (t == null) return fail(p);
                GRASPS.removeIf(g -> g.target == t);
                GRASPS.add(new Grasp(p, t));
                hit(t, 4F, p, p);
                Fx.send(l, Fx.Type.TETHER, p.position(), t.position(), p.getId(), t.getId(), GRASP_TOTAL, 1F);
                play(l, t.position(), ModRegistry.GRASP.get(), 1.2F, 1F);
            }
            case BALL_LIGHTNING -> {
                BallLightningEntity b = new BallLightningEntity(l, p);
                Vec3 start = eye.add(look.scale(1.2)).add(0, -0.3, 0);
                b.setPos(start.x, start.y, start.z);
                b.setDeltaMovement(look.scale(0.42));
                l.addFreshEntity(b);
                play(l, start, ModRegistry.BALL_HUM.get(), 1.3F, 1F);
            }
            case CHAIN_LIGHTNING -> {
                LivingEntity first = aimEntity(p, 30, true);
                if (first == null) return fail(p);
                play(l, p.position(), ModRegistry.ARC.get(), 1.4F, 1F);
                chain(l, p, p, first, new HashSet<>(), 0, 10F);
            }
            case THUNDER_NOVA -> {
                p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 14, 1, false, false, false));
                Fx.follow(l, Fx.Type.CHARGE, p, 16, 1.6F);
                play(l, p.position(), ModRegistry.CHARGE.get(), 1.5F, 0.8F);
                schedule(l, 16, lv -> nova(lv, p));
            }
            case LIGHTNING_SPEAR -> {
                Fx.follow(l, Fx.Type.CHARGE, p, 12, 0.8F);
                play(l, p.position(), ModRegistry.CHARGE.get(), 1.2F, 1.5F);
                schedule(l, 12, lv -> spear(lv, p));
            }
            default -> { return false; }
        }
        return true;
    }

    private static boolean fail(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("msg.stormtree.no_target"), true);
        return false;
    }

    // ------------------------------------------------------------------ skill pieces

    /** one lightning strike from the sky at pos */
    public static void strike(ServerLevel l, ServerPlayer caster, Vec3 pos, float dmg, float radius, boolean big) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
        if (bolt != null) {
            bolt.moveTo(pos.x, pos.y, pos.z);
            bolt.setVisualOnly(true);
            l.addFreshEntity(bolt);
        }
        Fx.at(l, Fx.Type.STRIKE, pos, big ? 14 : 10, big ? 1.4F : 1F);
        Fx.at(l, Fx.Type.BURST, pos.add(0, 0.15, 0), 10, radius + 0.5F);
        play(l, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5F, 1F);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius, 3, radius), e -> canHit(caster, e, false))) {
            hit(e, dmg, caster, caster);
            e.setSecondsOnFire(2);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
        }
        sparks(l, pos.add(0, 0.3, 0), 25, 0.5);
    }

    private static void chain(ServerLevel l, ServerPlayer p, Entity from, LivingEntity to, Set<Integer> hitIds, int jump, float dmg) {
        hitIds.add(to.getId());
        Fx.arc(l, from, to, 12, jump == 0 ? 1.5F : 1.1F);
        hit(to, dmg, p, p);
        to.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
        sparks(l, to.position().add(0, to.getBbHeight() / 2, 0), 12, 0.35);
        play(l, to.position(), ModRegistry.ZAP.get(), 0.9F, 0.9F + jump * 0.08F);
        if (jump >= 7) return;
        LivingEntity next = enemiesAround(p, to.position(), 9, true).stream().filter(e -> !hitIds.contains(e.getId()) && e != to)
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(to))).orElse(null);
        if (next != null) schedule(l, 3, lv -> chain(lv, p, to, next, hitIds, jump + 1, dmg * 0.85F));
    }

    private static void nova(ServerLevel l, ServerPlayer p) {
        if (!p.isAlive()) return;
        Vec3 c = p.position();
        p.fallDistance = 0;
        Fx.at(l, Fx.Type.NOVA, c.add(0, 0.2, 0), 24, 12F);
        Fx.at(l, Fx.Type.BURST, c.add(0, 1, 0), 16, 6F);
        play(l, c, ModRegistry.NOVA.get(), 3F, 1F);
        play(l, c, SoundEvents.LIGHTNING_BOLT_THUNDER, 2F, 1.2F);
        int bolts = 0;
        for (LivingEntity e : enemiesAround(p, c, 12, true)) {
            double dist = e.distanceTo(p);
            hit(e, (float) (16 * (1 - dist / 24)), p, p);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 1));
            push(e, c, 1.6);
            if (bolts++ < 8) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(l);
                if (bolt != null) {
                    bolt.moveTo(e.getX(), e.getY(), e.getZ());
                    bolt.setVisualOnly(true);
                    l.addFreshEntity(bolt);
                }
            }
        }
        sparks(l, c.add(0, 0.5, 0), 80, 1.4);
        l.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
    }

    private static void spear(ServerLevel l, ServerPlayer p) {
        if (!p.isAlive()) return;
        Vec3 start = hand(p);
        Vec3 look = p.getLookAngle();
        Vec3 end = p.getEyePosition().add(look.scale(64));
        BlockHitResult bh = l.clip(new ClipContext(p.getEyePosition(), end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        Fx.send(l, Fx.Type.BEAM, start, end, p.getId(), -1, 16, 1F);
        Fx.at(l, Fx.Type.BURST, end, 12, 3F);
        play(l, start, ModRegistry.SPEAR.get(), 2F, 1F);
        play(l, end, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.5F, 1.2F);
        AABB box = new AABB(start, end).inflate(1.5);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, box, e -> canHit(p, e, false))) {
            if (e.getBoundingBox().inflate(0.9).clip(start, end).isPresent()) {
                hit(e, 20F, p, p);
                e.setSecondsOnFire(3);
                sparks(l, e.position().add(0, e.getBbHeight() / 2, 0), 15, 0.4);
            }
        }
        sparks(l, end, 30, 0.6);
        p.push(-look.x * 0.6, 0.1, -look.z * 0.6);
        p.hurtMarked = true;
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        StormData d = StormCapability.get(p);
        ServerLevel l = p.serverLevel();
        d.energy = Math.min(d.maxEnergy(), d.energy + d.regenPerTick());
        int step = d.avatarTicks > 0 && p.tickCount % 2 == 0 ? 2 : 1;
        for (int i = 0; i < d.cooldowns.length; i++) if (d.cooldowns[i] > 0) d.cooldowns[i] = Math.max(0, d.cooldowns[i] - step);

        if (d.domeTicks > 0) {
            d.domeTicks--;
            for (Projectile pr : l.getEntitiesOfClass(Projectile.class, p.getBoundingBox().inflate(3.2), pr -> pr.getOwner() != p)) {
                Vec3 v = pr.getDeltaMovement();
                Vec3 toPlayer = p.position().add(0, 1, 0).subtract(pr.position());
                if (v.dot(toPlayer) > 0) {
                    pr.setDeltaMovement(v.scale(-1.1));
                    pr.setOwner(p);
                    pr.hurtMarked = true;
                    sparks(l, pr.position(), 10, 0.3);
                    play(l, pr.position(), ModRegistry.ZAP.get(), 0.7F, 1.6F);
                }
            }
        }
        if (d.ringTicks > 0) {
            d.ringTicks--;
            if (d.ringTicks % 10 == 0) {
                int n = 0;
                for (LivingEntity t : enemiesAround(p, p.position(), 4.6, true)) {
                    if (n++ >= 4) break;
                    double ang = Math.atan2(t.getZ() - p.getZ(), t.getX() - p.getX());
                    Vec3 from = p.position().add(Math.cos(ang) * 3, 1.0, Math.sin(ang) * 3);
                    hit(t, 3F, p, p);
                    Fx.arc(l, from, t.position().add(0, t.getBbHeight() / 2, 0), 6);
                    sparks(l, t.position().add(0, t.getBbHeight() / 2, 0), 6, 0.25);
                }
                if (n > 0) play(l, p.position(), ModRegistry.ZAP.get(), 0.6F, 1.4F);
            }
        }
        if (d.avatarTicks > 0) {
            d.avatarTicks--;
            if (p.tickCount % 3 == 0) sparks(l, p.position().add(0, 1, 0), 2, 0.4);
        }
        if (p.tickCount % 5 == 0) Net.sync(p);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!TASKS.isEmpty()) {
            List<Task> run = new ArrayList<>();
            for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
                Task t = it.next();
                if (t.level.getGameTime() >= t.due) { run.add(t); it.remove(); }
            }
            for (Task t : run) t.run.accept(t.level);
        }
        for (Iterator<Grasp> it = GRASPS.iterator(); it.hasNext(); ) {
            Grasp g = it.next();
            LivingEntity t = g.target;
            if (!t.isAlive() || !g.caster.isAlive() || t.level() != g.caster.level() || g.age++ > GRASP_TOTAL) { it.remove(); continue; }
            boolean boss = t.getType().is(Tags.EntityTypes.BOSSES);
            if (g.age <= GRASP_PULL && !boss) {
                Vec3 dest = g.caster.getEyePosition().add(g.caster.getLookAngle().scale(2.6)).add(0, -t.getBbHeight() / 2, 0);
                t.setDeltaMovement(dest.subtract(t.position()).scale(0.32));
                t.hurtMarked = true;
                t.fallDistance = 0;
            } else {
                t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y) * 0.5, 0);
                t.hurtMarked = true;
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, boss ? 2 : 9, false, false));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 2, false, false));
                if (t instanceof Mob m) m.getNavigation().stop();
            }
            if (g.age % 10 == 0) {
                hit(t, 2F, g.caster, g.caster);
                ServerLevel l = (ServerLevel) t.level();
                sparks(l, t.position().add(0, t.getBbHeight() / 2, 0), 6, 0.3);
            }
        }
    }

    public static void schedule(ServerLevel l, int delay, Consumer<ServerLevel> r) { TASKS.add(new Task(l, l.getGameTime() + delay, r)); }

    // ------------------------------------------------------------------ passives & defence

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        ServerLevel l = (ServerLevel) victim.level();
        // dome: damage reduction + shock melee attackers
        if (victim instanceof ServerPlayer vp) {
            StormData vd = StormCapability.get(vp);
            if (vd.domeTicks > 0) {
                e.setAmount(e.getAmount() * 0.4F);
                if (e.getSource().getDirectEntity() instanceof LivingEntity att && att != vp && att.distanceTo(vp) < 6) {
                    schedule(l, 1, lv -> {
                        hit(att, 4F, vp, vp);
                        push(att, vp.position(), 1.2);
                        Fx.arc(lv, vp, att, 6, 1F);
                    });
                }
            }
        }
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        StormData d = StormCapability.get(p);
        boolean melee = e.getSource().is(DamageTypes.PLAYER_ATTACK) && e.getSource().getDirectEntity() == p;
        if (melee) {
            if (d.has(Skill.STATIC_CORE) && p.getRandom().nextFloat() < (d.avatarTicks > 0 ? 0.6F : 0.3F)) {
                schedule(l, 1, lv -> {
                    if (!victim.isAlive()) return;
                    hit(victim, 3F, p, p);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    Fx.arc(lv, p, victim, 6, 0.8F);
                    play(lv, victim.position(), ModRegistry.ZAP.get(), 0.7F, 1.3F);
                });
            }
            if (d.avatarTicks > 0) {
                schedule(l, 2, lv -> {
                    int n = 0;
                    for (LivingEntity o : enemiesAround(p, victim.position(), 6, true)) {
                        if (o == victim) continue;
                        if (n++ >= 2) break;
                        hit(o, 4F, p, p);
                        Fx.arc(lv, victim, o, 8, 1F);
                    }
                });
            }
        }
        // reaper execute
        if (e.getSource().is(ModRegistry.STORM_DAMAGE) && d.has(Skill.STORM_REAPER) && !victim.getType().is(Tags.EntityTypes.BOSSES)
                && !(victim instanceof Player)) {
            float after = victim.getHealth() - e.getAmount();
            if (after > 0 && after < victim.getMaxHealth() * 0.2F) {
                e.setAmount(victim.getHealth() + 50F);
                Fx.at(l, Fx.Type.SKULL, victim.position().add(0, victim.getBbHeight() + 0.3, 0), 30, 1F);
                play(l, victim.position(), ModRegistry.REAPER.get(), 1.2F, 1F);
                l.sendParticles(ParticleTypes.SOUL, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 12, 0.3, 0.4, 0.3, 0.04);
            }
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || !e.getSource().is(ModRegistry.STORM_DAMAGE)) return;
        StormData d = StormCapability.get(p);
        if (!d.has(Skill.STORM_REAPER)) return;
        d.energy = Math.min(d.maxEnergy(), d.energy + 10F);
        for (int i = 0; i < d.cooldowns.length; i++) d.cooldowns[i] = Math.max(0, d.cooldowns[i] - 20);
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getSource().is(DamageTypes.LIGHTNING_BOLT)) {
            StormData d = StormCapability.get(p);
            if (d.avatarTicks > 0 || d.has(Skill.STATIC_CORE)) e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onStruck(EntityStruckByLightningEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && StormCapability.get(p).has(Skill.STATIC_CORE)) {
            StormData d = StormCapability.get(p);
            d.energy = d.maxEnergy();
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }

    @SubscribeEvent
    public static void onDim(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }

    // ------------------------------------------------------------------ helpers

    /** damage dealt by every skill: credited to the caster, ignores the hurt cooldown */
    public static void hit(LivingEntity target, float amount, Entity direct, ServerPlayer caster) {
        if (!target.isAlive() || amount <= 0) return;
        target.invulnerableTime = 0;
        target.hurt(ModRegistry.storm(direct, caster), amount);
    }

    public static boolean canHit(ServerPlayer caster, Entity e, boolean strict) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == caster || e instanceof ArmorStand || le.isSpectator()) return false;
        if (e instanceof OwnableEntity o && o.getOwnerUUID() != null && o.getOwnerUUID().equals(caster.getUUID())) return false;
        if (caster.isAlliedTo(e)) return false;
        if (e instanceof Player pl) return caster.server.isPvpAllowed() && !pl.isCreative() && caster.canHarmPlayer(pl);
        if (!strict) return true;
        return e instanceof Enemy || (e instanceof Mob m && m.getTarget() == caster);
    }

    public static List<LivingEntity> enemiesAround(ServerPlayer p, Vec3 c, double r, boolean strict) {
        List<LivingEntity> list = p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> canHit(p, e, strict) && e.position().distanceTo(c) <= r + e.getBbWidth());
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(c)));
        return list;
    }

    /** entity under the crosshair, or the closest enemy within a narrow cone */
    public static LivingEntity aimEntity(ServerPlayer p, double range, boolean strictAssist) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        EntityHitResult eh = ProjectileUtil.getEntityHitResult(p, eye, end, p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5),
                e -> canHit(p, e, false), eye.distanceToSqr(end));
        if (eh != null && eh.getEntity() instanceof LivingEntity le) return le;
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : enemiesAround(p, eye, range, strictAssist)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            double cos = to.normalize().dot(look);
            if (cos < 0.96 || !p.hasLineOfSight(e)) continue;
            double score = (1 - cos) * 100 + to.length() * 0.05;
            if (score < bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    /** aimed block/entity position, else a point on the ground in front */
    public static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity e = aimEntity(p, range, true);
        if (e != null) return e.position();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 hit = bh.getType() == HitResult.Type.MISS ? end : bh.getLocation();
        return ground(p.serverLevel(), hit.add(0, 0.5, 0));
    }

    /** first solid surface below pos (searching 24 blocks down) */
    public static Vec3 ground(ServerLevel l, Vec3 pos) {
        Vec3 top = pos.add(0, 3, 0);
        BlockHitResult bh = l.clip(new ClipContext(top, top.add(0, -27, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return bh.getType() == HitResult.Type.MISS ? pos : bh.getLocation();
    }

    public static Vec3 hand(Player p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        Vec3 side = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw)).scale(0.38);
        return p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(side).add(0, -0.35, 0);
    }

    public static void push(LivingEntity e, Vec3 from, double strength) {
        Vec3 d = e.position().subtract(from);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        e.knockback(strength, -d.x, -d.z);
        e.hurtMarked = true;
    }

    public static void sparks(ServerLevel l, Vec3 c, int n, double spread) {
        l.sendParticles(ModRegistry.SPARK.get(), c.x, c.y, c.z, n, spread, spread, spread, 0.15);
        l.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y, c.z, n / 2 + 1, spread, spread, spread, 0.3);
    }

    public static void play(ServerLevel l, Vec3 pos, SoundEvent s, float vol, float pitch) {
        l.playSound(null, BlockPos.containing(pos), s, SoundSource.PLAYERS, vol, pitch);
    }
}
