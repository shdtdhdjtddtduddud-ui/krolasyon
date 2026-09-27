package com.krolasyon.bosses.morph;

import com.krolasyon.bosses.item.FormBladeItem;
import com.krolasyon.bosses.network.ModNetwork;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.joml.Vector3f;

import java.util.*;

/** Server side of the transformations: state, attributes, transform sequence, shared passives and helpers.
 *  The form specific abilities live in {@link Forms#LOGIC}. */
public final class MorphServer {
    private MorphServer() {}

    private static final String TAG = "krolasyon_aigoar";

    /** transient per-player runtime state */
    public static final class State {
        public int form = -1;
        public int ability = -1;
        public int tick;
        public int transformTick = -1;
        public final int[] cd = new int[Aigoar.ABILITIES];
        public final Set<Integer> hit = new HashSet<>();
        public final List<Integer> carried = new ArrayList<>();
        public boolean doubleJumped;
        public boolean slammed;
        public int lastSwing;
        public int ambient = 200;
        public boolean attackAlt;
        /** timed form buffs (dragon scale aegis, shade awakening, flame phoenix) */
        public int buffTicks;
        public Vec3 anchor = Vec3.ZERO;
        public int targetId = -1;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static State state(Player p) { return STATES.computeIfAbsent(p.getUUID(), k -> new State()); }

    public static void forget(Player p) { STATES.remove(p.getUUID()); }

    // ------------------------------------------------------------------ persistent form
    public static int form(Player p) {
        if (p.level().isClientSide()) return Aigoar.CLIENT_FORM.getOrDefault(p.getId(), -1);
        CompoundTag t = p.getPersistentData().getCompound(TAG);
        if (t.contains("Form")) return t.getInt("Form");
        return t.getBoolean("Morphed") ? Forms.AIGOAR : -1;
    }

    public static boolean isMorphed(Player p) { return form(p) >= 0; }

    private static void setForm(Player p, int f) {
        CompoundTag t = p.getPersistentData().getCompound(TAG);
        t.putInt("Form", f);
        t.putBoolean("Morphed", f >= 0);
        p.getPersistentData().put(TAG, t);
    }

    public static void copyOnClone(Player from, Player to) {
        if (from.getPersistentData().contains(TAG)) to.getPersistentData().put(TAG, from.getPersistentData().getCompound(TAG).copy());
    }

    public static void sync(ServerPlayer p) {
        ModNetwork.toTrackingAndSelf(p, new ModNetwork.SyncMsg(p.getId(), (byte) form(p)));
        sendCooldowns(p);
    }

    public static void syncTo(ServerPlayer target, ServerPlayer viewer) {
        int f = form(target);
        if (f >= 0) ModNetwork.toPlayer(viewer, new ModNetwork.SyncMsg(target.getId(), (byte) f));
    }

    public static void anim(ServerPlayer p, byte anim, boolean includeSelf) {
        ModNetwork.AnimMsg msg = new ModNetwork.AnimMsg(p.getId(), anim);
        if (includeSelf) ModNetwork.toTrackingAndSelf(p, msg);
        else ModNetwork.toTracking(p, msg);
    }

    private static void sendCooldowns(ServerPlayer p) {
        State s = state(p);
        int f = Math.max(0, form(p));
        ModNetwork.toPlayer(p, new ModNetwork.CooldownMsg(s.cd.clone(), Forms.COOLDOWN[f].clone()));
    }

    // ------------------------------------------------------------------ attributes
    private static final UUID HP = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a01");
    private static final UUID DMG = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a02");
    private static final UUID ARMOR = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a03");
    private static final UUID TOUGH = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a04");
    private static final UUID SPEED = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a05");
    private static final UUID KNOCK = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a06");
    private static final UUID REACH = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a07");
    private static final UUID SWIM = UUID.fromString("5f2b8c3e-5a0e-4d6b-9b8a-1c6f3a7e0a08");

    private static void mod(Player p, Attribute a, UUID id, String name, double v, AttributeModifier.Operation op, boolean on) {
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        boolean has = inst.getModifier(id) != null;
        if (on && !has) inst.addTransientModifier(new AttributeModifier(id, name, v, op));
        else if (!on && has) inst.removeModifier(id);
    }

    static void applyAttributes(Player p, boolean on) {
        mod(p, Attributes.MAX_HEALTH, HP, "form_health", 30.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ATTACK_DAMAGE, DMG, "form_damage", 7.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ARMOR, ARMOR, "form_armor", 10.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ARMOR_TOUGHNESS, TOUGH, "form_toughness", 4.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, "form_speed", 0.2, AttributeModifier.Operation.MULTIPLY_BASE, on);
        mod(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK, "form_knockback", 0.6, AttributeModifier.Operation.ADDITION, on);
        mod(p, ForgeMod.ENTITY_REACH.get(), REACH, "form_reach", 1.5, AttributeModifier.Operation.ADDITION, on);
        mod(p, ForgeMod.SWIM_SPEED.get(), SWIM, "form_swim", 1.0, AttributeModifier.Operation.MULTIPLY_BASE, on);
        if (!on && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // ------------------------------------------------------------------ transform / revert
    /** form of the transformation blade the player carries: main hand, off hand, then inventory */
    public static int bladeForm(Player p) {
        if (p.getMainHandItem().getItem() instanceof FormBladeItem b) return b.form;
        if (p.getOffhandItem().getItem() instanceof FormBladeItem b) return b.form;
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof FormBladeItem b) return b.form;
        return -1;
    }

    public static void transform(ServerPlayer p, int f) {
        if (isMorphed(p) || !Forms.valid(f)) return;
        State s = state(p);
        s.form = f;
        setForm(p, f);
        applyAttributes(p, true);
        p.setHealth(p.getMaxHealth());
        s.transformTick = 0;
        s.ability = -1;
        s.buffTicks = 0;
        Arrays.fill(s.cd, 0);
        sync(p);
        anim(p, Aigoar.ANIM_TRANSFORM, true);
        sound(p, Forms.transform(f), 1.6F, 1.0F);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Aigoar.TRANSFORM_BURST + 2, 6, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Aigoar.TRANSFORM_BURST + 10, 4, false, false));
        p.displayClientMessage(Component.translatable("msg.krolasyonbosses.transformed." + Forms.KEY[f]).withStyle(net.minecraft.ChatFormatting.GOLD), true);
    }

    /** kept for the Aigoar code paths and the self test */
    public static void transform(ServerPlayer p) { transform(p, Forms.AIGOAR); }

    public static void revert(ServerPlayer p) {
        int f = form(p);
        if (f < 0) return;
        State s = state(p);
        s.ability = -1;
        s.transformTick = -1;
        s.buffTicks = 0;
        setForm(p, -1);
        applyAttributes(p, false);
        sync(p);
        sound(p, Forms.revert(f), 1.2F, 1.0F);
        ServerLevel sl = p.serverLevel();
        if (f == Forms.AIGOAR) {
            sl.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1.2, p.getZ(), 80, 0.6, 1.0, 0.6, 0.2);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        } else if (f == Forms.SHADE) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 1.0, 0.5, 0.02);
        } else {
            sl.sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1.2, p.getZ(), 60, 0.5, 1.0, 0.5, 0.08);
            sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.5, 1.0, 0.5, 0.02);
        }
        sl.sendParticles(dust(Forms.COLOR[f], 1.0F), p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.6, 1.2, 0.6, 0);
        p.displayClientMessage(Component.translatable("msg.krolasyonbosses.reverted").withStyle(net.minecraft.ChatFormatting.GRAY), true);
    }

    public static void toggle(ServerPlayer p) {
        if (isMorphed(p)) {
            if (state(p).transformTick < 0) revert(p);
            return;
        }
        int f = bladeForm(p);
        if (f >= 0) transform(p, f);
        else p.displayClientMessage(Component.translatable("msg.krolasyonbosses.need_blade").withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    // ------------------------------------------------------------------ input
    public static void handleKey(ServerPlayer p, byte action) {
        if (action == ModNetwork.KeyMsg.TOGGLE) {
            toggle(p);
            return;
        }
        int f = form(p);
        if (f < 0 || !p.isAlive() || p.isSpectator()) return;
        State s = state(p);
        if (action >= 0 && action < Aigoar.ABILITIES) {
            startAbility(p, action);
        } else if (action == ModNetwork.KeyMsg.DOUBLE_JUMP) {
            if (!p.onGround() && !s.doubleJumped && s.transformTick < 0 && !p.getAbilities().flying && !p.isInWater()) {
                s.doubleJumped = true;
                Vec3 v = p.getDeltaMovement();
                Vec3 look = horizontalLook(p);
                p.setDeltaMovement(v.x * 0.5 + look.x * 0.45, 0.82, v.z * 0.5 + look.z * 0.45);
                p.hurtMarked = true;
                p.fallDistance = 0;
                ServerLevel sl = p.serverLevel();
                if (f == Forms.AIGOAR) {
                    sound(p, ModSounds.DOUBLE_JUMP.get(), 1.0F, 0.9F + p.getRandom().nextFloat() * 0.2F);
                    ring(sl, p.position(), 1.2, 24, ParticleTypes.SPLASH, 0.05);
                    sl.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY(), p.getZ(), 20, 0.5, 0.1, 0.5, 0.05);
                } else if (f == Forms.SHADE) {
                    sound(p, ModSounds.SHADOW_STEP.get(), 0.8F, 1.4F);
                    ring(sl, p.position(), 1.0, 20, ParticleTypes.LARGE_SMOKE, 0.02);
                } else {
                    sound(p, ModSounds.FIREBALL_WHOOSH.get(), 0.8F, 1.3F);
                    ring(sl, p.position(), 1.0, 20, ParticleTypes.FLAME, 0.05);
                    sl.sendParticles(ParticleTypes.SMOKE, p.getX(), p.getY(), p.getZ(), 10, 0.4, 0.1, 0.4, 0.02);
                }
                sl.sendParticles(dust(Forms.COLOR2[f], 0.9F), p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.4, 0.1, 0.4, 0);
                anim(p, Aigoar.ANIM_DOUBLE_JUMP, false);
            }
        } else if (action == ModNetwork.KeyMsg.SWING) {
            if (p.tickCount - s.lastSwing >= 5 && s.ability < 0) {
                s.lastSwing = p.tickCount;
                s.attackAlt = !s.attackAlt;
                anim(p, s.attackAlt ? Aigoar.ANIM_ATTACK_R : Aigoar.ANIM_ATTACK_L, false);
                sound(p, Forms.swing(f), 0.7F, 0.9F + p.getRandom().nextFloat() * 0.25F);
            }
        }
    }

    public static void startAbility(ServerPlayer p, int id) {
        State s = state(p);
        int f = form(p);
        if (f < 0 || s.transformTick >= 0 || s.ability >= 0) return;
        if (s.cd[id] > 0) {
            p.displayClientMessage(Component.translatable("msg.krolasyonbosses.cooldown",
                    Component.translatable("ability.krolasyonbosses." + Forms.KEY[f] + "." + id), String.format(Locale.ROOT, "%.1f", s.cd[id] / 20F))
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
            return;
        }
        s.ability = id;
        s.tick = 0;
        s.hit.clear();
        s.carried.clear();
        s.slammed = false;
        s.targetId = -1;
        int cd = Forms.COOLDOWN[f][id];
        s.cd[id] = p.isCreative() ? cd / 4 : cd;
        anim(p, (byte) id, true);
        sendCooldowns(p);
    }

    /** used by the CI visual self-test */
    public static void debugForce(ServerPlayer p, int id) {
        State s = state(p);
        s.ability = -1;
        s.transformTick = -1;
        Arrays.fill(s.cd, 0);
        startAbility(p, id);
    }

    // ------------------------------------------------------------------ tick
    public static void tick(ServerPlayer p) {
        State s = state(p);
        int f = form(p);
        if (p.tickCount % 20 == 0) applyAttributes(p, f >= 0);
        if (f < 0) {
            if (s.ability >= 0 || s.transformTick >= 0) { s.ability = -1; s.transformTick = -1; }
            s.buffTicks = 0;
            return;
        }
        s.form = f;
        boolean cdChanged = false;
        for (int i = 0; i < s.cd.length; i++) if (s.cd[i] > 0) { s.cd[i]--; cdChanged |= s.cd[i] == 0; }
        if (cdChanged) sendCooldowns(p);
        if (p.onGround() || p.isInWater()) s.doubleJumped = false;
        if (!p.isAlive()) return;
        ServerLevel sl = p.serverLevel();
        FormAbilities logic = Forms.LOGIC[f];

        if (Forms.fiery(f)) {
            // children of fire: immune to fire and lava, healed by it, attacks ignite
            if (p.tickCount % 20 == 0) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, true, false));
            if ((p.isOnFire() || p.isInLava()) && p.tickCount % 20 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1.0F);
            if (p.isInWaterRainOrBubble() && p.tickCount % 10 == 0)
                sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.8, p.getZ(), 2, 0.3, 0.3, 0.3, 0.02);
        } else if (f == Forms.SHADE) {
            if (p.tickCount % 40 == 0) p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
            if (sl.getMaxLocalRawBrightness(p.blockPosition()) < 7 && p.tickCount % 30 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1.0F);
        }
        logic.passive(p, s, sl);
        if (s.buffTicks > 0) s.buffTicks--;
        if (--s.ambient <= 0) {
            s.ambient = 180 + p.getRandom().nextInt(200);
            sound(p, Forms.ambient(f), 0.6F, 0.95F + p.getRandom().nextFloat() * 0.1F);
        }

        if (s.transformTick >= 0) {
            int t = s.transformTick;
            if (t == Aigoar.TRANSFORM_BURST) {
                logic.transformBurst(p, sl);
                Vec3 c = p.position();
                for (LivingEntity e : targets(p, new AABB(c, c).inflate(5, 3, 5))) {
                    if (hit(p, e, 5.0F, 1.4, 0.5) && Forms.fiery(f)) e.setSecondsOnFire(5);
                }
            } else {
                logic.transformTick(p, sl, t);
            }
            if (++s.transformTick >= Aigoar.TRANSFORM_TICKS) s.transformTick = -1;
        }
        if (s.ability >= 0) {
            int id = s.ability;
            logic.tick(p, s, sl, id, s.tick);
            s.tick++;
            if (s.ability == id && s.tick >= Forms.duration(f, id)) s.ability = -1;
        }
    }

    // ------------------------------------------------------------------ helpers
    /** who the abilities may hurt: monsters, anything fighting the player, and players when PvP allows it */
    public static boolean isTarget(Player p, Entity e) {
        if (!(e instanceof LivingEntity le) || e == p || !e.isAlive() || e instanceof ArmorStand) return false;
        if (e instanceof Player other) return !other.isCreative() && !other.isSpectator() && p.canHarmPlayer(other);
        if (e instanceof TamableAnimal ta && ta.isOwnedBy(p)) return false;
        if (e instanceof Enemy) return true;
        if (e instanceof Mob m && m.getTarget() == p) return true;
        return le.getLastHurtByMob() == p || p.getLastHurtMob() == le || p.getLastHurtByMob() == le;
    }

    public static List<LivingEntity> targets(Player p, AABB box) {
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> isTarget(p, e));
    }

    public static List<LivingEntity> cone(Player p, Vec3 dir, double range, double minDot) {
        Vec3 c = p.position();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : targets(p, p.getBoundingBox().inflate(range, 2.0, range))) {
            Vec3 d = e.position().subtract(c);
            Vec3 h = new Vec3(d.x, 0, d.z);
            double len = h.length();
            if (len > range + e.getBbWidth() * 0.5) continue;
            if (len < 1.0 || h.normalize().dot(dir) >= minDot) out.add(e);
        }
        return out;
    }

    public static boolean hit(Player p, LivingEntity e, float dmg, double knock, double lift) {
        if (!e.hurt(p.damageSources().playerAttack(p), dmg)) return false;
        Vec3 d = e.position().subtract(p.position());
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4) d = horizontalLook(p);
        d = d.normalize();
        double kr = 1.0 - e.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.6;
        e.setDeltaMovement(e.getDeltaMovement().add(d.x * knock * kr, lift * kr, d.z * knock * kr));
        e.hurtMarked = true;
        return true;
    }

    public static Vec3 horizontalLook(Player p) {
        float yaw = p.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** where the pressure beam leaves the claws (in front of the chest) */
    public static Vec3 beamOrigin(Player p) {
        return p.getEyePosition().add(0, -0.25, 0).add(p.getLookAngle().scale(1.1));
    }

    public static Vec3 aimPoint(Player p, double range) {
        Vec3 from = p.getEyePosition();
        Vec3 to = from.add(p.getLookAngle().scale(range));
        BlockHitResult bh = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 hitPos = bh.getType() == HitResult.Type.MISS ? to : bh.getLocation();
        // prefer an enemy close to the aim line
        LivingEntity best = null;
        double bestD = 4.0;
        for (LivingEntity e : targets(p, new AABB(from, hitPos).inflate(3))) {
            Vec3 ep = e.position().add(0, e.getBbHeight() * 0.5, 0);
            Vec3 d = hitPos.subtract(from);
            double t = Mth.clamp(ep.subtract(from).dot(d) / d.lengthSqr(), 0, 1);
            double dist = ep.distanceTo(from.add(d.scale(t)));
            if (dist < bestD) { bestD = dist; best = e; }
        }
        return best != null ? best.position() : hitPos;
    }

    public static void ring(ServerLevel sl, Vec3 c, double r, int n, ParticleOptions part, double speed) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            sl.sendParticles(part, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 1, 0, 0.05, 0, speed);
        }
    }

    public static ParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), scale);
    }

    public static void sound(Player p, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    public static void sound(Level level, Vec3 at, SoundEvent s, float vol, float pitch) {
        level.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** fire damage + burning, used by every flame ability */
    public static boolean burn(Player p, LivingEntity e, float dmg, double knock, double lift, int fireSeconds) {
        if (!hit(p, e, dmg, knock, lift)) return false;
        e.setSecondsOnFire(fireSeconds);
        return true;
    }

    /** slash arc of particles in front of the player */
    public static void arc(ServerLevel sl, Player p, Vec3 dir, double radius, double height, ParticleOptions a, ParticleOptions b) {
        Vec3 c = p.position().add(0, height, 0);
        Vec3 right = new Vec3(-dir.z, 0, dir.x);
        for (int i = 0; i <= 16; i++) {
            double ang = -1.3 + i * (2.6 / 16);
            Vec3 q = c.add(dir.scale(Math.cos(ang) * radius)).add(right.scale(Math.sin(ang) * radius));
            sl.sendParticles(i % 2 == 0 ? a : b, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    /** safe teleport destination along a direction (stops before walls) */
    public static Vec3 blinkTarget(Player p, Vec3 dir, double dist) {
        Vec3 from = p.position().add(0, 0.5, 0);
        Vec3 to = from.add(dir.scale(dist));
        BlockHitResult bh = p.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? to : bh.getLocation().subtract(dir.scale(0.8));
        Double g = com.krolasyon.bosses.entity.EruptionEntity.findGround(p.level(), end.x, end.z, end.y + 2, end.y - 6);
        return new Vec3(end.x, g != null ? g : end.y - 0.5, end.z);
    }

    public static boolean inTransform(Player p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.transformTick >= 0;
    }
}
