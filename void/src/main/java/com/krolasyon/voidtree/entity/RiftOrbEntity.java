package com.krolasyon.voidtree.entity;

import com.krolasyon.voidtree.Fx;
import com.krolasyon.voidtree.ModRegistry;
import com.krolasyon.voidtree.SkillLogic;
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

/** "Boyut Yırtığı": thrown rift; where it lands the caster steps through and the rift bursts. */
public class RiftOrbEntity extends Projectile {
    public RiftOrbEntity(EntityType<? extends RiftOrbEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public RiftOrbEntity(Level level, LivingEntity owner) {
        this(ModRegistry.RIFT_ORB.get(), level);
        setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        Vec3 next = pos.add(vel);
        if (level() instanceof ServerLevel sl) {
            if (!(getOwner() instanceof ServerPlayer owner) || owner.level() != sl) { discard(); return; }
            BlockHitResult bh = sl.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            Vec3 land = null;
            if (bh.getType() != HitResult.Type.MISS) land = bh.getLocation().add(Vec3.atLowerCornerOf(bh.getDirection().getNormal()).scale(0.6));
            if (land == null) {
                for (Entity e : sl.getEntities(this, getBoundingBox().expandTowards(vel).inflate(0.3), e -> SkillLogic.canHit(owner, e, false))) {
                    land = e.position().subtract(vel.normalize().scale(1.2));
                    break;
                }
            }
            if (land == null && tickCount > 40) land = pos;
            if (land != null) {
                open(sl, owner, land);
                return;
            }
            if (tickCount % 2 == 0) sl.sendParticles(ModRegistry.WISP.get(), getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.01);
        }
        setDeltaMovement(vel.scale(0.99).add(0, -0.03, 0));
        setPos(next.x, next.y, next.z);
    }

    private void open(ServerLevel sl, ServerPlayer owner, Vec3 at) {
        Vec3 from = owner.position();
        Vec3 dest = at;
        // find a spot the player fits into
        for (int i = 0; i < 6; i++) {
            if (sl.noCollision(owner, owner.getBoundingBox().move(dest.subtract(from)))) break;
            dest = dest.add(0, 0.5, 0);
        }
        Fx.at(sl, Fx.Type.RIFT, from.add(0, 1, 0), 18, 1.2F);
        Fx.at(sl, Fx.Type.RIFT, dest.add(0, 1, 0), 22, 1.4F);
        Fx.line(sl, Fx.Type.TRAIL, from, dest, 14, 0.6F);
        owner.teleportTo(dest.x, dest.y, dest.z);
        owner.fallDistance = 0;
        owner.setDeltaMovement(Vec3.ZERO);
        owner.hurtMarked = true;
        Vec3 c = dest.add(0, 1, 0);
        for (LivingEntity e : SkillLogic.enemiesAround(owner, c, 3.2, true)) {
            SkillLogic.hit(e, 8F, this, owner);
            SkillLogic.push(e, c, 0.9);
        }
        Fx.at(sl, Fx.Type.WISPBURST, c, 14, 3.2F);
        SkillLogic.wisps(sl, c, 30, 0.6);
        SkillLogic.play(sl, c, ModRegistry.RIFT.get(), 1.4F, 0.8F);
        discard();
    }

    @Override
    public boolean isPickable() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 128 * 128; }
}
