package com.sololeveling.entity;

import com.sololeveling.world.DungeonManager;
import com.sololeveling.world.GateManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** A dungeon gate (entrance) or return gate (exit). */
public class GateEntity extends Entity {
    public static final int ENTRANCE = 0, EXIT = 1;
    public static final int BREAK_TICKS = 20 * 60 * 40;   // 40 minutes until a dungeon break

    private static final EntityDataAccessor<String> RANK = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> THEME = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> RED = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BROKEN = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);

    public long dungeonId = -1;
    public String returnDim = "minecraft:overworld";
    public BlockPos returnPos = BlockPos.ZERO;
    public int age2 = 0;
    private final Map<UUID, Integer> inside = new HashMap<>();

    public GateEntity(EntityType<? extends GateEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RANK, "E");
        entityData.define(THEME, "goblin_cave");
        entityData.define(RED, false);
        entityData.define(MODE, ENTRANCE);
        entityData.define(BROKEN, false);
    }

    public String rank() { return entityData.get(RANK); }
    public String theme() { return entityData.get(THEME); }
    public boolean isRed() { return entityData.get(RED); }
    public int mode() { return entityData.get(MODE); }
    public boolean isBroken() { return entityData.get(BROKEN); }

    public void setup(String rank, String theme, boolean red, int mode) {
        entityData.set(RANK, rank);
        entityData.set(THEME, theme);
        entityData.set(RED, red);
        entityData.set(MODE, mode);
        if (mode == ENTRANCE) setCustomName(Component.translatable("gui.sololeveling.gate_name", Component.translatable("rank.sololeveling." + rank), Component.translatable("theme.sololeveling." + theme)));
        else setCustomName(Component.translatable("gui.sololeveling.exit_gate"));
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        GateManager.register(this);
    }

    @Override
    public void onRemovedFromWorld() {
        super.onRemovedFromWorld();
        GateManager.unregister(this);
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 256 * 256; }

    @Override
    public void tick() {
        super.tick();
        age2++;
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() + Math.cos(a) * 1.7, getY() + 2.4 + Math.sin(a) * 2.2, getZ() + Math.sin(a) * 0.3, 0, 0.02, 0);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        if (age2 % 80 == 0) sl.playSound(null, blockPosition(), SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 0.4F, 0.8F);
        if (mode() == ENTRANCE && !isBroken() && age2 > BREAK_TICKS) {
            entityData.set(BROKEN, true);
            DungeonManager.dungeonBreak(this);
        }
        // contact: stand inside for ~1.2s
        AABB box = getBoundingBox().inflate(0.2, 0, 0.2);
        Iterator<Map.Entry<UUID, Integer>> it = inside.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> en = it.next();
            Player p = sl.getPlayerByUUID(en.getKey());
            if (p == null || !p.getBoundingBox().intersects(box)) it.remove();
        }
        for (Player p : sl.getEntitiesOfClass(Player.class, box)) {
            if (!(p instanceof ServerPlayer sp) || sp.isSpectator()) continue;
            int t = inside.merge(p.getUUID(), 1, Integer::sum);
            if (t == 3) sl.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.AMBIENT, 1F, 1.5F);
            if (t >= 24) {
                inside.remove(p.getUUID());
                if (mode() == ENTRANCE) GateManager.enter(sp, this);
                else GateManager.exit(sp, this);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        setup(t.getString("rank"), t.getString("theme"), t.getBoolean("red"), t.getInt("mode"));
        entityData.set(BROKEN, t.getBoolean("broken"));
        dungeonId = t.contains("dungeon") ? t.getLong("dungeon") : -1;
        returnDim = t.contains("rdim") ? t.getString("rdim") : "minecraft:overworld";
        returnPos = new BlockPos(t.getInt("rx"), t.getInt("ry"), t.getInt("rz"));
        age2 = t.getInt("age2");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putString("rank", rank()); t.putString("theme", theme()); t.putBoolean("red", isRed()); t.putInt("mode", mode());
        t.putBoolean("broken", isBroken());
        t.putLong("dungeon", dungeonId);
        t.putString("rdim", returnDim);
        t.putInt("rx", returnPos.getX()); t.putInt("ry", returnPos.getY()); t.putInt("rz", returnPos.getZ());
        t.putInt("age2", age2);
    }
}
