package com.krolasyon.bosses.realm.item;

import com.krolasyon.bosses.KrolasyonBosses;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID)
public final class WeaponEvents {
    private WeaponEvents() {}

    static RealmWeapon.Kind held(Player p) {
        return p.getMainHandItem().getItem() instanceof RealmWeapon w ? w.kind : null;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getSource().getDirectEntity() instanceof Player p) || e.getSource().getEntity() != p) return;
        RealmWeapon.Kind k = held(p);
        if (k == null) return;
        LivingEntity t = e.getEntity();
        switch (k) {
            case BLOODTHIRSTER -> {
                p.heal(e.getAmount() * 0.25F);
                if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, t.getX(), t.getY() + 1, t.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
            }
            case SOVEREIGN -> p.heal(e.getAmount() * 0.1F);
            case SHADOWFANG -> {
                Vec3 look = t.getLookAngle().multiply(1, 0, 1);
                Vec3 to = t.position().subtract(p.position()).multiply(1, 0, 1);
                if (look.lengthSqr() > 1.0E-4 && to.lengthSqr() > 1.0E-4 && look.normalize().dot(to.normalize()) > 0.4) {
                    e.setAmount(e.getAmount() * 2F);
                    if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + 1, t.getZ(), 15, 0.3, 0.4, 0.3, 0.2);
                }
            }
            default -> {}
        }
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent e) {
        if (e.getSource().getEntity() instanceof Player p && held(p) == RealmWeapon.Kind.REAPER) {
            p.heal(4F);
            if (p.level() instanceof ServerLevel sl) {
                LivingEntity v = e.getEntity();
                sl.sendParticles(ParticleTypes.SOUL, v.getX(), v.getY() + 1, v.getZ(), 12, 0.3, 0.5, 0.3, 0.05);
            }
        }
    }
}
