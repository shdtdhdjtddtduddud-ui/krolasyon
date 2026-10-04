package com.rabona.arena.game;

import com.rabona.arena.entity.FootballerEntity;
import com.rabona.arena.net.Net;
import com.rabona.arena.net.S2C;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Jeton ve oyuncu kartlari. Maclarda jeton kazanilir, paketler acilir, kartlar kadroya alinir.
 * Kadrodaki kartlar takimdaki botlarin kimligi ve guclerine donusur.
 */
public final class Cards {
    private Cards() {}

    public static final int START_COINS = 400, SQUAD_MAX = 11;

    public record Card(String name, int pos, int ovr, int pac, int sho, int pas, int dri, int def, int phy, int rarity, int skin) {
        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("n", name);
            t.putIntArray("v", new int[]{pos, ovr, pac, sho, pas, dri, def, phy, rarity, skin});
            return t;
        }

        public static Card load(CompoundTag t) {
            int[] v = t.getIntArray("v");
            if (v.length < 10) v = Arrays.copyOf(v, 10);
            return new Card(t.getString("n"), v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9]);
        }

        public void write(FriendlyByteBuf b) {
            b.writeUtf(name, 40);
            for (int x : new int[]{pos, ovr, pac, sho, pas, dri, def, phy, rarity, skin}) b.writeByte(x);
        }

        public static Card read(FriendlyByteBuf b) {
            String n = b.readUtf(40);
            int[] v = new int[10];
            for (int i = 0; i < 10; i++) v[i] = b.readUnsignedByte();
            return new Card(n, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9]);
        }

        public int sellValue() { return 10 + Math.max(0, ovr - 50) * (rarity + 1) * 2; }
    }

    /** Paketler: fiyat, kart sayisi, puan araligi, efsane sansi. */
    public enum Pack {
        BRONZE(100, 3, 50, 64, 0.0), SILVER(250, 3, 62, 75, 0.0), GOLD(600, 3, 74, 85, 0.06), LEGEND(1500, 3, 82, 92, 0.35);

        public final int price, count, min, max;
        public final double legendChance;

        Pack(int price, int count, int min, int max, double legendChance) {
            this.price = price;
            this.count = count;
            this.min = min;
            this.max = max;
            this.legendChance = legendChance;
        }

        public static Pack byId(int i) { return values()[Math.max(0, Math.min(values().length - 1, i))]; }
    }

    public static class Profile {
        public int coins = START_COINS;
        public final List<Card> cards = new ArrayList<>();
        public final List<Integer> squad = new ArrayList<>();
    }

    // ================================================================ veri
    public static class Data extends SavedData {
        final Map<UUID, Profile> profiles = new HashMap<>();

        static Data load(CompoundTag t) {
            Data d = new Data();
            CompoundTag all = t.getCompound("p");
            for (String k : all.getAllKeys()) {
                CompoundTag pt = all.getCompound(k);
                Profile p = new Profile();
                p.coins = pt.getInt("coins");
                for (Tag c : pt.getList("cards", Tag.TAG_COMPOUND)) p.cards.add(Card.load((CompoundTag) c));
                for (int i : pt.getIntArray("squad")) if (i >= 0 && i < p.cards.size()) p.squad.add(i);
                try {
                    d.profiles.put(UUID.fromString(k), p);
                } catch (IllegalArgumentException ignored) {
                }
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            CompoundTag all = new CompoundTag();
            profiles.forEach((u, p) -> {
                CompoundTag pt = new CompoundTag();
                pt.putInt("coins", p.coins);
                ListTag l = new ListTag();
                for (Card c : p.cards) l.add(c.save());
                pt.put("cards", l);
                pt.putIntArray("squad", p.squad.stream().mapToInt(Integer::intValue).toArray());
                all.put(u.toString(), pt);
            });
            t.put("p", all);
            return t;
        }
    }

    private static Data data(ServerPlayer p) {
        return p.server.overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "rabonaarena_cards");
    }

    public static Profile profile(ServerPlayer p) {
        Data d = data(p);
        return d.profiles.computeIfAbsent(p.getUUID(), u -> {
            d.setDirty();
            return new Profile();
        });
    }

    // ================================================================ islemler
    public static void reward(ServerPlayer p, int coins, String reason) {
        Profile pr = profile(p);
        pr.coins += coins;
        data(p).setDirty();
        p.displayClientMessage(Component.translatable("msg.rabonaarena.coins", coins, Component.translatable("reason.rabonaarena." + reason))
                .withStyle(ChatFormatting.GOLD), true);
        sync(p, List.of());
    }

    public static void openPack(ServerPlayer p, Pack pack) {
        Profile pr = profile(p);
        if (pr.coins < pack.price) {
            p.displayClientMessage(Component.translatable("msg.rabonaarena.not_enough_coins", pack.price).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (pr.cards.size() + pack.count > 200) {
            p.displayClientMessage(Component.translatable("msg.rabonaarena.collection_full").withStyle(ChatFormatting.RED), true);
            return;
        }
        pr.coins -= pack.price;
        List<Card> got = new ArrayList<>();
        RandomSource r = p.getRandom();
        for (int i = 0; i < pack.count; i++) got.add(generate(r, pack));
        got.sort(Comparator.comparingInt(Card::ovr));
        pr.cards.addAll(got);
        // kadro bossa otomatik doldur
        for (int i = pr.cards.size() - got.size(); i < pr.cards.size() && pr.squad.size() < SQUAD_MAX; i++) pr.squad.add(i);
        data(p).setDirty();
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        sync(p, got);
    }

    public static void toggleSquad(ServerPlayer p, int idx) {
        Profile pr = profile(p);
        if (idx < 0 || idx >= pr.cards.size()) return;
        if (pr.squad.contains(idx)) pr.squad.remove((Integer) idx);
        else if (pr.squad.size() < SQUAD_MAX) pr.squad.add(idx);
        data(p).setDirty();
        sync(p, List.of());
        Match m = Match.get(p.server);
        Cards.applySquad(m, Match.teamOf(p));
    }

    public static void sell(ServerPlayer p, int idx) {
        Profile pr = profile(p);
        if (idx < 0 || idx >= pr.cards.size()) return;
        Card c = pr.cards.remove(idx);
        pr.coins += c.sellValue();
        List<Integer> sq = new ArrayList<>();
        for (int i : pr.squad) if (i != idx) sq.add(i > idx ? i - 1 : i);
        pr.squad.clear();
        pr.squad.addAll(sq);
        data(p).setDirty();
        p.displayClientMessage(Component.translatable("msg.rabonaarena.sold", c.name(), c.sellValue()).withStyle(ChatFormatting.YELLOW), true);
        sync(p, List.of());
    }

    public static void sync(ServerPlayer p, List<Card> opened) {
        Profile pr = profile(p);
        Net.toPlayer(p, new S2C.Profile(pr.coins, new ArrayList<>(pr.cards), new ArrayList<>(pr.squad), new ArrayList<>(opened)));
    }

    // ================================================================ kart uretimi
    static Card generate(RandomSource r, Pack pack) {
        boolean legend = r.nextDouble() < pack.legendChance;
        int ovr = legend ? 86 + r.nextInt(9) : pack.min + r.nextInt(pack.max - pack.min + 1);
        int rarity = legend ? 3 : ovr >= 75 ? 2 : ovr >= 65 ? 1 : 0;
        Pos pos = Pos.values()[r.nextInt(Pos.values().length)];
        int[] off = switch (pos.role) {
            case GK -> new int[]{-14, -30, -6, -18, 6, 2};
            case DEF -> new int[]{-2, -16, -4, -8, 8, 6};
            case MID -> new int[]{0, -2, 6, 4, -6, -2};
            case FWD -> new int[]{5, 7, -2, 5, -22, 0};
        };
        int[] st = new int[6];
        for (int i = 0; i < 6; i++) st[i] = Math.max(30, Math.min(99, ovr + off[i] + r.nextInt(9) - 4));
        return new Card(BotNames.random(r), pos.ordinal(), ovr, st[0], st[1], st[2], st[3], st[4], st[5], rarity,
                r.nextInt(FootballerEntity.SKINS));
    }

    /** Takim kaptaninin (ilk oyuncu) kadrosundaki kartlari botlara uygula. */
    public static void applySquad(Match m, Team t) {
        if (!t.playing()) return;
        List<ServerPlayer> humans = m.playersOf(t);
        if (humans.isEmpty()) return;
        Profile pr = profile(humans.get(0));
        if (pr.squad.isEmpty()) return;
        List<Card> pool = new ArrayList<>();
        for (int i : pr.squad) if (i >= 0 && i < pr.cards.size()) pool.add(pr.cards.get(i));
        List<FootballerEntity> bots = new ArrayList<>();
        for (FootballerEntity f : m.bots()) if (f.getSquad() == t) bots.add(f);
        bots.sort(Comparator.comparingInt(f -> f.isKeeper() ? 0 : 1));
        for (FootballerEntity f : bots) {
            if (pool.isEmpty()) break;
            Pos want = f.getFieldPos();
            Card best = null;
            double bd = 99;
            for (Card c : pool) {
                Pos cp = Pos.byId(c.pos());
                double d = (cp == want ? -2 : cp.dist(want)) - c.ovr() * 0.002;
                if ((want == Pos.GK) != (cp == Pos.GK)) d += 3;
                if (d < bd) { bd = d; best = c; }
            }
            pool.remove(best);
            f.setup(t, f.getNumber(), best.skin(), best.name(), best.ovr());
            f.setStats(best.pac(), best.sho(), best.pas(), best.dri(), best.def());
        }
    }
}
