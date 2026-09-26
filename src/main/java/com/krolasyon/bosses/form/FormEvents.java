package com.krolasyon.bosses.form;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormEvents {
    private FormEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer sp) DemonForm.tick(sp);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && DemonForm.hasSavedForm(sp)) DemonForm.restore(sp);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DemonForm.forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (DemonForm.hasSavedForm(sp) && !event.isEndConquered()) sp.getPersistentData().remove("krolasyon_demon_form");
            if (DemonForm.isDemon(sp)) DemonForm.restore(sp);
            else DemonForm.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && DemonForm.isDemon(sp)) {
            DemonForm.sync(sp);
            DemonForm.sendCooldowns(sp);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer && DemonForm.isDemon(target)) {
            DemonForm.syncTo(target, viewer);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && DemonForm.isDemon(sp)) {
            DemonForm.sound(sp, ModSounds.DEMON_DEATH.get(), 2.0F, 1.1F);
            DemonForm.revert(sp, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttacked(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !DemonForm.isDemon(sp)) return;
        DamageSource src = event.getSource();
        if (src.is(DamageTypeTags.IS_FIRE) || DemonForm.isInvulnerable(sp) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && DemonForm.isDemon(sp)) {
            if (event.getDistance() > 6) {
                sp.serverLevel().sendParticles(DemonForm.dust(DemonForm.SHADOW, 2.0F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 20, 0.8, 0.05, 0.8, 0);
                DemonForm.sound(sp, ModSounds.DEMON_STEP.get(), 1.5F, 0.6F);
            }
            event.setDistance(0);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource src = event.getSource();
        if (src.getEntity() instanceof ServerPlayer sp && src.getDirectEntity() == sp && src.is(DamageTypes.PLAYER_ATTACK)
                && DemonForm.isDemon(sp) && victim != sp) {
            DemonForm.onMelee(sp, victim, event.getAmount());
        }
        if (victim instanceof ServerPlayer sp && DemonForm.isDemon(sp) && event.getAmount() > 0.5F) {
            DemonForm.sound(sp, ModSounds.DEMON_HURT.get(), 0.8F, 1.1F + sp.getRandom().nextFloat() * 0.2F);
        }
    }
}
