package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.krolasyon.bosses.rpg.item.RpgLoot;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Fate: strangers in trouble on the road, and the people you once helped (or wronged) finding you again later,
 * somewhere else, when you least expect it.
 */
public final class Fate {
    private Fate() {}

    private record Rescue(UUID player, long until) {}

    private static final Map<UUID, Rescue> RESCUES = new HashMap<>();

    @Nullable
    private static BlockPos ground(ServerLevel l, double x, double z) {
        int ix = (int) Math.floor(x), iz = (int) Math.floor(z);
        int y = com.krolasyon.bosses.rpg.util.Heights.loadedGround(l, ix, iz);
        if (y == Integer.MIN_VALUE) return null;
        BlockPos p = new BlockPos(ix, y, iz);
        if (!l.getBlockState(p.below()).isSolid() || !l.getFluidState(p.below()).isEmpty()) return null;
        return p;
    }

    @Nullable
    private static BlockPos near(ServerPlayer p, double min, double max) {
        RandomSource r = p.getRandom();
        for (int i = 0; i < 10; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = min + r.nextDouble() * (max - min);
            BlockPos b = ground(p.serverLevel(), p.getX() + Math.cos(a) * d, p.getZ() + Math.sin(a) * d);
            if (b != null && Math.abs(b.getY() - p.getY()) < 12) return b;
        }
        return null;
    }

    /** spawn the NPC of a bond with the same identity as before */
    @Nullable
    private static RpgNpc incarnate(ServerLevel l, BlockPos at, PlayerRpg.Bond b, NpcRole role) {
        if (l.getEntity(b.npc) != null) return null;
        RpgNpc n = RpgEntities.NPC.get().create(l);
        if (n == null) return null;
        n.setup(Race.of(b.race), b.gender == 1, role, Math.max(0, b.kingdom), l.random);
        n.setLook(b.race, b.gender == 1, b.variant);
        n.setCustomName(Component.literal(b.name));
        n.setUUID(b.npc);
        n.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, l.random.nextFloat() * 360, 0);
        l.addFreshEntity(n);
        FX.send(l, ParticleTypes.POOF, n.position().add(0, 1, 0), 15, 0.4, 0.05);
        return n;
    }

    @Nullable
    public static RpgNpc summonHelper(ServerPlayer p, PlayerRpg.Bond b, String line) {
        BlockPos at = near(p, 3, 7);
        if (at == null) return null;
        RpgNpc n = incarnate(p.serverLevel(), at, b, NpcRole.ADVENTURER);
        if (n == null) return null;
        n.setLeader(p.getUUID());
        n.setFlag(RpgNpc.F_HELPER, true);
        n.setFlag(RpgNpc.F_FOLLOW, true);
        n.leaveAt = p.level().getGameTime() + 20 * 60 * 4;
        Relation rel = n.rel(p.getUUID());
        rel.affinity = Math.max(rel.affinity, 70);
        rel.trust = Math.max(rel.trust, 60);
        rel.rescued = true;
        n.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40 + b.helps * 10);
        n.setHealth(n.getMaxHealth());
        b.helps++;
        b.lastSeen = p.level().getGameTime();
        p.sendSystemMessage(Component.literal("§a" + b.name + ": \"" + line + "\""));
        FX.column(p.level(), ParticleTypes.END_ROD, n.position(), 2.5, 0.6, 40);
        return n;
    }

    /** every second per player */
    public static void tick(ServerPlayer p, PlayerRpg d, RpgWorldData w) {
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        if (l.dimension() != net.minecraft.world.level.Level.OVERWORLD || !d.originDone || d.chapter < 1) return;
        checkRescues(l);
        // help in a desperate fight
        if (p.getHealth() < p.getMaxHealth() * 0.35F && p.getLastHurtByMob() != null && now - p.getLastHurtByMobTimestamp() < 60) {
            for (PlayerRpg.Bond b : d.bonds) {
                if (b.dead || !(b.kind.equals("saved") || b.kind.equals("freed")) || now - b.lastSeen < 24000L * 2 || p.getRandom().nextInt(4) != 0) continue;
                RpgNpc n = summonHelper(p, b, pick(p.getRandom(), "Dayan! Bir zamanlar sen beni kurtarmıştın, şimdi sıra bende!",
                        "Yine mi başın belada? Hah! Sana borcumu ödeme vakti!", "Seni yalnız bırakmam. Asla!"));
                if (n != null) {
                    n.setTarget(p.getLastHurtByMob());
                    break;
                }
            }
        }
        if (now < d.nextFate) return;
        d.nextFate = now + 20L * 60 * (6 + p.getRandom().nextInt(6));
        Site here = WorldMap.siteAt(w, p.getX(), p.getZ());
        RandomSource r = p.getRandom();
        List<PlayerRpg.Bond> friendly = new ArrayList<>(), grudges = new ArrayList<>();
        for (PlayerRpg.Bond b : d.bonds) {
            if (b.dead || now - b.lastSeen < 24000L) continue;
            if (b.kind.equals("wronged") || b.kind.equals("betrayed")) grudges.add(b);
            else friendly.add(b);
        }
        if (here != null && here.type != Site.Type.CAMP && !friendly.isEmpty() && r.nextInt(2) == 0) {
            reunion(p, d, friendly.get(r.nextInt(friendly.size())), here);
        } else if (here == null && !grudges.isEmpty() && r.nextInt(3) == 0) {
            ambush(p, grudges.get(r.nextInt(grudges.size())));
        } else if (here == null && r.nextInt(3) != 0) {
            rescueEncounter(p, w);
        }
    }

    private static String pick(RandomSource r, String... s) { return s[r.nextInt(s.length)]; }

    private static void reunion(ServerPlayer p, PlayerRpg d, PlayerRpg.Bond b, Site here) {
        BlockPos at = near(p, 4, 9);
        if (at == null) return;
        RandomSource r = p.getRandom();
        NpcRole role = b.helps > 1 || r.nextBoolean() ? NpcRole.MERCHANT : r.nextBoolean() ? NpcRole.NOBLE : NpcRole.ADVENTURER;
        RpgNpc n = incarnate(p.serverLevel(), at, b, role);
        if (n == null) return;
        n.home = at;
        n.siteId = here.id;
        n.setKingdom(here.kingdom >= 0 ? here.kingdom : n.kingdomId());
        Relation rel = n.rel(p.getUUID());
        rel.affinity = Math.max(rel.affinity, 65);
        rel.trust = Math.max(rel.trust, 55);
        rel.rescued = true;
        rel.met = true;
        b.lastSeen = p.level().getGameTime();
        b.helps++;
        String job = role == NpcRole.MERCHANT ? "Artık kendi dükkanım var! Sana her zaman indirim yaparım." : role == NpcRole.NOBLE ? "Kaderin cilvesi... Artık bir soyluyum. Sarayda adını iyilikle anıyorum." : "Ben de bir maceracı oldum, senin gibi!";
        p.sendSystemMessage(Component.literal("§b" + b.name + ": \"" + pick(r, "Sen! Seni tanıdım! Hayatımı kurtaran kişi!", "Bu yüz... Sen o kişisin! Yıllardır seni arıyordum!",
                "Kader bizi yeniden buluşturdu, dostum!") + " " + job + "\""));
        NpcQuests.give(p, r.nextBoolean() ? RpgLoot.randomTome(r, 1, Math.min(5, 2 + b.helps)) : new net.minecraft.world.item.ItemStack(com.krolasyon.bosses.rpg.item.RpgItems.GOLD_COIN.get(), 1 + b.helps));
        p.sendSystemMessage(Component.literal("§7(" + b.name + " sana minnettarlığını bir hediyeyle gösterdi.)"));
        d.fame += 3;
        PlayerMagic.sync(p);
    }

    private static void ambush(ServerPlayer p, PlayerRpg.Bond b) {
        BlockPos at = near(p, 10, 16);
        if (at == null) return;
        RpgNpc n = incarnate(p.serverLevel(), at, b, NpcRole.BANDIT);
        if (n == null) return;
        n.setFlag(RpgNpc.F_HOSTILE, true);
        n.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(60);
        n.setHealth(60);
        n.setTarget(p);
        n.leaveAt = p.level().getGameTime() + 20 * 60 * 5;
        b.lastSeen = p.level().getGameTime();
        for (int i = 0; i < 2; i++) {
            RpgNpc thug = NpcFactory.spawn(p.serverLevel(), at.offset(i * 2 - 1, 0, 2), Race.of(p.getRandom().nextInt(Race.values().length)), false, NpcRole.BANDIT, -0, null, null);
            if (thug != null) { thug.setTarget(p); thug.leaveAt = n.leaveAt; }
        }
        p.sendSystemMessage(Component.literal("§c" + b.name + ": \"Beni hatırladın mı? " + (b.kind.equals("betrayed") ? "Bana ihanet ettiğin günü bir an olsun unutmadım!" : "Yaptığının hesabını sormaya geldim!") + "\""));
        p.playNotifySound(SoundEvents.PILLAGER_CELEBRATE, p.getSoundSource(), 1.0F, 0.8F);
    }

    private static void rescueEncounter(ServerPlayer p, RpgWorldData w) {
        ServerLevel l = p.serverLevel();
        RandomSource r = p.getRandom();
        BlockPos at = near(p, 18, 28);
        if (at == null) return;
        RegionId region = WorldMap.regionAt(l, at);
        int k = region.wild() ? r.nextInt(Kingdom.COUNT) : region.kingdom;
        Kingdom kk = Kingdom.of(k);
        NpcRole[] roles = {NpcRole.MERCHANT, NpcRole.PEASANT, NpcRole.NOBLE, NpcRole.PRIEST, NpcRole.ADVENTURER, NpcRole.WORKER};
        NpcRole role = roles[r.nextInt(roles.length)];
        RpgNpc v = NpcFactory.spawn(l, at, kk.citizens()[r.nextInt(kk.citizens().length)], r.nextBoolean(), role, k, null, null);
        if (v == null) return;
        v.setFlag(RpgNpc.F_DISTRESSED, true);
        v.distressTicks = 20 * 120;
        v.home = null;
        v.leaveAt = l.getGameTime() + 20 * 60 * 4;
        RESCUES.put(v.getUUID(), new Rescue(p.getUUID(), l.getGameTime() + 20 * 150));
        List<MonsterDef> pool = new ArrayList<>();
        for (MonsterDef d : RpgDefs.MONSTERS) {
            if (d.danger() > Math.min(5, 2 + RpgWorldData.player(p).level / 8) || d.has(RpgDefs.T_FLYING)) continue;
            for (RegionId x : d.regions()) if (x == region) { pool.add(d); break; }
        }
        boolean bandits = pool.isEmpty() || r.nextInt(3) == 0;
        int n = 2 + r.nextInt(2);
        for (int i = 0; i < n; i++) {
            BlockPos sp = at.offset(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
            Mob m;
            if (bandits) {
                m = NpcFactory.spawn(l, sp, Race.of(r.nextInt(Race.values().length)), r.nextInt(4) == 0, NpcRole.BANDIT, k, null, null);
                if (m instanceof RpgNpc rn) rn.leaveAt = v.leaveAt;
            } else {
                EntityType<?> t = RpgEntities.typeOf(pool.get(r.nextInt(pool.size())).id());
                Entity e = t == null ? null : t.spawn(l, sp, MobSpawnType.EVENT);
                m = e instanceof Mob mm ? mm : null;
            }
            if (m != null) {
                m.setTarget(v);
                m.getPersistentData().putUUID("FateVictim", v.getUUID());
            }
        }
        p.sendSystemMessage(Component.literal("§e⚠ Yakınlarda biri yardım için bağırıyor: \"" + pick(r, "İmdat! Biri yardım etsin!", "Yardım edin! Beni öldürecekler!", "Lütfen! Kimse yok mu?!") + "\" §7(" + NpcDialog.direction(p.getX(), p.getZ(), at.getX(), at.getZ()) + ")"));
        l.playSound(null, at, SoundEvents.VILLAGER_HURT, net.minecraft.sounds.SoundSource.NEUTRAL, 2.0F, 1.2F);
    }

    private static void checkRescues(ServerLevel l) {
        if (RESCUES.isEmpty()) return;
        Iterator<Map.Entry<UUID, Rescue>> it = RESCUES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Rescue> e = it.next();
            Entity ent = l.getEntity(e.getKey());
            ServerPlayer p = l.getServer().getPlayerList().getPlayer(e.getValue().player);
            if (p == null || l.getGameTime() > e.getValue().until) { it.remove(); continue; }
            if (!(ent instanceof RpgNpc v) || !v.isAlive()) {
                it.remove();
                if (p.distanceToSqr(ent == null ? p.position() : ent.position()) < 40 * 40 && p.getRandom().nextInt(3) == 0 && ent instanceof RpgNpc dead) {
                    PlayerRpg.Bond b = new PlayerRpg.Bond();
                    b.npc = UUID.randomUUID();
                    b.name = dead.race().randomName(p.getRandom(), !dead.female());
                    b.race = dead.race().ordinal();
                    b.gender = dead.female() ? 0 : 1;
                    b.kingdom = dead.kingdomId();
                    b.kind = "wronged";
                    b.since = l.getGameTime();
                    b.lastSeen = l.getGameTime();
                    RpgWorldData.player(p).bonds.add(b);
                    p.sendSystemMessage(Component.literal("§8" + dead.npcName() + " öldü. Uzaktan bir çift göz seni izliyordu... " + b.name + " bunu sana ödetmeye yemin etti."));
                }
                continue;
            }
            boolean threats = !l.getEntitiesOfClass(Mob.class, v.getBoundingBox().inflate(16), m -> m.isAlive() && m.getPersistentData().hasUUID("FateVictim")
                    && m.getPersistentData().getUUID("FateVictim").equals(v.getUUID())).isEmpty();
            if (!threats && p.distanceToSqr(v) < 32 * 32) {
                it.remove();
                saved(p, v);
            }
        }
    }

    private static void saved(ServerPlayer p, RpgNpc v) {
        v.setFlag(RpgNpc.F_DISTRESSED, false);
        Relation rel = v.rel(p.getUUID());
        rel.add(60, 45);
        rel.rescued = true;
        rel.met = true;
        Bonds.add(p, v, "saved");
        PlayerRpg d = RpgWorldData.player(p);
        d.fame += 4;
        d.addRep(v.kingdomId(), 20);
        int reward = switch (v.role()) { case NOBLE -> 300; case MERCHANT -> 120; case PRIEST -> 40; default -> 25; };
        NpcQuests.giveCoins(p, reward);
        d.addXp(60);
        v.leaveAt = p.level().getGameTime() + 20 * 45;
        v.home = v.blockPosition();
        FX.send(p.level(), ParticleTypes.HEART, v.position().add(0, v.getBbHeight() + 0.3, 0), 6, 0.4, 0.05);
        p.sendSystemMessage(Component.literal("§a" + v.npcName() + " (" + v.race().title + " " + v.role().title + "): \"Hayatımı kurtardın! Al, bu senin (" + NpcQuests.coins(reward)
                + "). Adını asla unutmayacağım... Bir gün bu iyiliğini ödeyeceğim, yemin ederim!\""));
        p.sendSystemMessage(Component.literal("§7(" + v.npcName() + " artık seninle kader bağıyla bağlı. Onu ileride başka bir yerde yeniden görebilirsin.)"));
        PlayerMagic.sync(p);
    }
}
