package com.sololeveling.skill;

import com.sololeveling.entity.NpcEntity;
import com.sololeveling.entity.ShadowEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

import java.util.ArrayList;
import java.util.List;

public final class ShadowUtil {
    private ShadowUtil() {}

    public static List<ShadowEntity> owned(ServerPlayer sp) {
        List<ShadowEntity> out = new ArrayList<>();
        for (ServerLevel l : sp.getServer().getAllLevels())
            for (Entity e : l.getAllEntities())
                if (e instanceof ShadowEntity s && s.isAlive() && s.isOwnedBy(sp)) out.add(s);
        return out;
    }

    public static boolean isEnemy(ServerPlayer sp, LivingEntity e) {
        if (e == sp || !e.isAlive()) return false;
        if (e instanceof NpcEntity || e instanceof ShadowEntity || e instanceof net.minecraft.world.entity.player.Player) return false;
        if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
        return e instanceof Enemy;
    }

    public static List<LivingEntity> enemies(ServerPlayer sp, double r) {
        return sp.level().getEntitiesOfClass(LivingEntity.class, sp.getBoundingBox().inflate(r, Math.max(2, r * 0.5), r), e -> isEnemy(sp, e));
    }
}
