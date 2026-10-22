package com.krolasyon.mythblades.block;

import com.krolasyon.mythblades.MythBlades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Sunaktaki kılıcın durumu: saklanan kılıç (boşsa yeni bir Kor Kılıcı), geri saplayan oyuncu (o, mühürsüz çekebilir),
 * çekilme ve sarsılma animasyonlarının başlangıç zamanları.
 */
public final class SwordAltarBlockEntity extends BlockEntity {
    public static final int PULL = 70, WOBBLE = 14;

    private ItemStack stored = ItemStack.EMPTY;
    @Nullable private UUID owner, puller;
    public long pullStart = -1, wobbleStart = -1000;

    public SwordAltarBlockEntity(BlockPos pos, BlockState state) {
        super(MythBlades.ALTAR_BE.get(), pos, state);
    }

    public boolean hasSword() {
        return getBlockState().getValue(SwordAltarBlock.SWORD);
    }

    public boolean pulling() {
        return pullStart >= 0;
    }

    /** Çekilme ilerlemesi 0..1 (istemcide çizim için). */
    public float progress(float pt) {
        if (pullStart < 0 || level == null) return 0;
        return Math.min(1F, (level.getGameTime() - pullStart + pt) / PULL);
    }

    public ItemStack swordStack() {
        return stored.isEmpty() ? new ItemStack(MythBlades.EMBER.get()) : stored;
    }

    private static boolean holdsSeal(Player p) {
        return p.getMainHandItem().is(MythBlades.SEAL.get()) || p.getOffhandItem().is(MythBlades.SEAL.get());
    }

    /** Sağ tık. */
    public boolean interact(Player p) {
        Level l = level;
        if (l == null) return false;
        if (!hasSword()) {
            ItemStack hand = p.getMainHandItem();
            if (!hand.is(MythBlades.EMBER.get())) {
                p.displayClientMessage(Component.translatable("msg.mythblades.altar_empty").withStyle(ChatFormatting.GRAY), true);
                return true;
            }
            // kılıcı geri sapla
            stored = hand.copy();
            owner = p.getUUID();
            if (!p.getAbilities().instabuild) hand.shrink(1);
            l.setBlock(worldPosition, getBlockState().setValue(SwordAltarBlock.SWORD, true), 3);
            l.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 0.7F);
            l.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.8F);
            ((ServerLevel) l).sendParticles(ParticleTypes.FLAME, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5,
                    30, 0.25, 0.1, 0.25, 0.05);
            p.displayClientMessage(Component.translatable("msg.mythblades.altar_return").withStyle(ChatFormatting.GOLD), true);
            sync();
            return true;
        }
        if (pulling()) return true;
        boolean free = p.getUUID().equals(owner);
        if (!free && !holdsSeal(p)) {
            // kılıç kımıldamaz: kabza yakar, oyuncu geri itilir
            wobbleStart = l.getGameTime();
            p.setSecondsOnFire(2);
            Vec3 away = p.position().subtract(Vec3.atCenterOf(worldPosition)).multiply(1, 0, 1).normalize();
            p.push(away.x * 0.6, 0.25, away.z * 0.6);
            p.hurtMarked = true;
            l.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 0.6F);
            l.playSound(null, worldPosition, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 1F, 0.5F);
            p.displayClientMessage(Component.translatable("msg.mythblades.altar_locked").withStyle(ChatFormatting.RED), true);
            sync();
            return true;
        }
        pullStart = l.getGameTime();
        puller = p.getUUID();
        l.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1F, 0.5F);
        l.playSound(null, worldPosition, SoundEvents.BLAZE_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.6F);
        p.displayClientMessage(Component.translatable(free ? "msg.mythblades.altar_pull_free" : "msg.mythblades.altar_pull").withStyle(ChatFormatting.GOLD), true);
        sync();
        return true;
    }

    public static void serverTick(Level l, BlockPos pos, BlockState state, SwordAltarBlockEntity be) {
        if (!be.pulling()) return;
        ServerLevel sl = (ServerLevel) l;
        long t = l.getGameTime() - be.pullStart;
        Player p = be.puller == null ? null : l.getPlayerByUUID(be.puller);
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (p == null || !p.isAlive() || p.distanceToSqr(x, y, z) > 36) {
            // çekiş yarım kaldı: kılıç yerine oturur
            be.pullStart = -1;
            be.puller = null;
            l.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1F, 0.6F);
            be.sync();
            return;
        }
        // taş çatlıyor, kor fışkırıyor
        if (t % 6 == 0) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SMOOTH_STONE.defaultBlockState()), x, y + 0.05, z, 8, 0.3, 0.02, 0.3, 0.1);
            l.playSound(null, pos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.9F, 0.5F + t / (float) PULL * 0.4F);
        }
        if (t % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, x, y + 0.1, z, 2 + (int) (t / 12), 0.2, 0.05, 0.2, 0.02 + t * 0.001);
        if (t % 9 == 0) sl.sendParticles(ParticleTypes.LAVA, x, y + 0.1, z, 2, 0.2, 0.05, 0.2, 0);
        if (t == PULL / 2) {
            l.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1F, 0.7F);
            l.playSound(null, pos, SoundEvents.BLAZE_BURN, SoundSource.BLOCKS, 1F, 0.8F);
        }
        if (t < PULL) return;
        // son: mühür erir, kılıç oyuncunun eline geçer
        boolean free = be.puller.equals(be.owner);
        if (!free) {
            ItemStack seal = p.getMainHandItem().is(MythBlades.SEAL.get()) ? p.getMainHandItem()
                    : p.getOffhandItem().is(MythBlades.SEAL.get()) ? p.getOffhandItem() : ItemStack.EMPTY;
            if (seal.isEmpty()) {
                be.pullStart = -1;
                be.puller = null;
                be.sync();
                return;
            }
            if (!p.getAbilities().instabuild) seal.shrink(1);
        }
        ItemStack sword = be.swordStack().copy();
        be.stored = ItemStack.EMPTY;
        be.owner = null;
        be.pullStart = -1;
        be.puller = null;
        l.setBlock(pos, state.setValue(SwordAltarBlock.SWORD, false), 3);
        if (p.getMainHandItem().isEmpty()) p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, sword);
        else if (!p.getInventory().add(sword)) p.drop(sword, false);
        sl.sendParticles(ParticleTypes.FLAME, x, y + 1.6, z, 80, 0.4, 0.9, 0.4, 0.12);
        sl.sendParticles(ParticleTypes.LAVA, x, y + 0.4, z, 16, 0.4, 0.3, 0.4, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION, x, y + 0.5, z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.FLASH, x, y + 1.5, z, 1, 0, 0, 0, 0);
        l.playSound(null, pos, SoundEvents.BLAZE_SHOOT, SoundSource.BLOCKS, 1.2F, 0.7F);
        l.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1F, 0.8F);
        l.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 1.5F, 0.6F);
        p.displayClientMessage(Component.translatable("msg.mythblades.altar_won").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        be.sync();
    }

    public static void clientTick(Level l, BlockPos pos, BlockState state, SwordAltarBlockEntity be) {
        if (!be.pulling()) return;
        float pr = be.progress(0);
        var r = l.random;
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        // kılıç boyunca alevler sarar
        int n = 1 + (int) (pr * 4);
        for (int i = 0; i < n; i++) {
            double h = 0.3 + r.nextDouble() * (1.2 + pr * 1.4);
            l.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.4, y + h, z + (r.nextDouble() - 0.5) * 0.25, 0, 0.03, 0);
        }
        if (r.nextInt(3) == 0) l.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.1, z, (r.nextDouble() - 0.5) * 0.05, 0.05, (r.nextDouble() - 0.5) * 0.05);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!stored.isEmpty()) tag.put("Sword", stored.save(new CompoundTag()));
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putLong("PullStart", pullStart);
        tag.putLong("Wobble", wobbleStart);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stored = tag.contains("Sword") ? ItemStack.of(tag.getCompound("Sword")) : ItemStack.EMPTY;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        pullStart = tag.contains("PullStart") ? tag.getLong("PullStart") : -1;
        wobbleStart = tag.contains("Wobble") ? tag.getLong("Wobble") : -1000;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.5, 0, 1.5).expandTowards(0, 4, 0);
    }
}
