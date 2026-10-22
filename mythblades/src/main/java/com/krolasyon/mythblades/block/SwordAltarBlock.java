package com.krolasyon.mythblades.block;

import com.krolasyon.mythblades.MythBlades;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Kor Sunağı: içine Kor Kılıcı saplanmış kırılmaz taş. SWORD = kılıç taşta mı. */
public final class SwordAltarBlock extends BaseEntityBlock {
    public static final BooleanProperty SWORD = BooleanProperty.create("sword");

    public SwordAltarBlock() {
        super(Properties.of().mapColor(MapColor.STONE).strength(-1F, 3600000F).noLootTable().pushReaction(PushReaction.BLOCK)
                .lightLevel(s -> s.getValue(SWORD) ? 13 : 3));
        registerDefaultState(stateDefinition.any().setValue(SWORD, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(SWORD);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwordAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MythBlades.ALTAR_BE.get(), level.isClientSide ? SwordAltarBlockEntity::clientTick : SwordAltarBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(level.getBlockEntity(pos) instanceof SwordAltarBlockEntity be)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        return be.interact(player) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(SWORD)) return;
        // kılıcın saplandığı yerden kor ve duman
        double x = pos.getX() + 0.5, y = pos.getY() + 1.02, z = pos.getZ() + 0.5;
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x + (r.nextDouble() - 0.5) * 0.3, y, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
        if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.2, y, z + (r.nextDouble() - 0.5) * 0.2, 0, 0.02, 0);
        if (r.nextInt(14) == 0) level.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
        if (r.nextInt(4) == 0) {
            double h = 0.2 + r.nextDouble() * 1.4;
            level.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.25, y + h, z + (r.nextDouble() - 0.5) * 0.1, 0, 0.015, 0);
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter g, BlockPos p) {
        return false;
    }
}
