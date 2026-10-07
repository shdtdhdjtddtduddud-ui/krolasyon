package com.krolasyon.furniture.blockentity;

import com.krolasyon.furniture.block.WideBlock;
import com.krolasyon.furniture.network.ModNetwork;
import com.krolasyon.furniture.network.PianoAnimPacket;
import com.krolasyon.furniture.registry.ModBlockEntities;
import com.krolasyon.furniture.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

public class PianoBlockEntity extends BlockEntity {
    public static final int NOTES = 24;
    /** how many notes in a row are needed for the "performance" buff */
    public static final int COMBO_NEEDED = 12;

    public final float[] key = new float[NOTES];
    public final float[] prevKey = new float[NOTES];
    public float pedal, prevPedal;
    private int pedalHold;

    private int combo;
    private long lastNote;
    private long cooldownUntil;

    public PianoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIANO.get(), pos, state);
    }

    /** client: a key was struck (from the network) */
    public void pressClient(int note) {
        if (note < 0 || note >= NOTES) return;
        key[note] = 1.0F;
        pedalHold = 14;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PianoBlockEntity be) {
        for (int i = 0; i < NOTES; i++) {
            be.prevKey[i] = be.key[i];
            be.key[i] = Math.max(0.0F, be.key[i] - 0.11F);
        }
        be.prevPedal = be.pedal;
        float target = be.pedalHold > 0 ? 1.0F : 0.0F;
        if (be.pedalHold > 0) be.pedalHold--;
        be.pedal += (target - be.pedal) * 0.35F;
    }

    /** server: play a note. Notes 0..23 span two octaves from C4. Playing a tune in a row inspires everyone nearby. */
    public static void playNote(ServerLevel level, BlockPos base, BlockState state, int note, ServerPlayer player) {
        if (note < 0 || note >= NOTES) return;
        Direction f = state.getValue(WideBlock.FACING);
        Direction toPartner = f.getCounterClockWise();
        double cx = base.getX() + 0.5D + toPartner.getStepX() * 0.5D;
        double cz = base.getZ() + 0.5D + toPartner.getStepZ() * 0.5D;
        double cy = base.getY() + 0.9D;
        SoundEvent ev = note < 12 ? ModSounds.PIANO_C4.get() : ModSounds.PIANO_C5.get();
        float pitch = (float) Math.pow(2.0D, (note % 12) / 12.0D);
        level.playSound(null, cx, cy, cz, ev, SoundSource.RECORDS, 3.0F, pitch);
        level.sendParticles(ParticleTypes.NOTE, cx + f.getStepX() * -0.1D, cy + 1.0D, cz + f.getStepZ() * -0.1D, 0, note / 24.0D, 0.0D, 0.0D, 1.0D);
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(cx, cy, cz, 48.0D, level.dimension())),
                new PianoAnimPacket(base, (byte) note));

        if (level.getBlockEntity(base) instanceof PianoBlockEntity be) {
            long now = level.getGameTime();
            if (now - be.lastNote > 30) be.combo = 0;
            be.lastNote = now;
            be.combo++;
            if (be.combo >= COMBO_NEEDED && now >= be.cooldownUntil) {
                be.combo = 0;
                be.cooldownUntil = now + 400;
                AABB area = new AABB(base).inflate(10.0D);
                for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, area)) {
                    p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0, false, true, true));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200, 0, false, true, true));
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 0, false, true, true));
                    p.displayClientMessage(Component.translatable("message.krolasyonfurniture.inspired"), true);
                }
                level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.RECORDS, 2.0F, 1.2F);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy + 1.0D, cz, 14, 0.8D, 0.4D, 0.6D, 0.0D);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(2.0D, 2.0D, 2.0D);
    }
}
