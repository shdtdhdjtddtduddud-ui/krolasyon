package com.krolasyon.labirent;

import com.krolasyon.labirent.entity.GladerEntity;
import com.krolasyon.labirent.entity.GrieverEntity;
import com.krolasyon.labirent.entity.ai.GladerSocial;
import com.krolasyon.labirent.maze.Dims;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.net.Net;
import com.krolasyon.labirent.net.ServerHooks;
import com.krolasyon.labirent.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Players trip and slip while sprinting through the maze; Gladers notice fights and remember what you did. */
@Mod.EventBusSubscriber(modid = Labirent.MODID)
public final class TripEvents {
    private TripEvents() {}

    private static final Map<UUID, Long> FALLEN = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || !Dims.isMaze(p.level())) return;
        long now = p.level().getGameTime();
        // the Glade's memory of where you have been
        if (now % 20 == 0) {
            GladerSocial.Memory mem = GladerSocial.mem(p.getUUID());
            boolean inMaze = !Glade.inside(p.getX(), p.getZ()) && MazeLayout.unitOf(p.getBlockX()) >= 0;
            if (inMaze) mem.mazeTicks += 20;
            else if (mem.mazeTicks >= 1200 && Glade.inside(p.getX(), p.getZ())) {
                mem.returnedAt = now;
                mem.mazeTicks = 0;
            } else if (!inMaze) mem.mazeTicks = Math.max(0, mem.mazeTicks - 20);
        }
        if (p.isCreative() || p.isSpectator() || !p.onGround() || !p.isSprinting()) return;
        Long until = FALLEN.get(p.getUUID());
        if (until != null && now < until + 100) return;
        if (Glade.inside(p.getX(), p.getZ()) && p.getRandom().nextFloat() > 0.2F) return;
        BlockState floor = p.level().getBlockState(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()));
        float chance = 0.00045F;
        boolean slippery = floor.is(ModBlocks.MAZE_FLOOR_MOSSY.get()) || floor.is(Blocks.MOSS_BLOCK) || p.level().isRainingAt(p.blockPosition());
        if (slippery) chance *= 2.5F;
        if (floor.is(Blocks.COARSE_DIRT) || floor.is(Blocks.GRAVEL)) chance *= 1.8F;
        boolean chased = !p.level().getEntitiesOfClass(GrieverEntity.class, p.getBoundingBox().inflate(14), g -> g.getTarget() == p).isEmpty();
        if (chased) chance *= 4F;
        if (p.getRandom().nextFloat() >= chance) return;
        int kind = slippery && p.getRandom().nextBoolean() ? 1 : 0;
        FALLEN.put(p.getUUID(), now);
        p.setSprinting(false);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 34, 6, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, 34, 250, false, false, false));
        p.setDeltaMovement(p.getDeltaMovement().multiply(0.3, 1, 0.3).add(p.getLookAngle().multiply(kind == 0 ? 0.25 : -0.2, 0, kind == 0 ? 0.25 : -0.2)));
        p.hurtMarked = true;
        p.hurt(p.damageSources().fall(), kind == 0 ? 1.0F : 1.5F);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_SMALL_FALL, SoundSource.PLAYERS, 1F, 0.8F);
        Net.stumble(p, kind, 34);
        p.displayClientMessage(Component.translatable(kind == 0 ? "msg.labirent.tripped" : "msg.labirent.slipped"), true);
    }

    /** Gladers react when someone near them is attacked or attacks a Griever */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (!(victim.level() instanceof ServerLevel level) || !Dims.isMaze(level)) return;
        LivingEntity src = e.getSource().getEntity() instanceof LivingEntity l ? l : null;
        if (src == null) return;
        LivingEntity monster = null, friend = null;
        if (src instanceof Enemy && (victim instanceof Player || victim instanceof GladerEntity)) { monster = src; friend = victim; }
        else if (victim instanceof GrieverEntity && src instanceof Player) { monster = victim; friend = src; }
        if (monster == null) return;
        long now = level.getGameTime();
        for (GladerEntity g : ServerHooks.allGladers(level))
            if (g != friend && g.distanceToSqr(friend) < 28 * 28) g.social().allyAttacked(friend, monster, now);
    }

    public static void rememberKill(Player p) {
        GladerSocial.mem(p.getUUID()).killAt = p.level().getGameTime();
    }

    static boolean active() { return MazeManager.get() != null; }
}
