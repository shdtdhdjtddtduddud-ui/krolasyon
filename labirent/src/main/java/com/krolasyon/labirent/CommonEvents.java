package com.krolasyon.labirent;

import com.krolasyon.labirent.maze.Dims;
import com.krolasyon.labirent.maze.Glade;
import com.krolasyon.labirent.maze.MazeLayout;
import com.krolasyon.labirent.maze.MazeManager;
import com.krolasyon.labirent.registry.ModEffects;
import com.krolasyon.labirent.team.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Role perks, venom outcome and commands. */
@Mod.EventBusSubscriber(modid = Labirent.MODID)
public final class CommonEvents {
    private CommonEvents() {}

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) { LabCommands.register(e.getDispatcher()); }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired e) {
        MobEffectInstance inst = e.getEffectInstance();
        if (inst == null) return;
        LivingEntity ent = e.getEntity();
        if (ent.level().isClientSide) return;
        if (inst.getEffect() == ModEffects.VENOM.get() && LabConfig.HARDCORE_VENOM.get()) {
            ent.hurt(ent.damageSources().magic(), 1000F);
            if (ent instanceof ServerPlayer sp) sp.sendSystemMessage(Component.translatable("msg.labirent.venom_death").withStyle(ChatFormatting.DARK_GREEN));
        }
        if (inst.getEffect() == ModEffects.CHANGING.get() && ent instanceof ServerPlayer sp) {
            MazeManager m = MazeManager.get();
            if (m != null) m.story.onRemember(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        MazeManager m = MazeManager.get();
        if (m == null || !Dims.isMaze(p.level())) return;
        Role r = m.state.role(p.getUUID());
        boolean inMaze = !Glade.inside(p.getX(), p.getZ()) && MazeLayout.unitOf(p.getBlockX()) >= 0;
        long phase = p.level().getDayTime() % 24000L;
        boolean night = phase >= 12500 && phase < 23300;
        switch (r) {
            case KOSUCU -> {
                if (inMaze) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 0, true, false, true));
                    if (p.getFoodData().getExhaustionLevel() > 0.2F) p.getFoodData().setExhaustion(p.getFoodData().getExhaustionLevel() - 0.2F);
                }
            }
            case BEKCI -> {
                if (night) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
            }
            case SIFACI -> {
                for (Player o : p.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(6)))
                    if (o.getHealth() < o.getMaxHealth()) o.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, false, true));
            }
            case ASCI -> p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 1, 0, true, false, false));
            default -> {}
        }
        if (m.isCaptain(p) && !inMaze) {
            for (Player o : p.level().getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(10)))
                if (o != p) o.addEffect(new MobEffectInstance(MobEffects.LUCK, 60, 0, true, false, true));
        }
    }

    /** gardeners coax crops along with a hoe */
    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getLevel().isClientSide || !(e.getItemStack().getItem() instanceof HoeItem)) return;
        MazeManager m = MazeManager.get();
        if (m == null || m.state.role(e.getEntity().getUUID()) != Role.BAHCIVAN) return;
        BlockState s = e.getLevel().getBlockState(e.getPos());
        if (s.getBlock() instanceof CropBlock c && !c.isMaxAge(s) && !e.getEntity().getCooldowns().isOnCooldown(e.getItemStack().getItem())) {
            e.getLevel().setBlock(e.getPos(), c.getStateForAge(Math.min(c.getMaxAge(), c.getAge(s) + 2)), 3);
            ((ServerLevel) e.getLevel()).sendParticles(ParticleTypes.HAPPY_VILLAGER, e.getPos().getX() + 0.5, e.getPos().getY() + 0.5,
                    e.getPos().getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
            e.getEntity().getCooldowns().addCooldown(e.getItemStack().getItem(), 10);
            e.setCanceled(true);
        }
    }

    /** slicers get more meat */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof Animal) || !(e.getSource().getEntity() instanceof Player p)) return;
        MazeManager m = MazeManager.get();
        if (m == null || m.state.role(p.getUUID()) != Role.KASAP) return;
        for (ItemEntity ie : e.getDrops().toArray(new ItemEntity[0])) {
            ItemStack s = ie.getItem();
            if (s.isEdible()) e.getDrops().add(new ItemEntity(ie.level(), ie.getX(), ie.getY(), ie.getZ(), s.copy()));
        }
    }
}
