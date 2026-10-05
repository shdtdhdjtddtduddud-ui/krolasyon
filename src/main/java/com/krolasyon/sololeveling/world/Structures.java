package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.block.RegionPortalBlockEntity;
import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.registry.ModBlocks;
import com.krolasyon.sololeveling.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

/** Block placing helpers for regions and dungeons. */
public final class Structures {
    private Structures() {}

    public static final int FLAGS = 2 | 16;

    public static void set(ServerLevel l, int x, int y, int z, BlockState s) { l.setBlock(new BlockPos(x, y, z), s, FLAGS); }

    public static void fill(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) l.setBlock(m.set(x, y, z), s, FLAGS);
    }

    /** Box with walls/floor/ceiling of one block and air inside. */
    public static void room(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1, BlockState wall, BlockState floor, BlockState ceil) {
        fill(l, x0, y0, z0, x1, y0, z1, floor);
        fill(l, x0, y1, z0, x1, y1, z1, ceil);
        fill(l, x0, y0 + 1, z0, x1, y1 - 1, z0, wall);
        fill(l, x0, y0 + 1, z1, x1, y1 - 1, z1, wall);
        fill(l, x0, y0 + 1, z0, x0, y1 - 1, z1, wall);
        fill(l, x1, y0 + 1, z0, x1, y1 - 1, z1, wall);
        fill(l, x0 + 1, y0 + 1, z0 + 1, x1 - 1, y1 - 1, z1 - 1, Blocks.AIR.defaultBlockState());
    }

    public static void portal(ServerLevel l, BlockPos pos, String target) {
        l.setBlock(pos, ModBlocks.REGION_PORTAL.get().defaultBlockState(), 3);
        if (l.getBlockEntity(pos) instanceof RegionPortalBlockEntity be) {
            be.target = target;
            be.setChanged();
        }
    }

    /** A 7 wide gate arch along the x axis at base (centre), opening facing z. */
    public static void portalArch(ServerLevel l, BlockPos base, String target, BlockState frame, BlockState accent) {
        int x = base.getX(), y = base.getY(), z = base.getZ();
        fill(l, x - 4, y - 1, z - 2, x + 4, y - 1, z + 2, frame);
        fill(l, x - 3, y, z - 1, x + 3, y + 8, z + 1, Blocks.AIR.defaultBlockState());
        for (int dy = 0; dy <= 8; dy++) {
            set(l, x - 3, y + dy, z, dy % 3 == 2 ? accent : frame);
            set(l, x + 3, y + dy, z, dy % 3 == 2 ? accent : frame);
        }
        fill(l, x - 3, y + 7, z, x + 3, y + 8, z, frame);
        set(l, x, y + 8, z, accent);
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = 0; dy <= 6; dy++) portal(l, new BlockPos(x + dx, y + dy, z), target);
    }

    public static void sign(ServerLevel l, BlockPos pos, BlockState state, Component... lines) {
        l.setBlock(pos, state, 3);
        if (l.getBlockEntity(pos) instanceof SignBlockEntity s) {
            SignText t = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) t = t.setMessage(i, lines[i]);
            s.setText(t, true);
            s.setChanged();
        }
    }

    public static SLMonster spawn(ServerLevel l, MobKind k, double x, double y, double z, String instance) {
        SLMonster m = ModEntities.mob(k).create(l);
        if (m == null) return null;
        m.moveTo(x, y, z, l.random.nextFloat() * 360, 0);
        m.instance = instance;
        m.setPersistenceRequired();
        m.finalizeSpawn(l, l.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.STRUCTURE, null, null);
        l.addFreshEntity(m);
        return m;
    }
}
