package com.krolasyon.sololeveling.system;

import com.krolasyon.sololeveling.command.SLCommand;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.entity.ShadowEntity;
import com.krolasyon.sololeveling.item.SLWeaponItem;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModItems;
import com.krolasyon.sololeveling.shadow.ShadowManager;
import com.krolasyon.sololeveling.world.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge game-bus listeners that drive the System. */
public final class SystemEvents {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        HunterData d = HunterCapability.get(p);
        Progression.applyAttributes(p, d);
        if (!d.flags.contains("first_join")) {
            d.flags.add("first_join");
            Progression.give(p, new ItemStack(ModItems.RUSTY_DAGGER.get()));
            Progression.give(p, new ItemStack(ModItems.HEALING_POTION.get(), 3));
            Progression.give(p, new ItemStack(ModItems.RETURN_STONE.get()));
            Progression.give(p, new ItemStack(ModItems.NEWSPAPER.get()));
            d.gold = 50;
            d.waypoints.add("seoul_plaza");
        }
        Sys.sync(p);
        if (!d.awakened) Scheduler.later(80, () -> {
            if (p.isAlive() && !p.hasDisconnected()) Net.to(p, new Net.Open("awaken", new CompoundTag()));
        });
        else Sys.notify(p, Sys.INFO, Sys.t("system.title"), Sys.t("welcome_back", d.level));
        WorldState.get(p.server).ensureOverworldPortal(p.server);
        if (d.shadowsOut) Scheduler.later(40, () -> {
            if (p.isAlive() && !p.hasDisconnected()) ShadowManager.summonAll(p);
        });
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) ShadowManager.onLogout(p);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        HunterData d = HunterCapability.get(p);
        Progression.applyAttributes(p, d);
        p.setHealth(p.getMaxHealth());
        d.mana = d.maxMana() * 0.5F;
        d.markDirty();
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        HunterData d = HunterCapability.get(p);
        ShadowManager.recall(p);
        if (d.shadowsOut) Scheduler.later(20, () -> {
            if (p.isAlive() && !p.hasDisconnected()) ShadowManager.summonAll(p);
        });
        Regions.onEnterDimension(p, e.getTo());
        d.markDirty();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        HunterData d = HunterCapability.get(p);
        for (var it = d.cooldowns.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            int v = en.getValue() - 1;
            if (v <= 0) it.remove();
            else en.setValue(v);
            if (v % 20 == 0) d.markDirty();
        }
        if (d.stealthTicks > 0) d.stealthTicks--;
        if (d.domainTicks > 0) {
            d.domainTicks--;
            if (p.tickCount % 5 == 0) {
                ServerLevel l = p.serverLevel();
                for (int i = 0; i < 6; i++) {
                    double a = p.getRandom().nextDouble() * Math.PI * 2, r = 2 + p.getRandom().nextDouble() * 6;
                    l.sendParticles(ParticleTypes.SQUID_INK, p.getX() + Math.cos(a) * r, p.getY() + 0.1, p.getZ() + Math.sin(a) * r, 1, 0, 0.05, 0, 0);
                }
            }
        }
        if (d.mapTravelCooldown > 0) d.mapTravelCooldown--;
        if (d.awakened && p.tickCount % 20 == 0) {
            float max = d.maxMana();
            if (d.mana < max) {
                d.mana = Math.min(max, d.mana + 1 + max * 0.012F + d.stat(Stat.INT) * 0.02F);
                d.markDirty();
            }
            if (d.mana > max) d.mana = max;
            // the System heals slowly over time with vitality
            if (p.getHealth() < p.getMaxHealth() && p.getFoodData().getFoodLevel() > 6)
                p.heal(d.stat(Stat.VIT) * 0.004F);
        }
        // running for the daily quest
        if (p.isSprinting() && !p.isPassenger()) {
            double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 2) {
                float acc = d.misc.getFloat("run") + (float) dist;
                if (acc >= 1) {
                    DailyQuest.progress(p, 3, (int) acc);
                    acc -= (int) acc;
                }
                d.misc.putFloat("run", acc);
            }
        }
        boolean crouch = p.isShiftKeyDown();
        boolean was = d.misc.getBoolean("crouch");
        if (crouch && !was && p.onGround()) DailyQuest.progress(p, 0, 1);
        if (crouch != was) d.misc.putBoolean("crouch", crouch);
        if (p.tickCount % 20 == 0) DailyQuest.tick(p, d);
        Regions.playerTick(p, d);
        if (d.dirty && p.tickCount % 4 == 0) Sys.sync(p);
    }

    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) DailyQuest.progress(p, 1, 1);
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof LivingEntity) DailyQuest.progress(p, 2, 1);
    }

    @SubscribeEvent
    public void onEffect(MobEffectEvent.Applicable e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getEffectInstance().getEffect() == MobEffects.POISON) {
            HunterData d = HunterCapability.get(p);
            if (d.awakened && d.hasSkill(Skill.DETOXIFICATION)) e.setResult(Event.Result.DENY);
        }
    }

    /** Dodge (perception) for players. */
    @SubscribeEvent
    public void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        HunterData d = HunterCapability.get(p);
        if (!d.awakened || e.getSource().getEntity() == null) return;
        double dodge = Math.min(0.25, d.stat(Stat.PER) * 0.0015 + d.stat(Stat.AGI) * 0.0005);
        if (p.getRandom().nextDouble() < dodge) {
            e.setCanceled(true);
            p.displayClientMessage(Sys.t("dodge"), true);
            p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        Entity src = e.getSource().getEntity();
        float amount = e.getAmount();
        // player dealing damage
        if (src instanceof ServerPlayer p) {
            HunterData d = HunterCapability.get(p);
            if (d.awakened) {
                ItemStack held = p.getMainHandItem();
                boolean direct = e.getSource().getDirectEntity() == p;
                if (direct && held.getItem() instanceof SLWeaponItem w && w.dagger && d.hasSkill(Skill.ADVANCED_DAGGER_ARTS)) amount *= 1.2F;
                if (d.stealthTicks > 0 && direct) {
                    amount *= 2F;
                    d.stealthTicks = 0;
                    p.removeEffect(MobEffects.INVISIBILITY);
                }
                double crit = Math.min(0.5, d.stat(Stat.PER) * 0.004);
                if (direct && p.getRandom().nextDouble() < crit) {
                    amount *= 1.5F;
                    p.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                }
                amount *= titleBonus(d, victim, p);
            }
        }
        if (src instanceof ShadowEntity s && s.getOwner() instanceof ServerPlayer op) {
            HunterData d = HunterCapability.get(op);
            if (d.title == Title.SHADOW_MONARCH) amount *= 1.2F;
        }
        // player taking damage
        if (victim instanceof ServerPlayer p) {
            HunterData d = HunterCapability.get(p);
            if (d.awakened) {
                if (d.hasSkill(Skill.TENACITY) && p.getHealth() < p.getMaxHealth() * 0.3F) amount *= 0.75F;
                if (d.title == Title.SHADOW_MONARCH) amount *= 0.9F;
            }
        }
        e.setAmount(amount);
    }

    private static float titleBonus(HunterData d, LivingEntity victim, ServerPlayer p) {
        float m = 1F;
        MobKind.Category cat = victim instanceof SLMonster sm ? sm.kind.category : null;
        switch (d.title) {
            case WOLF_ASSASSIN -> { if (cat == MobKind.Category.BEAST || victim instanceof net.minecraft.world.entity.animal.Wolf) m = 1.4F; }
            case DEMON_HUNTER -> { if (cat == MobKind.Category.DEMON) m = 1.4F; }
            case ANT_EXTERMINATOR -> { if (cat == MobKind.Category.INSECT || victim instanceof net.minecraft.world.entity.monster.Spider) m = 1.4F; }
            case KING_SLAYER -> { if (victim instanceof SLMonster sm && sm.kind.boss) m = 1.15F; }
            case ONE_WHO_OVERCAME -> { if (p.getHealth() < p.getMaxHealth() * 0.3F) m = 1.3F; }
            case SHADOW_MONARCH -> m = 1.1F;
            default -> {}
        }
        return m;
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        if (dead.level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        ServerPlayer killer = null;
        float share = 1F;
        if (src instanceof ServerPlayer p) killer = p;
        else if (src instanceof ShadowEntity s && s.getOwner() instanceof ServerPlayer p) {
            killer = p;
            share = 0.6F;
        } else if (src instanceof Projectile pr && pr.getOwner() instanceof ServerPlayer p) killer = p;
        if (dead instanceof ServerPlayer p) {
            ShadowManager.recall(p);
            Regions.onPlayerDeath(p);
            return;
        }
        if (killer == null) return;
        HunterData d = HunterCapability.get(killer);
        if (!d.awakened) return;
        Progression.addExp(killer, Math.round(Progression.expFor(dead) * share * (d.fatigue > 0 ? 1 : 1)));
        if (dead instanceof SLMonster m) {
            d.kills.merge(m.kind.id(), 1, Integer::sum);
            if (m.kind == MobKind.STEEL_FANGED_LYCAN && d.kills.get(m.kind.id()) >= 30 && d.titles.add(Title.WOLF_ASSASSIN))
                Sys.notify(killer, Sys.REWARD, Sys.t("title.acquired"), Sys.t("title.wolf_assassin"));
            if (m.kind.category == MobKind.Category.DEMON && totalDemons(d) >= 60 && d.titles.add(Title.DEMON_HUNTER))
                Sys.notify(killer, Sys.REWARD, Sys.t("title.acquired"), Sys.t("title.demon_hunter"));
            if (m.kind.boss && countBosses(d) >= 5 && d.titles.add(Title.KING_SLAYER))
                Sys.notify(killer, Sys.REWARD, Sys.t("title.acquired"), Sys.t("title.king_slayer"));
            if (m.kind.boss) killer.serverLevel().playSound(null, killer.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1F, 1F);
        }
        GuildManager.onKill(killer, dead);
        ShadowManager.onKill(killer, dead);
        Progression.checkQuest(killer);
        d.markDirty();
    }

    private static int totalDemons(HunterData d) {
        return d.kills.getOrDefault(MobKind.DEMON.id(), 0) + d.kills.getOrDefault(MobKind.CERBERUS.id(), 0) + d.kills.getOrDefault(MobKind.BARAN.id(), 0);
    }

    private static int countBosses(HunterData d) {
        int n = 0;
        for (MobKind k : MobKind.values()) if (k.boss) n += d.kills.getOrDefault(k.id(), 0);
        return n;
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Scheduler.tick();
        ShadowManager.tick();
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            GateManager.tick(server);
            NewsManager.get(server).tick(server);
            CityLife.tick(server);
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent e) {
        if (Boolean.getBoolean("sololeveling.servertest")) com.krolasyon.sololeveling.ServerSelfTest.start(e.getServer());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent e) { Scheduler.clear(); }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) { SLCommand.register(e.getDispatcher()); }
}
