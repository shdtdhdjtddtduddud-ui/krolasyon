package com.krolasyon.alien.block;

import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Fluorescent ceiling strip. Flickering variants stutter on and off on their own, like Sevastopol's failing power grid. */
public class CeilingLightBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty FLICKER = BooleanProperty.create("flicker");

    public CeilingLightBlock(Properties p) {
        super(p);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true).setValue(FLICKER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT, FLICKER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FLICKER, ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (state.getValue(FLICKER) && !old.is(this)) level.scheduleTick(pos, this, 20 + level.random.nextInt(60));
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (state.getValue(FLICKER) && !level.getBlockTicks().hasScheduledTick(pos, this)) level.scheduleTick(pos, this, 10);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource r) {
        if (!state.getValue(FLICKER)) {
            if (!state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, true), 2);
            return;
        }
        boolean lit = !state.getValue(LIT);
        level.setBlock(pos, state.setValue(LIT, lit), 2);
        if (lit) {
            if (r.nextInt(3) == 0) level.playSound(null, pos, ModSounds.LIGHT_BUZZ.get(), SoundSource.BLOCKS, 0.35F, 0.9F + r.nextFloat() * 0.2F);
            // mostly short bursts of light, sometimes long calm periods
            level.scheduleTick(pos, this, r.nextInt(4) == 0 ? 40 + r.nextInt(160) : 2 + r.nextInt(10));
        } else {
            level.scheduleTick(pos, this, 2 + r.nextInt(r.nextInt(5) == 0 ? 40 : 6));
        }
    }

    /** briefly kill the light (used when the alien drops out of a vent nearby). */
    public static void blackout(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (state.getBlock() instanceof CeilingLightBlock b && state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), 2);
            level.scheduleTick(pos, b, ticks);
        }
    }
}
