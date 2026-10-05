package com.sololeveling.system;

import com.sololeveling.entity.ShadowEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.List;
import java.util.function.Function;

public final class Travel {
    private Travel() {}

    /** Teleports a player (and nearby owned shadows) to any dimension/position. */
    public static void teleport(ServerPlayer sp, ServerLevel dest, double x, double y, double z, float yaw) {
        ServerLevel from = sp.serverLevel();
        List<ShadowEntity> shadows = from.getEntitiesOfClass(ShadowEntity.class, sp.getBoundingBox().inflate(40), s -> s.isOwnedBy(sp));
        dest.getChunk(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
        sp.teleportTo(dest, x, y, z, yaw, 0.0F);
        sp.setDeltaMovement(Vec3.ZERO);
        sp.fallDistance = 0;
        int i = 0;
        for (ShadowEntity s : shadows) {
            double ox = x + Math.cos(i * 1.7) * 2.5, oz = z + Math.sin(i * 1.7) * 2.5;
            i++;
            moveEntity(s, dest, ox, y, oz);
        }
    }

    public static void moveEntity(Entity e, ServerLevel dest, double x, double y, double z) {
        if (e.level() == dest) {
            e.teleportTo(x, y, z);
            return;
        }
        final Vec3 pos = new Vec3(x, y, z);
        e.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(pos, Vec3.ZERO, entity.getYRot(), entity.getXRot());
            }

            @Override
            public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                return repositionEntity.apply(false);
            }
        });
    }

    public static int surfaceY(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}
