package com.sololeveling.world;

import com.sololeveling.SoloLeveling;
import com.sololeveling.entity.GateEntity;
import com.sololeveling.entity.ShadowEntity;
import com.sololeveling.gen.Content;
import com.sololeveling.player.ModCaps;
import com.sololeveling.player.SLPlayer;
import com.sololeveling.registry.ModDimensions;
import com.sololeveling.registry.ModEntities;
import com.sololeveling.system.*;
import com.sololeveling.util.Ranks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Gate creation, entering/leaving and random world gate spawning. */
@Mod.EventBusSubscriber(modid = SoloLeveling.MODID)
public final class GateManager {
    private GateManager() {}

    public record Info(String dim, double x, double y, double z, String rank, String theme, boolean red) {}

    public static final Map<UUID, Info> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();

    public static boolean themeFits(String ranks, String rank) {
        int r = Ranks.index(rank);
        String[] parts = ranks.split("-");
        int lo = Ranks.index(parts[0]);
        int hi = parts.length > 1 ? Ranks.index(parts[1]) : lo;
        if (hi < lo) { int t = lo; lo = hi; hi = t; }
        return r >= lo && r <= hi;
    }

    public static String randomTheme(String rank, RandomSource rnd) {
        List<String> ok = new ArrayList<>();
        for (Content.ThemeDef t : Content.THEMES) if (themeFits(t.ranks(), rank)) ok.add(t.id());
        if (ok.isEmpty()) ok.add(Ranks.index(rank) >= 6 ? "dragon_lair" : "demon_castle");
        return ok.get(rnd.nextInt(ok.size()));
    }

    public static GateEntity spawnGate(ServerLevel level, double x, double y, double z, String rank, String theme, boolean red, boolean announce) {
        GateEntity g = ModEntities.GATE.get().create(level);
        if (g == null) return null;
        g.moveTo(x, y, z, level.random.nextFloat() * 360F, 0);
        g.setup(rank, theme, red, GateEntity.ENTRANCE);
        g.returnDim = level.dimension().location().toString();
        level.addFreshEntity(g);
        if (announce) {
            NewsManager.add(level.getServer(), NewsManager.GATE, Component.translatable(red ? "news.sololeveling.red_gate" : "news.sololeveling.gate",
                    Ranks.tag(rank), Component.translatable("theme.sololeveling." + theme), (int) x, (int) z));
        }
        return g;
    }

    /** gate opened with a key in front of the player */
    public static boolean openKeyGate(ServerPlayer sp, boolean red) {
        ServerLevel lvl = sp.serverLevel();
        if (lvl.dimension() == ModDimensions.DUNGEON) { Sys.warn(sp, "gui.sololeveling.no_gate_here"); return false; }
        SLPlayer d = ModCaps.get(sp);
        int idx = Ranks.index(Ranks.forLevel(d.level));
        int roll = sp.getRandom().nextInt(100);
        idx += roll < 20 ? -1 : (roll > 82 ? 1 : 0);
        if (red) idx += 1;
        idx = Math.max(0, Math.min(6, idx));
        String rank = Ranks.ORDER[idx];
        String theme = randomTheme(rank, sp.getRandom());
        Vec3 look = sp.getLookAngle().multiply(1, 0, 1).normalize();
        double x = sp.getX() + look.x * 5, z = sp.getZ() + look.z * 5;
        spawnGate(lvl, x, sp.getY(), z, rank, theme, red, true);
        lvl.playSound(null, sp.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.8F, 1.2F);
        Sys.notify(sp, Sys.WARN, Component.translatable("gui.sololeveling.gate_opened"), Component.translatable("gui.sololeveling.gate_name", Ranks.tag(rank), Component.translatable("theme.sololeveling." + theme)));
        return true;
    }

    public static void enter(ServerPlayer sp, GateEntity gate) {
        long now = sp.serverLevel().getGameTime();
        Long cd = COOLDOWN.get(sp.getUUID());
        if (cd != null && now < cd) return;
        COOLDOWN.put(sp.getUUID(), now + 60);
        MinecraftServer server = sp.getServer();
        ServerLevel dl = server.getLevel(ModDimensions.DUNGEON);
        if (dl == null) { Sys.warn(sp, "gui.sololeveling.no_dimension"); return; }
        SLPlayer d = ModCaps.get(sp);
        int need = switch (gate.rank()) { case "E" -> 1; case "D" -> 8; case "C" -> 20; case "B" -> 35; case "A" -> 55; case "S" -> 75; default -> 95; };
        if (d.level < need - 6)
            Sys.notify(sp, Sys.WARN, Component.translatable("gui.sololeveling.warning"), Component.translatable("gui.sololeveling.gate_dangerous", need));
        DungeonManager.Inst inst = DungeonManager.ensure(sp.serverLevel(), gate);
        if (inst == null) return;
        ServerLevel from = sp.serverLevel();
        List<ServerPlayer> party = new ArrayList<>();
        for (ServerPlayer p : from.players()) if (p.distanceToSqr(gate) < 12 * 12 && !p.isSpectator()) party.add(p);
        if (!party.contains(sp)) party.add(sp);
        int i = 0;
        for (ServerPlayer p : party) {
            Vec3 pos = new Vec3(inst.spawn.getX() + 0.5 + (i % 3) - 1, inst.spawn.getY(), inst.spawn.getZ() + 0.5 + (i / 3));
            i++;
            Travel.teleport(p, dl, pos.x, pos.y, pos.z, -90F);
            p.playNotifySound(com.sololeveling.registry.ModSounds.GATE_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Sys.notify(p, Sys.WARN, Component.translatable("gui.sololeveling.gate_entered"),
                    Component.translatable("gui.sololeveling.gate_name", Ranks.tag(inst.rank), Component.translatable("theme.sololeveling." + inst.theme)));
            Net(p, "gate");
        }
    }

    private static void Net(ServerPlayer p, String fx) {
        com.sololeveling.net.Net.toPlayer(p, new com.sololeveling.net.Packets.Fx(fx, 0));
    }

    public static void exit(ServerPlayer sp, GateEntity gate) {
        long now = sp.serverLevel().getGameTime();
        Long cd = COOLDOWN.get(sp.getUUID());
        if (cd != null && now < cd) return;
        COOLDOWN.put(sp.getUUID(), now + 60);
        MinecraftServer server = sp.getServer();
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(gate.returnDim));
        ServerLevel dest = server.getLevel(key);
        if (dest == null) dest = server.overworld();
        BlockPos rp = gate.returnPos;
        if (rp.equals(BlockPos.ZERO)) rp = dest.getSharedSpawnPos();
        ServerLevel from = sp.serverLevel();
        List<ServerPlayer> party = new ArrayList<>();
        for (ServerPlayer p : from.players()) if (p.distanceToSqr(gate) < 12 * 12 && !p.isSpectator()) party.add(p);
        if (!party.contains(sp)) party.add(sp);
        int i = 0;
        for (ServerPlayer p : party) {
            double x = rp.getX() + 0.5 + (i % 3) - 1, z = rp.getZ() + 4.5 + (i / 3);
            i++;
            int y = Travel.surfaceY(dest, (int) x, (int) z);
            p.playNotifySound(com.sololeveling.registry.ModSounds.GATE_ENTER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            Travel.teleport(p, dest, x, Math.max(y, rp.getY()), z, 0F);
            Net(p, "gate");
        }
    }

    // ---------------------------------------------------------- registry of active gates
    public static void register(GateEntity g) {
        if (g.mode() == GateEntity.ENTRANCE && !g.level().isClientSide)
            ACTIVE.put(g.getUUID(), new Info(g.level().dimension().location().toString(), g.getX(), g.getY(), g.getZ(), g.rank(), g.theme(), g.isRed()));
    }

    public static void unregister(GateEntity g) { ACTIVE.remove(g.getUUID()); }

    public static CompoundTag activeTag(String dim) {
        CompoundTag t = new CompoundTag();
        ListTag l = new ListTag();
        for (Info i : ACTIVE.values()) {
            if (!i.dim().equals(dim)) continue;
            CompoundTag c = new CompoundTag();
            c.putString("rank", i.rank()); c.putString("theme", i.theme()); c.putBoolean("red", i.red());
            c.putInt("x", (int) i.x()); c.putInt("y", (int) i.y()); c.putInt("z", (int) i.z());
            l.add(c);
        }
        t.put("g", l);
        return t;
    }

    // ---------------------------------------------------------- natural spawning
    private static int timer = 0;

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (++timer < 600) return;
        timer = 0;
        MinecraftServer server = e.getServer();
        if (server == null) return;
        for (ServerLevel lvl : server.getAllLevels()) {
            boolean hw = lvl.dimension() == ModDimensions.HUNTER_WORLD;
            if (!hw && lvl.dimension() != Level.OVERWORLD) continue;
            List<ServerPlayer> ps = lvl.players();
            if (ps.isEmpty()) continue;
            ServerPlayer p = ps.get(lvl.random.nextInt(ps.size()));
            if (lvl.random.nextInt(100) >= (hw ? 14 : 6)) continue;
            int near = 0;
            for (Info i : ACTIVE.values()) if (i.dim().equals(lvl.dimension().location().toString()) && Math.hypot(i.x() - p.getX(), i.z() - p.getZ()) < 260) near++;
            if (near >= 3) continue;
            int lv = ModCaps.get(p).level;
            int idx = Ranks.index(Ranks.forLevel(lv));
            int roll = lvl.random.nextInt(100);
            idx += roll < 25 ? -1 : (roll > 78 ? 1 : 0);
            idx = Math.max(0, Math.min(5, idx));
            String rank = Ranks.ORDER[idx];
            boolean red = idx >= 2 && lvl.random.nextInt(8) == 0;
            double a = lvl.random.nextDouble() * Math.PI * 2, dist = 45 + lvl.random.nextDouble() * 70;
            int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
            int y = Travel.surfaceY(lvl, x, z);
            if (hw) {
                Content.RegionDef reg = Regions.at((int) p.getX(), (int) p.getZ());
                if (reg != null) {
                    BlockPos spot = CityGen.gateSpot(reg, lvl.getSeed(), lvl.random);
                    if (spot == null) continue;
                    x = spot.getX(); y = spot.getY(); z = spot.getZ();
                } else if (Regions.at(x, z) != null) continue;
            }
            if (lvl.getBlockState(new BlockPos(x, y - 1, z)).getFluidState().isEmpty() == false) continue;
            spawnGate(lvl, x + 0.5, y, z + 0.5, rank, randomTheme(rank, lvl.random), red, true);
        }
    }
}
