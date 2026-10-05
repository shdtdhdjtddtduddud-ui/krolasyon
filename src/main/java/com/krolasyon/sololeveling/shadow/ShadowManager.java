package com.krolasyon.sololeveling.shadow;

import com.krolasyon.sololeveling.entity.MobKind;
import com.krolasyon.sololeveling.entity.SLMonster;
import com.krolasyon.sololeveling.entity.ShadowEntity;
import com.krolasyon.sololeveling.net.Net;
import com.krolasyon.sololeveling.registry.ModEntities;
import com.krolasyon.sololeveling.registry.ModSounds;
import com.krolasyon.sololeveling.system.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

/** "Arise!" — shadow extraction, storage, summoning and recalling. */
public final class ShadowManager {
    private static final DustParticleOptions SOUL = new DustParticleOptions(new Vector3f(0.55F, 0.35F, 1F), 1.4F);
    private static final List<Corpse> CORPSES = new ArrayList<>();
    private static final Map<UUID, List<ShadowEntity>> ACTIVE = new HashMap<>();
    public static final int CORPSE_TICKS = 20 * 25;

    private ShadowManager() {}

    static final class Corpse {
        final ServerLevel level;
        final Vec3 pos;
        final EntityType<?> type;
        final UUID killer;
        final MobKind kind;
        int age;
        int attempts;

        Corpse(ServerLevel level, Vec3 pos, EntityType<?> type, UUID killer, MobKind kind) {
            this.level = level;
            this.pos = pos;
            this.type = type;
            this.killer = killer;
            this.kind = kind;
        }
    }

    /** Called when a living entity dies to a player (or one of their shadows). */
    public static void onKill(ServerPlayer killer, LivingEntity dead) {
        if (dead instanceof Player || dead instanceof ShadowEntity) return;
        if (!(dead.level() instanceof ServerLevel sl)) return;
        HunterData d = HunterCapability.get(killer);
        if (d.job == Job.NONE) return;
        CORPSES.add(new Corpse(sl, dead.position(), dead.getType(), killer.getUUID(), ModEntities.kindOrNull(dead.getType())));
        if (CORPSES.size() > 200) CORPSES.remove(0);
        if (!d.flags.contains("hint_arise")) {
            d.flags.add("hint_arise");
            Sys.info(killer, "hint.arise");
        }
    }

    public static void tick() {
        Iterator<Corpse> it = CORPSES.iterator();
        while (it.hasNext()) {
            Corpse c = it.next();
            c.age++;
            if (c.age > CORPSE_TICKS) {
                it.remove();
                continue;
            }
            if (c.age % 8 == 0) {
                ServerPlayer p = (ServerPlayer) c.level.getPlayerByUUID(c.killer);
                if (p != null && p.distanceToSqr(c.pos) < 48 * 48)
                    c.level.sendParticles(p, SOUL, true, c.pos.x, c.pos.y + 0.3, c.pos.z, 4, 0.35, 0.15, 0.35, 0.01);
            }
        }
        ACTIVE.values().forEach(l -> l.removeIf(e -> e.isRemoved()));
    }

    /** The player pressed the "Arise" key. */
    public static void arise(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (d.job == Job.NONE) {
            Sys.warn(p, "arise.locked");
            return;
        }
        Corpse best = null;
        double bd = 7 * 7;
        for (Corpse c : CORPSES) {
            if (c.level != p.level() || !c.killer.equals(p.getUUID())) continue;
            double ds = c.pos.distanceToSqr(p.position());
            if (ds < bd) {
                bd = ds;
                best = c;
            }
        }
        if (best == null) {
            Sys.warn(p, "arise.no_target");
            return;
        }
        if (d.shadows.size() >= d.shadowCapacity()) {
            Sys.warn(p, "arise.full", d.shadowCapacity());
            return;
        }
        float mana = best.kind != null && best.kind.boss ? 80 : 15;
        if (d.mana < mana) {
            Sys.warn(p, "skill.no_mana");
            return;
        }
        d.mana -= mana;
        d.markDirty();
        Corpse c = best;
        int mobRank = c.kind == null ? 0 : c.kind.rank.ordinal();
        double chance = 0.75 + d.stat(Stat.INT) / 500.0 - mobRank * 0.07 + d.rank.ordinal() * 0.05;
        if (c.kind != null && c.kind.boss) chance -= 0.3;
        if (d.job == Job.SHADOW_MONARCH) chance += 0.25;
        chance = Math.max(0.08, Math.min(0.97, chance));
        Net.to(p, new Net.Fx("arise", c.pos.x, c.pos.y, c.pos.z, 0));
        c.level.playSound(null, c.pos.x, c.pos.y, c.pos.z, ModSounds.ARISE.get(), SoundSource.PLAYERS, 2F, 1F);
        c.level.sendParticles(ParticleTypes.SQUID_INK, c.pos.x, c.pos.y + 0.5, c.pos.z, 60, 0.6, 0.6, 0.6, 0.08);
        c.attempts++;
        if (p.getRandom().nextDouble() > chance) {
            if (c.attempts >= 3) {
                CORPSES.remove(c);
                Sys.notify(p, Sys.WARN, Sys.t("system.title"), Sys.t("arise.failed_final"));
            } else {
                Sys.notify(p, Sys.WARN, Sys.t("system.title"), Sys.t("arise.failed", 3 - c.attempts));
            }
            return;
        }
        CORPSES.remove(c);
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(c.type).toString();
        boolean named = c.kind != null && c.kind.shadowName != null;
        String name = named ? c.kind.shadowName : "#" + c.type.getDescriptionId();
        int lvl = 1 + (c.kind == null ? 0 : c.kind.rank.ordinal() * 3) + (named ? 10 : 0);
        HunterData.ShadowRecord rec = new HunterData.ShadowRecord(UUID.randomUUID(), typeId, name, lvl, named);
        d.shadows.add(rec);
        d.markDirty();
        Component display = displayName(rec);
        Sys.notify(p, Sys.ARISE, Sys.t("arise.title"), Sys.t("arise.success", display));
        if (named && c.kind == MobKind.BERU) d.titles.add(Title.ANT_EXTERMINATOR);
        spawn(p, rec, c.pos);
    }

    public static Component displayName(HunterData.ShadowRecord r) {
        if (r.name.startsWith("#")) return Component.translatable("sololeveling.shadow_of", Component.translatable(r.name.substring(1)));
        return Component.literal(r.name);
    }

    private static ShadowEntity spawn(ServerPlayer p, HunterData.ShadowRecord rec, Vec3 at) {
        ShadowEntity e = ModEntities.SHADOW.get().create(p.level());
        if (e == null) return null;
        e.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        e.setup(p, rec);
        e.setCustomName(displayName(rec));
        p.level().addFreshEntity(e);
        ACTIVE.computeIfAbsent(p.getUUID(), k -> new ArrayList<>()).add(e);
        return e;
    }

    public static List<ShadowEntity> active(Player p) {
        List<ShadowEntity> l = ACTIVE.get(p.getUUID());
        if (l == null) return List.of();
        l.removeIf(e -> e.isRemoved() || !e.isAlive());
        return l;
    }

    public static void toggle(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        if (d.shadows.isEmpty()) {
            Sys.warn(p, "shadows.none");
            return;
        }
        if (!active(p).isEmpty()) {
            recall(p);
            d.shadowsOut = false;
            Sys.info(p, "shadows.recalled");
        } else {
            summonAll(p);
            d.shadowsOut = true;
            Sys.info(p, "shadows.summoned", active(p).size());
        }
        d.markDirty();
    }

    public static void summonAll(ServerPlayer p) {
        HunterData d = HunterCapability.get(p);
        recall(p);
        int max = Math.min(d.shadows.size(), 6 + d.stat(Stat.INT) / 12 + (d.job == Job.SHADOW_MONARCH ? 10 : 0));
        p.level().playSound(null, p.blockPosition(), ModSounds.ARISE.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
        for (int i = 0; i < max; i++) {
            HunterData.ShadowRecord r = d.shadows.get(i);
            double a = (Math.PI * 2 * i) / max;
            double rad = 2.5 + max * 0.15;
            Vec3 at = p.position().add(Math.cos(a) * rad, 0, Math.sin(a) * rad);
            spawn(p, r, at);
        }
    }

    public static void summonOne(ServerPlayer p, int index) {
        HunterData d = HunterCapability.get(p);
        if (index < 0 || index >= d.shadows.size()) return;
        HunterData.ShadowRecord r = d.shadows.get(index);
        for (ShadowEntity e : active(p)) if (e.recordId.equals(r.id)) {
            e.discard();
            return;
        }
        Vec3 look = p.getLookAngle();
        spawn(p, r, p.position().add(look.x * 2.5, 0, look.z * 2.5));
        d.shadowsOut = true;
    }

    public static void release(ServerPlayer p, int index) {
        HunterData d = HunterCapability.get(p);
        if (index < 0 || index >= d.shadows.size()) return;
        HunterData.ShadowRecord r = d.shadows.remove(index);
        for (ShadowEntity e : new ArrayList<>(active(p))) if (e.recordId.equals(r.id)) e.discard();
        d.markDirty();
    }

    public static void recall(ServerPlayer p) {
        List<ShadowEntity> l = ACTIVE.remove(p.getUUID());
        if (l != null) for (ShadowEntity e : l) {
            if (e.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SQUID_INK, e.getX(), e.getY() + 0.8, e.getZ(), 25, 0.4, 0.6, 0.4, 0.03);
            e.discard();
        }
    }

    public static void onShadowLost(ShadowEntity e) {
        // the record survives: shadows are immortal and come back next time they are summoned
    }

    public static void onShadowKill(ShadowEntity e, ServerPlayer owner, LivingEntity victim) {
        HunterData d = HunterCapability.get(owner);
        for (HunterData.ShadowRecord r : d.shadows) {
            if (!r.id.equals(e.recordId)) continue;
            int xp = r.extra.getInt("xp") + 1 + (victim instanceof SLMonster m ? m.kind.rank.ordinal() * 2 : 0);
            int need = 6 + r.level * 3;
            if (xp >= need && r.level < 99) {
                xp -= need;
                r.level++;
                Sys.notify(owner, Sys.ARISE, Sys.t("system.title"), Sys.t("shadows.level_up", displayName(r), r.level));
            }
            r.extra.putInt("xp", xp);
            d.markDirty();
        }
    }

    /** Shadow Exchange: swap position with the shadow closest to where the player is looking. */
    public static boolean exchange(ServerPlayer p) {
        ShadowEntity best = null;
        double bs = -2;
        Vec3 look = p.getLookAngle();
        for (ShadowEntity e : active(p)) {
            Vec3 to = e.position().subtract(p.position());
            double s = to.normalize().dot(look) + (to.length() > 3 ? 0 : -1);
            if (s > bs) {
                bs = s;
                best = e;
            }
        }
        if (best == null) return false;
        Vec3 a = p.position(), b = best.position();
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.SQUID_INK, a.x, a.y + 1, a.z, 40, 0.3, 0.8, 0.3, 0.02);
        p.teleportTo(b.x, b.y, b.z);
        best.teleportTo(a.x, a.y, a.z);
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.SQUID_INK, b.x, b.y + 1, b.z, 40, 0.3, 0.8, 0.3, 0.02);
        return true;
    }

    public static void onLogout(ServerPlayer p) { recall(p); }
}
