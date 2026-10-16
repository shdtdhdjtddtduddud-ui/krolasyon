package com.krolasyon.alien.ai;

import com.krolasyon.alien.block.CeilingLightBlock;
import com.krolasyon.alien.block.GeneratorBlock;
import com.krolasyon.alien.block.SealedDoorBlock;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.network.Net;
import com.krolasyon.alien.registry.ModBlocks;
import com.krolasyon.alien.registry.ModItems;
import com.krolasyon.alien.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The way out of Sevastopol: find three fuses, restart the generator, take the keycard from the (now unsealed)
 * security room, open the escape pod bay and survive the launch countdown. Every step makes the alien angrier.
 */
public class Objectives extends SavedData {
    private static final String NAME = "alienisolation_objectives";
    public static final int NONE = 0, FUSES = 1, KEYCARD = 2, ESCAPE = 3, LAUNCH = 4, DONE = 5;
    public static final int FUSES_NEEDED = 3;
    private static final int LAUNCH_TICKS = 300;

    private int stage = NONE;
    private int fuses;
    private int countdown = -1;
    private long startTime;
    private BlockPos min = BlockPos.ZERO, max = BlockPos.ZERO;
    @Nullable private BlockPos generator, console;
    private final List<BlockPos> powerSeals = new ArrayList<>();

    public static Objectives get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Objectives::load, Objectives::new, NAME);
    }

    public int stage() { return stage; }

    @Nullable public BlockPos generatorPos() { return generator; }

    @Nullable public BlockPos consolePos() { return console; }

    /** 0..3: how far the players got; the alien gets sharper ears with every step. */
    public int aggression() { return Math.min(3, Math.max(0, stage - 1)); }

    // ------------------------------------------------------------------ setup
    public void begin(ServerLevel level, BlockPos min, BlockPos max, BlockPos generator, BlockPos console, List<BlockPos> powerSeals) {
        this.stage = FUSES;
        this.fuses = 0;
        this.countdown = -1;
        this.startTime = level.getGameTime();
        this.min = min.immutable();
        this.max = max.immutable();
        this.generator = generator.immutable();
        this.console = console.immutable();
        this.powerSeals.clear();
        powerSeals.forEach(p -> this.powerSeals.add(p.immutable()));
        setDirty();
        syncAll(level);
    }

    // ------------------------------------------------------------------ player actions
    public static void onGeneratorUsed(ServerLevel level, ServerPlayer p, BlockPos pos, @Nullable InteractionHand fuseHand) {
        Objectives o = get(level);
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof GeneratorBlock)) return;
        int inside = s.getValue(GeneratorBlock.FUSES);
        if (inside >= FUSES_NEEDED) {
            p.displayClientMessage(Component.translatable("message.alienisolation.generator_running").withStyle(ChatFormatting.GREEN), true);
            return;
        }
        if (fuseHand == null) {
            p.displayClientMessage(Component.translatable("message.alienisolation.generator_need", inside, FUSES_NEEDED).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (!p.getAbilities().instabuild) p.getItemInHand(fuseHand).shrink(1);
        inside++;
        level.setBlock(pos, s.setValue(GeneratorBlock.FUSES, inside), 3);
        level.playSound(null, pos, ModSounds.LOCKER.get(), SoundSource.BLOCKS, 1.0F, 1.4F);
        level.playSound(null, pos, ModSounds.SAVE_BEEP.get(), SoundSource.BLOCKS, 1.0F, 0.8F + inside * 0.2F);
        NoiseSystem.emit(level, pos, 16, NoiseSystem.Kind.DEVICE, p);
        if (pos.equals(o.generator) || o.generator == null) {
            o.fuses = inside;
            o.setDirty();
        }
        if (inside >= FUSES_NEEDED) o.powerOn(level, pos);
        else o.syncAll(level);
    }

    private void powerOn(ServerLevel level, BlockPos gen) {
        if (stage == FUSES || stage == NONE) stage = KEYCARD;
        fuses = FUSES_NEEDED;
        setDirty();
        level.playSound(null, gen, ModSounds.STATION_HUM.get(), SoundSource.BLOCKS, 3.0F, 0.7F);
        level.playSound(null, gen, ModSounds.STATION_ALARM.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        for (BlockPos p : powerSeals) if (level.getBlockState(p).is(ModBlocks.POWER_SEAL.get())) SealedDoorBlock.open(level, p);
        powerSeals.clear();
        // every light in the station comes back on, the failing ones steady
        if (!min.equals(max)) {
            for (BlockPos p : BlockPos.betweenClosed(min, max)) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof CeilingLightBlock)
                    level.setBlock(p, s.setValue(CeilingLightBlock.LIT, true).setValue(CeilingLightBlock.FLICKER, false), 2);
            }
        }
        // the roar of the generator brings the creature running
        NoiseSystem.emit(level, gen, 140, NoiseSystem.Kind.DEVICE, null);
        title(level, "message.alienisolation.power_on_title", "message.alienisolation.power_on");
        syncAll(level);
    }

    public static void onConsoleUsed(ServerLevel level, ServerPlayer p, BlockPos pos) {
        Objectives o = get(level);
        switch (o.stage) {
            case ESCAPE -> {
                o.stage = LAUNCH;
                o.countdown = LAUNCH_TICKS;
                o.console = pos.immutable();
                o.setDirty();
                level.playSound(null, pos, ModSounds.STATION_ALARM.get(), SoundSource.BLOCKS, 3.0F, 1.2F);
                title(level, "message.alienisolation.launch_title", "message.alienisolation.launch");
                o.syncAll(level);
            }
            case LAUNCH -> p.displayClientMessage(Component.translatable("message.alienisolation.launch_wait", o.countdown / 20)
                    .withStyle(ChatFormatting.RED), true);
            case DONE -> p.displayClientMessage(Component.translatable("message.alienisolation.already_escaped").withStyle(ChatFormatting.GRAY), true);
            case KEYCARD -> p.displayClientMessage(Component.translatable("message.alienisolation.need_keycard").withStyle(ChatFormatting.RED), true);
            default -> p.displayClientMessage(Component.translatable("message.alienisolation.need_power").withStyle(ChatFormatting.RED), true);
        }
    }

    // ------------------------------------------------------------------ ticking
    public static void tick(ServerLevel level) {
        Objectives o = get(level);
        if (o.stage == NONE || o.stage == DONE) return;
        long t = level.getGameTime();
        if (o.stage == KEYCARD && t % 10 == 0) {
            for (ServerPlayer p : level.players()) {
                if (p.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.KEYCARD.get()))) {
                    o.stage = ESCAPE;
                    o.setDirty();
                    title(level, "message.alienisolation.keycard_title", "message.alienisolation.keycard");
                    o.syncAll(level);
                    break;
                }
            }
        }
        if (o.stage == LAUNCH && o.console != null) {
            o.countdown--;
            if (o.countdown % 20 == 0) {
                level.playSound(null, o.console, ModSounds.SAVE_BEEP.get(), SoundSource.BLOCKS, 2.0F, 0.6F);
                o.syncAll(level);
            }
            // the countdown is loud: everything in the station knows where you are
            if (o.countdown % 40 == 0) NoiseSystem.emit(level, o.console, 90, NoiseSystem.Kind.DEVICE, null);
            if (o.countdown % 100 == 0) level.playSound(null, o.console, ModSounds.STATION_ALARM.get(), SoundSource.BLOCKS, 3.0F, 1.2F);
            if (o.countdown <= 0) o.escape(level);
            o.setDirty();
        }
        if (t % 100 == 0) o.syncAll(level);
    }

    private void escape(ServerLevel level) {
        stage = DONE;
        countdown = -1;
        setDirty();
        Vec3 c = Vec3.atCenterOf(console);
        long secs = (level.getGameTime() - startTime) / 20;
        String time = String.format("%d:%02d", secs / 60, secs % 60);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 1, 0, 0, 0, 0);
        level.playSound(null, console, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 0.6F);
        level.playSound(null, console, ModSounds.SAVE_DONE.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        for (ServerPlayer p : level.players()) {
            boolean aboard = p.isAlive() && p.position().distanceTo(c) < 10;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 120, 40));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(aboard ? "message.alienisolation.win_title" : "message.alienisolation.left_title")
                    .withStyle(aboard ? ChatFormatting.GREEN : ChatFormatting.DARK_RED)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.alienisolation.win", time).withStyle(ChatFormatting.GRAY)));
            if (aboard) p.removeAllEffects();
        }
        // the pod's launch vents the bay: the creature is blown into space
        for (XenomorphEntity x : level.getEntitiesOfClass(XenomorphEntity.class, new AABB(console).inflate(300))) x.discard();
        syncAll(level);
    }

    private static void title(ServerLevel level, String title, String sub) {
        for (ServerPlayer p : level.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(title).withStyle(ChatFormatting.YELLOW)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(sub).withStyle(ChatFormatting.GRAY)));
        }
    }

    // ------------------------------------------------------------------ sync
    public Net.ObjectiveState state() { return new Net.ObjectiveState(stage, fuses, Math.max(0, countdown)); }

    public void syncAll(ServerLevel level) {
        Net.ObjectiveState s = state();
        for (ServerPlayer p : level.players()) Net.toPlayer(p, s);
    }

    public static void sync(ServerPlayer p) { Net.toPlayer(p, get(p.serverLevel()).state()); }

    // ------------------------------------------------------------------ persistence
    public static Objectives load(CompoundTag tag) {
        Objectives o = new Objectives();
        o.stage = tag.getInt("Stage");
        o.fuses = tag.getInt("Fuses");
        o.countdown = tag.contains("Countdown") ? tag.getInt("Countdown") : -1;
        o.startTime = tag.getLong("Start");
        o.min = BlockPos.of(tag.getLong("Min"));
        o.max = BlockPos.of(tag.getLong("Max"));
        if (tag.contains("Generator")) o.generator = BlockPos.of(tag.getLong("Generator"));
        if (tag.contains("Console")) o.console = BlockPos.of(tag.getLong("Console"));
        for (long l : tag.getLongArray("Seals")) o.powerSeals.add(BlockPos.of(l));
        return o;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Stage", stage);
        tag.putInt("Fuses", fuses);
        tag.putInt("Countdown", countdown);
        tag.putLong("Start", startTime);
        tag.putLong("Min", min.asLong());
        tag.putLong("Max", max.asLong());
        if (generator != null) tag.putLong("Generator", generator.asLong());
        if (console != null) tag.putLong("Console", console.asLong());
        tag.putLongArray("Seals", powerSeals.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }
}
