package com.krolasyon.cille.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

/** Environment damage helpers: every terrain-changing attack goes through here and respects the mobGriefing rule. */
public final class Terrain {
    private Terrain() {}

    public static boolean allowed(Level level, @Nullable Entity source) {
        return source != null && ForgeEventFactory.getMobGriefingEvent(level, source);
    }

    /** blocks that may be torn out / shattered: no bedrock, obsidian-class or block entities (chests, furnaces...) */
    public static boolean breakable(Level level, BlockPos pos, BlockState state, float maxHardness) {
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) return false;
        float h = state.getDestroySpeed(level, pos);
        return h >= 0F && h <= maxHardness;
    }

    public static boolean exposed(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) if (level.getBlockState(pos.relative(d)).isAir()) return true;
        return false;
    }

    /** rips a block out of the ground as a flying falling-block; returns it or null */
    @Nullable
    public static FallingBlockEntity rip(ServerLevel level, BlockPos pos, Vec3 velocity) {
        BlockState state = level.getBlockState(pos);
        FallingBlockEntity fb = FallingBlockEntity.fall(level, pos, state);
        fb.dropItem = false;
        fb.setDeltaMovement(velocity);
        fb.hurtMarked = true;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.1);
        return fb;
    }

    /** smashes a bowl-shaped crater; part of the blocks are thrown into the air as debris */
    public static void crater(ServerLevel level, Entity source, Vec3 c, double radius, int maxFlying, float maxHardness) {
        if (!allowed(level, source)) return;
        RandomSource r = level.getRandom();
        int R = Mth.ceil(radius);
        int flying = 0;
        BlockPos center = BlockPos.containing(c.x, c.y - 0.5, c.z);
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                for (int dy = -R / 2 - 1; dy <= 1; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + (dy * 1.8) * (dy * 1.8));
                    if (d > radius - r.nextDouble() * 0.8) continue;
                    BlockPos p = center.offset(dx, dy, dz);
                    BlockState st = level.getBlockState(p);
                    if (!breakable(level, p, st, maxHardness)) continue;
                    if (flying < maxFlying && r.nextFloat() < 0.35F && exposed(level, p)) {
                        Vec3 out = new Vec3(dx, 0, dz);
                        out = out.lengthSqr() < 0.01 ? Vec3.ZERO : out.normalize();
                        rip(level, p, new Vec3(out.x * (0.2 + r.nextDouble() * 0.35), 0.45 + r.nextDouble() * 0.45, out.z * (0.2 + r.nextDouble() * 0.35)));
                        flying++;
                    } else {
                        level.destroyBlock(p, r.nextFloat() < 0.15F, source);
                    }
                }
            }
        }
    }

    /** shatters soft blocks (plants, leaves, glass, snow, ...) in a small box */
    public static void shatterSoft(ServerLevel level, Entity source, BlockPos base, int rx, int ry) {
        if (!allowed(level, source)) return;
        for (BlockPos p : BlockPos.betweenClosed(base.offset(-rx, 0, -rx), base.offset(rx, ry, rx))) {
            BlockState st = level.getBlockState(p);
            if (breakable(level, p, st, 0.6F)) level.destroyBlock(p, false, source);
        }
    }
}
