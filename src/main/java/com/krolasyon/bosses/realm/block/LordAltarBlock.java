package com.krolasyon.bosses.realm.block;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.story.Summoning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Lord altar in each kingdom keep: offering a challenge awakens the kingdom's ruler. */
public class LordAltarBlock extends Block {
    public static final IntegerProperty FACTION = IntegerProperty.create("faction", 0, 4);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(2, 4, 2, 14, 12, 14), Block.box(1, 12, 1, 15, 15, 15));

    public LordAltarBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACTION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACTION); }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Faction f = Faction.byId(state.getValue(FACTION));
        if (f != null && player instanceof ServerPlayer sp) Summoning.useAltar((ServerLevel) level, pos, f, sp);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        Faction f = Faction.byId(state.getValue(FACTION));
        if (f == null) return;
        var p = switch (f) {
            case ASH -> ParticleTypes.FLAME;
            case BLOOD -> ParticleTypes.CRIMSON_SPORE;
            case SHADOW -> ParticleTypes.PORTAL;
            case LEGION -> ParticleTypes.LAVA;
            case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        if (r.nextInt(3) == 0) level.addParticle(p, pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.0, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0, 0.04, 0);
    }
}
