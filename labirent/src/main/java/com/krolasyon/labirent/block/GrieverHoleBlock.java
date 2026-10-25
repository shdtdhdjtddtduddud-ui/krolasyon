package com.krolasyon.labirent.block;

import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Griever Hole off the Cliff: looks like empty sky. Only someone carrying a Griever core can pass through. */
public class GrieverHoleBlock extends Block {
    public GrieverHoleBlock(Properties p) { super(p); }

    private static boolean hasCore(Entity e) {
        return e instanceof Player p && p.getInventory().hasAnyMatching(s -> s.is(ModItems.GRIEVER_CORE.get()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return c instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player pl && pl.isCreative() ? Shapes.block() : Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        if (c instanceof EntityCollisionContext ec && ec.getEntity() != null && hasCore(ec.getEntity())) return Shapes.empty();
        return Shapes.block();
    }

    @Override
    @SuppressWarnings("deprecation")
    public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1F; }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState s, Level level, BlockPos pos, Entity e) {
        if (level.isClientSide || !(e instanceof ServerPlayer sp) || !hasCore(sp)) return;
        MazeManager m = MazeManager.get();
        if (m != null) m.story.enterHole(sp);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState s, Entity e) {
        if (!level.isClientSide && e instanceof Player p && !hasCore(p) && level.getGameTime() % 30 == 0)
            p.displayClientMessage(Component.translatable("msg.labirent.hole_blocked"), true);
    }

    @Override
    public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(6) == 0) {
            Vec3 c = Vec3.atCenterOf(pos).add((r.nextDouble() - 0.5), (r.nextDouble() - 0.5), (r.nextDouble() - 0.5));
            level.addParticle(ParticleTypes.SMOKE, c.x, c.y, c.z, 0, 0.01, 0);
        }
        if (r.nextInt(40) == 0) level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0, 0);
    }
}
