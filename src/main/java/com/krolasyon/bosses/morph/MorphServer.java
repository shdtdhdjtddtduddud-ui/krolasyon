package com.krolasyon.bosses.morph;

import com.krolasyon.bosses.entity.TideGeyserEntity;
import com.krolasyon.bosses.entity.TideVortexEntity;
import com.krolasyon.bosses.entity.TsunamiWaveEntity;
import com.krolasyon.bosses.item.TideBladeItem;
import com.krolasyon.bosses.network.ModNetwork;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import org.joml.Vector3f;

import java.util.*;

/** Server side of the Aigoar transformation: state, attributes, transform sequence and the five abilities. */
public final class MorphServer {
    private MorphServer() {}

    private static final String TAG = "krolasyon_aigoar";

    /** transient per-player runtime state */
    static final class State {
        int ability = -1;
        int tick;
        int transformTick = -1;
        final int[] cd = new int[Aigoar.ABILITIES];
        final Set<Integer> hit = new HashSet<>();
        boolean doubleJumped;
        boolean slammed;
        int lastSwing;
        int ambient = 200;
        boolean attackAlt;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    static State state(Player p) { return STATES.computeIfAbsent(p.getUUID(), k -> new State()); }

    public static void forget(Player p) { STATES.remove(p.getUUID()); }

    // ------------------------------------------------------------------ persistent flag
    public static boolean isMorphed(Player p) {
        if (p.level().isClientSide()) return Aigoar.CLIENT_MORPHED.contains(p.getId());
        return p.getPersistentData().getCompound(TAG).getBoolean("Morphed");
    }

    private static void setMorphedFlag(Player p, boolean b) {
        CompoundTag t = p.getPersistentData().getCompound(TAG);
        t.putBoolean("Morphed", b);
        p.getPersistentData().put(TAG, t);
    }

    public static void copyOnClone(Player from, Player to) {
        if (from.getPersistentData().contains(TAG)) to.getPersistentData().put(TAG, from.getPersistentData().getCompound(TAG).copy());
    }

    public static void sync(ServerPlayer p) {
        ModNetwork.toTrackingAndSelf(p, new ModNetwork.SyncMsg(p.getId(), isMorphed(p)));
        sendCooldowns(p);
    }

    public static void syncTo(ServerPlayer target, ServerPlayer viewer) {
        if (isMorphed(target)) ModNetwork.toPlayer(viewer, new ModNetwork.SyncMsg(target.getId(), true));
    }

    private static void anim(ServerPlayer p, byte anim, boolean includeSelf) {
        ModNetwork.AnimMsg msg = new ModNetwork.AnimMsg(p.getId(), anim);
        if (includeSelf) ModNetwork.toTrackingAndSelf(p, msg);
        else ModNetwork.toTracking(p, msg);
    }

    private static void sendCooldowns(ServerPlayer p) {
        State s = state(p);
        ModNetwork.toPlayer(p, new ModNetwork.CooldownMsg(s.cd.clone(), Aigoar.COOLDOWN.clone()));
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
        mod(p, Attributes.MAX_HEALTH, HP, "aigoar_health", 30.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ATTACK_DAMAGE, DMG, "aigoar_damage", 7.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ARMOR, ARMOR, "aigoar_armor", 10.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.ARMOR_TOUGHNESS, TOUGH, "aigoar_toughness", 4.0, AttributeModifier.Operation.ADDITION, on);
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, "aigoar_speed", 0.2, AttributeModifier.Operation.MULTIPLY_BASE, on);
        mod(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK, "aigoar_knockback", 0.6, AttributeModifier.Operation.ADDITION, on);
        mod(p, ForgeMod.ENTITY_REACH.get(), REACH, "aigoar_reach", 1.5, AttributeModifier.Operation.ADDITION, on);
        mod(p, ForgeMod.SWIM_SPEED.get(), SWIM, "aigoar_swim", 1.0, AttributeModifier.Operation.MULTIPLY_BASE, on);
        if (!on && p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    // ------------------------------------------------------------------ transform / revert
    public static boolean hasBlade(Player p) {
        for (ItemStack s : p.getInventory().items) if (s.getItem() instanceof TideBladeItem) return true;
        return p.getOffhandItem().getItem() instanceof TideBladeItem || p.isCreative() && p.getMainHandItem().getItem() instanceof TideBladeItem;
    }

    public static void transform(ServerPlayer p) {
        if (isMorphed(p)) return;
        State s = state(p);
        setMorphedFlag(p, true);
        applyAttributes(p, true);
        p.setHealth(p.getMaxHealth());
        s.transformTick = 0;
        s.ability = -1;
        sync(p);
        anim(p, Aigoar.ANIM_TRANSFORM, true);
        sound(p, ModSounds.TRANSFORM.get(), 1.6F, 1.0F);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Aigoar.TRANSFORM_BURST + 2, 6, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Aigoar.TRANSFORM_BURST + 10, 4, false, false));
        p.displayClientMessage(Component.translatable("msg.krolasyonbosses.transformed").withStyle(net.minecraft.ChatFormatting.AQUA), true);
    }

    public static void revert(ServerPlayer p) {
        if (!isMorphed(p)) return;
        State s = state(p);
        s.ability = -1;
        s.transformTick = -1;
        setMorphedFlag(p, false);
        applyAttributes(p, false);
        sync(p);
        sound(p, ModSounds.REVERT.get(), 1.2F, 1.0F);
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 1.2, p.getZ(), 80, 0.6, 1.0, 0.6, 0.2);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY() + 1.2, p.getZ(), 40, 0.5, 1.0, 0.5, 0.05);
        sl.sendParticles(dust(0x3FE0E6, 1.0F), p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.6, 1.2, 0.6, 0);
        p.displayClientMessage(Component.translatable("msg.krolasyonbosses.reverted").withStyle(net.minecraft.ChatFormatting.GRAY), true);
    }

    public static void toggle(ServerPlayer p) {
        if (isMorphed(p)) {
            if (state(p).transformTick < 0) revert(p);
        } else if (hasBlade(p)) {
            transform(p);
        } else {
            p.displayClientMessage(Component.translatable("msg.krolasyonbosses.need_blade").withStyle(net.minecraft.ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ input
    public static void handleKey(ServerPlayer p, byte action) {
        if (action == ModNetwork.KeyMsg.TOGGLE) {
            toggle(p);
            return;
        }
        if (!isMorphed(p) || !p.isAlive() || p.isSpectator()) return;
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
                sound(p, ModSounds.DOUBLE_JUMP.get(), 1.0F, 0.9F + p.getRandom().nextFloat() * 0.2F);
                ServerLevel sl = p.serverLevel();
                ring(sl, p.position(), 1.2, 24, ParticleTypes.SPLASH, 0.05);
                sl.sendParticles(ParticleTypes.BUBBLE_POP, p.getX(), p.getY(), p.getZ(), 20, 0.5, 0.1, 0.5, 0.05);
                sl.sendParticles(dust(0x7FFFF6, 0.9F), p.getX(), p.getY() + 0.2, p.getZ(), 10, 0.4, 0.1, 0.4, 0);
                anim(p, Aigoar.ANIM_DOUBLE_JUMP, false);
            }
        } else if (action == ModNetwork.KeyMsg.SWING) {
            if (p.tickCount - s.lastSwing >= 5 && s.ability < 0) {
                s.lastSwing = p.tickCount;
                s.attackAlt = !s.attackAlt;
                anim(p, s.attackAlt ? Aigoar.ANIM_ATTACK_R : Aigoar.ANIM_ATTACK_L, false);
                sound(p, ModSounds.CLAW_SWIPE.get(), 0.7F, 0.9F + p.getRandom().nextFloat() * 0.25F);
            }
        }
    }

    public static void startAbility(ServerPlayer p, int id) {
        State s = state(p);
        if (s.transformTick >= 0 || s.ability >= 0) return;
        if (s.cd[id] > 0) {
            p.displayClientMessage(Component.translatable("msg.krolasyonbosses.cooldown",
                    Component.translatable("ability.krolasyonbosses." + id), String.format(Locale.ROOT, "%.1f", s.cd[id] / 20F)).withStyle(net.minecraft.ChatFormatting.DARK_AQUA), true);
            return;
        }
        s.ability = id;
        s.tick = 0;
        s.hit.clear();
        s.slammed = false;
        s.cd[id] = p.isCreative() ? Aigoar.COOLDOWN[id] / 4 : Aigoar.COOLDOWN[id];
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
        boolean morphed = isMorphed(p);
        if (p.tickCount % 20 == 0) applyAttributes(p, morphed);
        if (!morphed) {
            if (s.ability >= 0 || s.transformTick >= 0) { s.ability = -1; s.transformTick = -1; }
            return;
        }
        boolean cdChanged = false;
        for (int i = 0; i < s.cd.length; i++) if (s.cd[i] > 0) { s.cd[i]--; cdChanged |= s.cd[i] == 0; }
        if (cdChanged) sendCooldowns(p);
        if (p.onGround() || p.isInWater()) s.doubleJumped = false;
        if (!p.isAlive()) return;

        // passives: water breathing, regeneration and grace in water or rain
        p.setAirSupply(p.getMaxAirSupply());
        boolean wet = p.isInWaterRainOrBubble();
        if (wet && p.tickCount % 30 == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1.0F);
        if (p.isInWater() && p.tickCount % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false));
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
        }
        if (p.isOnFire() && p.tickCount % 10 == 0) p.clearFire();
        if (--s.ambient <= 0) {
            s.ambient = 180 + p.getRandom().nextInt(200);
            sound(p, ModSounds.AIGOAR_AMBIENT.get(), 0.6F, 0.95F + p.getRandom().nextFloat() * 0.1F);
        }

        if (s.transformTick >= 0) {
            tickTransform(p, s.transformTick);
            if (++s.transformTick >= Aigoar.TRANSFORM_TICKS) s.transformTick = -1;
        }
        if (s.ability >= 0) {
            int id = s.ability;
            tickAbility(p, s, id, s.tick);
            s.tick++;
            if (s.ability == id && s.tick >= Aigoar.DURATION[id]) s.ability = -1;
        }
    }

    private static void tickTransform(ServerPlayer p, int t) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        if (t < Aigoar.TRANSFORM_BURST) {
            // rising spiral cocoon of water
            float k = t / (float) Aigoar.TRANSFORM_BURST;
            for (int i = 0; i < 3; i++) {
                double a = t * 0.55 + i * Math.PI * 2 / 3;
                double r = 1.6 - k * 0.9;
                double y = (t % 10) * 0.28;
                sl.sendParticles(dust(i == 0 ? 0x7FFFF6 : 0x2BC8D6, 0.9F), c.x + Math.cos(a) * r, c.y + y, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                sl.sendParticles(ParticleTypes.SPLASH, c.x + Math.cos(a + 1) * r, c.y + y * 0.5, c.z + Math.sin(a + 1) * r, 2, 0, 0.1, 0, 0.1);
            }
            sl.sendParticles(ParticleTypes.FALLING_WATER, c.x, c.y + 3.2, c.z, 1, 0.6, 0.2, 0.6, 0);
            if (t % 3 == 0) sl.sendParticles(ParticleTypes.GLOW, c.x, c.y + 1.2, c.z, 2, 0.5, 0.8, 0.5, 0.02);
        } else if (t == Aigoar.TRANSFORM_BURST) {
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1.5, c.z, 220, 1.4, 1.4, 1.4, 0.4);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 1.5, c.z, 80, 1.2, 1.2, 1.2, 0.1);
            sl.sendParticles(ParticleTypes.GLOW, c.x, c.y + 1.6, c.z, 40, 1.0, 1.2, 1.0, 0.2);
            sl.sendParticles(ParticleTypes.NAUTILUS, c.x, c.y + 1.6, c.z, 60, 0.3, 0.4, 0.3, 1.2);
            ring(sl, c.add(0, 0.1, 0), 2.5, 40, dust(0x7FFFF6, 1.0F), 0);
            ring(sl, c.add(0, 0.1, 0), 4.0, 50, ParticleTypes.SPLASH, 0.1);
            for (LivingEntity e : targets(p, new AABB(c, c).inflate(5, 3, 5))) {
                hit(p, e, 5.0F, 1.4, 0.5);
            }
        } else if (t < Aigoar.TRANSFORM_BURST + 20 && t % 2 == 0) {
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.1, c.z, 3, 0.8, 0.0, 0.8, 0.05);
        }
    }

    private static void tickAbility(ServerPlayer p, State s, int id, int t) {
        ServerLevel sl = p.serverLevel();
        switch (id) {
            case Aigoar.REND -> rend(p, s, sl, t);
            case Aigoar.MAELSTROM -> maelstrom(p, sl, t);
            case Aigoar.GEYSER -> geyser(p, sl, t);
            case Aigoar.BEAM -> beam(p, s, sl, t);
            case Aigoar.TSUNAMI -> tsunami(p, s, sl, t);
            default -> {}
        }
    }

    // 1) Tidal Rend — water-propelled dash through enemies, then a cross slash and a rising slash
    private static void rend(ServerPlayer p, State s, ServerLevel sl, int t) {
        Vec3 dir = horizontalLook(p);
        if (t == 0) sound(p, ModSounds.TIDAL_REND.get(), 1.2F, 1.0F);
        if (t == 4) {
            p.setDeltaMovement(dir.x * 2.1, 0.15, dir.z * 2.1);
            p.hurtMarked = true;
        }
        if (t >= 4 && t <= 12) {
            Vec3 c = p.position().add(0, 1.0, 0);
            sl.sendParticles(dust(0x3FE0E6, 0.8F), c.x, c.y, c.z, 3, 0.4, 0.6, 0.4, 0);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y - 0.8, c.z, 10, 0.4, 0.1, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 6, 0.4, 0.5, 0.4, 0.05);
            for (LivingEntity e : targets(p, p.getBoundingBox().inflate(1.6, 0.8, 1.6))) {
                if (s.hit.add(e.getId())) {
                    if (hit(p, e, 8.0F, 0.6, 0.35)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
                        sl.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY(0.5), e.getZ(), 1, 0, 0, 0, 0);
                    }
                }
            }
        }
        if (t == 11 || t == 15) {
            boolean rising = t == 15;
            sound(p, ModSounds.CLAW_SWIPE.get(), 1.0F, rising ? 0.8F : 1.1F);
            Vec3 c = p.getEyePosition().add(dir.scale(1.6));
            arc(sl, p, dir, rising);
            for (LivingEntity e : cone(p, dir, 4.8, 0.45)) {
                if (hit(p, e, rising ? 6.0F : 7.0F, rising ? 0.3 : 1.0, rising ? 0.85 : 0.25))
                    sl.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(0.6), e.getZ(), 20, 0.3, 0.3, 0.3, 0.2);
            }
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, c.x, c.y - 0.4, c.z, 3, 0.8, 0.2, 0.8, 0);
        }
    }

    // 2) Maelstrom — hurls a vortex that drags enemies in and bursts
    private static void maelstrom(ServerPlayer p, ServerLevel sl, int t) {
        if (t == 0) sound(p, ModSounds.MAELSTROM.get(), 1.2F, 1.0F);
        if (t < 14) {
            // water gathering above the head
            Vec3 c = p.position().add(0, 3.3, 0);
            double a = t * 0.7;
            for (int i = 0; i < 4; i++) {
                double b = a + i * Math.PI / 2;
                sl.sendParticles(dust(0x3FE0E6, 0.9F), c.x + Math.cos(b) * 1.2, c.y, c.z + Math.sin(b) * 1.2, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y, c.z, 3, 0.5, 0.2, 0.5, 0.02);
        }
        if (t == 14) {
            Vec3 target = aimPoint(p, 22.0);
            Double gy = TideGeyserEntity.findGround(sl, target.x, target.z, target.y + 2, target.y - 8);
            Vec3 at = new Vec3(target.x, gy != null ? gy : target.y, target.z);
            TideVortexEntity.spawn(p, at);
            Vec3 from = p.getEyePosition();
            Vec3 d = at.add(0, 1, 0).subtract(from);
            int n = (int) (d.length() * 2);
            for (int i = 0; i < n; i++) {
                Vec3 q = from.add(d.scale(i / (double) n));
                sl.sendParticles(dust(0x7FFFF6, 1.0F), q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    // 3) Abyssal Geysers — slam the ground, a line of geysers erupts forward plus a ring around
    private static void geyser(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 dir = horizontalLook(p);
        if (t == 0) {
            p.setDeltaMovement(p.getDeltaMovement().x * 0.3, 0.5, p.getDeltaMovement().z * 0.3);
            p.hurtMarked = true;
        }
        if (t == 10) {
            sound(p, ModSounds.GEYSER_SLAM.get(), 1.4F, 1.0F);
            Vec3 c = p.position();
            ring(sl, c.add(0, 0.1, 0), 2.2, 36, ParticleTypes.SPLASH, 0.2);
            ring(sl, c.add(0, 0.1, 0), 3.0, 36, dust(0x2BC8D6, 1.0F), 0);
            sl.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.2, c.z, 14, 1.2, 0.1, 1.2, 0.05);
            for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.2, 1.5, 3.2))) hit(p, e, 5.0F, 0.8, 0.4);
            for (int i = 1; i <= 10; i++) {
                Vec3 q = c.add(dir.scale(1.2 + i * 1.7));
                TideGeyserEntity.spawn(p, q.x, q.z, c.y, 2 + i * 2, 9.0F, i % 2 == 1);
            }
            for (int i = 0; i < 7; i++) {
                double a = i * Math.PI * 2 / 7;
                TideGeyserEntity.spawn(p, c.x + Math.cos(a) * 3.4, c.z + Math.sin(a) * 3.4, c.y, 5, 7.0F, i == 0);
            }
        }
    }

    // 4) Pressure Beam — gather a water orb, then a piercing high pressure torrent
    private static void beam(ServerPlayer p, State s, ServerLevel sl, int t) {
        if (t == 0) {
            sound(p, ModSounds.BEAM_CHARGE.get(), 1.2F, 1.0F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Aigoar.DURATION[Aigoar.BEAM], 2, false, false));
        }
        Vec3 origin = beamOrigin(p);
        if (t < Aigoar.BEAM_START) {
            for (int i = 0; i < 3; i++) {
                Vec3 off = new Vec3(p.getRandom().nextGaussian(), p.getRandom().nextGaussian(), p.getRandom().nextGaussian()).normalize().scale(1.8);
                // nautilus particles fly from pos + velocity towards pos
                sl.sendParticles(ParticleTypes.NAUTILUS, origin.x, origin.y, origin.z, 0, off.x, off.y, off.z, 1.0);
            }
            sl.sendParticles(dust(0x7FFFF6, 1.0F), origin.x, origin.y, origin.z, 3, 0.15, 0.15, 0.15, 0);
            return;
        }
        if (t == Aigoar.BEAM_START) sound(p, ModSounds.BEAM_FIRE.get(), 1.4F, 1.0F);
        if (t >= Aigoar.BEAM_END) return;
        Vec3 look = p.getLookAngle();
        Vec3 end = origin.add(look.scale(Aigoar.BEAM_RANGE));
        BlockHitResult bh = sl.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        // impact
        sl.sendParticles(ParticleTypes.SPLASH, end.x, end.y, end.z, 12, 0.3, 0.3, 0.3, 0.3);
        sl.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, 4, 0.3, 0.3, 0.3, 0.05);
        if (t % 3 == 0) sl.sendParticles(ParticleTypes.CLOUD, end.x, end.y, end.z, 2, 0.2, 0.2, 0.2, 0.03);
        if (bh.getType() == HitResult.Type.BLOCK) {
            BlockPos fire = bh.getBlockPos().relative(bh.getDirection());
            if (sl.getBlockState(fire).is(Blocks.FIRE)) sl.removeBlock(fire, false);
        }
        if (t % 4 == 0) {
            Vec3 d = end.subtract(origin);
            AABB box = new AABB(origin, end).inflate(1.0);
            for (LivingEntity e : targets(p, box)) {
                Optional<Vec3> clip = e.getBoundingBox().inflate(0.7).clip(origin, end);
                if (clip.isEmpty() && !e.getBoundingBox().inflate(0.7).contains(origin)) continue;
                if (e.hurt(p.damageSources().playerAttack(p), 4.5F)) {
                    Vec3 push = d.normalize().scale(0.45);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.08, push.z));
                    e.hurtMarked = true;
                    e.clearFire();
                    sl.sendParticles(ParticleTypes.SPLASH, e.getX(), e.getY(0.5), e.getZ(), 16, 0.3, 0.4, 0.3, 0.2);
                }
            }
        }
    }

    // 5) Tsunami Crash — leap into a flip, crash down and release a giant expanding tidal wave
    private static void tsunami(ServerPlayer p, State s, ServerLevel sl, int t) {
        Vec3 dir = horizontalLook(p);
        if (t == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 50, 1, false, false));
        }
        if (t == 6) {
            p.setDeltaMovement(dir.x * 0.55, 1.25, dir.z * 0.55);
            p.hurtMarked = true;
            sound(p, ModSounds.TSUNAMI_LEAP.get(), 1.2F, 1.0F);
            ring(sl, p.position().add(0, 0.1, 0), 1.5, 30, ParticleTypes.SPLASH, 0.2);
            sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.1, p.getZ(), 10, 0.6, 0.1, 0.6, 0.05);
        }
        if (t > 6 && !s.slammed) {
            Vec3 c = p.position().add(0, 1.2, 0);
            sl.sendParticles(dust(0x3FE0E6, 0.8F), c.x, c.y, c.z, 3, 0.5, 0.8, 0.5, 0);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y - 0.6, c.z, 4, 0.5, 0.3, 0.5, 0.1);
        }
        if (t == 17 && !p.onGround()) {
            p.setDeltaMovement(dir.x * 0.3, -2.4, dir.z * 0.3);
            p.hurtMarked = true;
        }
        if (!s.slammed && t >= 10 && (p.onGround() || p.isInWater() || t >= 32)) {
            s.slammed = true;
            p.fallDistance = 0;
            Vec3 c = p.position();
            sound(p, ModSounds.TSUNAMI_CRASH.get(), 2.0F, 1.0F);
            TsunamiWaveEntity.spawn(p, c);
            ring(sl, c.add(0, 0.2, 0), 2.5, 30, ParticleTypes.POOF, 0.15);
            sl.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 0.5, c.z, 200, 2.0, 0.6, 2.0, 0.5);
            sl.sendParticles(ParticleTypes.NAUTILUS, c.x, c.y + 1.0, c.z, 60, 0.4, 0.4, 0.4, 1.5);
            for (LivingEntity e : targets(p, new AABB(c, c).inflate(3.5, 2.0, 3.5))) hit(p, e, 10.0F, 1.0, 1.0);
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

    private static List<LivingEntity> cone(Player p, Vec3 dir, double range, double minDot) {
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

    private static Vec3 aimPoint(Player p, double range) {
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

    private static void arc(ServerLevel sl, Player p, Vec3 dir, boolean rising) {
        Vec3 c = p.position().add(0, rising ? 0.6 : 1.4, 0);
        Vec3 right = new Vec3(-dir.z, 0, dir.x);
        for (int i = 0; i <= 14; i++) {
            double a = -1.2 + i * (2.4 / 14);
            Vec3 q = c.add(dir.scale(Math.cos(a) * 2.6)).add(right.scale(Math.sin(a) * 2.6));
            if (rising) q = q.add(0, i * 0.18, 0);
            sl.sendParticles(dust(i % 2 == 0 ? 0x7FFFF6 : 0x2BC8D6, 0.9F), q.x, q.y, q.z, 1, 0, 0, 0, 0);
            if (i % 3 == 0) sl.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, 3, 0.1, 0.1, 0.1, 0.1);
        }
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

    public static boolean inTransform(Player p) {
        State s = STATES.get(p.getUUID());
        return s != null && s.transformTick >= 0;
    }
}
