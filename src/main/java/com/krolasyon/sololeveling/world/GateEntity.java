package com.krolasyon.sololeveling.world;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.registry.ModSounds;
import com.krolasyon.sololeveling.system.Rank;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** A dimensional gate: a swirling vortex standing in the world that leads to a dungeon instance. */
public class GateEntity extends Entity {
    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RED = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(GateEntity.class, EntityDataSerializers.INT);

    public String instance = "";
    public String place = "seoul";
    public int age;
    /** Ticks until a dungeon break happens when nobody clears the gate. */
    public int breakAt = 20 * 60 * 40;
    /** >0: an NPC raid team entered and will clear the gate at this age. */
    public int npcClearAt;
    public String npcGuild = "";
    private int closeTimer;

    public GateEntity(EntityType<?> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(RANK, 0);
        entityData.define(RED, false);
        entityData.define(STATE, 0);
    }

    public Rank rank() { return Rank.byOrdinal(entityData.get(RANK)); }

    public boolean isRed() { return entityData.get(RED); }

    public boolean isClosing() { return entityData.get(STATE) == 1; }

    public void setup(Rank r, boolean red, String instance, String place) {
        entityData.set(RANK, r.ordinal());
        entityData.set(RED, red);
        this.instance = instance;
        this.place = place;
    }

    public void close() {
        entityData.set(STATE, 1);
        closeTimer = 60;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientFx();
            return;
        }
        age++;
        if (isClosing()) {
            if (--closeTimer <= 0) discard();
            return;
        }
        if (age % 80 == 0) level().playSound(null, blockPosition(), ModSounds.GATE_HUM.get(), SoundSource.AMBIENT, 1.2F, isRed() ? 0.7F : 1F);
        if (age % 5 == 0) {
            for (ServerPlayer p : level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().deflate(0.6, 0, 0.6))) {
                if (p.isOnPortalCooldown() || p.isSpectator()) continue;
                p.setPortalCooldown(80);
                DungeonManager.Instance in = DungeonManager.get(p.server).get(instance);
                if (in == null) {
                    discard();
                    return;
                }
                if (in.gatePos.equals(net.minecraft.core.BlockPos.ZERO)) {
                    in.gateDim = level().dimension().location().toString();
                    in.gatePos = blockPosition();
                }
                DungeonManager.get(p.server).enter(p, in);
            }
        }
        if (npcClearAt > 0 && age >= npcClearAt) GateManager.npcCleared((ServerLevel) level(), this);
        else if (age >= breakAt) GateManager.dungeonBreak((ServerLevel) level(), this);
    }

    private void clientFx() {
        int c = isRed() ? 0xFF2030 : rank().color;
        DustParticleOptions dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F), 1.5F);
        float h = getBbHeight();
        for (int i = 0; i < 3; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 1.2 + random.nextDouble() * 1.6;
            level().addParticle(dust, getX() + Math.cos(a) * r * 0.25, getY() + h * 0.5 + Math.sin(a) * r, getZ() + Math.cos(a) * r, 0, 0, 0);
        }
        if (random.nextInt(3) == 0)
            level().addParticle(ParticleTypes.PORTAL, getX(), getY() + h * 0.5, getZ(), (random.nextDouble() - 0.5) * 2, (random.nextDouble() - 0.5) * 2, (random.nextDouble() - 0.5) * 2);
    }

    /** Monster type that pours out during a dungeon break. */
    public MobKind breakMob() {
        DungeonManager.Instance in = level() instanceof ServerLevel sl ? DungeonManager.get(sl.getServer()).get(instance) : null;
        if (in == null) return MobKind.GOBLIN;
        return in.theme.mobs.get(random.nextInt(in.theme.mobs.size()));
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 256 * 256; }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(RANK, t.getInt("Rank"));
        entityData.set(RED, t.getBoolean("Red"));
        instance = t.getString("Instance");
        place = t.getString("Place");
        age = t.getInt("Age");
        breakAt = t.contains("BreakAt") ? t.getInt("BreakAt") : breakAt;
        npcClearAt = t.getInt("NpcClearAt");
        npcGuild = t.getString("NpcGuild");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("Rank", entityData.get(RANK));
        t.putBoolean("Red", isRed());
        t.putString("Instance", instance);
        t.putString("Place", place);
        t.putInt("Age", age);
        t.putInt("BreakAt", breakAt);
        t.putInt("NpcClearAt", npcClearAt);
        t.putString("NpcGuild", npcGuild);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }

    @Override
    public boolean isNoGravity() { return true; }
}
