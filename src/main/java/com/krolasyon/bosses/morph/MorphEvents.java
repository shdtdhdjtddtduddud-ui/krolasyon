package com.krolasyon.bosses.morph;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MorphEvents {
    private MorphEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.player instanceof ServerPlayer sp) MorphServer.tick(sp);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            MorphServer.applyAttributes(sp, MorphServer.isMorphed(sp));
            MorphServer.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        MorphServer.forget(e.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            MorphServer.forget(sp);
            MorphServer.applyAttributes(sp, MorphServer.isMorphed(sp));
            MorphServer.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) MorphServer.sync(sp);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath()) MorphServer.copyOnClone(e.getOriginal(), e.getEntity());
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (e.getTarget() instanceof ServerPlayer target && e.getEntity() instanceof ServerPlayer viewer) MorphServer.syncTo(target, viewer);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof Player p) || !MorphServer.isMorphed(p)) return;
        int f = MorphServer.form(p);
        if (!p.level().isClientSide() && e.getDistance() > 3.5F) {
            ServerLevel sl = (ServerLevel) p.level();
            if (f == Forms.AIGOAR) {
                MorphServer.sound(p, ModSounds.AIGOAR_LAND.get(), Math.min(1.6F, 0.5F + e.getDistance() * 0.05F), 1.0F);
                MorphServer.ring(sl, p.position().add(0, 0.1, 0), 1.3, 24, ParticleTypes.SPLASH, 0.15);
            } else {
                MorphServer.sound(p, ModSounds.DIVE.get(), Math.min(1.2F, 0.3F + e.getDistance() * 0.04F), 1.3F);
                MorphServer.ring(sl, p.position().add(0, 0.1, 0), 1.3, 24, f == Forms.SHADE ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, 0.05);
            }
            sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.5, 0.05, 0.5, 0.03);
        }
        e.setDistance(0F);
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp && MorphServer.isMorphed(sp)) {
            ServerLevel sl = sp.serverLevel();
            sl.sendParticles(ParticleTypes.SPLASH, sp.getX(), sp.getY() + 1, sp.getZ(), 120, 0.8, 1.0, 0.8, 0.3);
            sl.sendParticles(ParticleTypes.BUBBLE_POP, sp.getX(), sp.getY() + 1, sp.getZ(), 40, 0.8, 1.0, 0.8, 0.05);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        Player p = e.getEntity();
        int f = MorphServer.form(p);
        if (p.level().isClientSide() || f < 0 || !(e.getTarget() instanceof LivingEntity t)) return;
        ServerLevel sl = (ServerLevel) p.level();
        boolean full = p.getAttackStrengthScale(0.5F) > 0.9F;
        switch (f) {
            case Forms.AIGOAR -> {
                sl.sendParticles(ParticleTypes.SPLASH, t.getX(), t.getY(0.6), t.getZ(), 18, 0.3, 0.3, 0.3, 0.25);
                sl.sendParticles(MorphServer.dust(0x3FE0E6, 1.4F), t.getX(), t.getY(0.6), t.getZ(), 8, 0.3, 0.4, 0.3, 0);
                if (full) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
            case Forms.SHADE -> {
                sl.sendParticles(MorphServer.dust(0xD01828, 1.2F), t.getX(), t.getY(0.6), t.getZ(), 12, 0.3, 0.4, 0.3, 0);
                sl.sendParticles(ParticleTypes.SMOKE, t.getX(), t.getY(0.6), t.getZ(), 6, 0.2, 0.3, 0.2, 0.02);
                if (full) t.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            }
            default -> {
                sl.sendParticles(ParticleTypes.FLAME, t.getX(), t.getY(0.6), t.getZ(), 16, 0.3, 0.4, 0.3, 0.06);
                sl.sendParticles(ParticleTypes.LAVA, t.getX(), t.getY(0.6), t.getZ(), 2, 0.2, 0.2, 0.2, 0);
                t.setSecondsOnFire(full ? 5 : 2);
            }
        }
    }

    /** form buffs: the Dragon's Scale Aegis burns attackers, the Shade's awakening drains life */
    @SubscribeEvent
    public static void onHurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if (e.getEntity().level().isClientSide()) return;
        if (e.getEntity() instanceof ServerPlayer victim && MorphServer.form(victim) == Forms.DRAGON && MorphServer.state(victim).buffTicks > 0) {
            e.setAmount(e.getAmount() * 0.6F);
            if (e.getSource().getEntity() instanceof LivingEntity att && att != victim) {
                att.setSecondsOnFire(4);
                att.hurt(victim.damageSources().thorns(victim), 3F);
            }
        }
        if (e.getSource().getEntity() instanceof ServerPlayer att && MorphServer.form(att) == Forms.SHADE && MorphServer.state(att).buffTicks > 0) {
            att.heal(e.getAmount() * 0.25F);
        }
    }

    // ------------------------------------------------------------------ voice & footsteps of the tide lord
    @SubscribeEvent
    public static void onSoundAtPos(PlayLevelSoundEvent.AtPosition e) {
        Holder<SoundEvent> h = e.getSound();
        if (h == null || !isPlayerSound(h.value())) return;
        Vec3 pos = e.getPosition();
        Level level = e.getLevel();
        List<Player> ps = level.getEntitiesOfClass(Player.class, new AABB(pos, pos).inflate(0.05), MorphServer::isMorphed);
        if (ps.isEmpty()) return;
        SoundEvent repl = replacement(h.value(), MorphServer.form(ps.get(0)));
        if (repl != null) swap(e, repl);
    }

    @SubscribeEvent
    public static void onSoundAtEntity(PlayLevelSoundEvent.AtEntity e) {
        Holder<SoundEvent> h = e.getSound();
        if (h == null || !(e.getEntity() instanceof Player p) || !MorphServer.isMorphed(p)) return;
        SoundEvent repl = replacement(h.value(), MorphServer.form(p));
        if (repl != null) swap(e, repl);
    }

    private static void swap(PlayLevelSoundEvent e, SoundEvent repl) {
        e.setSound(ForgeRegistries.SOUND_EVENTS.getHolder(repl).orElse(Holder.direct(repl)));
        if (repl == ModSounds.AIGOAR_STEP.get() || repl.getLocation().getPath().endsWith("_step")) e.setNewVolume(Math.max(0.35F, e.getOriginalVolume()));
        e.setSource(SoundSource.PLAYERS);
    }

    private static boolean isPlayerSound(SoundEvent s) {
        ResourceLocation id = s.getLocation();
        if (!"minecraft".equals(id.getNamespace())) return false;
        String p = id.getPath();
        return p.startsWith("entity.player.") || (p.endsWith(".step") && p.startsWith("block."));
    }

    @Nullable
    private static SoundEvent replacement(SoundEvent s, int form) {
        if (form < 0) return null;
        ResourceLocation id = s.getLocation();
        if (!"minecraft".equals(id.getNamespace())) return null;
        String p = id.getPath();
        if (p.startsWith("entity.player.hurt")) return Forms.hurt(form);
        if (p.equals("entity.player.death")) return Forms.death(form);
        if (p.startsWith("entity.player.attack.")) return Forms.swing(form);
        if (p.equals("entity.player.big_fall") || p.equals("entity.player.small_fall")) return form == Forms.AIGOAR ? ModSounds.AIGOAR_LAND.get() : ModSounds.DIVE.get();
        if (p.endsWith(".step") && p.startsWith("block.")) return Forms.step(form);
        return null;
    }
}
