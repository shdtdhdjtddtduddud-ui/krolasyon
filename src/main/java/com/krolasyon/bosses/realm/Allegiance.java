package com.krolasyon.bosses.realm;

import com.krolasyon.bosses.entity.BossEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Who may hurt whom: shared by spells, weapons, mobs, bosses and their projectiles. */
public final class Allegiance {
    private Allegiance() {}

    public interface Member {
        /** true if this entity should be harmed by area attacks of {@code attacker} */
        boolean isHostileTo(Entity other);
    }

    public static boolean hostile(LivingEntity attacker, Entity victim) {
        if (!(victim instanceof LivingEntity lv) || victim == attacker || !victim.isAlive()) return false;
        if (attacker instanceof BossEntity boss) return boss.isHostileTo(victim);
        if (attacker instanceof Member m) return m.isHostileTo(victim);
        if (attacker instanceof Player p) return playerMayHit(p, lv);
        if (attacker instanceof Mob mob) return mob.getTarget() == victim || (victim instanceof Mob vm && vm.getTarget() == attacker) || victim instanceof Player;
        return true;
    }

    /** spells and weapon area attacks of a player never hit other players, their pets, their summons or allied kingdoms */
    public static boolean playerMayHit(Player p, LivingEntity v) {
        if (v instanceof Player) return false;
        if (v instanceof TamableAnimal t && t.isTame()) return false;
        if (v instanceof Member) return ((Member) v).isHostileTo(p);
        if (v instanceof BossEntity) return true;
        if (v instanceof Enemy) return true;
        return v instanceof Mob m && m.getTarget() == p;
    }
}
