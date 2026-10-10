package com.krolasyon.storm.entity;

import com.krolasyon.storm.Fx;
import com.krolasyon.storm.ModRegistry;
import com.krolasyon.storm.SkillLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/** "Şimşek Oku": very fast piercing bolt, hits up to 3 targets. */
public class BoltProjectile extends Projectile {
    private final Set<Integer> hit = new HashSet<>();

    public BoltProjectile(EntityType<? extends BoltProjectile> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public BoltProjectile(Level level, LivingEntity owner) {
        this(ModRegistry.BOLT.get(), level);
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
            BlockHitResult bh = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            boolean wall = bh.getType() != HitResult.Type.MISS;
            if (wall) next = bh.getLocation();
            if (getOwner() instanceof ServerPlayer owner) {
                final Vec3 end = next;
                for (Entity e : sl.getEntities(this, getBoundingBox().expandTowards(vel).inflate(1.0), e -> SkillLogic.canHit(owner, e, false))) {
                    if (hit.contains(e.getId()) || e.getBoundingBox().inflate(0.35).clip(pos, end).isEmpty()) continue;
                    hit.add(e.getId());
                    LivingEntity le = (LivingEntity) e;
                    SkillLogic.hit(le, 7F, this, owner);
                    le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    Fx.at(sl, Fx.Type.BURST, le.position().add(0, le.getBbHeight() / 2, 0), 8, 1.2F);
                    SkillLogic.sparks(sl, le.position().add(0, le.getBbHeight() / 2, 0), 10, 0.3);
                    SkillLogic.play(sl, le.position(), ModRegistry.ZAP.get(), 0.8F, 1.4F);
                }
            }
            if (wall || hit.size() >= 3 || tickCount > 30) {
                SkillLogic.sparks(sl, next, 14, 0.3);
                Fx.at(sl, Fx.Type.BURST, next, 8, 1.4F);
                discard();
                return;
            }
        }
        setPos(next.x, next.y, next.z);
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }
}
