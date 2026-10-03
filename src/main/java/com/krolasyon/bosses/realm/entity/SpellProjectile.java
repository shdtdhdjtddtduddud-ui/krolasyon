package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.entity.Ability.Element;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.HashSet;
import java.util.Set;

/** Elemental spell orb used by creatures, lords and the player's tomes. Optional gravity, splash and piercing. */
public class SpellProjectile extends Projectile {
    private static final EntityDataAccessor<Integer> DATA_ELEMENT = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_BIG = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.BOOLEAN);

    private float damage = 6F, gravity, aoe;
    private boolean pierce;
    private final Set<Integer> pierced = new HashSet<>();

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    public static SpellProjectile shoot(LivingEntity owner, Vec3 from, Vec3 velocity, Element el, float damage, float gravity, float aoe, boolean pierce) {
        SpellProjectile p = new SpellProjectile(RealmEntities.SPELL.get(), owner.level());
        p.setOwner(owner);
        p.moveTo(from.x, from.y, from.z, owner.getYRot(), owner.getXRot());
        p.setDeltaMovement(velocity);
        p.entityData.set(DATA_ELEMENT, el.ordinal());
        p.damage = damage;
        p.gravity = gravity;
        p.aoe = aoe;
        p.pierce = pierce;
        owner.level().addFreshEntity(p);
        return p;
    }

    public void setBig(boolean b) { this.entityData.set(DATA_BIG, b); }

    public boolean isBig() { return this.entityData.get(DATA_BIG); }

    public Element element() { return Element.values()[Math.floorMod(this.entityData.get(DATA_ELEMENT), Element.values().length)]; }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_ELEMENT, 0);
        this.entityData.define(DATA_BIG, false);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        if (!level().isClientSide()) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hit)) {
                onHit(hit);
                if (isRemoved()) return;
            }
            if (tickCount > (gravity > 0 ? 160 : 90)) {
                if (aoe > 0) splash(position());
                discard();
                return;
            }
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        if (gravity > 0) setDeltaMovement(v.x * 0.995, v.y - gravity, v.z * 0.995);
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
        if (level().isClientSide()) trail();
    }

    private void trail() {
        Element el = element();
        int n = isBig() ? 4 : 2;
        for (int i = 0; i < n; i++) {
            double ox = (random.nextDouble() - 0.5) * 0.3, oy = (random.nextDouble() - 0.5) * 0.3, oz = (random.nextDouble() - 0.5) * 0.3;
            level().addParticle(Abilities.dust(i % 2 == 0 ? el.color : el.light, isBig() ? 1.8F : 1.1F), getX() + ox, getY() + 0.2 + oy, getZ() + oz, 0, 0, 0);
        }
        if (random.nextInt(2) == 0) level().addParticle(Abilities.particle(el), getX(), getY() + 0.2, getZ(), 0, 0, 0);
        if (isBig() && random.nextInt(2) == 0) level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 0, 0.02, 0);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e) || e instanceof SpellProjectile || pierced.contains(e.getId())) return false;
        Entity owner = getOwner();
        return !(owner instanceof LivingEntity le) || Allegiance.hostile(le, e);
    }

    @Override
    protected void onHitEntity(EntityHitResult r) {
        Entity owner = getOwner();
        if (r.getEntity() instanceof LivingEntity e) {
            boolean hurt = e.hurt(damageSources().indirectMagic(this, owner), damage);
            if (hurt) {
                Abilities.applyElement(owner instanceof LivingEntity le ? le : null, e, element(), damage, 1);
                Vec3 k = getDeltaMovement().multiply(1, 0, 1);
                if (k.lengthSqr() > 1.0E-4) e.setDeltaMovement(e.getDeltaMovement().add(k.normalize().scale(0.35)).add(0, 0.12, 0));
            }
            if (pierce) {
                pierced.add(e.getId());
                return;
            }
        }
        if (aoe > 0) splash(position());
        else impactFx();
        discard();
    }

    @Override
    protected void onHit(HitResult r) {
        if (r.getType() == HitResult.Type.ENTITY) {
            onHitEntity((EntityHitResult) r);
            return;
        }
        if (aoe > 0) splash(r.getLocation());
        else impactFx();
        discard();
    }

    private void impactFx() {
        if (!(level() instanceof ServerLevel sl)) return;
        Element el = element();
        sl.sendParticles(Abilities.dust(el.color, 1.6F), getX(), getY(), getZ(), 12, 0.25, 0.25, 0.25, 0);
        sl.sendParticles(Abilities.particle(el), getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.5F, 1.6F);
    }

    private void splash(Vec3 c) {
        if (!(level() instanceof ServerLevel sl)) return;
        Element el = element();
        Entity owner = getOwner();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(aoe))) {
            if (owner instanceof LivingEntity le ? !Allegiance.hostile(le, e) : false) continue;
            double d = Math.sqrt(e.distanceToSqr(c));
            if (d > aoe + 0.5) continue;
            float f = (float) (1.0 - 0.5 * d / (aoe + 0.5));
            if (e.hurt(damageSources().explosion(this, owner), damage * f)) {
                Abilities.applyElement(owner instanceof LivingEntity le ? le : null, e, el, damage, 1);
                Vec3 k = e.position().subtract(c).multiply(1, 0, 1);
                if (k.lengthSqr() > 1.0E-4) k = k.normalize().scale(0.6);
                e.setDeltaMovement(e.getDeltaMovement().add(k.x, 0.35, k.z));
                e.hurtMarked = true;
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, isBig() ? 3 : 1, 0.4, 0.3, 0.4, 0);
        sl.sendParticles(Abilities.particle(el), c.x, c.y + 0.3, c.z, 30, aoe * 0.3, 0.3, aoe * 0.3, 0.15);
        sl.sendParticles(Abilities.dust(el.color, 2.2F), c.x, c.y + 0.3, c.z, 30, aoe * 0.4, 0.4, aoe * 0.4, 0);
        level().playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, isBig() ? 1.6F : 1.0F, 1.0F + random.nextFloat() * 0.3F);
    }

    @Override
    public boolean isOnFire() { return false; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { discard(); }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
