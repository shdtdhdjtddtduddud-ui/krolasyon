package com.sololeveling.event;

import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.NpcEntity;
import com.sololeveling.entity.SLMonster;
import com.sololeveling.entity.ShadowEntity;
import com.sololeveling.item.SLSwordItem;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.system.*;
import com.sololeveling.util.Ranks;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class CombatEvents {
    private CombatEvents() {}

    @SubscribeEvent
    public static void hurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        Entity src = e.getSource().getEntity();
        // player dodge via SENSE
        if (victim instanceof ServerPlayer vp && e.getSource().getEntity() != null) {
            SLPlayer vd = ModCaps.get(vp);
            double dodge = Math.min(0.25, (vd.sense() - 10) * 0.003);
            if (dodge > 0 && vp.getRandom().nextDouble() < dodge) {
                e.setCanceled(true);
                vp.displayClientMessage(Component.translatable("gui.sololeveling.dodge"), true);
                return;
            }
        }
        if (victim instanceof NpcEntity) { e.setCanceled(true); return; }
        // shadows empowered by owner INT
        if (src instanceof ShadowEntity sh) {
            Player o = sh.owner();
            if (o != null) e.setAmount(e.getAmount() * (1F + ModCaps.get(o).intel() * 0.01F));
            return;
        }
        if (!(src instanceof ServerPlayer sp)) return;
        SLPlayer d = ModCaps.get(sp);
        float amt = e.getAmount() * (float) Guilds.damageMultiplier(d);
        ItemStack held = sp.getMainHandItem();
        boolean melee = e.getSource().getDirectEntity() == sp;
        if (melee && held.getItem() instanceof SLSwordItem sw) {
            if (d.level < sw.def.level()) {
                amt *= 0.35F;
                sp.displayClientMessage(Component.translatable("gui.sololeveling.weapon_too_heavy", sw.def.level()), true);
            } else {
                amt = special(sp, victim, sw, amt);
            }
        }
        // critical strike via SENSE
        double crit = (d.sense() - 10) * 0.004 + d.level * 0.0004;
        if (melee && crit > 0 && sp.getRandom().nextDouble() < crit) {
            amt *= 1.6F;
            ((ServerLevel) victim.level()).sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
        }
        e.setAmount(amt);
    }

    private static float special(ServerPlayer sp, LivingEntity victim, SLSwordItem sw, float amt) {
        ServerLevel sl = (ServerLevel) victim.level();
        int p = sw.def.power();
        switch (sw.def.special()) {
            case "knight" -> { if (victim.getArmorValue() > 0 || victim instanceof SLMonster m && m.def.id().contains("knight")) amt += p; }
            case "frost" -> victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40 + p * 20, Math.min(3, p)));
            case "poison" -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60 + p * 20, 1));
            case "knockback" -> victim.knockback(p * 0.6, Math.sin(sp.getYRot() * Math.PI / 180.0), -Math.cos(sp.getYRot() * Math.PI / 180.0));
            case "bleed" -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 40 + p * 20, 0));
            case "lifesteal" -> sp.heal(amt * p / 100F);
            case "fire" -> victim.setSecondsOnFire(p);
            case "inferno" -> {
                victim.setSecondsOnFire(p);
                for (LivingEntity o : sl.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(3), x -> ShadowUtil_isEnemy(sp, x) && x != victim)) {
                    o.hurt(sp.damageSources().onFire(), amt * 0.4F);
                    o.setSecondsOnFire(4);
                }
                sl.sendParticles(ParticleTypes.FLAME, victim.getX(), victim.getY() + 1, victim.getZ(), 25, 0.8, 0.5, 0.8, 0.05);
            }
            case "shadow" -> {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + 1, victim.getZ(), 20, 0.6, 0.6, 0.6, 0.08);
                for (LivingEntity o : sl.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(3.5), x -> ShadowUtil_isEnemy(sp, x) && x != victim))
                    o.hurt(sp.damageSources().magic(), amt * 0.35F);
                amt *= 1.1F;
            }
            default -> { }
        }
        return amt;
    }

    private static boolean ShadowUtil_isEnemy(ServerPlayer sp, LivingEntity e) { return com.sololeveling.skill.ShadowUtil.isEnemy(sp, e); }

    @SubscribeEvent
    public static void death(LivingDeathEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || v instanceof Player) return;
        Entity src = e.getSource().getEntity();
        ServerPlayer killer = null;
        double share = 1.0;
        if (src instanceof ServerPlayer sp) killer = sp;
        else if (src instanceof ShadowEntity sh && sh.owner() instanceof ServerPlayer o) { killer = o; share = 0.6; }
        else if (src instanceof net.minecraft.world.entity.projectile.Projectile pr && pr.getOwner() instanceof ServerPlayer sp2) killer = sp2;
        else if (src instanceof net.minecraft.world.entity.projectile.Projectile pr2 && pr2.getOwner() instanceof ShadowEntity sh2 && sh2.owner() instanceof ServerPlayer o2) { killer = o2; share = 0.6; }
        if (!(v instanceof Enemy)) return;
        if (killer == null) {
            // also let nearby players harvest corpses of monsters killed by others
            Player near = v.level().getNearestPlayer(v, 30);
            if (near != null) Corpses.add(v);
            return;
        }
        Corpses.add(v);
        SLPlayer d = ModCaps.get(killer);
        long xp;
        int mi;
        if (v instanceof SLMonster m) { xp = m.def.xp(); mi = Ranks.index(m.def.rank()); }
        else { xp = Math.max(1, (long) (v.getMaxHealth() * 0.6)); mi = XpHandler.rankIdxForHealth(v.getMaxHealth()); }
        int pi = Ranks.index(d.rank());
        if (pi > mi) xp = (long) (xp * Math.pow(0.55, pi - mi));
        xp = (long) (xp * share);
        XpHandler.giveXp(killer, Math.max(1, xp));
        Quests.onKill(killer, v);
        Loot.dropFor(v, true, killer.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.LUCK));
        PlayerSync.sync(killer);
    }
}
