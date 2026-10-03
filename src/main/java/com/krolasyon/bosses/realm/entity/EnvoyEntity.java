package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.realm.Allegiance;
import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.story.Politics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/** Kingdom envoy: talk to them to offer tribute, take quests and forge alliances. */
public class EnvoyEntity extends PathfinderMob implements FactionMember, AnimState.Holder, Allegiance.Member {
    public final MobSpec spec;
    private final AnimState anim = new AnimState();

    public EnvoyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.spec = RealmEntities.spec(type);
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4D));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, e -> !(e instanceof FactionMember fm) || fm.faction() == null
                || fm.faction().atWarWith(faction()), 8.0F, 1.0D, 1.3D, e -> true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.55D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.08F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public Faction faction() { return spec.faction(); }

    @Override
    public boolean isEnvoy() { return true; }

    @Override
    public AnimState anim() { return anim; }

    @Override
    public boolean isHostileTo(Entity other) { return false; }

    public void gesture() { this.level().broadcastEntityEvent(this, (byte) (RealmMob.EVENT_ANIM + 3)); }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !isAlive()) return InteractionResult.PASS;
        if (!level().isClientSide() && player instanceof ServerPlayer sp) {
            getNavigation().stop();
            getLookControl().setLookAt(player);
            gesture();
            Politics.openEnvoy(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            anim.tick(false);
            if (random.nextInt(12) == 0) {
                var p = switch (faction()) {
                    case ASH -> ParticleTypes.SMALL_FLAME;
                    case BLOOD -> ParticleTypes.CRIMSON_SPORE;
                    case SHADOW -> ParticleTypes.PORTAL;
                    case LEGION -> ParticleTypes.SMOKE;
                    case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
                };
                level().addParticle(p, getRandomX(0.6), getY() + getBbHeight() * 0.6, getRandomZ(0.6), 0, 0.02, 0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id >= RealmMob.EVENT_ANIM && id < RealmMob.EVENT_ANIM + 8) anim.play(id - RealmMob.EVENT_ANIM, tickCount);
        else super.handleEntityEvent(id);
    }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return super.hurt(src, amount);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() { return MobSpec.sound(spec.ambient()); }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return MobSpec.sound(spec.hurt()); }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() { return MobSpec.sound(spec.death()); }

    @Override
    public float getVoicePitch() { return spec.pitch(); }
}
