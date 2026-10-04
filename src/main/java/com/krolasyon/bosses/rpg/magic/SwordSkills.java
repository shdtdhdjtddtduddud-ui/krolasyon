package com.krolasyon.bosses.rpg.magic;

import com.krolasyon.bosses.rpg.def.RpgDefs.SwordPassive;
import com.krolasyon.bosses.rpg.def.RpgDefs.SwordSkill;
import com.krolasyon.bosses.rpg.entity.MagicBolt;
import com.krolasyon.bosses.rpg.entity.MagicBolt.OnHit;
import com.krolasyon.bosses.rpg.entity.Summon;
import com.krolasyon.bosses.rpg.item.RpgSwordItem;
import com.krolasyon.bosses.rpg.mob.AbilityLogic;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.util.Combat;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.util.Tasks;
import com.krolasyon.bosses.rpg.util.TempBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Active skills of the named swords. Each is a short burst of melee + magic with heavy particle work. */
public final class SwordSkills {
    private SwordSkills() {}

    public static String passiveName(SwordPassive p) {
        return switch (p) {
            case NONE -> "Yok"; case BURN -> "Yakma"; case FREEZE -> "Dondurma"; case SHOCK -> "Şok (sersemletme şansı)"; case POISON -> "Zehir";
            case LIFESTEAL -> "Can çalma"; case WITHER -> "Solduran lanet"; case BLEED -> "Kanatma"; case KNOCKUP -> "Havaya savurma";
            case HOLY -> "Kutsal (ölümsüzlere ek hasar)"; case EXECUTE -> "İnfaz (düşük canlıya ek hasar)"; case CRIT -> "Kritik vuruş şansı";
            case MANA_STEAL -> "Mana çalma"; case SLOW -> "Yavaşlatma"; case BLIND -> "Kör etme şansı"; case WEAKEN -> "Zayıflatma";
            case GIANT_SLAYER -> "Dev avcısı"; case DEMON_SLAYER -> "İblis avcısı"; case UNDEAD_SLAYER -> "Ölümsüz avcısı"; case DRAGON_SLAYER -> "Ejder avcısı";
        };
    }

    public static String skillName(SwordSkill s) {
        return switch (s) {
            case FLAME_WAVE -> "Alev Dalgası"; case FROST_NOVA -> "Ayaz Patlaması"; case THUNDER_STRIKE -> "Gök Yıldırımı"; case POISON_FAN -> "Zehirli Yelpaze";
            case BLOOD_FRENZY -> "Kan Çılgınlığı"; case SHADOW_DASH -> "Gölge Atılımı"; case WHIRLWIND -> "Kasırga Dönüşü"; case GROUND_SLAM -> "Yer Sarsıntısı";
            case HOLY_SMITE -> "Kutsal Darbe"; case CRESCENT_SLASH -> "Hilal Kesiği"; case BLADE_STORM -> "Kılıç Fırtınası"; case DRAGON_BREATH -> "Ejder Nefesi";
            case SOUL_REAP -> "Ruh Hasadı"; case WIND_CUTTER -> "Rüzgar Kesen"; case EARTH_SPLITTER -> "Yer Yaran"; case STAR_FALL -> "Yıldız Yağmuru";
            case VOID_RIFT -> "Boşluk Yarığı"; case MIRROR_CLONES -> "Ayna İkizleri"; case BERSERK -> "Cinnet"; case WAR_CRY -> "Savaş Narası";
            case TIDE_SLASH -> "Gelgit Kesiği"; case METEOR_DROP -> "Meteor İnişi"; case PHANTOM_BLADES -> "Hayalet Kılıçlar"; case ICE_PRISON -> "Buz Hapsi";
            case CHAIN_SHOCK -> "Zincirleme Şok"; case LIFE_BLOOM -> "Hayat Çiçeği"; case EXECUTION -> "İnfaz"; case ASH_CYCLONE -> "Kül Hortumu";
            case GRAVITY_CRUSH -> "Yerçekimi Ezişi"; case SPIRIT_ARMY -> "Ruh Ordusu"; case SUN_BURST -> "Güneş Patlaması"; case ROYAL_DECREE -> "Kraliyet Fermanı";
        };
    }

    public static String skillDesc(SwordSkill s) {
        return switch (s) {
            case FLAME_WAVE -> "Önündeki her şeyi yakan alevden bir hilal fırlatır.";
            case FROST_NOVA -> "Etrafındaki düşmanları dondurur ve yavaşlatır.";
            case THUNDER_STRIKE -> "Hedefin üzerine üç yıldırım indirir.";
            case POISON_FAN -> "Yelpaze şeklinde zehirli hançerler savurur.";
            case BLOOD_FRENZY -> "Canından feda ederek saldırı hızı ve can çalma kazanır.";
            case SHADOW_DASH -> "Gölgeye dönüşüp ileri atılır, yolundakileri keser.";
            case WHIRLWIND -> "Etrafında dönerek art arda kesikler atar.";
            case GROUND_SLAM -> "Kılıcı yere vurup şok dalgası yaratır.";
            case HOLY_SMITE -> "Gökten kutsal ışık indirir.";
            case CRESCENT_SLASH -> "Uzağa uçan keskin bir kesik dalgası.";
            case BLADE_STORM -> "Hızla ileri atılarak yedi kez keser.";
            case DRAGON_BREATH -> "Kılıçtan ejder alevi püskürtür.";
            case SOUL_REAP -> "Etraftaki düşmanların ruhunu biçer, can kazanır.";
            case WIND_CUTTER -> "Delip geçen rüzgar kesikleri fırlatır.";
            case EARTH_SPLITTER -> "Yeri ikiye yaran bir çatlak açar.";
            case STAR_FALL -> "Hedef bölgeye yıldızlar yağdırır.";
            case VOID_RIFT -> "Düşmanları içine çeken bir boşluk yarığı açar.";
            case MIRROR_CLONES -> "Seninle savaşan üç ayna ikizi çağırır.";
            case BERSERK -> "Kısa süre devasa güç ve hız kazanırsın.";
            case WAR_CRY -> "Dostlarını güçlendirir, düşmanları korkutur.";
            case TIDE_SLASH -> "Düşmanları geri iten dev bir dalga.";
            case METEOR_DROP -> "Havaya sıçrayıp meteor gibi yere çakılırsın.";
            case PHANTOM_BLADES -> "Hedefe güdümlü hayalet kılıçlar yollar.";
            case ICE_PRISON -> "Hedefi buzdan bir hapse kapatır.";
            case CHAIN_SHOCK -> "Düşmandan düşmana sıçrayan şimşek.";
            case LIFE_BLOOM -> "Kendini ve dostlarını iyileştiren çiçekler açar.";
            case EXECUTION -> "Tek, ölümcül bir darbe; düşük canlıları anında bitirir.";
            case ASH_CYCLONE -> "Etrafında yakıcı kül hortumu döndürür.";
            case GRAVITY_CRUSH -> "Hedef bölgedeki her şeyi yere ezer.";
            case SPIRIT_ARMY -> "Atalarının ruhlarından bir bölük çağırır.";
            case SUN_BURST -> "Kör edici bir güneş patlaması.";
            case ROYAL_DECREE -> "Kraliyet ışığıyla dostlarını korur, düşmanları diz çöktürür.";
        };
    }

    public static int cooldown(SwordSkill s) {
        return switch (s) {
            case CRESCENT_SLASH, WIND_CUTTER, PHANTOM_BLADES -> 60;
            case FLAME_WAVE, POISON_FAN, TIDE_SLASH, SHADOW_DASH, CHAIN_SHOCK -> 100;
            case WHIRLWIND, GROUND_SLAM, FROST_NOVA, THUNDER_STRIKE, HOLY_SMITE, DRAGON_BREATH, EARTH_SPLITTER, ICE_PRISON -> 160;
            case BLADE_STORM, SOUL_REAP, VOID_RIFT, METEOR_DROP, ASH_CYCLONE, GRAVITY_CRUSH, STAR_FALL, SUN_BURST, EXECUTION -> 240;
            case BLOOD_FRENZY, BERSERK, WAR_CRY, LIFE_BLOOM, MIRROR_CLONES, SPIRIT_ARMY, ROYAL_DECREE -> 600;
        };
    }

    private static Vec3 fwd(ServerPlayer p) { return p.getLookAngle().multiply(1, 0, 1).normalize(); }

    private static void slashWave(ServerPlayer p, float dmg, ParticleOptions pt, int color, OnHit h, double range, double width) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = fwd(p);
        Vec3 start = p.position().add(0, 1.0, 0);
        float yaw = p.getYRot();
        java.util.Set<Integer> hit = new java.util.HashSet<>();
        Tasks.add(level, (int) (range / 1.2) + 1, t -> {
            Vec3 c = start.add(dir.scale(1 + t * 1.2));
            FX.arc(level, pt, c.subtract(dir.scale(1.2)), yaw, width, 120, 0, 18);
            FX.arc(level, FX.dust(color, 1.6F), c.subtract(dir.scale(1.2)), yaw, width * 0.9, 110, 0.1, 14);
            for (LivingEntity e : Combat.victims(p, c, width)) {
                if (hit.add(e.getId()) && Combat.magic(p, null, e, dmg)) {
                    Combat.applyOnHit(p, e, h, dmg);
                    Combat.knock(p.position(), e, 0.5, 0.2);
                }
            }
            return true;
        });
    }

    public static void use(ServerPlayer p, RpgSwordItem item, SwordSkill s, float power) {
        ServerLevel level = p.serverLevel();
        float base = (float) (p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) * 0.9 + 3) * power;
        int blade = item.def.blade(), gem = item.def.gem() < 0 ? blade : item.def.gem();
        Vec3 pos = p.position();
        switch (s) {
            case FLAME_WAVE -> { FX.sound(level, pos, SoundEvents.BLAZE_SHOOT, 1.2F, 0.7F); slashWave(p, base * 1.2F, ParticleTypes.FLAME, 0xFF6010, OnHit.BURN, 12, 2.2); }
            case CRESCENT_SLASH -> { FX.sound(level, pos, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.8F); slashWave(p, base, ParticleTypes.SWEEP_ATTACK, blade, OnHit.NONE, 14, 1.8); }
            case WIND_CUTTER -> {
                FX.sound(level, pos, SoundEvents.PHANTOM_FLAP, 1.4F, 1.6F);
                for (int i = -1; i <= 1; i++) {
                    MagicBolt b = MagicBolt.create(level, p, 0xE0FFF0, 0x80C0A0, 0.4F, MagicBolt.WIND);
                    b.aim(p.getLookAngle().yRot(i * 0.2F), 1.8).dmg(base * 0.8F, OnHit.KNOCK).pierce(4).life(30).fire();
                }
            }
            case TIDE_SLASH -> { FX.sound(level, pos, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.4F, 0.8F); slashWave(p, base, ParticleTypes.SPLASH, 0x3080FF, OnHit.KNOCK, 12, 3.0); }
            case POISON_FAN -> {
                FX.sound(level, pos, SoundEvents.TRIDENT_THROW, 1.2F, 1.4F);
                for (int i = -3; i <= 3; i++) {
                    MagicBolt b = MagicBolt.create(level, p, 0x70E020, 0x2A6010, 0.25F, MagicBolt.POISON);
                    b.aim(p.getLookAngle().yRot(i * 0.12F), 1.5).dmg(base * 0.5F, OnHit.POISON).life(25).fire();
                }
            }
            case FROST_NOVA -> {
                FX.sound(level, pos, SoundEvents.GLASS_BREAK, 1.5F, 0.6F);
                FX.burstRing(level, ParticleTypes.SNOWFLAKE, pos.add(0, 0.5, 0), 60, 0.5);
                FX.ring(level, FX.dust(0x90E0FF, 2.0F), pos.add(0, 0.2, 0), 5, 50, 0.1);
                for (LivingEntity e : Combat.victims(p, pos, 6)) { Combat.magic(p, null, e, base); Combat.applyOnHit(p, e, OnHit.FREEZE, base); e.setTicksFrozen(260); }
            }
            case THUNDER_STRIKE -> {
                LivingEntity t = SpellCaster.aimEntity(p, 24);
                Vec3 at = t != null ? t.position() : SpellCaster.aimPoint(p, 20);
                Tasks.add(level, 30, k -> { if (k % 10 == 0) AbilityLogic.strike(p, at.add((k / 10 - 1) * 0.8, 0, 0), base * 0.9F); return true; });
            }
            case BLOOD_FRENZY -> {
                p.hurt(p.damageSources().magic(), 4);
                p.addEffect(new MobEffectInstance(RpgEffects.BLOOD_PACT.get(), 200, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 2));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                FX.column(level, FX.dust(0xC01020, 1.6F), pos, 2.2, 0.8, 80);
                FX.sound(level, pos, SoundEvents.RAVAGER_ROAR, 1.0F, 1.4F);
            }
            case BERSERK -> {
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2));
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0));
                FX.column(level, ParticleTypes.ANGRY_VILLAGER, pos, 2.2, 0.8, 20);
                FX.burstRing(level, FX.dust(0xFF2020, 2.0F), pos.add(0, 1, 0), 40, 0.6);
                FX.sound(level, pos, SoundEvents.RAVAGER_ROAR, 1.4F, 0.9F);
            }
            case SHADOW_DASH, BLADE_STORM -> {
                boolean storm = s == SwordSkill.BLADE_STORM;
                Vec3 d = fwd(p);
                java.util.Set<Integer> hit = new java.util.HashSet<>();
                if (!storm) p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0));
                FX.sound(level, pos, storm ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.4F, 1.2F);
                Tasks.add(level, storm ? 14 : 6, k -> {
                    p.setDeltaMovement(d.x * 1.1, 0.05, d.z * 1.1);
                    p.hurtMarked = true;
                    FX.send(level, storm ? ParticleTypes.SWEEP_ATTACK : ParticleTypes.SQUID_INK, p.position().add(0, 1, 0), 3, 0.4, 0.02);
                    FX.send(level, FX.dust(storm ? blade : 0x4A2A6A, 1.5F), p.position().add(0, 1, 0), 6, 0.5, 0.02);
                    for (LivingEntity e : Combat.victims(p, p.position().add(0, 1, 0), 2.5)) {
                        if (storm ? k % 2 == 0 : hit.add(e.getId())) { e.invulnerableTime = 0; Combat.magic(p, null, e, base * (storm ? 0.5F : 1.4F)); if (!storm) Combat.effect(e, MobEffects.BLINDNESS, 40, 0); }
                    }
                    return true;
                });
            }
            case WHIRLWIND, ASH_CYCLONE -> {
                boolean ash = s == SwordSkill.ASH_CYCLONE;
                Tasks.add(level, 40, k -> {
                    Vec3 c = p.position();
                    FX.ring(level, ash ? ParticleTypes.FLAME : ParticleTypes.SWEEP_ATTACK, c.add(0, 1, 0), 2.5, ash ? 16 : 6, 0.05);
                    if (ash) FX.spiral(level, ParticleTypes.LARGE_SMOKE, c, 3, 2.5, 1, 12);
                    if (k % 5 == 0) {
                        FX.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F + k * 0.01F);
                        for (LivingEntity e : Combat.victims(p, c, 3.3)) { e.invulnerableTime = 0; Combat.magic(p, null, e, base * 0.45F); if (ash) e.setSecondsOnFire(3); }
                    }
                    return true;
                });
            }
            case GROUND_SLAM, EARTH_SPLITTER -> {
                FX.sound(level, pos, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.6F);
                BlockState below = level.getBlockState(p.blockPosition().below());
                if (below.isAir()) below = Blocks.DIRT.defaultBlockState();
                BlockParticleOption bp = new BlockParticleOption(ParticleTypes.BLOCK, below);
                if (s == SwordSkill.GROUND_SLAM) {
                    FX.burstRing(level, bp, pos.add(0, 0.2, 0), 60, 0.4);
                    FX.ring(level, ParticleTypes.CLOUD, pos.add(0, 0.2, 0), 4, 40, 0.1);
                    for (LivingEntity e : Combat.victims(p, pos, 5.5)) { Combat.magic(p, null, e, base * 1.2F); Combat.knock(pos, e, 1.0, 0.7); Combat.effect(e, RpgEffects.STUN.get(), 30, 0); }
                } else {
                    Vec3 d = fwd(p);
                    BlockParticleOption fbp = bp;
                    java.util.Set<Integer> hit = new java.util.HashSet<>();
                    Tasks.add(level, 12, k -> {
                        Vec3 c = pos.add(d.scale(1 + k * 1.2));
                        FX.send(level, fbp, c.x, c.y + 0.5, c.z, 20, 0.4, 0.4, 0.4, 0.2);
                        FX.send(level, ParticleTypes.LAVA, c, 2, 0.3, 0);
                        for (LivingEntity e : Combat.victims(p, c, 1.8)) if (hit.add(e.getId())) { Combat.magic(p, null, e, base * 1.3F); e.setDeltaMovement(e.getDeltaMovement().add(0, 0.8, 0)); e.hurtMarked = true; }
                        return true;
                    });
                }
            }
            case HOLY_SMITE, SUN_BURST -> {
                boolean sun = s == SwordSkill.SUN_BURST;
                Vec3 at = sun ? pos : SpellCaster.aimPoint(p, 20);
                FX.column(level, ParticleTypes.END_ROD, at, 10, sun ? 3 : 1.2, 120);
                FX.send(level, ParticleTypes.FLASH, at.add(0, 1, 0), 1, 0, 0);
                FX.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.6F);
                for (LivingEntity e : Combat.victims(p, at.add(0, 1, 0), sun ? 6 : 3)) {
                    float m = e.getMobType() == MobType.UNDEAD ? 2 : 1;
                    Combat.magic(p, null, e, base * 1.2F * m);
                    Combat.applyOnHit(p, e, OnHit.HOLY, base);
                    if (sun) Combat.effect(e, MobEffects.BLINDNESS, 80, 0);
                }
            }
            case DRAGON_BREATH -> {
                FX.sound(level, pos, SoundEvents.ENDER_DRAGON_SHOOT, 1.2F, 0.8F);
                Tasks.add(level, 30, k -> {
                    Vec3 o = p.getEyePosition().subtract(0, 0.4, 0), d = p.getLookAngle();
                    FX.cone(level, ParticleTypes.FLAME, o, d, 9, 0.4, 20);
                    FX.cone(level, ParticleTypes.DRAGON_BREATH, o, d, 9, 0.3, 8);
                    if (k % 4 == 0) for (LivingEntity e : Combat.inCone(p, o, d, 9, 0.82)) { Combat.magic(p, null, e, base * 0.5F); e.setSecondsOnFire(5); }
                    return true;
                });
            }
            case SOUL_REAP -> {
                FX.sound(level, pos, SoundEvents.SOUL_ESCAPE, 2.0F, 0.6F);
                FX.ring(level, ParticleTypes.SOUL, pos.add(0, 1, 0), 4.5, 50, 0.2);
                FX.arc(level, ParticleTypes.SOUL_FIRE_FLAME, pos, p.getYRot(), 4, 360, 1, 40);
                for (LivingEntity e : Combat.victims(p, pos, 5)) {
                    if (Combat.magic(p, null, e, base * 1.1F)) {
                        p.heal(2.5F);
                        FX.line(level, ParticleTypes.SOUL, e.position().add(0, 1, 0), p.position().add(0, 1, 0), 0.5, 0.05);
                    }
                }
            }
            case STAR_FALL, METEOR_DROP -> {
                if (s == SwordSkill.METEOR_DROP) {
                    p.setDeltaMovement(p.getLookAngle().x * 0.8, 1.3, p.getLookAngle().z * 0.8);
                    p.hurtMarked = true;
                    Tasks.add(level, 60, k -> {
                        if (k > 6 && k < 14) { p.setDeltaMovement(p.getDeltaMovement().x, -2.2, p.getDeltaMovement().z); p.hurtMarked = true; }
                        FX.send(level, ParticleTypes.FLAME, p.position(), 6, 0.3, 0.05);
                        if (k > 8 && p.onGround()) {
                            p.fallDistance = 0;
                            FX.send(level, ParticleTypes.EXPLOSION_EMITTER, p.position(), 1, 0, 0);
                            FX.burstRing(level, ParticleTypes.FLAME, p.position().add(0, 0.3, 0), 50, 0.5);
                            FX.sound(level, p.position(), SoundEvents.GENERIC_EXPLODE, 1.5F, 0.7F);
                            for (LivingEntity e : Combat.victims(p, p.position(), 5)) { Combat.magic(p, null, e, base * 1.8F); e.setSecondsOnFire(5); Combat.knock(p.position(), e, 1.2, 0.6); }
                            return false;
                        }
                        return true;
                    });
                } else {
                    Vec3 at = SpellCaster.aimPoint(p, 28);
                    Tasks.add(level, 50, k -> {
                        if (k % 5 == 0) {
                            Vec3 q = at.add((p.getRandom().nextDouble() - 0.5) * 8, 0, (p.getRandom().nextDouble() - 0.5) * 8);
                            MagicBolt b = MagicBolt.create(level, p, 0xE0E0FF, gem, 0.7F, MagicBolt.ARCANE);
                            b.setPos(q.x + 3, q.y + 16, q.z);
                            b.aim(q.subtract(b.position()), 1.2).dmg(base * 0.8F, OnHit.NONE).boom(2.0F).life(40).fire();
                        }
                        return true;
                    });
                }
            }
            case VOID_RIFT, GRAVITY_CRUSH -> {
                Vec3 at = SpellCaster.aimPoint(p, 20).add(0, 1, 0);
                boolean rift = s == SwordSkill.VOID_RIFT;
                FX.sound(level, at, SoundEvents.PORTAL_TRIGGER, 0.8F, 1.6F);
                Tasks.add(level, 60, k -> {
                    FX.spiral(level, FX.dust(rift ? 0x6A2AAA : 0x3A2A5A, 1.4F), at.subtract(0, 1, 0), 3, 1 + (k % 10) * 0.35, 2, 24);
                    FX.sphere(level, ParticleTypes.REVERSE_PORTAL, at, 1, 10);
                    for (LivingEntity e : Combat.victims(p, at, 7)) {
                        Vec3 d = rift ? at.subtract(e.position()).normalize().scale(0.14) : new Vec3(0, -0.4, 0);
                        e.setDeltaMovement(e.getDeltaMovement().add(d));
                        e.hurtMarked = true;
                        if (!rift) Combat.effect(e, MobEffects.MOVEMENT_SLOWDOWN, 20, 4);
                        if (k % 10 == 0) { e.invulnerableTime = 0; Combat.magic(p, null, e, base * 0.5F); }
                    }
                    return true;
                });
            }
            case MIRROR_CLONES, SPIRIT_ARMY -> {
                Summon.Kind kind = s == SwordSkill.MIRROR_CLONES ? Summon.Kind.MIRROR : Summon.Kind.SPIRIT_WARRIOR;
                int n = s == SwordSkill.MIRROR_CLONES ? 3 : 4;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    Summon.spawn(p, kind, pos.x + Math.cos(a) * 2, pos.y, pos.z + Math.sin(a) * 2, 400, power);
                }
                FX.sound(level, pos, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.2F, 1.0F);
            }
            case WAR_CRY, ROYAL_DECREE -> {
                boolean royal = s == SwordSkill.ROYAL_DECREE;
                FX.sound(level, pos, royal ? SoundEvents.BELL_RESONATE : SoundEvents.RAID_HORN.value(), 2.0F, 1.0F);
                FX.burstRing(level, FX.dust(royal ? 0xFFD040 : 0xFF6020, 2.0F), pos.add(0, 1, 0), 60, 0.7);
                for (LivingEntity a : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(10), e -> Combat.sameSide(p, e))) {
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, royal ? 1 : 0));
                    a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, royal ? 1 : 0));
                    if (royal) a.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 2));
                }
                for (LivingEntity e : Combat.victims(p, pos, 10)) {
                    Combat.effect(e, MobEffects.WEAKNESS, 200, 1);
                    if (royal) Combat.effect(e, RpgEffects.STUN.get(), 40, 0);
                }
            }
            case PHANTOM_BLADES -> {
                LivingEntity t = SpellCaster.aimEntity(p, 30);
                FX.sound(level, pos, SoundEvents.TRIDENT_RIPTIDE_1, 1.0F, 1.6F);
                for (int i = 0; i < 5; i++) {
                    MagicBolt b = MagicBolt.create(level, p, 0xC0E0FF, 0xFFFFFF, 0.3F, MagicBolt.LIGHT);
                    Vec3 side = new Vec3(-fwd(p).z, 0, fwd(p).x).scale((i - 2) * 0.6);
                    b.setPos(b.getX() + side.x, b.getY() + 0.5, b.getZ() + side.z);
                    b.aim(p.getLookAngle().add(0, 0.2, 0), 1.0).dmg(base * 0.5F, OnHit.NONE).life(60);
                    if (t != null) b.home(t, 0.2F);
                    b.fire();
                }
            }
            case ICE_PRISON -> {
                LivingEntity t = SpellCaster.aimEntity(p, 20);
                if (t != null) {
                    BlockPos c = t.blockPosition();
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int dy = 0; dy <= 2; dy++) {
                        if (dx == 0 && dz == 0 && dy < 2) continue;
                        TempBlocks.place(level, c.offset(dx, dy, dz), Blocks.ICE.defaultBlockState(), 80);
                    }
                    Combat.magic(p, null, t, base);
                    Combat.applyOnHit(p, t, OnHit.FREEZE, base);
                    t.setTicksFrozen(300);
                    FX.sound(level, t.position(), SoundEvents.GLASS_PLACE, 1.5F, 0.6F);
                }
            }
            case CHAIN_SHOCK -> {
                LivingEntity t = SpellCaster.aimEntity(p, 20);
                if (t == null) break;
                java.util.Set<Integer> hit = new java.util.HashSet<>();
                LivingEntity cur = t;
                Vec3 prev = p.getEyePosition();
                for (int i = 0; i < 5 && cur != null; i++) {
                    Vec3 q = cur.position().add(0, 1, 0);
                    FX.zigzag(level, FX.dust(0xFFE040, 1.0F), prev, q, 6, 0.6);
                    Combat.magic(p, null, cur, base * 0.9F);
                    Combat.applyOnHit(p, cur, OnHit.SHOCK, base);
                    hit.add(cur.getId());
                    prev = q;
                    LivingEntity nx = null;
                    for (LivingEntity e : Combat.victims(p, cur.position(), 7)) if (!hit.contains(e.getId())) { nx = e; break; }
                    cur = nx;
                }
                FX.sound(level, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.0F, 1.5F);
            }
            case LIFE_BLOOM -> {
                for (LivingEntity a : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(8), e -> Combat.sameSide(p, e))) {
                    a.heal(8);
                    a.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    FX.spiral(level, ParticleTypes.CHERRY_LEAVES, a.position(), 2.5, 0.8, 2, 30);
                }
                FX.ring(level, ParticleTypes.HAPPY_VILLAGER, pos.add(0, 0.2, 0), 6, 40, 0.3);
                FX.sound(level, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5F, 1.2F);
            }
            case EXECUTION -> {
                LivingEntity t = SpellCaster.aimEntity(p, 5);
                if (t != null) {
                    float dmg = t.getHealth() < t.getMaxHealth() * 0.3F && !(t instanceof com.krolasyon.bosses.rpg.mob.RpgBoss) ? t.getHealth() + 10 : base * 2.2F;
                    t.invulnerableTime = 0;
                    Combat.magic(p, null, t, dmg);
                    FX.send(level, ParticleTypes.DAMAGE_INDICATOR, t.position().add(0, 1, 0), 30, 0.4, 0.2);
                    FX.arc(level, FX.dust(0xFF0000, 2.0F), t.position(), p.getYRot(), 1.5, 160, 1.2, 24);
                    FX.sound(level, t.position(), SoundEvents.ANVIL_LAND, 1.0F, 0.6F);
                }
            }
        }
    }
}
