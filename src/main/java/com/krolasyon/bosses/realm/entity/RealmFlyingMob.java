package com.krolasyon.bosses.realm.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/** hovering / winged realm creature */
public class RealmFlyingMob extends RealmMob {
    public RealmFlyingMob(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CastGoal(this));
        this.goalSelector.addGoal(2, new HoverChaseGoal(this));
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this));
        this.goalSelector.addGoal(7, new WanderGoal(this));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        addTargetGoals();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    @Override
    public boolean onClimbable() { return false; }

    /** circles above the target, swooping in to attack range */
    static class HoverChaseGoal extends Goal {
        private final RealmFlyingMob mob;
        private float orbit;

        HoverChaseGoal(RealmFlyingMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive() && !mob.isCasting();
        }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30F, 30F);
            double prefer = Math.max(mob.spec.preferRange(), mob.attackReach() * 0.8);
            orbit += 0.06F;
            double r = Math.max(1.2, prefer * 0.8);
            Vec3 goal = t.position().add(Mth.cos(orbit) * r, 1.5 + Math.min(4.0, prefer * 0.3) + Mth.sin(orbit * 1.7F) * 0.6, Mth.sin(orbit) * r);
            mob.getMoveControl().setWantedPosition(goal.x, goal.y, goal.z, 1.0);
            mob.setRunning(mob.distanceToSqr(t) > 100);
        }
    }

    static class WanderGoal extends Goal {
        private final RealmFlyingMob mob;

        WanderGoal(RealmFlyingMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.getTarget() == null && !mob.getMoveControl().hasWanted() && mob.getRandom().nextInt(20) == 0;
        }

        @Override
        public boolean canContinueToUse() { return false; }

        @Override
        public void start() {
            Vec3 p = mob.position();
            int ground = mob.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, mob.getBlockX(), mob.getBlockZ());
            double y = Math.min(p.y + mob.getRandom().nextInt(7) - 3, ground + 10);
            y = Math.max(y, ground + 2);
            mob.getMoveControl().setWantedPosition(p.x + mob.getRandom().nextInt(17) - 8, y, p.z + mob.getRandom().nextInt(17) - 8, 0.7);
        }
    }
}
