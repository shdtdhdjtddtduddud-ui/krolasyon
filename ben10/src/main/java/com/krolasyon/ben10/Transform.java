package com.krolasyon.ben10;

import com.krolasyon.ben10.ability.Fx;
import com.krolasyon.ben10.ability.Powers;
import com.krolasyon.ben10.entity.FxEntity;
import com.krolasyon.ben10.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import org.joml.Vector3f;

import java.util.UUID;

/** Transforming into / out of aliens and the per-alien attribute modifiers. */
public final class Transform {
    private static final UUID HEALTH = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10001");
    private static final UUID SPEED = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10002");
    private static final UUID DAMAGE = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10003");
    private static final UUID ARMOR = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10004");
    private static final UUID TOUGH = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10005");
    private static final UUID KNOCK = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10006");
    private static final UUID STEP = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10007");
    private static final UUID REACH = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10008");
    public static final UUID GUARD_SLOW = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b10009");
    public static final UUID BOOST = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b1000a");
    private static final UUID SWIM = UUID.fromString("6f0b8f6e-1d1a-4b0e-9a51-0a1e10b1000b");

    private Transform() {}

    public static boolean hasOmnitrix(Player p) {
        if (p.isCreative()) return true;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModRegistry.OMNITRIX.get())) return true;
        }
        return false;
    }

    public static void request(ServerPlayer p, int id) {
        OmniData d = OmniData.get(p);
        if (!p.isAlive() || p.isSpectator()) return;
        if (id < 0) {
            if (d.alien != null) detransform(p, false);
            return;
        }
        Alien a = Alien.byId(id);
        if (a == null || !hasOmnitrix(p)) return;
        if (d.recharge > 0 && d.alien == null) {
            p.displayClientMessage(Component.translatable("msg.krolben10.recharging", (d.recharge + 19) / 20).withStyle(ChatFormatting.RED), true);
            p.level().playSound(null, p.blockPosition(), ModRegistry.S_BEEP.get(), SoundSource.PLAYERS, 0.8F, 0.6F);
            return;
        }
        if (d.alien == a) return;
        transform(p, a);
    }

    public static void transform(ServerPlayer p, Alien a) {
        OmniData d = OmniData.get(p);
        boolean fresh = d.alien == null;
        if (d.alien != null) {
            Powers.cancel(p, d);
            removeModifiers(p);
        }
        d.alien = a;
        if (fresh) d.timeLeft = Config.durationTicks();
        d.ability = -1;
        d.blocking = false;
        d.flags = 0;
        d.targetId = -1;
        d.sinceTransform = 0;
        for (int i = 0; i < 4; i++) d.cooldown[i] = d.cooldownMax[i] = 0;
        float before = p.getMaxHealth();
        applyModifiers(p, a);
        p.setHealth(Math.min(p.getMaxHealth(), p.getHealth() + (p.getMaxHealth() - before)));
        updateFlight(p, a);
        p.clearFire();
        p.refreshDimensions();
        // green Omnitrix flash
        Fx.sound(p.serverLevel(), p.position(), ModRegistry.S_TRANSFORM.get(), 1.2F, 1.0F);
        Fx.particles(p.serverLevel(), new DustParticleOptions(new Vector3f(0.35F, 1F, 0.2F), 1.3F), p.getX(), p.getY() + a.height / 2, p.getZ(), 50, a.width, a.height / 2, a.width, 0.02);
        Fx.particles(p.serverLevel(), ParticleTypes.END_ROD, p.getX(), p.getY() + a.height / 2, p.getZ(), 30, 0.4, a.height / 2, 0.4, 0.15);
        FxEntity.spawn(p.serverLevel(), p.position().add(0, a.height / 2, 0), FxEntity.FLASH, 14, 3.0F, 0x55FF33, p);
        Net.sync(p);
        save(p);
    }

    public static void detransform(ServerPlayer p, boolean timeout) {
        OmniData d = OmniData.get(p);
        if (d.alien == null) return;
        Powers.cancel(p, d);
        Alien old = d.alien;
        d.alien = null;
        d.ability = -1;
        d.blocking = false;
        d.flags = 0;
        d.targetId = -1;
        d.sinceTransform = 0;
        d.recharge = timeout ? Config.rechargeTicks() : 60;
        removeModifiers(p);
        p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
        updateFlight(p, null);
        p.refreshDimensions();
        p.setInvisible(false);
        Fx.sound(p.serverLevel(), p.position(), timeout ? ModRegistry.S_TIMEOUT.get() : ModRegistry.S_DETRANSFORM.get(), 1.0F, 1.0F);
        int col = timeout ? 0xFF3322 : 0x55FF33;
        Fx.particles(p.serverLevel(), new DustParticleOptions(new Vector3f((col >> 16 & 255) / 255F, (col >> 8 & 255) / 255F, (col & 255) / 255F), 2.0F),
                p.getX(), p.getY() + 1, p.getZ(), 60, old.width, 1.0, old.width, 0.02);
        FxEntity.spawn(p.serverLevel(), p.position().add(0, 1, 0), FxEntity.FLASH, 12, 2.4F, col, p);
        if (timeout) p.displayClientMessage(Component.translatable("msg.krolben10.timeout").withStyle(ChatFormatting.RED), true);
        Net.sync(p);
        save(p);
    }

    /** server tick for a player (both transformed and recharging) */
    public static void tick(ServerPlayer p) {
        OmniData d = OmniData.get(p);
        if (d.sinceTransform < 1000) d.sinceTransform++;
        if (d.recharge > 0) {
            d.recharge--;
            if (d.recharge == 0) {
                Fx.sound(p.serverLevel(), p.position(), ModRegistry.S_BEEP.get(), 0.7F, 1.6F);
                d.dirty = true;
            }
        }
        if (d.alien == null) {
            if (d.dirty || p.tickCount % 40 == 0) Net.sync(p);
            return;
        }
        if (!hasOmnitrix(p)) {
            detransform(p, false);
            return;
        }
        if (Config.durationTicks() > 0) {
            d.timeLeft--;
            if (d.timeLeft <= 200 && d.timeLeft > 0 && d.timeLeft % 20 == 0) {
                Fx.sound(p.serverLevel(), p.position(), ModRegistry.S_BEEP.get(), 0.9F, d.timeLeft <= 60 ? 1.4F : 1.1F);
            }
            if (d.timeLeft <= 0) {
                detransform(p, true);
                return;
            }
        }
        for (int i = 0; i < 4; i++) if (d.cooldown[i] > 0) d.cooldown[i]--;
        Powers.tick(p, d);
        if (d.alien != null) {
            AttributeInstance mh = p.getAttribute(Attributes.MAX_HEALTH);
            if (mh != null && mh.getModifier(HEALTH) == null) {
                applyModifiers(p, d.alien);
                updateFlight(p, d.alien);
            }
        }
        if (d.dirty || p.tickCount % 40 == 0) Net.sync(p);
        if (p.tickCount % 100 == 0) save(p);
    }

    public static void save(Player p) {
        p.getPersistentData().put(Ben10.MODID, OmniData.get(p).save());
    }

    public static void load(ServerPlayer p) {
        OmniData d = OmniData.get(p);
        d.load(p.getPersistentData().getCompound(Ben10.MODID));
        removeModifiers(p);
        if (d.alien != null) {
            applyModifiers(p, d.alien);
            updateFlight(p, d.alien);
        }
        p.refreshDimensions();
        Net.sync(p);
    }

    public static void applyModifiers(Player p, Alien a) {
        removeModifiers(p);
        add(p, Attributes.MAX_HEALTH, HEALTH, a.bonusHealth, AttributeModifier.Operation.ADDITION);
        add(p, Attributes.MOVEMENT_SPEED, SPEED, a.speed - 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
        add(p, Attributes.ATTACK_DAMAGE, DAMAGE, a.damage, AttributeModifier.Operation.ADDITION);
        add(p, Attributes.ARMOR, ARMOR, a.armor, AttributeModifier.Operation.ADDITION);
        add(p, Attributes.ARMOR_TOUGHNESS, TOUGH, a.armor / 3.0, AttributeModifier.Operation.ADDITION);
        add(p, Attributes.KNOCKBACK_RESISTANCE, KNOCK, a.knockbackRes, AttributeModifier.Operation.ADDITION);
        add(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP, a.height > 2.05F || a == Alien.XLR8 || a == Alien.CANNONBOLT ? 0.6 : 0.0, AttributeModifier.Operation.ADDITION);
        add(p, ForgeMod.ENTITY_REACH.get(), REACH, a.height > 2.05F ? 1.0 : 0.0, AttributeModifier.Operation.ADDITION);
        add(p, ForgeMod.SWIM_SPEED.get(), SWIM, a == Alien.XLR8 ? 1.0 : 0.0, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    public static void removeModifiers(Player p) {
        for (UUID u : new UUID[]{HEALTH, SPEED, DAMAGE, ARMOR, TOUGH, KNOCK, STEP, REACH, GUARD_SLOW, BOOST, SWIM}) {
            for (Attribute at : new Attribute[]{Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.ARMOR,
                    Attributes.ARMOR_TOUGHNESS, Attributes.KNOCKBACK_RESISTANCE, ForgeMod.STEP_HEIGHT_ADDITION.get(), ForgeMod.ENTITY_REACH.get(), ForgeMod.SWIM_SPEED.get()}) {
                AttributeInstance inst = p.getAttribute(at);
                if (inst != null) inst.removeModifier(u);
            }
        }
    }

    public static void add(Player p, Attribute at, UUID id, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(at);
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) inst.addTransientModifier(new AttributeModifier(id, "krolben10", amount, op));
    }

    public static void updateFlight(ServerPlayer p, Alien a) {
        boolean fly = a != null && a.has(Alien.Flag.FLY);
        boolean native_ = p.isCreative() || p.isSpectator();
        if (fly) {
            p.getAbilities().mayfly = true;
        } else if (!native_) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
        }
        p.onUpdateAbilities();
    }
}
