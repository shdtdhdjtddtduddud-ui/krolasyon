package com.krolasyon.omnitrix;

import com.krolasyon.omnitrix.net.Net;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/** Transforming, reverting, the time limit and the stat changes of each form. */
@Mod.EventBusSubscriber(modid = Omnitrix.MODID)
public final class Transform {
    private static final UUID HEALTH = UUID.fromString("6f1d2a8e-0b1a-4b7e-9a50-1e0d2c3b4a01");
    private static final UUID SPEED = UUID.fromString("6f1d2a8e-0b1a-4b7e-9a50-1e0d2c3b4a02");
    private static final UUID DAMAGE = UUID.fromString("6f1d2a8e-0b1a-4b7e-9a50-1e0d2c3b4a03");
    private static final UUID ARMOR = UUID.fromString("6f1d2a8e-0b1a-4b7e-9a50-1e0d2c3b4a04");
    private static final UUID KNOCKBACK = UUID.fromString("6f1d2a8e-0b1a-4b7e-9a50-1e0d2c3b4a05");
    private static final UUID STEP = UUID.fromString("6f1d2a8e-0b1a-4b7e-9a50-1e0d2c3b4a06");
    public static final int TRANSFORM_ANIM = 30;

    private Transform() {}

    /** the watch dial pops up (animation only) */
    public static void activate(ServerPlayer p) {
        OmniData d = OmniCapability.get(p);
        if (!d.wearing || d.transformed()) return;
        if (d.recharge > 0) {
            p.displayClientMessage(Component.translatable("msg.omnitrix.recharging", (d.recharge + 19) / 20), true);
            play(p, ModRegistry.TIMEOUT.get(), 0.6F, 1.6F);
            return;
        }
        play(p, ModRegistry.ACTIVATE.get(), 1F, 1F);
    }

    public static void transform(ServerPlayer p, int alienId) {
        OmniData d = OmniCapability.get(p);
        Alien a = Alien.byId(alienId);
        if (a == null || !d.wearing || !p.isAlive()) return;
        if (d.recharge > 0) {
            p.displayClientMessage(Component.translatable("msg.omnitrix.recharging", (d.recharge + 19) / 20), true);
            return;
        }
        if (d.transformed()) removeStats(p);
        d.clearStates();
        d.alien = a.ordinal();
        d.timer = OmniData.TRANSFORM_TIME;
        d.transformTime = p.level().getGameTime();
        d.castAbility = -1;
        applyStats(p, a);
        p.setHealth(p.getMaxHealth());
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, TRANSFORM_ANIM, 4, false, false, false));
        ServerLevel l = p.serverLevel();
        Fx.follow(l, Fx.Type.TRANSFORM, p, TRANSFORM_ANIM + 10, 1F);
        play(p, ModRegistry.TRANSFORM.get(), 1.4F, 1F);
        p.displayClientMessage(a.displayName().copy().withStyle(s -> s.withColor(a.color).withBold(true)), true);
        Net.sync(p);
    }

    public static void revert(ServerPlayer p, boolean timeout) {
        OmniData d = OmniCapability.get(p);
        if (!d.transformed()) return;
        removeStats(p);
        d.alien = -1;
        d.timer = 0;
        d.clearStates();
        d.detransformTime = p.level().getGameTime();
        if (timeout) d.recharge = OmniData.RECHARGE_TIME;
        if (!p.isCreative() && !p.isSpectator()) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.onUpdateAbilities();
        }
        p.removeEffect(MobEffects.INVISIBILITY);
        p.fallDistance = 0;
        Fx.follow(p.serverLevel(), Fx.Type.TRANSFORM, p, 24, timeout ? -1F : 0F);
        play(p, timeout ? ModRegistry.TIMEOUT.get() : ModRegistry.REVERT.get(), 1.2F, 1F);
        if (timeout) p.displayClientMessage(Component.translatable("msg.omnitrix.timeout"), true);
        Net.sync(p);
    }

    private static void applyStats(Player p, Alien a) {
        mod(p, Attributes.MAX_HEALTH, HEALTH, a.health, AttributeModifier.Operation.ADDITION);
        mod(p, Attributes.MOVEMENT_SPEED, SPEED, a.speed, AttributeModifier.Operation.ADDITION);
        mod(p, Attributes.ATTACK_DAMAGE, DAMAGE, a.damage, AttributeModifier.Operation.ADDITION);
        mod(p, Attributes.ARMOR, ARMOR, a.armor, AttributeModifier.Operation.ADDITION);
        double kb = switch (a) { case FOURARMS -> 0.7; case DIAMONDHEAD -> 0.5; case TERRASPIN -> 0.3; default -> 0; };
        if (kb > 0) mod(p, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, kb, AttributeModifier.Operation.ADDITION);
        double step = switch (a) { case XLR8 -> 0.65; case FOURARMS -> 0.5; default -> 0; };
        if (step > 0) mod(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP, step, AttributeModifier.Operation.ADDITION);
    }

    private static void removeStats(Player p) {
        for (Attribute at : new Attribute[]{Attributes.MAX_HEALTH, Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.ARMOR,
                Attributes.KNOCKBACK_RESISTANCE, ForgeMod.STEP_HEIGHT_ADDITION.get()}) {
            AttributeInstance ins = p.getAttribute(at);
            if (ins == null) continue;
            for (UUID id : new UUID[]{HEALTH, SPEED, DAMAGE, ARMOR, KNOCKBACK, STEP}) ins.removeModifier(id);
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    private static void mod(Player p, Attribute at, UUID id, double v, AttributeModifier.Operation op) {
        AttributeInstance ins = p.getAttribute(at);
        if (ins == null) return;
        ins.removeModifier(id);
        ins.addTransientModifier(new AttributeModifier(id, "omnitrix", v, op));
    }

    static void play(ServerPlayer p, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.blockPosition(), s, SoundSource.PLAYERS, vol, pitch);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        OmniData d = OmniCapability.get(p);
        if (d.recharge > 0 && --d.recharge == 0) {
            play(p, ModRegistry.BEEP.get(), 0.8F, 1.4F);
            Net.sync(p);
        }
        if (!d.transformed()) return;
        // transient modifiers are lost on relog/dimension change: keep them applied
        if (p.tickCount % 20 == 0) applyStats(p, d.current());
        if (!p.isCreative()) d.timer--;
        if (d.timer <= 200 && d.timer > 0 && d.timer % 20 == 0) play(p, ModRegistry.BEEP.get(), 0.7F, d.timer <= 60 ? 1.8F : 1.3F);
        if (d.timer <= 0) revert(p, true);
    }

    @SubscribeEvent
    public static void onTrack(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof Player other && e.getEntity() instanceof ServerPlayer viewer) Net.appearanceTo(viewer, other);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            OmniData d = OmniCapability.get(p);
            if (d.transformed()) applyStats(p, d.current());
            Net.sync(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Net.sync(p);
    }

    @SubscribeEvent
    public static void onDim(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Net.sync(p);
    }
}
