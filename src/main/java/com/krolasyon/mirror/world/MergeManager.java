package com.krolasyon.mirror.world;

import com.krolasyon.mirror.MirrorWorld;
import com.krolasyon.mirror.block.MirrorBlockEntity;
import com.krolasyon.mirror.entity.ReflectionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * When a mirror breaks the two worlds merge: every block the reflection changed in the mirror dimension is copied,
 * block entities included, to the same coordinates of the world the mirror stood in, and the reflection steps out.
 */
public final class MergeManager {
    private static final int BLOCKS_PER_TICK = 30;
    private static final List<Merge> ACTIVE = new ArrayList<>();

    private static final class Merge {
        final ResourceKey<Level> dim;
        final long[] positions;
        final BlockPos mirrorPos;
        final Direction facing;
        @Nullable final UUID owner;
        final String ownerName;
        @Nullable final BlockPos home;
        int index;
        int age;

        Merge(ResourceKey<Level> dim, long[] positions, BlockPos mirrorPos, Direction facing, @Nullable UUID owner, String ownerName, @Nullable BlockPos home) {
            this.dim = dim;
            this.positions = positions;
            this.mirrorPos = mirrorPos;
            this.facing = facing;
            this.owner = owner;
            this.ownerName = ownerName;
            this.home = home;
        }
    }

    public static void mirrorBroken(ServerLevel level, BlockPos pos, Direction facing, MirrorBlockEntity be) {
        if (level.dimension() == MirrorWorld.MIRROR_DIM) return;
        ACTIVE.add(new Merge(level.dimension(), be.changedPositions(), pos.immutable(), facing, be.getOwner(), be.getOwnerName(), be.homePos()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;
        MinecraftServer server = event.getServer();
        Iterator<Merge> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Merge m = it.next();
            ServerLevel target = server.getLevel(m.dim);
            ServerLevel source = server.getLevel(MirrorWorld.MIRROR_DIM);
            if (target == null || source == null) {
                it.remove();
                continue;
            }
            if (step(m, target, source)) it.remove();
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        // finish pending merges synchronously so nothing is lost on shutdown
        MinecraftServer server = event.getServer();
        for (Merge m : ACTIVE) {
            ServerLevel target = server.getLevel(m.dim);
            ServerLevel source = server.getLevel(MirrorWorld.MIRROR_DIM);
            if (target == null || source == null) continue;
            int guard = 0;
            while (!step(m, target, source) && guard++ < 100000) { }
        }
        ACTIVE.clear();
    }

    /** @return true when finished */
    private static boolean step(Merge m, ServerLevel target, ServerLevel source) {
        double cx = m.mirrorPos.getX() + 0.5, cy = m.mirrorPos.getY() + 1.0, cz = m.mirrorPos.getZ() + 0.5;
        if (m.age++ == 0) {
            target.playSound(null, m.mirrorPos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
            target.playSound(null, m.mirrorPos, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.BLOCKS, 2.0F, 0.7F);
            target.playSound(null, m.mirrorPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 0.6F);
            target.sendParticles(ParticleTypes.FLASH, cx, cy, cz, 2, 0, 0, 0, 0);
            target.sendParticles(ParticleTypes.REVERSE_PORTAL, cx, cy, cz, 300, 0.6, 1.0, 0.6, 0.15);
            target.sendParticles(ParticleTypes.END_ROD, cx, cy, cz, 60, 0.4, 0.8, 0.4, 0.08);
        }
        int end = Math.min(m.positions.length, m.index + BLOCKS_PER_TICK);
        for (; m.index < end; m.index++) {
            BlockPos p = BlockPos.of(m.positions[m.index]);
            BlockState state = source.getBlockState(p);
            BlockEntity sbe = source.getBlockEntity(p);
            CompoundTag nbt = sbe != null ? sbe.saveWithoutMetadata() : null;
            target.setBlock(p, state, 2 | 16);
            if (nbt != null) {
                BlockEntity tbe = target.getBlockEntity(p);
                if (tbe != null) {
                    tbe.load(nbt);
                    tbe.setChanged();
                }
            }
            if (m.index % 5 == 0 && !state.isAir()) {
                target.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.4, 0.4, 0.4, 0.02);
            }
        }
        if (m.index % 90 == 0 && m.index > 0) {
            target.playSound(null, BlockPos.of(m.positions[m.index - 1]), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        if (m.index < m.positions.length) return false;
        finish(m, target);
        return true;
    }

    private static void finish(Merge m, ServerLevel target) {
        ReflectionEntity reflection = MirrorWorld.REFLECTION.get().create(target);
        if (reflection != null) {
            reflection.moveTo(m.mirrorPos.getX() + 0.5, m.mirrorPos.getY(), m.mirrorPos.getZ() + 0.5, m.facing.toYRot(), 0);
            reflection.setYHeadRot(m.facing.toYRot());
            reflection.setYBodyRot(m.facing.toYRot());
            reflection.setOwner(m.owner, m.ownerName);
            if (m.home != null) reflection.setHome(m.home);
            reflection.finalizeSpawn(target, target.getCurrentDifficultyAt(m.mirrorPos), MobSpawnType.EVENT, null, null);
            target.addFreshEntity(reflection);
        }
        target.playSound(null, m.mirrorPos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2.0F, 0.9F);
        target.sendParticles(ParticleTypes.END_ROD, m.mirrorPos.getX() + 0.5, m.mirrorPos.getY() + 1.0, m.mirrorPos.getZ() + 0.5, 40, 0.3, 0.8, 0.3, 0.05);
        Component msg = Component.translatable(m.positions.length > 0 ? "mirrorworld.merge.done" : "mirrorworld.merge.empty", m.positions.length)
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
        ServerPlayer owner = m.owner != null ? target.getServer().getPlayerList().getPlayer(m.owner) : null;
        if (owner != null) owner.sendSystemMessage(msg);
        for (ServerPlayer p : target.players()) {
            if (p != owner && p.distanceToSqr(m.mirrorPos.getCenter()) < 48 * 48) p.sendSystemMessage(msg);
        }
    }

    private MergeManager() {}
}
