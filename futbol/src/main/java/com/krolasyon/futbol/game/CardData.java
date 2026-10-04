package com.krolasyon.futbol.game;

import com.krolasyon.futbol.network.Net;
import com.krolasyon.futbol.network.ProfileS2C;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/** Persistent per-player club data: jeton (coins) and the player card collection / squad. */
public class CardData extends SavedData {
    public static final int START_COINS = 300;
    public static final int MAX_SQUAD = 10;
    public static final String[] RARITY = {"Bronz", "Gümüş", "Altın", "Efsane"};
    public static final int[] SELL = {20, 50, 120, 300};
    public static final int[] PACK_PRICE = {100, 300, 800};
    public static final String[] PACK_NAME = {"Bronz Paket", "Altın Paket", "Efsane Paket"};
    private static final String[] STATS = {"HIZ", "ŞUT", "PAS", "DRİ", "DEF", "KAL"};
    private static final String[] LEGENDS = {"Lefter", "Metin", "Hakan", "Rüştü", "Tugay", "Arda", "Emre", "Nihat", "Tanju", "Rıdvan",
            "Bülent", "Oğuz", "Feyyaz", "Hami", "Can", "Sergen"};

    public static final class Card {
        public int id;
        public String name;
        public int role;
        public int rarity;
        public int[] stats = new int[6];
        public int skin;
        public boolean squad;

        public int overall() {
            Role r = Role.byId(role);
            int[] w = switch (r) {
                case GK -> new int[]{1, 0, 1, 0, 2, 6};
                case DEF -> new int[]{2, 0, 2, 1, 5, 0};
                case MID -> new int[]{2, 1, 4, 2, 1, 0};
                case WING -> new int[]{4, 2, 2, 4, 0, 0};
                default -> new int[]{3, 5, 1, 3, 0, 0};
            };
            int sum = 0, ws = 0;
            for (int i = 0; i < 6; i++) {
                sum += w[i] * stats[i];
                ws += w[i];
            }
            return Math.round(sum / (float) ws);
        }

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("id", id);
            t.putString("name", name);
            t.putInt("role", role);
            t.putInt("rarity", rarity);
            t.putIntArray("stats", stats);
            t.putInt("skin", skin);
            t.putBoolean("squad", squad);
            return t;
        }

        static Card load(CompoundTag t) {
            Card c = new Card();
            c.id = t.getInt("id");
            c.name = t.getString("name");
            c.role = t.getInt("role");
            c.rarity = t.getInt("rarity");
            int[] s = t.getIntArray("stats");
            if (s.length == 6) c.stats = s;
            c.skin = t.getInt("skin");
            c.squad = t.getBoolean("squad");
            return c;
        }

        public void write(FriendlyByteBuf b) {
            b.writeVarInt(id);
            b.writeUtf(name, 64);
            b.writeByte(role);
            b.writeByte(rarity);
            for (int s : stats) b.writeByte(s);
            b.writeByte(skin);
            b.writeBoolean(squad);
        }

        public static Card read(FriendlyByteBuf b) {
            Card c = new Card();
            c.id = b.readVarInt();
            c.name = b.readUtf(64);
            c.role = b.readByte();
            c.rarity = b.readByte();
            for (int i = 0; i < 6; i++) c.stats[i] = b.readByte();
            c.skin = b.readByte();
            c.squad = b.readBoolean();
            return c;
        }
    }

    public static final class Profile {
        public int coins = START_COINS;
        public int nextId = 1;
        public final List<Card> cards = new ArrayList<>();

        public List<Card> squad() {
            List<Card> l = new ArrayList<>();
            for (Card c : cards) if (c.squad) l.add(c);
            return l;
        }
    }

    private final Map<UUID, Profile> profiles = new HashMap<>();

    public static CardData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(CardData::load, CardData::new, "krolasyonfutbol_cards");
    }

    public Profile profile(UUID id) {
        return profiles.computeIfAbsent(id, k -> {
            setDirty();
            return new Profile();
        });
    }

    private static CardData load(CompoundTag tag) {
        CardData d = new CardData();
        CompoundTag ps = tag.getCompound("profiles");
        for (String k : ps.getAllKeys()) {
            CompoundTag pt = ps.getCompound(k);
            Profile p = new Profile();
            p.coins = pt.getInt("coins");
            p.nextId = Math.max(1, pt.getInt("next"));
            ListTag cl = pt.getList("cards", Tag.TAG_COMPOUND);
            for (int i = 0; i < cl.size(); i++) p.cards.add(Card.load(cl.getCompound(i)));
            try {
                d.profiles.put(UUID.fromString(k), p);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag ps = new CompoundTag();
        for (Map.Entry<UUID, Profile> e : profiles.entrySet()) {
            CompoundTag pt = new CompoundTag();
            pt.putInt("coins", e.getValue().coins);
            pt.putInt("next", e.getValue().nextId);
            ListTag cl = new ListTag();
            for (Card c : e.getValue().cards) cl.add(c.save());
            pt.put("cards", cl);
            ps.put(e.getKey().toString(), pt);
        }
        tag.put("profiles", ps);
        return tag;
    }

    // ------------------------------------------------------------------ actions

    public static void sync(ServerPlayer sp, int revealed) {
        Profile p = get(sp.server).profile(sp.getUUID());
        Net.sendTo(sp, new ProfileS2C(p.coins, p.cards, revealed));
    }

    public static void addCoins(ServerPlayer sp, int amount, String why) {
        CardData d = get(sp.server);
        Profile p = d.profile(sp.getUUID());
        p.coins += amount;
        d.setDirty();
        sp.displayClientMessage(Component.literal("+" + amount + " jeton  (" + why + ")").withStyle(ChatFormatting.GOLD), false);
        sync(sp, -1);
    }

    public static void openPack(ServerPlayer sp, int type) {
        type = Mth.clamp(type, 0, 2);
        CardData d = get(sp.server);
        Profile p = d.profile(sp.getUUID());
        if (p.coins < PACK_PRICE[type]) {
            sp.displayClientMessage(Component.literal("Yetersiz jeton! (" + PACK_PRICE[type] + " gerekli)").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (p.cards.size() >= 120) {
            sp.displayClientMessage(Component.literal("Koleksiyon dolu (120). Kart sat!").withStyle(ChatFormatting.RED), true);
            return;
        }
        p.coins -= PACK_PRICE[type];
        RandomSource r = sp.getRandom();
        float x = r.nextFloat();
        int rarity = switch (type) {
            case 0 -> x < 0.70F ? 0 : x < 0.95F ? 1 : 2;
            case 1 -> x < 0.55F ? 1 : x < 0.95F ? 2 : 3;
            default -> x < 0.6F ? 2 : 3;
        };
        Card c = generate(r, rarity);
        c.id = p.nextId++;
        p.cards.add(c);
        d.setDirty();
        sp.level().playSound(null, sp.blockPosition(), rarity >= 2 ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.8F, 1.0F);
        if (rarity == 3)
            MatchManager.broadcast(Component.literal("✦ " + sp.getName().getString() + " EFSANE kart çıkardı: " + c.name + " (" + c.overall() + ")")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        sync(sp, c.id);
    }

    public static void toggleSquad(ServerPlayer sp, int id) {
        CardData d = get(sp.server);
        Profile p = d.profile(sp.getUUID());
        for (Card c : p.cards) {
            if (c.id != id) continue;
            if (!c.squad && p.squad().size() >= MAX_SQUAD) {
                sp.displayClientMessage(Component.literal("Kadro en fazla " + MAX_SQUAD + " kart!").withStyle(ChatFormatting.RED), true);
                return;
            }
            c.squad = !c.squad;
            d.setDirty();
        }
        sync(sp, -1);
    }

    public static void sell(ServerPlayer sp, int id) {
        CardData d = get(sp.server);
        Profile p = d.profile(sp.getUUID());
        Iterator<Card> it = p.cards.iterator();
        while (it.hasNext()) {
            Card c = it.next();
            if (c.id == id) {
                it.remove();
                p.coins += SELL[c.rarity];
                d.setDirty();
                sp.displayClientMessage(Component.literal(c.name + " satıldı: +" + SELL[c.rarity] + " jeton").withStyle(ChatFormatting.GOLD), true);
                break;
            }
        }
        sync(sp, -1);
    }

    public static Card generate(RandomSource r, int rarity) {
        Card c = new Card();
        c.rarity = rarity;
        c.role = r.nextInt(Role.values().length);
        if (r.nextFloat() < 0.1F) c.role = Role.GK.ordinal();
        int lo = new int[]{45, 60, 72, 84}[rarity], hi = new int[]{64, 74, 86, 97}[rarity];
        for (int i = 0; i < 6; i++) c.stats[i] = lo + r.nextInt(hi - lo + 1);
        Role role = Role.byId(c.role);
        int boost = 4 + rarity * 2;
        switch (role) {
            case GK -> {
                c.stats[5] = Math.min(99, c.stats[5] + boost);
                c.stats[1] = Math.max(30, c.stats[1] - 20);
            }
            case DEF -> {
                c.stats[4] = Math.min(99, c.stats[4] + boost);
                c.stats[5] = Math.max(20, c.stats[5] - 25);
            }
            case MID -> {
                c.stats[2] = Math.min(99, c.stats[2] + boost);
                c.stats[5] = Math.max(20, c.stats[5] - 25);
            }
            case WING -> {
                c.stats[0] = Math.min(99, c.stats[0] + boost);
                c.stats[3] = Math.min(99, c.stats[3] + boost / 2);
                c.stats[5] = Math.max(20, c.stats[5] - 25);
            }
            default -> {
                c.stats[1] = Math.min(99, c.stats[1] + boost);
                c.stats[5] = Math.max(20, c.stats[5] - 25);
            }
        }
        c.skin = r.nextInt(8);
        c.name = rarity == 3 ? LEGENDS[r.nextInt(LEGENDS.length)] : BotNames.random(r);
        return c;
    }

    public static String statName(int i) { return STATS[i]; }
}
