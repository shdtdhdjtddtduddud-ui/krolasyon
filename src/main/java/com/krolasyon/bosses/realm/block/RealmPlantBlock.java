package com.krolasyon.bosses.realm.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Glowing flora of the realm: grows on any realm soil, nylium, dirt or sand. */
public class RealmPlantBlock extends BushBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 13, 14);
    @Nullable private final Supplier<ParticleOptions> particle;
    private final boolean thorny;

    public RealmPlantBlock(Properties props, @Nullable Supplier<ParticleOptions> particle, boolean thorny) {
        super(props);
        this.particle = particle;
        this.thorny = thorny;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.NYLIUM) || state.is(BlockTags.SAND) || state.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)
                || state.is(com.krolasyon.bosses.realm.registry.RealmBlocks.REALM_SOIL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (particle != null && r.nextInt(4) == 0) {
            level.addParticle(particle.get(), pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.6 + r.nextDouble() * 0.3,
                    pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.015, 0);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (thorny && !level.isClientSide() && entity instanceof LivingEntity le && level.getGameTime() % 10 == 0) {
            le.hurt(level.damageSources().sweetBerryBush(), 1.5F);
        }
    }
}
