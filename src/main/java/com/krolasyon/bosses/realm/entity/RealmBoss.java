package com.krolasyon.bosses.realm.entity;

import com.krolasyon.bosses.entity.BossEntity;
import com.krolasyon.bosses.realm.Faction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Kingdom lord or the Tyrant: a boss whose moveset comes from its spec and the shared ability engine. */
public class RealmBoss extends BossEntity implements FactionMember {
    public final MobSpec spec;
    public final Abilities.Scratch scratch = new Abilities.Scratch();

    public RealmBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, color(RealmEntities.spec(type)), RealmEntities.spec(type).abilities().length);
        this.spec = RealmEntities.spec(type);
        this.xpReward = spec.xp();
    }

    private static BossEvent.BossBarColor color(MobSpec s) {
        if (s.faction() == null) return BossEvent.BossBarColor.RED;
        return switch (s.faction()) {
            case ASH -> BossEvent.BossBarColor.YELLOW;
            case BLOOD -> BossEvent.BossBarColor.RED;
            case SHADOW -> BossEvent.BossBarColor.PURPLE;
            case LEGION -> BossEvent.BossBarColor.WHITE;
            case SOUL -> BossEvent.BossBarColor.BLUE;
        };
    }

    @Nullable
    @Override
    public Faction faction() { return spec.faction(); }

    @Override
    public boolean isLord() { return spec.kind() == MobSpec.Kind.LORD; }

    @Override
    public boolean isTyrant() { return spec.kind() == MobSpec.Kind.TYRANT; }

    public Ability ability(int i) { return spec.abilities()[i]; }

    @Override
    protected boolean isMonsterPrey(LivingEntity e) {
        if (e instanceof FactionMember fm && (fm.faction() == faction() || fm.isEnvoy())) return false;
        if (e instanceof RealmMob rm && rm.isAlly()) return true;
        return isTyrant() ? false : super.isMonsterPrey(e) && e instanceof FactionMember fm2 && faction() != null && faction().atWarWith(fm2.faction());
    }

    @Override
    public boolean isHostileTo(Entity e) {
        if (e instanceof FactionMember fm && fm.faction() != null && (fm.faction() == faction() || isTyrant()) && !(e instanceof RealmMob rm && rm.isAlly())) return false;
        if (e instanceof FactionMember fm && fm.isEnvoy()) return false;
        return super.isHostileTo(e);
    }

    @Override
    protected int abilityDuration(int id) { return ability(id).duration(); }

    @Override
    protected int abilityCooldown(int id) { return ability(id).cooldown(); }

    @Override
    protected void startAbility(int id, LivingEntity target) {
        scratch.reset();
        super.startAbility(id, target);
        this.level().broadcastEntityEvent(this, (byte) (EVENT_ANIM_BASE + ability(id).anim()));
    }

    @Override
    public void handleEntityEvent(byte id) {
        // the ability index is broadcast first by BossEntity; the anim slot follows right after and wins
        super.handleEntityEvent(id);
    }

    @Override
    protected void tickAbility(int id, int t, @Nullable LivingEntity target) {
        Abilities.tick(this, ability(id), t, target, scratch);
    }

    @Override
    protected boolean isAirborneAbility() {
        return currentAbility >= 0 && (ability(currentAbility).type() == Ability.Type.LEAP || ability(currentAbility).type() == Ability.Type.CHARGE);
    }

    @Override
    protected int chooseAbility(LivingEntity target, double distSqr) {
        double d = Math.sqrt(distSqr);
        boolean meleeReady = false;
        int total = 0;
        int[] w = new int[cooldowns.length];
        for (int i = 0; i < cooldowns.length; i++) {
            Ability a = ability(i);
            if (!ready(i) || d < a.minRange() || d > a.maxRange() + target.getBbWidth() * 0.5) continue;
            if (a.melee()) {
                meleeReady = true;
                continue;
            }
            if (globalCooldown > 0) continue;
            if (a.type() == Ability.Type.HEAL && getHealth() > getMaxHealth() * 0.75F) continue;
            w[i] = a.weight() * (isPhase2() ? 2 : 1);
            total += w[i];
        }
        if (total > 0 && tickCount % 6 == 0 && random.nextFloat() < 0.7F) {
            int roll = random.nextInt(total);
            for (int i = 0; i < w.length; i++) {
                roll -= w[i];
                if (roll < 0) return i;
            }
        }
        if (meleeReady) for (int i = 0; i < cooldowns.length; i++) if (ability(i).melee() && ready(i) && d <= ability(i).maxRange() + target.getBbWidth() * 0.5) return i;
        return -1;
    }

    @Override
    protected void onPhase2() {
        ServerLevel sl = serverLevel();
        sound(SoundEvents.WITHER_SPAWN, 1.6F, 0.7F);
        Abilities.burst(this, spec.abilities()[0].element(), 3.5F, 60);
        for (Ability a : spec.abilities()) {
            if (a.type() == Ability.Type.SUMMON) {
                Abilities.summon(this, a, getTarget());
                break;
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
    }

    @Override
    protected void ambientFx() { Abilities.ambientFx(this); }

    @Override
    protected void deathFx(int t) {
        if (level().isClientSide()) {
            Ability.Element el = spec.abilities()[0].element();
            for (int i = 0; i < 6; i++) {
                level().addParticle(t % 2 == 0 ? Abilities.dust(el.color, 2.2F) : Abilities.dust(el.light, 1.6F),
                        getRandomX(1.2), getY() + random.nextDouble() * getBbHeight(), getRandomZ(1.2), 0, 0.08, 0);
            }
            if (t > 40) level().addParticle(ParticleTypes.EXPLOSION, getRandomX(1), getY() + random.nextDouble() * getBbHeight(), getRandomZ(1), 0, 0, 0);
        } else if (t == 50) {
            sound(SoundEvents.GENERIC_EXPLODE, 2.5F, 0.6F);
        }
    }

    @Override
    public double meleeReach() {
        for (Ability a : spec.abilities()) if (a.melee()) return a.maxRange();
        return 3.5;
    }

    @Override
    public double walkSpeed() { return 0.9; }

    @Override
    public double runSpeed() { return 1.35; }

    public Vec3 eye() { return position().add(0, getBbHeight() * 0.8, 0); }

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

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof FactionMember fm && fm.faction() == faction() && faction() != null) return false;
        return super.hurt(src, amount);
    }

    public boolean phase2() { return isPhase2(); }

    public boolean challengerAlive(Player p) { return p.isAlive(); }
}
