package com.krolasyon.bosses.block;

import com.krolasyon.bosses.registry.ModBlocks;
import com.krolasyon.bosses.world.HellGates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** The swirling gate between the overworld and Azrakor. Standing inside it for a few seconds carries you across. */
public class HellPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    protected static final VoxelShape X_AXIS = Block.box(0.0D, 0.0D, 6.0D, 16.0D, 16.0D, 10.0D);
    protected static final VoxelShape Z_AXIS = Block.box(6.0D, 0.0D, 0.0D, 10.0D, 16.0D, 16.0D);

    public HellPortalBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS : X_AXIS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return switch (rot) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z);
            default -> state;
        };
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction.Axis a = state.getValue(AXIS);
        boolean inPlane = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == a;
        if (inPlane && !neighbor.is(this) && !neighbor.is(ModBlocks.get("hellgate_stone"))) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && !entity.isPassenger() && !entity.isVehicle() && entity.canChangeDimensions()) HellGates.touch(entity, pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(100) == 0) level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4F, rand.nextFloat() * 0.4F + 0.5F, false);
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + rand.nextDouble();
            double y = pos.getY() + rand.nextDouble();
            double z = pos.getZ() + rand.nextDouble();
            double dx = (rand.nextFloat() - 0.5) * 0.4, dy = rand.nextFloat() * 0.4 + 0.05, dz = (rand.nextFloat() - 0.5) * 0.4;
            level.addParticle(i == 0 ? ParticleTypes.SMOKE : (i == 1 ? ParticleTypes.FLAME : ParticleTypes.ASH), x, y, z, dx * 0.2, dy * 0.3, dz * 0.2);
        }
    }
}
