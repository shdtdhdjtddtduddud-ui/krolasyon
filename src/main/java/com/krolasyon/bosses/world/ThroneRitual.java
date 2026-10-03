package com.krolasyon.bosses.world;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.entity.HellMob;
import com.krolasyon.bosses.faction.Faction;
import com.krolasyon.bosses.registry.ModBlocks;
import com.krolasyon.bosses.registry.ModEntities;
import com.krolasyon.bosses.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The sealed wall in the Throne Hall melts for anyone who carries the sigils of three houses; behind it waits the Ash Sovereign. */
@Mod.EventBusSubscriber(modid = KrolasyonBosses.MODID)
public final class ThroneRitual {
    private ThroneRitual() {}

    public static final int SIGILS_NEEDED = 3;

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer sp) || !(e.getLevel() instanceof ServerLevel level)) return;
        if (!level.getBlockState(e.getPos()).is(ModBlocks.get("hellgate_stone"))) return;
        List<BlockPos> wall = flood(level, e.getPos());
        if (wall.size() < 40) return;   // an ordinary portal frame
        e.setCanceled(true);
        int have = 0;
        for (Faction f : Faction.HOUSES) {
            ItemStack s = new ItemStack(ModItems.MATERIALS.get(f.id + "_sigil").get());
            for (ItemStack inv : sp.getInventory().items) if (inv.is(s.getItem())) { have++; break; }
        }
        if (have < SIGILS_NEEDED && !sp.isCreative()) {
            sp.displayClientMessage(Component.translatable("msg.seal.locked", have, SIGILS_NEEDED), true);
            level.playSound(null, e.getPos(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6F, 0.5F);
            return;
        }
        int minY = Integer.MAX_VALUE;
        for (BlockPos p : wall) minY = Math.min(minY, p.getY());
        for (BlockPos p : wall) {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            if (level.random.nextInt(3) == 0) level.sendParticles(ParticleTypes.EXPLOSION, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 1, 0.2, 0.2, 0.2, 0);
            if (level.random.nextInt(2) == 0) level.sendParticles(ParticleTypes.LAVA, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0);
        }
        level.playSound(null, e.getPos(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.playSound(null, e.getPos(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.5F, 0.6F);
        sp.server.getPlayerList().broadcastSystemMessage(Component.translatable("msg.seal.broken", sp.getDisplayName()), false);
        // the Sovereign rises behind the wall, on the side away from the player
        Direction away = Direction.getNearest(e.getPos().getX() + 0.5 - sp.getX(), 0, e.getPos().getZ() + 0.5 - sp.getZ());
        BlockPos at = new BlockPos(e.getPos().getX(), minY, e.getPos().getZ()).relative(away, 6);
        boolean exists = !level.getEntitiesOfClass(HellMob.class, new net.minecraft.world.phys.AABB(at).inflate(48), m -> m.getType() == ModEntities.MOBS.get("ash_sovereign").get()).isEmpty();
        if (!exists) {
            HellMob boss = ModEntities.MOBS.get("ash_sovereign").get().create(level);
            if (boss != null) {
                boss.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, away.getOpposite().toYRot(), 0);
                level.addFreshEntity(boss);
            }
        }
    }

    private static List<BlockPos> flood(ServerLevel level, BlockPos start) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start);
        var stone = ModBlocks.get("hellgate_stone");
        while (!q.isEmpty() && out.size() <= 400) {
            BlockPos p = q.poll();
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n) && level.getBlockState(n).is(stone)) q.add(n);
            }
        }
        return out;
    }
}
