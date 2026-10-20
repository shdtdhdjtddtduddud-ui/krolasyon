package com.krolasyon.advgear.block;

import com.krolasyon.advgear.item.KeyItem;
import com.krolasyon.advgear.item.KeyRingItem;
import com.krolasyon.advgear.item.LockpickItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * Kilitli Sandık: mağaralarda bulunur. Doğru anahtarla (ya da şansla maymuncukla) açılınca sıradan bir sandığa dönüşür
 * ve kilit türüne göre ganimetle dolar. Kırılırsa ganimeti yok olur.
 */
public class LockedChestBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<LockType> LOCK = EnumProperty.create("lock", LockType.class);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public LockedChestBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LOCK, LockType.IRON));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LOCK);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        LockType lock = state.getValue(LOCK);
        ItemStack held = player.getItemInHand(hand);
        boolean open = false;
        if (held.getItem() instanceof KeyItem k && k.lock == lock) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            open = true;
        } else if (held.getItem() instanceof KeyRingItem && KeyRingItem.consume(held, lock)) {
            open = true;
        } else if (held.getItem() instanceof LockpickItem) {
            level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 1F, 1.6F);
            if (level.random.nextFloat() < lock.pickChance) {
                held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                open = true;
            } else {
                held.hurtAndBreak(2, player, p -> p.broadcastBreakEvent(hand));
                player.displayClientMessage(Component.translatable("msg.advgear.pick_failed").withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
        }
        if (!open) {
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.8F);
            player.displayClientMessage(Component.translatable("msg.advgear.locked_" + lock.id).withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }
        unlock((ServerLevel) level, pos, state, player);
        return InteractionResult.CONSUME;
    }

    public static void unlock(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        LockType lock = state.getValue(LOCK);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, state.getValue(FACING)), 3);
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity be) {
            be.setLootTable(lock.loot(), level.random.nextLong());
            if (player != null) player.openMenu(be);
        }
        level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1F, 1.4F);
        level.playSound(null, pos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1F, 1.0F);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4F, 1.6F);
        Vector3f color = lock == LockType.HEART ? new Vector3f(1F, 0.3F, 0.4F) : lock == LockType.BONE ? new Vector3f(0.9F, 0.88F, 0.75F)
                : new Vector3f(0.8F, 0.8F, 0.85F);
        level.sendParticles(new DustParticleOptions(color, 1.2F), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 30, 0.4, 0.3, 0.4, 0);
        level.sendParticles(ParticleTypes.WAX_OFF, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 15, 0.3, 0.3, 0.3, 0.5);
    }
}
