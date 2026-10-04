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

    /** Kadro: 11 ilk on bes (taktigin dizilis sirasiyla) + 7 yedek. -1 = bos yer. */
    public static final int START_COINS = 400, SQUAD_MAX = 11, BENCH_MAX = 7, LINEUP = SQUAD_MAX + BENCH_MAX, MANAGER_PRICE = 350;

    /** Menajer karti: taktigi takimin oyun planini, puani takimin gucunu belirler. */
    public record Manager(String name, int tactic, int ovr, int rarity, int skin) {
        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("n", name);
            t.putIntArray("v", new int[]{tactic, ovr, rarity, skin});
            return t;
        }

        public static Manager load(CompoundTag t) {
            int[] v = Arrays.copyOf(t.getIntArray("v"), 4);
            return new Manager(t.getString("n"), v[0], v[1], v[2], v[3]);
        }

        public void write(FriendlyByteBuf b) {
            b.writeUtf(name, 40);
            b.writeByte(tactic);
            b.writeByte(ovr);
            b.writeByte(rarity);
            b.writeByte(skin);
        }

        public static Manager read(FriendlyByteBuf b) {
            return new Manager(b.readUtf(40), b.readUnsignedByte(), b.readUnsignedByte(), b.readUnsignedByte(), b.readUnsignedByte());
        }

        public Tactic tacticEnum() { return Tactic.byId(tactic); }

        /** Takim botlarina eklenen guc (menajer puanina gore). */
        public int bonus() { return Math.max(-2, (ovr - 72) / 5); }

        public static Manager random(RandomSource r, int min, int max) {
            int ovr = min + r.nextInt(Math.max(1, max - min + 1));
            int rarity = ovr >= 86 ? 3 : ovr >= 75 ? 2 : ovr >= 65 ? 1 : 0;
            return new Manager(BotNames.random(r), r.nextInt(Tactic.values().length), ovr, rarity, r.nextInt(FootballerEntity.SKINS));
        }
    }

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
        /** Kadro sirasi: [0..10] ilk 11 (dizilis yuvasi), [11..17] yedekler; -1 bos. */
        public final List<Integer> squad = new ArrayList<>();
        public final List<Manager> managers = new ArrayList<>();
        public int manager = -1;

        public Manager activeManager() { return manager >= 0 && manager < managers.size() ? managers.get(manager) : null; }

        public int squadCount() {
            int n = 0;
            for (int i : squad) if (i >= 0) n++;
            return n;
        }

        public int at(int slot) { return slot >= 0 && slot < squad.size() ? squad.get(slot) : -1; }

        void put(int slot, int card) {
            while (squad.size() <= slot) squad.add(-1);
            squad.set(slot, card);
            while (!squad.isEmpty() && squad.get(squad.size() - 1) < 0) squad.remove(squad.size() - 1);
        }

        /** Karti ilk bos yere koy (once ilk 11, sonra yedek). */
        boolean add(int card) {
            for (int i = 0; i < LINEUP; i++) {
                if (at(i) < 0) {
                    put(i, card);
                    return true;
                }
            }
            return false;
        }
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
                for (int i : pt.getIntArray("squad")) if (p.squad.size() < LINEUP) p.squad.add(i >= 0 && i < p.cards.size() ? i : -1);
                for (Tag c : pt.getList("mgrs", Tag.TAG_COMPOUND)) p.managers.add(Manager.load((CompoundTag) c));
                p.manager = pt.contains("mgr") ? pt.getInt("mgr") : -1;
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
                ListTag ml = new ListTag();
                for (Manager m : p.managers) ml.add(m.save());
                pt.put("mgrs", ml);
                pt.putInt("mgr", p.manager);
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
        for (int i = pr.cards.size() - got.size(); i < pr.cards.size(); i++) if (!pr.add(i)) break;
        data(p).setDirty();
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        sync(p, got);
    }

    public static void toggleSquad(ServerPlayer p, int idx) {
        Profile pr = profile(p);
        if (idx < 0 || idx >= pr.cards.size()) return;
        int at = pr.squad.indexOf(idx);
        if (at >= 0) pr.put(at, -1);
        else pr.add(idx);
        changed(p);
    }

    /** Kadro duzenleme: iki yuvanin yerini degistir (ilk 11 <-> yedek dahil). */
    public static void swapSlots(ServerPlayer p, int a, int b) {
        Profile pr = profile(p);
        if (a < 0 || b < 0 || a >= LINEUP || b >= LINEUP || a == b) return;
        int ca = pr.at(a), cb = pr.at(b);
        pr.put(a, cb);
        pr.put(b, ca);
        changed(p);
    }

    /** Koleksiyondaki karti bir yuvaya koy (yuvadaki kart koleksiyona doner). */
    public static void placeCard(ServerPlayer p, int slot, int card) {
        Profile pr = profile(p);
        if (slot < 0 || slot >= LINEUP || card < 0 || card >= pr.cards.size()) return;
        int was = pr.squad.indexOf(card);
        if (was >= 0) {
            swapSlots(p, was, slot);
            return;
        }
        pr.put(slot, card);
        changed(p);
    }

    public static void clearSlot(ServerPlayer p, int slot) {
        Profile pr = profile(p);
        if (slot < 0 || slot >= LINEUP) return;
        pr.put(slot, -1);
        changed(p);
    }

    /** En iyi 11'i otomatik kur: taktigin dizilisindeki her yuvaya en uygun kart. */
    public static void autoLineup(ServerPlayer p) {
        Profile pr = profile(p);
        Manager mg = pr.activeManager();
        Pos[] f = Pos.formation(11, mg == null ? Tactic.BALANCED : mg.tacticEnum());
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < pr.cards.size(); i++) pool.add(i);
        pool.sort(Comparator.comparingInt(i -> -pr.cards.get(i).ovr()));
        pr.squad.clear();
        for (int s = 0; s < f.length; s++) {
            int best = -1;
            double bd = 1e9;
            for (int i : pool) {
                Card c = pr.cards.get(i);
                Pos cp = Pos.byId(c.pos());
                double d = (cp == f[s] ? 0 : cp.dist(f[s]) + 0.4) * 30 - c.ovr();
                if ((f[s] == Pos.GK) != (cp == Pos.GK)) d += 200;
                if (d < bd) { bd = d; best = i; }
            }
            pr.put(s, best);
            if (best >= 0) pool.remove((Integer) best);
        }
        for (int s = SQUAD_MAX; s < LINEUP && !pool.isEmpty(); s++) pr.put(s, pool.remove(0));
        changed(p);
    }

    // ================================================================ menajer
    public static void buyManager(ServerPlayer p) {
        Profile pr = profile(p);
        if (pr.coins < MANAGER_PRICE) {
            p.displayClientMessage(Component.translatable("msg.rabonaarena.not_enough_coins", MANAGER_PRICE).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (pr.managers.size() >= 30) {
            p.displayClientMessage(Component.translatable("msg.rabonaarena.collection_full").withStyle(ChatFormatting.RED), true);
            return;
        }
        pr.coins -= MANAGER_PRICE;
        RandomSource r = p.getRandom();
        Manager m = Manager.random(r, 60, r.nextFloat() < 0.15 ? 93 : 84);
        pr.managers.add(m);
        if (pr.manager < 0) pr.manager = pr.managers.size() - 1;
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.9f);
        p.displayClientMessage(Component.translatable("msg.rabonaarena.manager_new", m.name(), m.tacticEnum().title()).withStyle(ChatFormatting.AQUA), false);
        changed(p);
    }

    public static void setManager(ServerPlayer p, int idx) {
        Profile pr = profile(p);
        if (idx < -1 || idx >= pr.managers.size()) return;
        pr.manager = idx;
        Manager m = pr.activeManager();
        if (m != null) p.displayClientMessage(Component.translatable("msg.rabonaarena.manager_set", m.name(), m.tacticEnum().title(), m.tacticEnum().shape)
                .withStyle(ChatFormatting.AQUA), true);
        changed(p);
    }

    private static void changed(ServerPlayer p) {
        data(p).setDirty();
        sync(p, List.of());
        Match m = Match.get(p.server);
        Team t = Match.teamOf(p);
        if (t.playing()) m.refreshTeam(t);
    }

    public static void sell(ServerPlayer p, int idx) {
        Profile pr = profile(p);
        if (idx < 0 || idx >= pr.cards.size()) return;
        Card c = pr.cards.remove(idx);
        pr.coins += c.sellValue();
        List<Integer> sq = new ArrayList<>();
        for (int i : pr.squad) sq.add(i == idx ? -1 : i > idx ? i - 1 : i);
        pr.squad.clear();
        pr.squad.addAll(sq);
        data(p).setDirty();
        p.displayClientMessage(Component.translatable("msg.rabonaarena.sold", c.name(), c.sellValue()).withStyle(ChatFormatting.YELLOW), true);
        sync(p, List.of());
    }

    public static void sync(ServerPlayer p, List<Card> opened) {
        Profile pr = profile(p);
        Net.toPlayer(p, new S2C.Profile(pr.coins, new ArrayList<>(pr.cards), new ArrayList<>(pr.squad), new ArrayList<>(opened),
                new ArrayList<>(pr.managers), pr.manager));
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

    /** Takimin kaptani (ilk insan oyuncu) - kadro ve menajer onun profilinden gelir. */
    public static Profile captain(Match m, Team t) {
        List<ServerPlayer> humans = m.playersOf(t);
        return humans.isEmpty() ? null : profile(humans.get(0));
    }

    /** Ilk 11 yuvasindaki kartlari, ayni yuvadaki (mevkideki) botlara uygula. */
    public static void applySquad(Match m, Team t) {
        if (!t.playing()) return;
        Profile pr = captain(m, t);
        if (pr == null) return;
        Manager mg = pr.activeManager();
        int bonus = mg == null ? 0 : mg.bonus();
        Pos[] eleven = Pos.formation(11, m.tactic(t));
        List<Pos> slotPos = new ArrayList<>();
        List<Card> slotCard = new ArrayList<>();
        for (int s = 0; s < SQUAD_MAX; s++) {
            int ci = pr.at(s);
            if (ci < 0 || ci >= pr.cards.size()) continue;
            slotPos.add(eleven[s]);
            slotCard.add(pr.cards.get(ci));
        }
        if (slotCard.isEmpty()) return;
        List<FootballerEntity> bots = new ArrayList<>();
        for (FootballerEntity f : m.bots()) if (f.getSquad() == t) bots.add(f);
        bots.sort(Comparator.comparingInt(f -> f.isKeeper() ? 0 : 1));
        for (FootballerEntity f : bots) {
            if (slotCard.isEmpty()) break;
            if (m.subbedIn(f)) continue;
            Pos want = f.getFieldPos();
            int best = -1;
            double bd = 99;
            for (int k = 0; k < slotPos.size(); k++) {
                Pos sp = slotPos.get(k);
                double d = sp == want ? -2 : sp.dist(want);
                if ((want == Pos.GK) != (sp == Pos.GK)) d += 3;
                if (d < bd) { bd = d; best = k; }
            }
            Card c = slotCard.remove(best);
            slotPos.remove(best);
            apply(f, t, c, bonus);
        }
    }

    public static void apply(FootballerEntity f, Team t, Card c, int bonus) {
        f.setup(t, f.getNumber(), c.skin(), c.name(), Math.min(99, c.ovr() + bonus));
        f.setStats(c.pac() + bonus, c.sho() + bonus, c.pas() + bonus, c.dri() + bonus, c.def() + bonus);
    }

    /** Yedek kulubesi: kaptanin yedekleri, yoksa rastgele yedekler. */
    public static List<Card> bench(Match m, Team t) {
        List<Card> out = new ArrayList<>();
        Profile pr = captain(m, t);
        if (pr != null) {
            for (int s = SQUAD_MAX; s < LINEUP; s++) {
                int ci = pr.at(s);
                if (ci >= 0 && ci < pr.cards.size()) out.add(pr.cards.get(ci));
            }
        }
        RandomSource r = m.level().getRandom();
        Pack pk = m.difficulty >= 2 ? Pack.GOLD : m.difficulty == 1 ? Pack.SILVER : Pack.BRONZE;
        while (out.size() < 5) out.add(generate(r, pk));
        return out;
    }

    public static Card fromBot(FootballerEntity f) {
        return new Card(f.getBaseName(), f.getFieldPos().ordinal(), f.getSkill(), f.pace, f.shooting, f.passing, f.dribbling,
                f.defending, (f.pace + f.defending) / 2, f.getSkill() >= 86 ? 3 : f.getSkill() >= 75 ? 2 : f.getSkill() >= 65 ? 1 : 0, f.getSkinId());
    }
}
