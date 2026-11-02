package com.krolasyon.horror.world;

import com.krolasyon.horror.entity.HatmanEntity;
import com.krolasyon.horror.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Instead of ordinary mob spawning, a director decides when a player is "haunted": at night or deep in the dark,
 * after a quiet build-up, a single Hatman unfolds out of sight 26-40 blocks away. Never more than one per player.
 */
public final class HauntDirector {
    private HauntDirector() {}

    private static final Map<UUID, Integer> DREAD = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Player p = e.player;
        if (!(p.level() instanceof ServerLevel level) || p.tickCount % 100 != 0) return;
        if (p.isCreative() || p.isSpectator() || level.getDifficulty() == Difficulty.PEACEFUL || level.dimension() != Level.OVERWORLD) return;
        if (Boolean.getBoolean("krolasyon.autotest")) return;
        boolean inHouse = com.krolasyon.horror.registry.ModBlocks.isHouseFloor(level.getBlockState(p.blockPosition()).getBlock());
        boolean dark = inHouse || level.getMaxLocalRawBrightness(p.blockPosition()) <= 5;
        boolean underground = p.getY() < level.getSeaLevel() - 12 && !level.canSeeSky(p.blockPosition());
        if (!level.isNight() && !(underground && dark) && !inHouse) {
            DREAD.put(p.getUUID(), Math.max(0, DREAD.getOrDefault(p.getUUID(), 0) - 2));
            return;
        }
        if (!level.getEntitiesOfClass(HatmanEntity.class, new AABB(p.blockPosition()).inflate(160)).isEmpty()) return;
        int dread = DREAD.getOrDefault(p.getUUID(), 0) + (inHouse ? 4 : dark ? 2 : 1);
        DREAD.put(p.getUUID(), dread);
        // ~5 minutes of quiet build-up, then an increasing chance each check
        if (dread < 6 || p.getRandom().nextInt(Math.max(2, 22 - dread)) != 0) return;
        if (inHouse ? trySpawn(level, p, 8, 10) : trySpawn(level, p, 26, 14)) DREAD.put(p.getUUID(), 0);
    }

    public static boolean trySpawn(ServerLevel level, Player p, double minR, double spreadR) {
        HatmanEntity probe = ModEntities.HATMAN.get().create(level);
        if (probe == null) return false;
        for (int i = 0; i < 24; i++) {
            double ang = p.getRandom().nextDouble() * Math.PI * 2;
            double r = minR + p.getRandom().nextDouble() * spreadR;
            int x = (int) Math.floor(p.getX() + Math.cos(ang) * r), z = (int) Math.floor(p.getZ() + Math.sin(ang) * r);
            for (int dy = 8; dy >= -10; dy--) {
                BlockPos pos = new BlockPos(x, (int) p.getY() + dy, z);
                if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) continue;
                if (!level.getFluidState(pos).isEmpty()) continue;
                probe.moveTo(x + 0.5, pos.getY(), z + 0.5, p.getRandom().nextFloat() * 360F, 0F);
                if (!level.noCollision(probe)) continue;
                Vec3 head = new Vec3(x + 0.5, pos.getY() + 2.5, z + 0.5);
                if (level.clip(new ClipContext(p.getEyePosition(), head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, p)).getType() == HitResult.Type.MISS)
                    continue;  // must begin out of sight
                level.addFreshEntity(probe);
                return true;
            }
        }
        return false;
    }
}
