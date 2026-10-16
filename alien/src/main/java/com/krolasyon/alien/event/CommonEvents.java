package com.krolasyon.alien.event;

import com.krolasyon.alien.AlienConfig;
import com.krolasyon.alien.AlienMod;
import com.krolasyon.alien.ai.*;
import com.krolasyon.alien.entity.XenomorphEntity;
import com.krolasyon.alien.registry.ModEntities;
import com.krolasyon.alien.world.StationGenerator;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AlienMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEvents {
    private CommonEvents() {}

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();

    // ------------------------------------------------------------------ per player server tick
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        HidingManager.tick(p);
        SaveManager.tick(p);
        FlashlightTracker.tick(p);
        footsteps(p);
    }

    /** walking is quiet, sprinting carries far, sneaking is silent. */
    private static void footsteps(ServerPlayer p) {
        if (p.tickCount % 4 != 0 || p.isSpectator() || p.isCreative() || HidingManager.isHidden(p)) return;
        Vec3 now = p.position();
        Vec3 last = LAST_POS.put(p.getUUID(), now);
        if (last == null || !p.onGround()) return;
        double moved = Math.sqrt((now.x - last.x) * (now.x - last.x) + (now.z - last.z) * (now.z - last.z));
        if (moved < 0.12 || p.isCrouching()) return;
        ServerLevel level = p.serverLevel();
        if (p.isSprinting()) NoiseSystem.emit(level, now, 15, NoiseSystem.Kind.RUN, p);
        else if (p.tickCount % 8 == 0) NoiseSystem.emit(level, now, 6, NoiseSystem.Kind.STEP, p);
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && e.getDistance() > 2.5F && !p.isCreative())
            NoiseSystem.emit(p.serverLevel(), p.position(), 10 + Math.min(10, e.getDistance()), NoiseSystem.Kind.LAND, p);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) NoiseSystem.emit(p.serverLevel(), p.position(), 12, NoiseSystem.Kind.VOICE, p);
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !(e.getTarget() instanceof XenomorphEntity))
            NoiseSystem.emit(p.serverLevel(), p.position(), 10, NoiseSystem.Kind.COMBAT, p);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && !p.isCreative())
            NoiseSystem.emit(p.serverLevel(), e.getPos(), 7, NoiseSystem.Kind.DEVICE, p);
    }

    // ------------------------------------------------------------------ no block breaking
    private static boolean protectedBlock(Player p, Block b) {
        if (p.isCreative() || p.isSpectator()) return false;
        if (AlienConfig.NO_BLOCK_BREAKING.get()) return true;
        if (!AlienConfig.PROTECT_STATION.get()) return false;
        var key = ForgeRegistries.BLOCKS.getKey(b);
        return key != null && AlienMod.MODID.equals(key.getNamespace());
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (protectedBlock(e.getPlayer(), e.getState().getBlock())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock e) {
        if (protectedBlock(e.getEntity(), e.getLevel().getBlockState(e.getPos()).getBlock())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (protectedBlock(e.getEntity(), e.getState().getBlock())) e.setCanceled(true);
    }

    // ------------------------------------------------------------------ no vanilla monsters inside the station
    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.SpawnPlacementCheck e) {
        if (e.getEntityType() == ModEntities.XENOMORPH.get()) return;
        var below = ForgeRegistries.BLOCKS.getKey(e.getLevel().getBlockState(e.getPos().below()).getBlock());
        if (below != null && AlienMod.MODID.equals(below.getNamespace())) e.setResult(Event.Result.DENY);
    }

    // ------------------------------------------------------------------ objectives
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent e) {
        if (e.phase == TickEvent.Phase.END && e.level instanceof ServerLevel sl) Objectives.tick(sl);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Objectives.sync(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) Objectives.sync(p);
    }

    // ------------------------------------------------------------------ cleanup
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            HidingManager.release(p);
            SaveManager.cancel(p);
            FlashlightTracker.remove(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            HidingManager.release(p);
            SaveManager.cancel(p);
            FlashlightTracker.remove(p);
            LAST_POS.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            HidingManager.release(p);
            FlashlightTracker.remove(p);
            Objectives.sync(p);
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        HidingManager.clear();
        SaveManager.clear();
        NoiseSystem.clear();
        LAST_POS.clear();
    }

    // ------------------------------------------------------------------ commands
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent e) {
        register(e.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("sevastopol").requires(s -> s.hasPermission(2))
                .then(Commands.literal("olustur").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    StationGenerator.generate(p.serverLevel(), p, p.getRandom().nextLong());
                    return 1;
                }))
                .then(Commands.literal("uzayli").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    XenomorphEntity x = ModEntities.XENOMORPH.get().create(p.serverLevel());
                    if (x == null) return 0;
                    Vec3 at = p.position().add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(-12));
                    x.moveTo(at.x, p.getY(), at.z, 0, 0);
                    p.serverLevel().addFreshEntity(x);
                    if (!VentNetwork.get(p.serverLevel()).isEmpty()) x.startInVents(100);
                    c.getSource().sendSuccess(() -> Component.translatable("message.alienisolation.released"), true);
                    return 1;
                }))
                .then(Commands.literal("temizle").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = 0;
                    for (XenomorphEntity x : p.serverLevel().getEntitiesOfClass(XenomorphEntity.class, p.getBoundingBox().inflate(512))) {
                        x.discard();
                        n++;
                    }
                    int count = n;
                    c.getSource().sendSuccess(() -> Component.literal("-" + count), true);
                    return n;
                })));
    }
}
