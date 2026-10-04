package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.world.Race;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.List;

public final class NpcFactory {
    private NpcFactory() {}

    @Nullable
    public static RpgNpc spawn(ServerLevel level, BlockPos pos, Race race, boolean female, NpcRole role, int kingdom, @Nullable String name, @Nullable String storyId) {
        RpgNpc n = RpgEntities.NPC.get().create(level);
        if (n == null) return null;
        n.setup(race, female, role, kingdom, level.random);
        if (name != null) n.setCustomName(Component.literal(name));
        if (storyId != null) n.storyId = storyId;
        n.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360, 0);
        n.home = pos;
        level.addFreshEntity(n);
        return n;
    }

    @Nullable
    public static RpgNpc findStory(ServerLevel level, BlockPos near, String storyId, double radius) {
        List<RpgNpc> l = level.getEntitiesOfClass(RpgNpc.class, new net.minecraft.world.phys.AABB(near).inflate(radius), n -> storyId.equals(n.storyId) && n.isAlive());
        return l.isEmpty() ? null : l.get(0);
    }
}
