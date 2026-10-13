package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

/**
 * A large beyblade stadium (about 19 blocks across). Purely visual for players; beyblades inside it ride the curved
 * bowl (see {@link BeybladeEntity}), bounce off the striped walls and are knocked out ("Over Finish") when they fly
 * out through one of the four pockets between the walls. Sneak + right click or hit it to pick it up.
 */
public class StadiumEntity extends Entity {
    /** bowl radius and floor curve: y = FLOOR_Y0 + FLOOR_RISE * (r / BOWL_R)^2 */
    public static final float BOWL_R = 7.0F, FLOOR_Y0 = 0.30F, FLOOR_RISE = 0.85F;
    public static final float LIP_R = 7.45F, LIP_Y = 1.25F, LEDGE_R = 8.3F;
    public static final float WALL_IN = 7.45F, WALL_OUT = 7.8F, WALL_TOP = 2.05F;
    /** walls span +-30 degrees around 45 + 90k; the 30-degree gaps between them are the over-finish pockets */
    public static final float WALL_HALF_DEG = 30F;

    public StadiumEntity(EntityType<? extends StadiumEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
    }

    public static boolean isGap(double angleRad) {
        double deg = Math.toDegrees(angleRad);
        double m = ((deg - 45.0) % 90.0 + 90.0) % 90.0;     // 0 at a wall centre
        double off = Math.min(m, 90.0 - m);
        return off > WALL_HALF_DEG;
    }

    /** height of the playing surface above the stadium origin at horizontal distance r, or -1 beyond the ledge */
    public static double surface(double r) {
        if (r <= BOWL_R) {
            double k = r / BOWL_R;
            return FLOOR_Y0 + FLOOR_RISE * k * k;
        }
        if (r <= LIP_R) return FLOOR_Y0 + FLOOR_RISE + (LIP_Y - FLOOR_Y0 - FLOOR_RISE) * (r - BOWL_R) / (LIP_R - BOWL_R);
        if (r <= LEDGE_R) return LIP_Y;
        return -1;
    }

    public static double slope(double r) {
        return r <= BOWL_R ? 2.0 * FLOOR_RISE * r / (BOWL_R * BOWL_R) : 0.0;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
    }

    private void pickUp(Player player) {
        if (this.level().isClientSide() || isRemoved()) return;
        ItemStack s = new ItemStack(ModItems.STADIUM.get());
        if (!player.getAbilities().instabuild && !player.getInventory().add(s)) spawnAtLocation(s, 0.5F);
        ((ServerLevel) this.level()).sendParticles(ParticleTypes.POOF, getX(), getY() + 0.6, getZ(), 40, 4, 0.3, 4, 0.02);
        this.level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.7F);
        discard();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        pickUp(player);
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (source.getEntity() instanceof Player p) {
            pickUp(p);
            return true;
        }
        return false;
    }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(10.5, 2.5, 10.5); }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 160 * 160; }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
