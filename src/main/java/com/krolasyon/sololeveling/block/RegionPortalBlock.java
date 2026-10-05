package com.krolasyon.sololeveling.block;

import com.krolasyon.sololeveling.world.Regions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A swirling blue gate surface that sends players to another region of the world. */
public class RegionPortalBlock extends BaseEntityBlock {
    public RegionPortalBlock(Properties p) { super(p); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RegionPortalBlockEntity(pos, state); }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || !(e instanceof ServerPlayer p) || p.isOnPortalCooldown()) return;
        String target = level.getBlockEntity(pos) instanceof RegionPortalBlockEntity be ? be.target : "seoul_plaza";
        if (level.dimension() == Regions.SEOUL) {
            String t = com.krolasyon.sololeveling.world.Landmark.portalTarget(pos.getX(), pos.getZ());
            if (t != null) target = t;
        }
        p.setPortalCooldown(60);
        Regions.teleport(p, target);
    }

    @Override
    public void animateTick(BlockState s, Level l, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 2; i++)
            l.addParticle(ParticleTypes.PORTAL, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 0.5, 0, (r.nextDouble() - 0.5) * 0.5);
    }

    @Override
    public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1F; }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }
}
