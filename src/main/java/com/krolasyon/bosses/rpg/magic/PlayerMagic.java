package com.krolasyon.bosses.rpg.magic;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.Quest;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.SpellShape;
import com.krolasyon.bosses.rpg.def.SpellDef;
import com.krolasyon.bosses.rpg.item.StaffItem;
import com.krolasyon.bosses.rpg.net.RpgNet;
import com.krolasyon.bosses.rpg.registry.RpgEffects;
import com.krolasyon.bosses.rpg.world.Kingdom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.UUID;

/** Player side of the RPG stats: mana, spell casting, cooldowns, attribute bonuses and client sync. */
public final class PlayerMagic {
    private PlayerMagic() {}

    private static final UUID HEALTH_ID = UUID.fromString("8e3b1c52-7f41-4f0e-9b8a-31a0c6f10001");
    private static final UUID DAMAGE_ID = UUID.fromString("8e3b1c52-7f41-4f0e-9b8a-31a0c6f10002");
    private static final UUID SPEED_ID = UUID.fromString("8e3b1c52-7f41-4f0e-9b8a-31a0c6f10003");
    private static final UUID ASPEED_ID = UUID.fromString("8e3b1c52-7f41-4f0e-9b8a-31a0c6f10004");

    public static String shapeName(SpellShape s) {
        return switch (s) {
            case BOLT -> "Ok"; case BALL -> "Küre"; case BEAM -> "Işın"; case CONE -> "Koni"; case NOVA -> "Halka"; case RAIN -> "Yağmur";
            case WALL -> "Duvar"; case BUFF -> "Güçlendirme"; case HEAL -> "Şifa"; case SUMMON -> "Çağırma"; case TELEPORT -> "Işınlanma";
            case CHAIN -> "Zincir"; case AURA -> "Aura"; case TRAP -> "Tuzak"; case PULL -> "Çekim"; case STRIKE -> "Darbe";
        };
    }

    public static float staffPower(ServerPlayer p) {
        ItemStack m = p.getMainHandItem(), o = p.getOffhandItem();
        if (m.getItem() instanceof StaffItem s) return s.power();
        if (o.getItem() instanceof StaffItem s) return s.power() * 0.9F;
        return 0.75F;
    }

    public static void castSelected(ServerPlayer p) {
        PlayerRpg d = RpgWorldData.player(p);
        if (d.spells.isEmpty()) {
            p.displayClientMessage(Component.literal("§7Henüz hiç büyü bilmiyorsun. Büyü kitapları bul ya da bir büyücüden satın al."), true);
            return;
        }
        d.selected = Math.floorMod(d.selected, d.spells.size());
        SpellDef s = RpgDefs.SPELL_BY_ID.get(d.spells.get(d.selected));
        if (s == null) return;
        Integer cd = d.cooldowns.get(s.id());
        if (cd != null && cd > 0) {
            p.displayClientMessage(Component.literal("§c" + s.name() + " hazır değil (" + String.format("%.1f", cd / 20F) + " sn)"), true);
            return;
        }
        float cost = s.mana();
        if (p.hasEffect(RpgEffects.MANA_FLOW.get())) cost *= 0.7F;
        if (!p.getAbilities().instabuild && d.mana < cost) {
            p.displayClientMessage(Component.literal("§9Yeterli mana yok!"), true);
            p.playNotifySound(SoundEvents.FIRE_EXTINGUISH, p.getSoundSource(), 0.5F, 1.6F);
            return;
        }
        if (!p.getAbilities().instabuild) d.mana -= cost;
        d.cooldowns.put(s.id(), s.cooldown());
        SpellCaster.cast(p, s, staffPower(p) * d.spellPower());
        sync(p);
    }

    public static void cycle(ServerPlayer p, int dir) {
        PlayerRpg d = RpgWorldData.player(p);
        if (d.spells.isEmpty()) return;
        d.selected = Math.floorMod(d.selected + dir, d.spells.size());
        SpellDef s = RpgDefs.SPELL_BY_ID.get(d.spells.get(d.selected));
        if (s != null) p.displayClientMessage(Component.literal("§dSeçili büyü: §f" + s.name() + " §7(" + RpgDefs.schoolName(s.school()) + ", " + s.mana() + " mana)"), true);
        sync(p);
    }

    public static void select(ServerPlayer p, int idx) {
        PlayerRpg d = RpgWorldData.player(p);
        if (idx >= 0 && idx < d.spells.size()) d.selected = idx;
        sync(p);
    }

    public static void allocate(ServerPlayer p, int stat) {
        PlayerRpg d = RpgWorldData.player(p);
        if (d.statPoints <= 0) return;
        d.statPoints--;
        switch (stat) {
            case 0 -> d.str++;
            case 1 -> d.agi++;
            case 2 -> d.intel++;
            default -> d.vit++;
        }
        applyAttributes(p, d);
        sync(p);
    }

    private static void modifier(ServerPlayer p, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id, String name, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = p.getAttribute(attr);
        if (a == null) return;
        AttributeModifier cur = a.getModifier(id);
        if (cur != null && cur.getAmount() == amount) return;
        if (cur != null) a.removeModifier(id);
        if (amount != 0) a.addPermanentModifier(new AttributeModifier(id, name, amount, op));
    }

    public static void applyAttributes(ServerPlayer p, PlayerRpg d) {
        modifier(p, Attributes.MAX_HEALTH, HEALTH_ID, "rpg_vitality", d.vit * 2.0 + (d.level - 1) * 0.5, AttributeModifier.Operation.ADDITION);
        modifier(p, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "rpg_strength", d.str * 0.5, AttributeModifier.Operation.ADDITION);
        modifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, "rpg_agility", Math.min(0.5, d.agi * 0.01), AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(p, Attributes.ATTACK_SPEED, ASPEED_ID, "rpg_agility_attack", Math.min(1.0, d.agi * 0.02), AttributeModifier.Operation.MULTIPLY_BASE);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }

    /** every tick on the server */
    public static void tick(ServerPlayer p) {
        PlayerRpg d = RpgWorldData.player(p);
        if (!d.cooldowns.isEmpty()) {
            d.cooldowns.replaceAll((k, v) -> v - 1);
            d.cooldowns.values().removeIf(v -> v <= 0);
        }
        if (p.tickCount % 10 == 0) {
            float regen = d.manaRegen() * (p.getMainHandItem().getItem() instanceof StaffItem s ? 1 + s.tier * 0.2F : 1);
            if (p.hasEffect(RpgEffects.MANA_FLOW.get())) regen *= 2;
            if (p.isSleeping()) regen *= 4;
            float before = d.mana;
            d.mana = Math.min(d.maxMana(), d.mana + regen * 0.5F);
            if (p.tickCount % 40 == 0 || (int) before != (int) d.mana && p.tickCount % 20 == 0) sync(p);
        }
        if (p.tickCount % 100 == 0) applyAttributes(p, d);
    }

    public static void sync(ServerPlayer p) {
        PlayerRpg d = RpgWorldData.player(p);
        RpgWorldData w = RpgWorldData.get(p.server);
        CompoundTag t = new CompoundTag();
        t.putInt("Level", d.level);
        t.putLong("Xp", d.xp);
        t.putLong("XpNext", d.xpForNext());
        t.putInt("Points", d.statPoints);
        t.putIntArray("Stats", new int[]{d.str, d.agi, d.intel, d.vit});
        t.putFloat("Mana", d.mana);
        t.putInt("MaxMana", d.maxMana());
        ListTag sp = new ListTag();
        for (String s : d.spells) sp.add(StringTag.valueOf(s));
        t.put("Spells", sp);
        t.putInt("Selected", d.selected);
        CompoundTag cds = new CompoundTag();
        for (Map.Entry<String, Integer> e : d.cooldowns.entrySet()) cds.putInt(e.getKey(), e.getValue());
        t.put("Cds", cds);
        t.putString("Social", d.socialTitle());
        t.putInt("SocialIdx", d.social);
        t.putString("Guild", d.guildRank());
        t.putInt("GuildXp", d.guildXp);
        t.putInt("Fame", d.fame);
        t.put("Rep", new IntArrayTag(d.rep));
        t.put("Bounty", new IntArrayTag(d.bounty));
        t.putInt("Allegiance", d.allegiance);
        ListTag q = new ListTag();
        for (Quest x : d.quests) {
            CompoundTag qt = new CompoundTag();
            qt.putString("Title", x.title);
            qt.putString("Desc", x.describe());
            qt.putString("Giver", x.giverName);
            qt.putBoolean("Done", x.done());
            q.add(qt);
        }
        t.put("Quests", q);
        t.putString("Story", com.krolasyon.bosses.rpg.story.Story.objective(d));
        t.putString("Chapter", com.krolasyon.bosses.rpg.story.Story.chapterTitle(d));
        t.putString("Spouse", d.spouseName);
        t.putInt("Children", d.children.size());
        t.putInt("Party", d.party.size());
        t.putInt("Kills", d.kills);
        t.putInt("BossKills", d.bossKills);
        t.putString("Region", d.lastRegion);
        int[] war = new int[Kingdom.COUNT * Kingdom.COUNT];
        int[] rel = new int[Kingdom.COUNT * Kingdom.COUNT];
        for (int i = 0; i < Kingdom.COUNT; i++) for (int j = 0; j < Kingdom.COUNT; j++) {
            war[i * Kingdom.COUNT + j] = w.war[i][j] ? 1 : 0;
            rel[i * Kingdom.COUNT + j] = w.relations[i][j];
        }
        t.putIntArray("War", war);
        t.putIntArray("Relations", rel);
        ListTag news = new ListTag();
        for (String n : w.news) news.add(StringTag.valueOf(n));
        t.put("News", news);
        ListTag bonds = new ListTag();
        for (PlayerRpg.Bond b : d.bonds) bonds.add(StringTag.valueOf(b.name + " — " + bondTitle(b.kind) + (b.dead ? " (öldü)" : "")));
        t.put("Bonds", bonds);
        ListTag sites = new ListTag();
        for (com.krolasyon.bosses.rpg.world.Site s : w.sites) {
            if (s.type == com.krolasyon.bosses.rpg.world.Site.Type.LAIR && !s.bossAlive) continue;
            CompoundTag st = new CompoundTag();
            st.putString("N", s.name);
            st.putByte("T", (byte) s.type.ordinal());
            st.putInt("X", s.x);
            st.putInt("Z", s.z);
            st.putByte("K", (byte) s.kingdom);
            sites.add(st);
        }
        t.put("Sites", sites);
        RpgNet.send(p, RpgNet.SYNC, t);
    }

    public static String bondTitle(String kind) {
        return switch (kind) {
            case "saved" -> "hayatını kurtardın";
            case "freed" -> "özgürlüğüne kavuşturdun";
            case "wronged" -> "sana kin besliyor";
            case "betrayed" -> "ona ihanet ettin";
            case "friend" -> "can dostun";
            default -> kind;
        };
    }
}
