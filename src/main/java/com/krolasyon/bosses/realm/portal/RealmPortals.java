package com.krolasyon.bosses.realm.portal;

import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.RealmConfig;
import com.krolasyon.bosses.realm.registry.RealmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

public final class RealmPortals {
    private RealmPortals() {}

    private static final String LAST = "krolasyon_portal_last", COUNT = "krolasyon_portal_count", LOCK = "krolasyon_portal_lock";

    public static void onEntityInside(Entity entity, BlockPos pos, Direction.Axis axis) {
        CompoundTag d = entity.getPersistentData();
        long now = entity.level().getGameTime();
        long last = d.getLong(LAST);
        if (last == now) return;
        if (now - last > 4) {
            d.putInt(COUNT, 0);
            d.putBoolean(LOCK, false);
        }
        d.putLong(LAST, now);
        if (d.getBoolean(LOCK)) return;
        int c = d.getInt(COUNT) + 1;
        d.putInt(COUNT, c);
        int need = entity instanceof Player p ? (p.getAbilities().invulnerable ? 2 : RealmConfig.PORTAL_WAIT.get()) : 4;
        if (entity instanceof ServerPlayer sp && c == 1) {
            sp.playNotifySound(net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER, net.minecraft.sounds.SoundSource.AMBIENT, 0.6F, 0.6F);
        }
        if (c >= need) {
            d.putBoolean(LOCK, true);
            teleport(entity, pos, axis);
        }
    }

    public static void teleport(Entity entity, BlockPos from, Direction.Axis axis) {
        if (!(entity.level() instanceof ServerLevel src)) return;
        ResourceKey<Level> destKey = src.dimension() == Realm.REALM ? Level.OVERWORLD : Realm.REALM;
        ServerLevel dest = src.getServer().getLevel(destKey);
        if (dest == null) return;
        PortalLinks.get(src).add(lowest(src, from));
        BlockPos approx = new BlockPos(from.getX(), 64, from.getZ());
        PortalLinks links = PortalLinks.get(dest);
        BlockPos target = links.nearest(dest, approx, 128, true);
        if (target == null) {
            int y = PortalShape.surfaceY(dest, from.getX(), from.getZ());
            BlockState platform = destKey == Realm.REALM ? RealmBlocks.INFERNAL_BRICKS.get().defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState();
            target = PortalShape.build(dest, new BlockPos(from.getX(), y, from.getZ()), axis, platform);
            links.add(target);
        }
        target = lowest(dest, target);
        Vec3 spot = Vec3.atBottomCenterOf(target);
        entity.changeDimension(dest, new Teleporter(spot));
    }

    private static BlockPos lowest(Level l, BlockPos p) {
        int guard = 0;
        while (l.getBlockState(p.below()).is(RealmBlocks.REALM_PORTAL.get()) && guard++ < 24) p = p.below();
        return p;
    }

    record Teleporter(Vec3 spot) implements ITeleporter {
        @Override
        public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
            return repositionEntity.apply(false);
        }

        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
            return new PortalInfo(spot, Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) { return true; }
    }
}
