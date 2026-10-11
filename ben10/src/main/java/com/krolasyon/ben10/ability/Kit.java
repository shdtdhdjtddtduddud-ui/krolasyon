package com.krolasyon.ben10.ability;

import com.krolasyon.ben10.OmniData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/** One alien's power set. Server-side unless noted. */
public abstract class Kit {
    /** ability started (cooldown already applied). Return false to refuse (cooldown refunded). */
    public abstract boolean start(ServerPlayer p, OmniData d, int slot);

    /** called every tick while the ability runs, t = 1.. duration */
    public abstract void tick(ServerPlayer p, OmniData d, int slot, int t);

    /** ability finished or was interrupted */
    public void end(ServerPlayer p, OmniData d, int slot) {}

    /** every server tick while transformed */
    public void passive(ServerPlayer p, OmniData d) {}

    /** both sides every tick while transformed; movement for the local player happens here on the client */
    public void common(Player p, OmniData d) {}

    /** the alien hit something in melee (amount may be changed) */
    public void melee(ServerPlayer p, OmniData d, LivingEntity target, LivingHurtEvent e) {}

    /** an attack is about to hit the alien while guarding; return true to cancel it completely */
    public boolean guardCancel(ServerPlayer p, OmniData d, DamageSource src) {
        return false;
    }

    /** damage taken while guarding was reduced; attacker may be null */
    public void guardHit(ServerPlayer p, OmniData d, DamageSource src, LivingEntity attacker) {}

    /** passive damage immunity (fire, phasing...) */
    public boolean immune(ServerPlayer p, OmniData d, DamageSource src) {
        return false;
    }

    public void land(Player p, OmniData d, float distance) {}
}
