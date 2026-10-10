package com.krolasyon.voidtree;

import com.krolasyon.voidtree.entity.RiftOrbEntity;
import com.krolasyon.voidtree.entity.ShadowCloneEntity;
import com.krolasyon.voidtree.net.Net;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** All server side gameplay of the void tree: unlocking, casting, passives, timed zones. */
@Mod.EventBusSubscriber(modid = VoidTree.MODID)
public final class SkillLogic {
    private record Task(ServerLevel level, long due, Consumer<ServerLevel> run) {}

    private enum ZoneType { VORTEX, TWISTER, HOLE, SEAL, COSMIC }

    private static final class Zone {
        final ZoneType type;
        final ServerPlayer owner;
        final ServerLevel level;
        final Vec3 start, end;
        final int duration;
        final double radius;
        int age;

        Zone(ZoneType type, ServerPlayer owner, Vec3 start, Vec3 end, int duration, double radius) {
            this.type = type;
            this.owner = owner;
            this.level = owner.serverLevel();
            this.start = start;
            this.end = end;
            this.duration = duration;
            this.radius = radius;
        }

        Vec3 pos() { return start.add(end.subtract(start).scale(Math.min(1.0, (double) age / duration))); }
    }

    private static final class Prison {
        final ServerPlayer caster;
        final LivingEntity target;
        final Vec3 at;
        int age;

        Prison(ServerPlayer caster, LivingEntity target) {
            this.caster = caster;
            this.target = target;
            this.at = target.position().add(0, target.getType().is(Tags.EntityTypes.BOSSES) ? 0 : 0.6, 0);
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Zone> ZONES = new ArrayList<>();
    private static final List<Prison> PRISONS = new ArrayList<>();
    public static final int PRISON_TICKS = 80;

    private SkillLogic() {}

    // ------------------------------------------------------------------ unlocking / slots

    public static void unlock(ServerPlayer p, int id) {
        Skill s = Skill.byId(id);
        VoidData d = VoidCapability.get(p);
        if (s == null || d.has(s)) return;
        if (!s.requirementsMet(d.unlocked)) {
            p.displayClientMessage(Component.translatable("msg.voidtree.locked"), true);
            return;
        }
        if (!p.getAbilities().instabuild) {
            if (p.experienceLevel < s.levelCost) {
                p.displayClientMessage(Component.translatable("msg.voidtree.no_xp", s.levelCost), true);
                return;
            }
            p.giveExperienceLevels(-s.levelCost);
        }
        d.unlocked.add(s);
        if (!s.passive) {
            for (int i = 0; i < VoidData.SLOTS; i++) {
                if (d.slots[i] == null) { d.slots[i] = s; break; }
            }
        }
        ServerLevel l = p.serverLevel();
        play(l, p.position(), ModRegistry.UNLOCK.get(), 1F, 1F);
        Fx.at(l, Fx.Type.WISPBURST, p.position().add(0, 1, 0), 16, 2.5F);
        wisps(l, p.position().add(0, 1, 0), 30, 0.6);
        Net.sync(p);
    }

    public static void assign(ServerPlayer p, int slot, int id) {
        if (slot < 0 || slot >= VoidData.SLOTS) return;
        VoidData d = VoidCapability.get(p);
        Skill s = Skill.byId(id);
        if (s != null && (!d.has(s) || s.passive)) return;
        for (int i = 0; i < VoidData.SLOTS; i++) if (s != null && d.slots[i] == s) d.slots[i] = d.slots[slot];
        d.slots[slot] = s;
        Net.sync(p);
    }

    // ------------------------------------------------------------------ casting

    public static void castSlot(ServerPlayer p, int slot) {
        if (slot < 0 || slot >= VoidData.SLOTS) return;
        Skill s = VoidCapability.get(p).slots[slot];
        if (s != null) cast(p, s, false);
    }

    public static float energyCost(Player p, Skill s) {
        float c = s.energy;
        if (GrimoireItem.carries(p)) c *= 0.8F;
        if (s != Skill.ASCENSION && p.getCapability(VoidCapability.CAP).map(d -> d.ascendTicks > 0).orElse(false)) c *= 0.5F;
        return c;
    }

    /** @param force ignore cooldown/energy/unlock (commands and the CI self test) */
    public static boolean cast(ServerPlayer p, Skill s, boolean force) {
        VoidData d = VoidCapability.get(p);
        if (s.passive || p.isSpectator() || !p.isAlive()) return false;
        float cost = energyCost(p, s);
        if (!force) {
            if (!d.has(s)) return false;
            if (d.cooldowns[s.ordinal()] > 0) return false;
            if (d.energy < cost) {
                p.displayClientMessage(Component.translatable("msg.voidtree.no_energy"), true);
                p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 0.6F);
                return false;
            }
        }
        if (!execute(p, s, d)) return false;
        if (!force) d.energy -= cost;
        if (!force || d.has(s)) d.cooldowns[s.ordinal()] = s.cooldown;
        Net.anim(p, s.ordinal());
        Net.sync(p);
        return true;
    }

    private static boolean execute(ServerPlayer p, Skill s, VoidData d) {
        ServerLevel l = p.serverLevel();
        Vec3 look = p.getLookAngle();
        switch (s) {
            case SHADOW_DASH -> dash(l, p, look);
            case PHANTOM_FORM -> {
                d.phantomTicks = 120;
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, false, false, true));
                for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m.getTarget() == p)) m.setTarget(null);
                Fx.follow(l, Fx.Type.PHANTOM, p, 120, 1F);
                Fx.at(l, Fx.Type.WISPBURST, p.position().add(0, 1, 0), 12, 2F);
                play(l, p.position(), ModRegistry.PHANTOM.get(), 1F, 1F);
                wisps(l, p.position().add(0, 1, 0), 30, 0.5);
            }
            case VOID_VORTEX -> {
                Vec3 c = aimPoint(p, 30);
                ZONES.add(new Zone(ZoneType.VORTEX, p, c, c, 100, 7));
                Fx.at(l, Fx.Type.VORTEX, c, 100, 7F);
                play(l, c, ModRegistry.VORTEX.get(), 1.5F, 1F);
            }
            case SPIRIT_TWISTER -> {
                Vec3 flat = new Vec3(look.x, 0, look.z);
                if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
                flat = flat.normalize();
                Vec3 a = ground(l, p.position().add(flat.scale(2)));
                Vec3 b = a.add(flat.scale(20));
                BlockHitResult bh = l.clip(new ClipContext(a.add(0, 1, 0), b.add(0, 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (bh.getType() != HitResult.Type.MISS) b = bh.getLocation().add(0, -1, 0).subtract(flat.scale(0.5));
                ZONES.add(new Zone(ZoneType.TWISTER, p, a, b, 80, 2.6));
                Fx.line(l, Fx.Type.TWISTER, a, b, 80, 2.6F);
                play(l, a, ModRegistry.VORTEX.get(), 1.4F, 1.4F);
            }
            case SHADOW_CLAWS -> {
                for (int i = 0; i < 3; i++) {
                    final int k = i;
                    schedule(l, i * 4, lv -> claw(lv, p, k));
                }
            }
            case SOUL_PRISON -> {
                LivingEntity t = aimEntity(p, 24, false);
                if (t == null) return fail(p);
                PRISONS.removeIf(pr -> pr.target == t);
                PRISONS.add(new Prison(p, t));
                hit(t, 3F, p, p);
                Fx.send(l, Fx.Type.PRISON, p.position(), t.position(), p.getId(), t.getId(), PRISON_TICKS, 1F);
                Fx.send(l, Fx.Type.TETHER, p.position(), t.position(), p.getId(), t.getId(), 14, 1F);
                play(l, t.position(), ModRegistry.PRISON.get(), 1.2F, 1F);
            }
            case SHADOW_RAIN -> {
                Vec3 c = aimPoint(p, 36);
                Vec3 fall = new Vec3(look.x, 0, look.z);
                fall = (fall.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : fall.normalize()).scale(5);
                Fx.at(l, Fx.Type.SEAL, c.add(0, 0.05, 0), 50, 5.5F);
                for (int i = 0; i < 16; i++) {
                    final Vec3 dir = fall;
                    schedule(l, 2 + i * 3, lv -> {
                        Vec3 at = null;
                        if (lv.random.nextBoolean()) {
                            List<LivingEntity> en = enemiesAround(p, c, 6, true);
                            if (!en.isEmpty()) at = en.get(lv.random.nextInt(en.size())).position();
                        }
                        if (at == null) at = ground(lv, c.add((lv.random.nextDouble() - 0.5) * 10, 0, (lv.random.nextDouble() - 0.5) * 10));
                        Vec3 hitPos = at;
                        Fx.line(lv, Fx.Type.BLADE, hitPos.subtract(dir).add(0, 11, 0), hitPos, 9, 1F);
                        schedule(lv, 5, l2 -> {
                            for (LivingEntity e : enemiesAround(p, hitPos, 1.8, true)) {
                                hit(e, 5F, p, p);
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                            }
                            Fx.at(l2, Fx.Type.WISPBURST, hitPos.add(0, 0.2, 0), 10, 1.6F);
                            wisps(l2, hitPos.add(0, 0.3, 0), 6, 0.3);
                            play(l2, hitPos, ModRegistry.BLADE.get(), 0.8F, 0.8F + l2.random.nextFloat() * 0.4F);
                        });
                    });
                }
            }
            case BLACK_HOLE -> {
                Vec3 c = aimPoint(p, 32).add(0, 1.6, 0);
                ZONES.add(new Zone(ZoneType.HOLE, p, c, c, 120, 10));
                Fx.at(l, Fx.Type.HOLE, c, 120, 10F);
                play(l, c, ModRegistry.HOLE.get(), 2F, 1F);
            }
            case RIFT_WALK -> {
                RiftOrbEntity orb = new RiftOrbEntity(l, p);
                Vec3 start = p.getEyePosition().add(look.scale(0.8)).add(0, -0.2, 0);
                orb.setPos(start.x, start.y, start.z);
                orb.shoot(look.x, look.y, look.z, 1.8F, 0F);
                l.addFreshEntity(orb);
                Fx.at(l, Fx.Type.RIFT, start, 16, 0.7F);
                play(l, start, ModRegistry.RIFT.get(), 1F, 1.3F);
            }
            case ASCENSION -> {
                d.ascendTicks = 300;
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 1, false, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 300, 0, false, false, true));
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.6, 0));
                p.hurtMarked = true;
                Fx.follow(l, Fx.Type.ASCEND, p, 300, 1F);
                Fx.at(l, Fx.Type.WISPBURST, p.position().add(0, 1, 0), 16, 4F);
                Fx.at(l, Fx.Type.SEAL, p.position().add(0, 0.05, 0), 30, 3F);
                play(l, p.position(), ModRegistry.ASCEND.get(), 1.5F, 1F);
            }
            case VOID_SEAL -> {
                Vec3 c = p.position().add(0, 0.05, 0);
                ZONES.add(new Zone(ZoneType.SEAL, p, c, c, 160, 8));
                Fx.at(l, Fx.Type.SEAL, c, 160, 8F);
                play(l, c, ModRegistry.SEAL.get(), 1.6F, 1F);
            }
            case SHADOW_CLONES -> {
                ShadowCloneEntity.discardOwnedBy(l, p);
                for (int i = 0; i < 3; i++) {
                    double ang = Math.toRadians(p.getYRot() + 90 + i * 120);
                    Vec3 at = ground(l, p.position().add(Math.cos(ang) * 2.2, 0, Math.sin(ang) * 2.2));
                    ShadowCloneEntity c = new ShadowCloneEntity(l, p);
                    c.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
                    l.addFreshEntity(c);
                    Fx.at(l, Fx.Type.RIFT, at.add(0, 1, 0), 18, 1.1F);
                    Fx.at(l, Fx.Type.WISPBURST, at.add(0, 1, 0), 14, 2F);
                    wisps(l, at.add(0, 1, 0), 20, 0.5);
                }
                play(l, p.position(), ModRegistry.SUMMON.get(), 1.5F, 1F);
            }
            case COSMIC_COLLAPSE -> {
                Vec3 c = aimPoint(p, 40);
                ZONES.add(new Zone(ZoneType.COSMIC, p, c, c, 70, 12));
                Fx.at(l, Fx.Type.COSMIC, c, 70, 12F);
                Fx.follow(l, Fx.Type.CHARGE, p, 20, 1.4F);
                play(l, c, ModRegistry.COSMIC.get(), 3F, 1F);
            }
            default -> { return false; }
        }
        return true;
    }

    private static boolean fail(ServerPlayer p) {
        p.displayClientMessage(Component.translatable("msg.voidtree.no_target"), true);
        return false;
    }

    // ------------------------------------------------------------------ skill pieces

    private static void dash(ServerLevel l, ServerPlayer p, Vec3 look) {
        Vec3 start = p.position();
        Vec3 dir = look.y < -0.3 ? look : new Vec3(look.x, Math.max(look.y, -0.3), look.z).normalize();
        Vec3 dest = start;
        for (double dist = 9; dist >= 0.5; dist -= 0.5) {
            Vec3 c = start.add(dir.scale(dist));
            BlockHitResult bh = l.clip(new ClipContext(start.add(0, 0.6, 0), c.add(0, 0.6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (bh.getType() != HitResult.Type.MISS) continue;
            if (l.noCollision(p, p.getBoundingBox().move(c.subtract(start)))) { dest = c; break; }
        }
        Vec3 mid0 = start.add(0, 1, 0), mid1 = dest.add(0, 1, 0);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(mid0, mid1).inflate(1.5), e -> canHit(p, e, false))) {
            if (e.getBoundingBox().inflate(1.0).clip(mid0, mid1).isPresent()) {
                hit(e, 5F, p, p);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                wisps(l, e.position().add(0, e.getBbHeight() / 2, 0), 8, 0.3);
            }
        }
        Fx.line(l, Fx.Type.TRAIL, start, dest, 18, 1F);
        wisps(l, start.add(0, 1, 0), 16, 0.4);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.setDeltaMovement(dir.scale(0.4));
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.invulnerableTime = 15;
        wisps(l, dest.add(0, 1, 0), 16, 0.4);
        play(l, dest, ModRegistry.WHOOSH.get(), 1F, 1F);
    }

    private static void claw(ServerLevel l, ServerPlayer p, int k) {
        if (!p.isAlive()) return;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        float dmg = k == 2 ? 8F : 5F;
        Fx.send(l, Fx.Type.SLASH, eye, eye.add(look), p.getId(), -1, 7, k == 2 ? 2F : (k == 0 ? 1F : -1F));
        play(l, p.position(), ModRegistry.SLASH.get(), 1F, 0.9F + k * 0.15F);
        for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(5), e -> canHit(p, e, false))) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > 5 || to.normalize().dot(look) < 0.45) continue;
            hit(e, dmg, p, p);
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            if (k == 2) push(e, p.position(), 0.9);
            wisps(l, e.position().add(0, e.getBbHeight() / 2, 0), 6, 0.3);
        }
    }

    // ------------------------------------------------------------------ ticking

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        VoidData d = VoidCapability.get(p);
        ServerLevel l = p.serverLevel();
        float regen = d.regenPerTick();
        if (d.has(Skill.VOID_MANTLE) && dark(p)) regen *= 1.5F;
        d.energy = Math.min(d.maxEnergy(), d.energy + regen);
        for (int i = 0; i < d.cooldowns.length; i++) if (d.cooldowns[i] > 0) d.cooldowns[i]--;
        if (d.phantomTicks > 0) {
            d.phantomTicks--;
            if (p.tickCount % 4 == 0) l.sendParticles(ModRegistry.WISP.get(), p.getX(), p.getY() + 0.3, p.getZ(), 1, 0.2, 0.1, 0.2, 0.01);
            if (d.phantomTicks == 0) p.removeEffect(MobEffects.INVISIBILITY);
        }
        if (d.ascendTicks > 0) {
            d.ascendTicks--;
            if (!p.getAbilities().mayfly) {
                p.getAbilities().mayfly = true;
                p.onUpdateAbilities();
            }
            if (d.ascendTicks == 0) endAscension(p, d);
        }
        if (p.tickCount % 5 == 0) Net.sync(p);
    }

    public static void endAscension(ServerPlayer p, VoidData d) {
        if (d.ascendTicks <= 0 && !p.getAbilities().mayfly) return;
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false, true));
        }
        p.fallDistance = 0;
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
        List<Zone> ended = new ArrayList<>();
        for (Iterator<Zone> it = ZONES.iterator(); it.hasNext(); ) {
            Zone z = it.next();
            if (!z.owner.isAlive() || z.owner.level() != z.level) { it.remove(); continue; }
            tickZone(z);
            if (++z.age >= z.duration) { it.remove(); ended.add(z); }
        }
        for (Zone z : ended) endZone(z);
        for (Iterator<Prison> it = PRISONS.iterator(); it.hasNext(); ) {
            Prison pr = it.next();
            LivingEntity t = pr.target;
            if (!t.isAlive() || !pr.caster.isAlive() || t.level() != pr.caster.level() || pr.age++ > PRISON_TICKS) { it.remove(); continue; }
            boolean boss = t.getType().is(Tags.EntityTypes.BOSSES);
            if (!boss) {
                t.teleportTo(pr.at.x, pr.at.y, pr.at.z);
                t.setDeltaMovement(Vec3.ZERO);
                t.fallDistance = 0;
                if (t instanceof Mob m) { m.getNavigation().stop(); m.setTarget(null); }
            }
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, boss ? 2 : 9, false, false));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 5, 3, false, false));
            if (pr.age % 20 == 0) {
                hit(t, 3F, pr.caster, pr.caster);
                wisps((ServerLevel) t.level(), t.position().add(0, t.getBbHeight() / 2, 0), 6, 0.3);
            }
        }
    }

    private static void tickZone(Zone z) {
        ServerLevel l = z.level;
        Vec3 c = z.pos();
        switch (z.type) {
            case VORTEX -> {
                for (LivingEntity e : enemiesAround(z.owner, c, z.radius, true)) {
                    pull(e, c, 0.11, 0.25);
                    if (z.age % 10 == 0) hit(e, 2F, z.owner, z.owner);
                }
                if (z.age % 3 == 0) l.sendParticles(ModRegistry.WISP.get(), c.x, c.y + 0.3, c.z, 3, z.radius * 0.5, 0.1, z.radius * 0.5, 0.02);
            }
            case TWISTER -> {
                for (LivingEntity e : enemiesAround(z.owner, c, z.radius, true)) {
                    Vec3 r = e.position().subtract(c);
                    Vec3 tangent = new Vec3(-r.z, 0, r.x).normalize().scale(0.35);
                    Vec3 in = c.subtract(e.position()).multiply(1, 0, 1).scale(0.15);
                    e.setDeltaMovement(tangent.add(in).add(0, e.getY() - c.y < 3.5 ? 0.32 : 0.02, 0));
                    e.hurtMarked = true;
                    e.fallDistance = 0;
                    if (z.age % 10 == 0) hit(e, 3F, z.owner, z.owner);
                }
                if (z.age % 2 == 0) l.sendParticles(ModRegistry.WISP.get(), c.x, c.y + 1.5, c.z, 3, 0.8, 1.2, 0.8, 0.05);
                if (z.age % 20 == 0) play(l, c, ModRegistry.VORTEX.get(), 0.7F, 1.6F);
            }
            case HOLE -> {
                for (LivingEntity e : enemiesAround(z.owner, c, z.radius, true)) {
                    pull(e, c, 0.2, 0.5);
                    if (z.age % 10 == 0 && e.distanceToSqr(c) < 3.2 * 3.2) hit(e, 3F, z.owner, z.owner);
                }
                for (ItemEntity it : l.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(z.radius))) pull(it, c, 0.08, 0.3);
                for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(z.radius * 0.6), pr -> pr.getOwner() != z.owner)) {
                    pr.setDeltaMovement(pr.getDeltaMovement().scale(0.6).add(c.subtract(pr.position()).normalize().scale(0.25)));
                    pr.hurtMarked = true;
                }
                if (z.age % 30 == 0) play(l, c, ModRegistry.HOLE.get(), 1F, 0.8F);
            }
            case SEAL -> {
                for (LivingEntity e : enemiesAround(z.owner, c, z.radius, true)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 2, false, false));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 1, false, false));
                    if (z.age % 20 == 0) {
                        hit(e, 2F, z.owner, z.owner);
                        Fx.at(l, Fx.Type.MARK, e.position().add(0, e.getBbHeight() + 0.3, 0), 10, 1F);
                    }
                }
                for (Projectile pr : l.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(z.radius, 4, z.radius),
                        pr -> !(pr.getOwner() instanceof Player) && pr.position().distanceTo(c) < z.radius)) {
                    wisps(l, pr.position(), 6, 0.2);
                    pr.discard();
                }
            }
            case COSMIC -> {
                if (z.age % 6 == 3 && z.age < z.duration - 10) {
                    Vec3 at;
                    List<LivingEntity> en = enemiesAround(z.owner, c, z.radius, true);
                    if (!en.isEmpty() && l.random.nextBoolean()) at = en.get(l.random.nextInt(en.size())).position();
                    else at = ground(l, c.add((l.random.nextDouble() - 0.5) * z.radius * 1.6, 0, (l.random.nextDouble() - 0.5) * z.radius * 1.6));
                    Vec3 hitPos = at;
                    Fx.line(l, Fx.Type.BLADE, hitPos.add(3, 14, 2), hitPos, 8, 2F);
                    schedule(l, 4, lv -> {
                        for (LivingEntity e : enemiesAround(z.owner, hitPos, 2.5, true)) hit(e, 6F, z.owner, z.owner);
                        Fx.at(lv, Fx.Type.WISPBURST, hitPos.add(0, 0.2, 0), 12, 2.5F);
                        Fx.at(lv, Fx.Type.MARK, hitPos.add(0, 0.6, 0), 10, 2F);
                        play(lv, hitPos, ModRegistry.BLADE.get(), 1.2F, 0.6F);
                    });
                }
                for (LivingEntity e : enemiesAround(z.owner, c, z.radius, true)) e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1, false, false));
            }
        }
    }

    private static void endZone(Zone z) {
        ServerLevel l = z.level;
        Vec3 c = z.pos();
        switch (z.type) {
            case HOLE -> {
                for (LivingEntity e : enemiesAround(z.owner, c, 6, true)) {
                    hit(e, 15F, z.owner, z.owner);
                    push(e, c, 1.4);
                }
                Fx.at(l, Fx.Type.IMPLODE, c, 16, 7F);
                l.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                play(l, c, ModRegistry.IMPLODE.get(), 2.5F, 1F);
            }
            case COSMIC -> {
                for (LivingEntity e : enemiesAround(z.owner, c, z.radius, true)) {
                    hit(e, 18F, z.owner, z.owner);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                    e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 12, 2));
                }
                Fx.at(l, Fx.Type.IMPLODE, c.add(0, 1, 0), 22, 12F);
                l.sendParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
                play(l, c, ModRegistry.IMPLODE.get(), 3F, 0.7F);
            }
            default -> {}
        }
    }

    public static void schedule(ServerLevel l, int delay, Consumer<ServerLevel> r) { TASKS.add(new Task(l, l.getGameTime() + delay, r)); }

    // ------------------------------------------------------------------ passives & events

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p)) return;
        VoidData d = VoidCapability.get(p);
        boolean melee = e.getSource().is(DamageTypes.PLAYER_ATTACK) && e.getSource().getDirectEntity() == p;
        if (d.has(Skill.VOID_MANTLE) && dark(p)) e.setAmount(e.getAmount() * 1.2F);
        if (melee && d.phantomTicks > 0) {
            // ambush from the shadows
            e.setAmount(e.getAmount() * 1.5F + 6F);
            d.phantomTicks = 0;
            p.removeEffect(MobEffects.INVISIBILITY);
            ServerLevel l = (ServerLevel) victim.level();
            Fx.at(l, Fx.Type.WISPBURST, victim.position().add(0, victim.getBbHeight() / 2, 0), 12, 2F);
            Fx.at(l, Fx.Type.MARK, victim.position().add(0, victim.getBbHeight() + 0.2, 0), 14, 1.5F);
            play(l, victim.position(), ModRegistry.SLASH.get(), 1.2F, 0.7F);
            Net.sync(p);
        }
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        VoidData d = VoidCapability.get(p);
        if (d.has(Skill.VOID_MANTLE) && e.getSource().getDirectEntity() instanceof LivingEntity && e.getSource().getDirectEntity() != p
                && p.getRandom().nextFloat() < 0.15F) {
            e.setCanceled(true);
            ServerLevel l = p.serverLevel();
            wisps(l, p.position().add(0, 1, 0), 14, 0.4);
            Fx.follow(l, Fx.Type.PHANTOM, p, 10, 0.5F);
            play(l, p.position(), ModRegistry.EVADE.get(), 0.8F, 1.2F);
        }
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent e) {
        if (e.getNewTarget() instanceof ServerPlayer p && VoidCapability.get(p).phantomTicks > 0) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEnderTeleport(EntityTeleportEvent.EnderEntity e) {
        for (Zone z : ZONES) {
            if (z.type == ZoneType.SEAL && z.level == e.getEntity().level() && e.getEntity().position().distanceTo(z.pos()) < z.radius) {
                e.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            if (!p.isCreative() && !p.isSpectator() && p.getAbilities().mayfly) {
                p.getAbilities().mayfly = false;
                p.onUpdateAbilities();
            }
            Net.sync(p);
        }
    }

    @SubscribeEvent
    public static void onDim(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) Net.sync(p); }

    // ------------------------------------------------------------------ helpers

    public static boolean dark(Player p) { return p.level().getMaxLocalRawBrightness(p.blockPosition()) < 8; }

    /** damage dealt by every skill: credited to the caster, ignores the hurt cooldown */
    public static void hit(LivingEntity target, float amount, Entity direct, ServerPlayer caster) {
        if (!target.isAlive() || amount <= 0) return;
        target.invulnerableTime = 0;
        target.hurt(ModRegistry.voidDamage(direct, caster), amount);
    }

    public static boolean canHit(ServerPlayer caster, Entity e, boolean strict) {
        if (!(e instanceof LivingEntity le) || !le.isAlive() || e == caster || e instanceof ArmorStand || le.isSpectator()) return false;
        if (e instanceof ShadowCloneEntity) return false;
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

    public static Vec3 aimPoint(ServerPlayer p, double range) {
        LivingEntity e = aimEntity(p, range, true);
        if (e != null) return e.position();
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 hit = bh.getType() == HitResult.Type.MISS ? end : bh.getLocation();
        return ground(p.serverLevel(), hit.add(0, 0.5, 0));
    }

    public static Vec3 ground(ServerLevel l, Vec3 pos) {
        Vec3 top = pos.add(0, 3, 0);
        BlockHitResult bh = l.clip(new ClipContext(top, top.add(0, -27, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return bh.getType() == HitResult.Type.MISS ? pos : bh.getLocation();
    }

    /** accelerate e toward c; strength grows near the edge so things spiral in */
    private static void pull(Entity e, Vec3 c, double strength, double swirl) {
        Vec3 d = c.subtract(e.position().add(0, e.getBbHeight() * 0.4, 0));
        double len = d.length();
        if (len < 0.6) {
            e.setDeltaMovement(e.getDeltaMovement().scale(0.5));
        } else {
            Vec3 n = d.scale(1 / len);
            Vec3 tangent = new Vec3(-n.z, 0, n.x).scale(swirl * strength);
            e.setDeltaMovement(e.getDeltaMovement().scale(0.8).add(n.scale(strength)).add(tangent));
        }
        e.hurtMarked = true;
        e.fallDistance = 0;
    }

    public static void push(LivingEntity e, Vec3 from, double strength) {
        Vec3 d = e.position().subtract(from);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        e.knockback(strength, -d.x, -d.z);
        e.hurtMarked = true;
    }

    public static void wisps(ServerLevel l, Vec3 c, int n, double spread) {
        l.sendParticles(ModRegistry.WISP.get(), c.x, c.y, c.z, n, spread, spread, spread, 0.04);
        l.sendParticles(ModRegistry.STAR.get(), c.x, c.y, c.z, n / 3 + 1, spread, spread, spread, 0.08);
    }

    public static void play(ServerLevel l, Vec3 pos, SoundEvent s, float vol, float pitch) {
        l.playSound(null, BlockPos.containing(pos), s, SoundSource.PLAYERS, vol, pitch);
    }

    static float lerp(float t, float a, float b) { return Mth.lerp(t, a, b); }
}
