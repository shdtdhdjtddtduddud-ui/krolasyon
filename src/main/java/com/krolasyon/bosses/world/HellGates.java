package com.krolasyon.bosses.world;

import com.krolasyon.bosses.KrolasyonBosses;
import com.krolasyon.bosses.block.HellPortalBlock;
import com.krolasyon.bosses.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Portal travel between the overworld and Azrakor: delay, destination gate lookup / creation, teleporter. */
public final class HellGates {
    private HellGates() {}

    public static final ResourceKey<Level> AZRAKOR = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(KrolasyonBosses.MODID, "azrakor"));

    // ------------------------------------------------------------------ entering the portal
    public static void touch(Entity entity, BlockPos pos) {
        Level level = entity.level();
        CompoundTag data = entity.getPersistentData();
        long now = level.getGameTime();
        int t = now - data.getLong("kr_gate_last") > 4 ? 0 : data.getInt("kr_gate_t");
        t++;
        data.putLong("kr_gate_last", now);
        data.putInt("kr_gate_t", t);
        boolean instant = entity instanceof Player p && p.getAbilities().instabuild;
        if (t >= (instant ? 2 : 90)) {
            data.putInt("kr_gate_t", 0);
            travel(entity, pos);
        }
    }

    private static void travel(Entity entity, BlockPos pos) {
        MinecraftServer server = entity.getServer();
        if (server == null) return;
        ResourceKey<Level> target = entity.level().dimension() == AZRAKOR ? Level.OVERWORLD : AZRAKOR;
        ServerLevel dest = server.getLevel(target);
        if (dest == null) return;
        entity.changeDimension(dest, new Teleporter(pos));
    }

    public static final class Teleporter implements ITeleporter {
        private final BlockPos from;

        public Teleporter(BlockPos from) { this.from = from; }

        @Override
        public PortalInfo getPortalInfo(Entity entity, ServerLevel dest, Function<ServerLevel, PortalInfo> defaultInfo) {
            Vec3 p = destination((ServerLevel) entity.level(), dest, from);
            return new PortalInfo(p, Vec3.ZERO, entity.getYRot(), entity.getXRot());
        }

        @Override
        public boolean playTeleportSound(ServerPlayer player, ServerLevel src, ServerLevel dst) { return false; }

        @Override
        public Entity placeEntity(Entity entity, ServerLevel src, ServerLevel dst, float yaw, Function<Boolean, Entity> reposition) {
            Entity e = reposition.apply(false);
            if (e != null) dst.playSound(null, e.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 0.6F + dst.random.nextFloat() * 0.3F);
            return e;
        }
    }

    // ------------------------------------------------------------------ gate bookkeeping
    public static class Data extends SavedData {
        public final Map<Long, Long> over2azra = new HashMap<>();
        public final Map<Long, Long> azra2over = new HashMap<>();

        public static Data load(CompoundTag tag) {
            Data d = new Data();
            ListTag l = tag.getList("pairs", Tag.TAG_COMPOUND);
            for (int i = 0; i < l.size(); i++) {
                CompoundTag c = l.getCompound(i);
                d.over2azra.put(c.getLong("o"), c.getLong("a"));
                d.azra2over.put(c.getLong("a"), c.getLong("o"));
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag l = new ListTag();
            for (Map.Entry<Long, Long> e : over2azra.entrySet()) {
                CompoundTag c = new CompoundTag();
                c.putLong("o", e.getKey());
                c.putLong("a", e.getValue());
                l.add(c);
            }
            tag.put("pairs", l);
            return tag;
        }

        public void link(BlockPos over, BlockPos azra) {
            over2azra.put(over.asLong(), azra.asLong());
            azra2over.put(azra.asLong(), over.asLong());
            setDirty();
        }
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "krolasyon_hellgates");
    }

    /** lowest, western-most block of the portal sheet containing pos */
    public static BlockPos origin(Level level, BlockPos pos) {
        BlockPos p = pos;
        while (level.getBlockState(p.below()).is(ModBlocks.HELL_PORTAL.get())) p = p.below();
        BlockState st = level.getBlockState(p);
        Direction back = st.getValue(HellPortalBlock.AXIS) == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        while (level.getBlockState(p.relative(back)).is(ModBlocks.HELL_PORTAL.get())) p = p.relative(back);
        return p;
    }

    /** where the traveller must be placed: in front of the destination gate (which is created if needed) */
    static Vec3 destination(ServerLevel src, ServerLevel dst, BlockPos from) {
        MinecraftServer server = src.getServer();
        Data d = data(server);
        BlockPos origin = src.getBlockState(from).is(ModBlocks.HELL_PORTAL.get()) ? origin(src, from) : from;
        boolean toAzra = dst.dimension() == AZRAKOR;
        Long mapped = toAzra ? d.over2azra.get(origin.asLong()) : d.azra2over.get(origin.asLong());
        BlockPos gate;
        if (mapped != null && dst.getBlockState(BlockPos.of(mapped)).is(ModBlocks.HELL_PORTAL.get())) {
            gate = BlockPos.of(mapped);
        } else {
            int x = origin.getX(), z = origin.getZ();
            dst.getChunk(x >> 4, z >> 4);
            int y = dst.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            if (toAzra && y < dst.getSeaLevel() + 2) y = dst.getSeaLevel() + 2;
            if (!toAzra) y = Math.max(y, dst.getMinBuildHeight() + 2);
            gate = buildGate(dst, new BlockPos(x, y, z), Direction.Axis.X);
            if (toAzra) d.link(origin, gate);
            else d.link(gate, origin);
        }
        return frontOf(dst, gate);
    }

    /** a free standing spot beside the gate, on the side the gate faces */
    public static Vec3 frontOf(ServerLevel level, BlockPos gate) {
        BlockState st = level.getBlockState(gate);
        Direction.Axis axis = st.is(ModBlocks.HELL_PORTAL.get()) ? st.getValue(HellPortalBlock.AXIS) : Direction.Axis.X;
        Direction side = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        for (Direction dir : new Direction[]{side, side.getOpposite()}) {
            for (int dist = 2; dist <= 3; dist++) {
                BlockPos p = gate.relative(dir, dist).relative(axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, 1);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && !level.getBlockState(p.below()).isAir()) return Vec3.atBottomCenterOf(p);
            }
        }
        return Vec3.atBottomCenterOf(gate.above(1).relative(side, 2));
    }

    /**
     * Builds a small ruined gate (frame 4x5, lit portal, braziers, apron) with its portal origin at {@code base}; returns the origin
     * of the portal sheet.
     */
    public static BlockPos buildGate(ServerLevel level, BlockPos base, Direction.Axis axis) {
        BlockState frame = ModBlocks.get("hellgate_stone").defaultBlockState();
        BlockState floor = ModBlocks.get("ashstone_bricks").defaultBlockState();
        BlockState lamp = ModBlocks.get("ember_lamp").defaultBlockState();
        BlockState portal = ModBlocks.HELL_PORTAL.get().defaultBlockState().setValue(HellPortalBlock.AXIS, axis);
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction across = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
        BlockPos o = base;   // origin of the lowest-west portal block
        // clear a generous volume and lay the apron
        for (int a = -4; a <= 6; a++) {
            for (int c = -4; c <= 4; c++) {
                for (int y = -1; y <= 7; y++) {
                    BlockPos p = o.relative(along, a).relative(across, c).above(y);
                    boolean inside = Math.abs(a - 1) <= 4 && Math.abs(c) <= 3;
                    if (y == -1) {
                        if (inside) level.setBlock(p, floor, 2);
                    } else if (inside) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        // frame: columns at a=-1 and a=2 (interior a=0..1), rows at y=-1 (floor row) and y=3
        for (int y = -1; y <= 3; y++) {
            level.setBlock(o.relative(along, -1).above(y), frame, 2);
            level.setBlock(o.relative(along, 2).above(y), frame, 2);
        }
        for (int a = 0; a <= 1; a++) {
            level.setBlock(o.relative(along, a).above(-1), frame, 2);
            level.setBlock(o.relative(along, a).above(3), frame, 2);
        }
        for (int a = 0; a <= 1; a++) for (int y = 0; y <= 2; y++) level.setBlock(o.relative(along, a).above(y), portal, 18);
        // braziers on top of the pillars and two standing lamps in front
        level.setBlock(o.relative(along, -1).above(4), lamp, 2);
        level.setBlock(o.relative(along, 2).above(4), lamp, 2);
        level.setBlock(o.relative(along, -3).relative(across, 2), lamp, 2);
        level.setBlock(o.relative(along, 4).relative(across, 2), lamp, 2);
        return o;
    }
}
