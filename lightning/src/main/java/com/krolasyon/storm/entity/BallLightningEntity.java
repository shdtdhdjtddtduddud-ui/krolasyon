package com.krolasyon.storm.entity;

import com.krolasyon.storm.Fx;
import com.krolasyon.storm.ModRegistry;
import com.krolasyon.storm.SkillLogic;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** "Top Yıldırım": slow orb that zaps nearby enemies on its way and detonates at the end. */
public class BallLightningEntity extends Projectile {
    public static final int LIFE = 100;

    public BallLightningEntity(EntityType<? extends BallLightningEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public BallLightningEntity(Level level, LivingEntity owner) {
        this(ModRegistry.BALL.get(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public boolean isNoGravity() { return true; }

    @Override
    public void tick() {
        super.tick();
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        Vec3 next = pos.add(vel);
        if (level() instanceof ServerLevel sl) {
            if (!(getOwner() instanceof ServerPlayer owner)) { discard(); return; }
            BlockHitResult bh = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean impact = bh.getType() != HitResult.Type.MISS;
            if (impact) next = bh.getLocation();
            if (!impact) {
                for (Entity e : sl.getEntities(this, getBoundingBox().expandTowards(vel).inflate(0.2), e -> SkillLogic.canHit(owner, e, false))) {
                    impact = true;
                    break;
                }
            }
            if (tickCount % 8 == 0) {
                int n = 0;
                for (LivingEntity t : SkillLogic.enemiesAround(owner, pos, 6, true)) {
                    if (n++ >= 2) break;
                    SkillLogic.hit(t, 4F, this, owner);
                    Fx.send(sl, Fx.Type.ARC, pos, t.position().add(0, t.getBbHeight() / 2, 0), getId(), t.getId(), 6, 1F);
                    SkillLogic.sparks(sl, t.position().add(0, t.getBbHeight() / 2, 0), 6, 0.25);
                }
                if (n > 0) SkillLogic.play(sl, pos, ModRegistry.ZAP.get(), 0.7F, 1.2F);
            }
            if (tickCount % 20 == 0) SkillLogic.play(sl, pos, ModRegistry.BALL_HUM.get(), 0.6F, 1.1F);
            if (impact || tickCount >= LIFE) {
                explode(sl, owner, next);
                return;
            }
        } else if (random.nextInt(2) == 0) {
            level().addParticle(ModRegistry.SPARK.get(), getX() + (random.nextDouble() - 0.5) * 0.8, getY() + 0.45 + (random.nextDouble() - 0.5) * 0.8,
                    getZ() + (random.nextDouble() - 0.5) * 0.8, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1, (random.nextDouble() - 0.5) * 0.1);
        }
        setDeltaMovement(vel.scale(0.995));
        setPos(next.x, next.y, next.z);
    }

    private void explode(ServerLevel sl, ServerPlayer owner, Vec3 at) {
        Vec3 c = at.add(0, 0.45, 0);
        for (LivingEntity t : SkillLogic.enemiesAround(owner, c, 4.5, false)) {
            if (!SkillLogic.canHit(owner, t, true) && t.distanceToSqr(c) > 2.5 * 2.5) continue;
            SkillLogic.hit(t, 10F, this, owner);
            SkillLogic.push(t, c, 1.0);
        }
        Fx.at(sl, Fx.Type.NOVA, c, 14, 4.5F);
        Fx.at(sl, Fx.Type.BURST, c, 14, 3F);
        SkillLogic.sparks(sl, c, 50, 0.8);
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        SkillLogic.play(sl, c, ModRegistry.NOVA.get(), 1.6F, 1.5F);
        discard();
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }
}
