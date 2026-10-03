package com.krolasyon.bosses.realm.story;

import com.krolasyon.bosses.realm.Faction;
import com.krolasyon.bosses.realm.Realm;
import com.krolasyon.bosses.realm.data.RealmData;
import com.krolasyon.bosses.realm.entity.Abilities;
import com.krolasyon.bosses.realm.entity.RealmBoss;
import com.krolasyon.bosses.realm.entity.RealmEntities;
import com.krolasyon.bosses.realm.registry.RealmItems;
import com.krolasyon.bosses.realm.world.Builders;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Calling out the kingdom lords and the Tyrant. */
public final class Summoning {
    private Summoning() {}

    public static String lordId(Faction f) {
        return switch (f) {
            case ASH -> "varkhas";
            case BLOOD -> "serathis";
            case SHADOW -> "nyxar";
            case LEGION -> "grommak";
            case SOUL -> "ilvaine";
        };
    }

    public static String biome(Faction f) {
        return switch (f) {
            case ASH -> "ash_wastes";
            case BLOOD -> "blood_marsh";
            case SHADOW -> "obsidian_forest";
            case LEGION -> "basalt_warfields";
            case SOUL -> "soul_valley";
        };
    }

    public static boolean inBiome(ServerLevel level, BlockPos pos, String biome) {
        return level.getBiome(pos).is(ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Realm.rl(biome)));
    }

    static boolean present(ServerLevel level, BlockPos pos, String id) {
        EntityType<? extends Mob> t = RealmEntities.type(id);
        return t != null && !level.getEntities(t, new net.minecraft.world.phys.AABB(pos).inflate(80), e -> e.isAlive()).isEmpty();
    }

    public static void useAltar(ServerLevel level, BlockPos pos, Faction f, ServerPlayer p) {
        RealmData d = RealmData.get(p);
        if (d.conquered[f.ordinal()] && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.lord_conquered", f.lord()).withStyle(ChatFormatting.GOLD), true);
            return;
        }
        challenge(level, pos.above(), f, p);
    }

    /** war horn / altar: calls the lord of the kingdom to a duel */
    public static boolean challenge(ServerLevel level, BlockPos at, Faction f, ServerPlayer p) {
        String id = lordId(f);
        if (present(level, at, id)) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.lord_present").withStyle(ChatFormatting.RED), true);
            return false;
        }
        RealmData d = RealmData.get(p);
        long now = level.getGameTime();
        if (now - d.lastHorn < 400 && !p.getAbilities().instabuild) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.lord_cooldown").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        d.lastHorn = now;
        d.save(p);
        BlockPos spot = spot(level, at, p, 7);
        spawnBoss(level, id, spot, p);
        level.getServer().getPlayerList().getPlayers().stream().filter(pl -> pl.level() == level && pl.distanceToSqr(spot.getX(), spot.getY(), spot.getZ()) < 96 * 96)
                .forEach(pl -> pl.sendSystemMessage(Component.translatable("message.krolasyonbosses.lord_arrives", f.lord()).withStyle(f.chat, ChatFormatting.BOLD)));
        return true;
    }

    public static void useThrone(ServerLevel level, BlockPos pos, ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (!held.is(RealmItems.THRONE_KEY.get())) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.throne_need_key").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (summonTyrant(level, pos.relative(net.minecraft.core.Direction.SOUTH, 6), p) && !p.getAbilities().instabuild) held.shrink(1);
    }

    /** Throne Key used in the open Throne Wastes: the ruined throne rises from the sand and the Tyrant descends */
    public static boolean useKey(ServerLevel level, ServerPlayer p) {
        if (!Realm.inRealm(level) || !inBiome(level, p.blockPosition(), "throne_wastes")) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.throne_wrong_place").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (present(level, p.blockPosition(), "azgaroth")) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.tyrant_present").withStyle(ChatFormatting.RED), true);
            return false;
        }
        BlockPos c = p.blockPosition().relative(p.getDirection(), 12);
        c = new BlockPos(c.getX(), level.getHeight(Heightmap.Types.WORLD_SURFACE, c.getX(), c.getZ()), c.getZ());
        Builders.keep(level, c, p.getRandom(), null);
        return summonTyrant(level, c.south(5), p);
    }

    static boolean summonTyrant(ServerLevel level, BlockPos at, ServerPlayer p) {
        if (present(level, at, "azgaroth")) {
            p.displayClientMessage(Component.translatable("message.krolasyonbosses.tyrant_present").withStyle(ChatFormatting.RED), true);
            return false;
        }
        BlockPos spot = spot(level, at, p, 0);
        spawnBoss(level, "azgaroth", spot, p);
        for (ServerPlayer pl : level.players()) {
            if (pl.distanceToSqr(spot.getX(), spot.getY(), spot.getZ()) < 128 * 128) {
                pl.sendSystemMessage(Component.translatable("message.krolasyonbosses.tyrant_arrives").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                pl.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.0F, 0.5F);
            }
        }
        return true;
    }

    static BlockPos spot(ServerLevel level, BlockPos near, ServerPlayer p, int away) {
        BlockPos base = near;
        if (away > 0) {
            var dir = p.getLookAngle().multiply(1, 0, 1).normalize();
            base = BlockPos.containing(p.getX() + dir.x * away, p.getY(), p.getZ() + dir.z * away);
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, base.getX(), base.getZ());
        if (Math.abs(y - p.getBlockY()) > 12) y = p.getBlockY();
        return new BlockPos(base.getX(), y, base.getZ());
    }

    static void spawnBoss(ServerLevel level, String id, BlockPos spot, ServerPlayer p) {
        EntityType<? extends Mob> t = RealmEntities.type(id);
        if (t == null) return;
        Mob boss = t.create(level);
        if (boss == null) return;
        boss.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getYRot() + 180F, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
        boss.setTarget(p);
        level.addFreshEntity(boss);
        for (int i = 0; i < 4; i++) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(spot.getX() + level.random.nextInt(9) - 4, spot.getY(), spot.getZ() + level.random.nextInt(9) - 4);
                bolt.setVisualOnly(true);
                level.addFreshEntity(bolt);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5, 1, 0, 0, 0, 0);
        if (boss instanceof RealmBoss rb) Abilities.burst(rb, rb.spec.abilities()[0].element(), 3F, 80);
        level.playSound(null, spot, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.0F, 0.8F);
    }
}
