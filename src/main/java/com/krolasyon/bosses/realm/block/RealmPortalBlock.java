package com.krolasyon.bosses.realm.block;

import com.krolasyon.bosses.realm.portal.RealmPortals;
import com.krolasyon.bosses.realm.registry.RealmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/** Kızıl Geçit: the swirling crimson gate between the Overworld and the Crimson Realm. */
public class RealmPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);
    private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.2F), 1.4F);
    private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.2F), 1.0F);

    public RealmPortalBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(AXIS); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos npos) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean inPlane = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == axis;
        if (inPlane && !neighbor.is(this) && !neighbor.is(RealmBlocks.CURSED_OBSIDIAN.get())) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, npos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && !entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) {
            RealmPortals.onEntityInside(entity, pos, state.getValue(AXIS));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
                    0.4F, 0.55F + r.nextFloat() * 0.2F, false);
        }
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + r.nextDouble();
            double y = pos.getY() + r.nextDouble();
            double z = pos.getZ() + r.nextDouble();
            double vx = (r.nextDouble() - 0.5) * 0.4, vz = (r.nextDouble() - 0.5) * 0.4;
            if (state.getValue(AXIS) == Direction.Axis.X) z = pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.3;
            else x = pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.3;
            switch (i) {
                case 0 -> level.addParticle(CRIMSON, x, y, z, 0, 0.02, 0);
                case 1 -> { if (r.nextInt(3) == 0) level.addParticle(EMBER, x, y, z, 0, 0.05, 0); }
                default -> { if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, vx * 0.2, 0.05, vz * 0.2); }
            }
        }
        if (r.nextInt(10) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0.01, 0);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        if (rot == Rotation.CLOCKWISE_90 || rot == Rotation.COUNTERCLOCKWISE_90) {
            return state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        }
        return state;
    }
}
