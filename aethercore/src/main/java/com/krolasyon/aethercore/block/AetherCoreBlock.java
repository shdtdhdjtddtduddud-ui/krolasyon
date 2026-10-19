package com.krolasyon.aethercore.block;

import com.krolasyon.aethercore.AetherCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public final class AetherCoreBlock extends BaseEntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);

    public AetherCoreBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(4.0F, 1200.0F)
                .sound(SoundType.AMETHYST).noOcclusion().pushReaction(PushReaction.BLOCK)
                .lightLevel(s -> s.getValue(ACTIVE) ? 15 : 7));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AetherCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, AetherCore.AETHER_CORE_BE.get(),
                level.isClientSide ? AetherCoreBlockEntity::clientTick : AetherCoreBlockEntity::serverTick);
    }

    /** Yakıt değeri (tick). */
    public static int fuelValue(ItemStack stack) {
        if (stack.is(Items.AMETHYST_SHARD)) return 20 * 60 * 2;
        if (stack.is(Items.DIAMOND)) return 20 * 60 * 10;
        if (stack.is(Items.ECHO_SHARD)) return 20 * 60 * 25;
        if (stack.is(Items.NETHER_STAR)) return 20 * 60 * 60 * 4;
        return 0;
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AetherCoreBlockEntity core)) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(hand);
        int fuel = fuelValue(stack);
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (fuel > 0) {
            if (core.getEnergy() >= AetherCoreBlockEntity.MAX_ENERGY - 20) {
                player.displayClientMessage(Component.translatable("message.aethercore.full"), true);
                return InteractionResult.CONSUME;
            }
            core.addEnergy(fuel);
            if (!player.getAbilities().instabuild) stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.aethercore.charged",
                    AetherCoreBlockEntity.formatTime(fuel), AetherCoreBlockEntity.formatTime(core.getEnergy())), true);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 1.2F);
            if (!state.getValue(ACTIVE)) core.setActive(true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() || !stack.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.aethercore.status",
                    Component.translatable(state.getValue(ACTIVE) ? "message.aethercore.active" : "message.aethercore.dormant"),
                    AetherCoreBlockEntity.formatTime(core.getEnergy())), true);
            return InteractionResult.CONSUME;
        }
        if (!state.getValue(ACTIVE) && core.getEnergy() <= 0) {
            player.displayClientMessage(Component.translatable("message.aethercore.empty"), true);
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.6F, 1.6F);
            return InteractionResult.CONSUME;
        }
        core.setActive(!state.getValue(ACTIVE));
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(ACTIVE)) {
            if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.8,
                    pos.getY() + 0.5 + (r.nextDouble() - 0.5) * 0.8, pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
            return;
        }
        for (int i = 0; i < 2; i++) {
            double a = r.nextDouble() * Math.PI * 2, rad = 0.7 + r.nextDouble() * 0.4;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + Math.cos(a) * rad, pos.getY() + 0.5 + (r.nextDouble() - 0.5),
                    pos.getZ() + 0.5 + Math.sin(a) * rad, -Math.cos(a) * 0.05, 0, -Math.sin(a) * 0.05);
        }
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                (r.nextDouble() - 0.5) * 0.12, (r.nextDouble() - 0.3) * 0.12, (r.nextDouble() - 0.5) * 0.12);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) ActiveCores.set(level, pos, false);
        super.onRemove(state, level, pos, newState, moving);
    }
}
