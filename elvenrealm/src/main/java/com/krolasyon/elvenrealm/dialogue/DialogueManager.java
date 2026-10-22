package com.krolasyon.elvenrealm.dialogue;

import com.krolasyon.elvenrealm.entity.ElfEntity;
import com.krolasyon.elvenrealm.entity.ElfKind;
import com.krolasyon.elvenrealm.network.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sunucu tarafı diyalog akışı. Her seçenek bir "eylem" adına bağlanır; oyuncunun seçimi bu eylemi çalıştırır
 * ve elfin cevabı yeni seçeneklerle birlikte geri gönderilir.
 */
public final class DialogueManager {
    private record Option(Component label, String action) {}

    private record Session(int entityId, List<String> actions) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private DialogueManager() {}

    public static void open(ServerPlayer p, ElfEntity elf) {
        elf.startTalking(p);
        if (elf.isRecovering()) {
            long h = elf.recoverRemaining() / 1000;
            show(p, elf, line(elf, p, "recovering").append(gray("\n\n(" + h + " saat sonra iyileşecek)")), List.of());
            return;
        }
        boolean friend = PlayerData.friendship(p, elf.kind) >= PlayerData.HIRE_FRIENDSHIP && Lines.has(elf.kind, "greet_friend");
        if (elf.isHired() && p.getUUID().equals(elf.ownerId())) {
            show(p, elf, line(elf, p, "greet_friend").append(gray("\n\n(Sözleşmenin bitmesine " + hours(elf.hireRemaining()) + " saat var)")),
                    root(p, elf));
        } else {
            show(p, elf, line(elf, p, friend ? "greet_friend" : "greet"), root(p, elf));
        }
    }

    public static void choose(ServerPlayer p, int entityId, int index) {
        Session s = SESSIONS.get(p.getUUID());
        Entity e = p.level().getEntity(entityId);
        ElfEntity elf = e instanceof ElfEntity x ? x : null;
        if (s == null || s.entityId != entityId || elf == null || index < 0 || index >= s.actions.size() || elf.distanceToSqr(p) > 100) {
            SESSIONS.remove(p.getUUID());
            if (elf != null) elf.stopTalking();
            return;
        }
        elf.startTalking(p);
        act(p, elf, s.actions.get(index));
    }

    private static List<Option> root(ServerPlayer p, ElfEntity elf) {
        ElfKind k = elf.kind;
        List<Option> o = new ArrayList<>();
        if (k.citizen()) {
            o.add(opt("Biraz kendinden bahset.", "about"));
            o.add(opt(elf.cityName() + " nasıl bir yer?", "city"));
            o.add(opt("Kentte neler oluyor?", "gossip"));
            o.add(opt("Hoşça kal.", "bye"));
            return o;
        }
        if (!k.unique()) {
            o.add(opt(elf.cityName() + " kentinin kuralları nedir?", "about"));
            o.add(opt("Kentte neler oluyor?", "gossip"));
            o.add(opt("Hoşça kal.", "bye"));
            return o;
        }
        o.add(opt("Biraz kendinden bahset.", "about"));
        o.add(opt(elf.cityName() + " nasıl bir yer?", "city"));
        o.add(opt("Kentte neler oluyor?", "gossip"));
        List<Quests.Quest> qs = Quests.of(k);
        int qi = PlayerData.questIndex(p, k);
        if (PlayerData.questActive(p, k) && qi < qs.size()) {
            o.add(new Option(Component.literal("[Görev] ").withStyle(ChatFormatting.GOLD).append(Component.literal(qs.get(qi).title() + " — teslim etmeye geldim.")
                    .withStyle(ChatFormatting.WHITE)), "turnin"));
        } else if (qi < qs.size()) {
            o.add(new Option(Component.literal("[Görev] ").withStyle(ChatFormatting.GOLD).append(Component.literal("Yardıma ihtiyacın var mı?")
                    .withStyle(ChatFormatting.WHITE)), "offer"));
        }
        ItemStack hand = p.getMainHandItem();
        o.add(new Option(Component.literal("Sana bir hediyem var: ").append(hand.isEmpty() ? Component.literal("(elim boş)") : hand.getHoverName()), "gift"));
        if (elf.isHired() && p.getUUID().equals(elf.ownerId())) {
            o.add(new Option(Component.literal("Sözleşmeyi bir gün uzatalım. ").append(gray("(" + k.hirePrice + " zümrüt)")), "extend"));
            o.add(opt("Artık yollarımız ayrılsın.", "dismiss"));
        } else if (!elf.isHired() && k.hireable()) {
            o.add(new Option(Component.literal("[Paralı Asker] ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("Benimle yola çık.").withStyle(ChatFormatting.WHITE)).append(gray(" (" + k.hirePrice + " zümrüt / gün)")), "hire"));
        }
        o.add(opt("Hoşça kal.", "bye"));
        return o;
    }

    private static void act(ServerPlayer p, ElfEntity elf, String action) {
        ElfKind k = elf.kind;
        switch (action) {
            case "about", "city", "gossip" -> show(p, elf, line(elf, p, action), root(p, elf));
            case "bye" -> {
                show(p, elf, line(elf, p, "farewell"), List.of());
            }
            case "offer" -> {
                Quests.Quest q = current(p, k);
                if (q == null) {
                    show(p, elf, line(elf, p, "about"), root(p, elf));
                    return;
                }
                MutableComponent t = Component.literal(q.offer()).append(Component.literal("\n\n» " + q.title()).withStyle(ChatFormatting.GOLD))
                        .append(requirements(p, k, q));
                show(p, elf, t, List.of(opt("Kabul ediyorum.", "accept"), opt("Şimdi olmaz.", "back")));
            }
            case "accept" -> {
                Quests.Quest q = current(p, k);
                if (q == null) return;
                PlayerData.startQuest(p, k);
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.4F, 1.4F);
                show(p, elf, Component.literal("Görev alındı: ").withStyle(ChatFormatting.GOLD).append(Component.literal(q.title()).withStyle(ChatFormatting.YELLOW))
                        .append(requirements(p, k, q)), root(p, elf));
            }
            case "back" -> show(p, elf, line(elf, p, "greet"), root(p, elf));
            case "turnin" -> turnIn(p, elf);
            case "gift" -> gift(p, elf);
            case "hire" -> hire(p, elf);
            case "extend" -> {
                if (!take(p, elf.kind.hirePrice)) {
                    show(p, elf, gray("Yeterli zümrüdün yok. (" + elf.kind.hirePrice + " gerekli)"), root(p, elf));
                    return;
                }
                elf.extend(ElfEntity.DAY);
                PlayerData.root(p).putLong("companion_until", p.level().getGameTime() + elf.hireRemaining());
                show(p, elf, line(elf, p, "hire_extend").append(gray("\n\n(Kalan süre: " + hours(elf.hireRemaining()) + " saat)")), root(p, elf));
            }
            case "dismiss" -> {
                MutableComponent t = line(elf, p, "hire_dismiss");
                elf.unhire(null);
                PlayerData.root(p).remove("companion_until");
                show(p, elf, t, List.of());
            }
            default -> show(p, elf, line(elf, p, "greet"), root(p, elf));
        }
    }

    // ------------------------------------------------------------------ görevler

    private static Quests.Quest current(Player p, ElfKind k) {
        List<Quests.Quest> qs = Quests.of(k);
        int i = PlayerData.questIndex(p, k);
        return i < qs.size() ? qs.get(i) : null;
    }

    private static int count(Player p, Quests.Need n) {
        int c = 0;
        for (ItemStack s : p.getInventory().items) if (n.matches(s)) c += s.getCount();
        return c;
    }

    private static boolean ready(Player p, ElfKind k, Quests.Quest q) {
        if (q.hunt()) return PlayerData.kills(p, k) >= q.kills();
        for (Quests.Need n : q.items()) if (count(p, n) < n.count()) return false;
        return true;
    }

    private static MutableComponent requirements(Player p, ElfKind k, Quests.Quest q) {
        MutableComponent t = Component.literal("");
        boolean active = PlayerData.questActive(p, k);
        if (q.hunt()) {
            int have = active ? Math.min(PlayerData.kills(p, k), q.kills()) : 0;
            t.append(Component.literal("\n• " + q.kills() + " " + q.targetName() + " öldür (" + have + "/" + q.kills() + ")")
                    .withStyle(have >= q.kills() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            t.append(gray("\n  (Yol arkadaşının öldürdükleri de sayılır.)"));
        } else {
            for (Quests.Need n : q.items()) {
                int have = Math.min(count(p, n), n.count());
                MutableComponent name = n.saplings() ? Component.literal("Fidan (herhangi)") : n.item().getDescription().copy();
                t.append(Component.literal("\n• " + n.count() + " ").append(name).append(" (" + have + "/" + n.count() + ")")
                        .withStyle(have >= n.count() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        return t;
    }

    private static void turnIn(ServerPlayer p, ElfEntity elf) {
        ElfKind k = elf.kind;
        Quests.Quest q = current(p, k);
        if (q == null || !PlayerData.questActive(p, k)) {
            show(p, elf, line(elf, p, "greet"), root(p, elf));
            return;
        }
        if (!ready(p, k, q)) {
            show(p, elf, Component.literal(q.progress()).append(requirements(p, k, q)), root(p, elf));
            return;
        }
        for (Quests.Need n : q.items()) {
            int left = n.count();
            for (ItemStack s : p.getInventory().items) {
                if (left <= 0) break;
                if (!n.matches(s)) continue;
                int t = Math.min(left, s.getCount());
                s.shrink(t);
                left -= t;
            }
        }
        PlayerData.finishQuest(p, k);
        PlayerData.addFriendship(p, k, 3);
        MutableComponent rw = Component.literal("\n\nÖdül:").withStyle(ChatFormatting.GOLD);
        for (ItemStack s : q.rewards().get()) {
            rw.append(Component.literal("\n• " + s.getCount() + "× ").withStyle(ChatFormatting.YELLOW).append(s.getHoverName()));
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
        p.giveExperiencePoints(30);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        show(p, elf, Component.literal(q.done()).append(rw).append(Component.literal("\n+3 dostluk").withStyle(ChatFormatting.GREEN)), root(p, elf));
    }

    /** Av görevleri için öldürme sayımı (oyuncunun ya da kiralık elfinin öldürdükleri). */
    public static void onKill(ServerPlayer p, EntityType<?> type) {
        for (ElfKind k : ElfKind.values()) {
            if (!PlayerData.questActive(p, k)) continue;
            Quests.Quest q = current(p, k);
            if (q == null || !q.hunt() || !q.targets().contains(type)) continue;
            if (PlayerData.kills(p, k) >= q.kills()) continue;
            PlayerData.addKill(p, k);
            int n = PlayerData.kills(p, k);
            p.displayClientMessage(Component.literal(q.title() + ": " + n + "/" + q.kills()).withStyle(n >= q.kills() ? ChatFormatting.GREEN : ChatFormatting.GOLD), true);
            if (n >= q.kills()) p.sendSystemMessage(Component.literal("Görev tamamlandı: " + q.title() + " — " + k.name + "'a dön.").withStyle(ChatFormatting.GREEN));
        }
    }

    public static List<Component> journal(Player p) {
        List<Component> out = new ArrayList<>();
        for (ElfKind k : ElfKind.values()) {
            if (!k.unique()) continue;
            Quests.Quest q = current(p, k);
            MutableComponent head = Component.literal(k.name + " ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(hearts(PlayerData.friendship(p, k))).withStyle(ChatFormatting.RED));
            if (q != null && PlayerData.questActive(p, k)) head.append(Component.literal("  » " + q.title()).withStyle(ChatFormatting.GOLD)).append(requirements(p, k, q));
            else if (q == null) head.append(gray("  (tüm görevler tamam)"));
            out.add(head);
        }
        return out;
    }

    public static String hearts(int f) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < PlayerData.MAX_FRIENDSHIP; i++) b.append(i < f ? '❤' : '♡');
        return b.toString();
    }

    // ------------------------------------------------------------------ hediye ve kiralama

    private static void gift(ServerPlayer p, ElfEntity elf) {
        ElfKind k = elf.kind;
        ItemStack hand = p.getMainHandItem();
        long day = p.level().getDayTime() / 24000L;
        if (hand.isEmpty()) {
            show(p, elf, gray("Elinde verecek bir şey yok. Hediyeyi ana elinde tut."), root(p, elf));
        } else if (PlayerData.lastGiftDay(p, k) == day) {
            show(p, elf, line(elf, p, "gift_already"), root(p, elf));
        } else if (hand.is(k.favorite)) {
            hand.shrink(1);
            PlayerData.setGiftDay(p, k, day);
            PlayerData.addFriendship(p, k, 1);
            p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART, elf.getX(), elf.getY() + 2.1, elf.getZ(), 4, 0.3, 0.2, 0.3, 0);
            show(p, elf, line(elf, p, "gift_thanks").append(Component.literal("\n+1 dostluk").withStyle(ChatFormatting.GREEN)), root(p, elf));
        } else {
            show(p, elf, line(elf, p, "gift_wrong"), root(p, elf));
        }
    }

    private static void hire(ServerPlayer p, ElfEntity elf) {
        ElfKind k = elf.kind;
        int f = PlayerData.friendship(p, k);
        if (f < PlayerData.HIRE_FRIENDSHIP || PlayerData.questIndex(p, k) < 1) {
            show(p, elf, line(elf, p, "hire_refuse").append(gray("\n\n(Gerekli: dostluk " + PlayerData.HIRE_FRIENDSHIP + "/" + PlayerData.MAX_FRIENDSHIP
                    + " — şu an " + f + " — ve onun için tamamlanmış en az bir görev.)")), root(p, elf));
            return;
        }
        long until = PlayerData.root(p).getLong("companion_until");
        if (PlayerData.companion(p) != null && p.level().getGameTime() < until) {
            show(p, elf, gray("Zaten bir yol arkadaşın var. Sylvaren elfleri bir başkasıyla paylaşılan bir sözleşmeye girmez."), root(p, elf));
            return;
        }
        if (!take(p, k.hirePrice)) {
            show(p, elf, line(elf, p, "hire_refuse").append(gray("\n\n(Bir günlük ücret: " + k.hirePrice + " zümrüt. Envanterinde yeterli zümrüt yok.)")),
                    root(p, elf));
            return;
        }
        elf.hire(p, ElfEntity.DAY);
        PlayerData.root(p).putLong("companion_until", p.level().getGameTime() + ElfEntity.DAY);
        p.level().playSound(null, elf.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 0.5F, 1.6F);
        show(p, elf, line(elf, p, "hire_accept").append(gray("\n\n(Sözleşme: 1 gün. Ağır yaralanırsa Sylvaren'e döner ve dostluğunuz zedelenir.)")),
                List.of());
    }

    private static boolean take(Player p, int emeralds) {
        int have = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(Items.EMERALD)) have += s.getCount();
        if (have < emeralds) return false;
        int left = emeralds;
        for (ItemStack s : p.getInventory().items) {
            if (left <= 0) break;
            if (!s.is(Items.EMERALD)) continue;
            int t = Math.min(left, s.getCount());
            s.shrink(t);
            left -= t;
        }
        return true;
    }

    // ------------------------------------------------------------------ yardımcılar

    private static MutableComponent line(ElfEntity elf, Player p, String key) {
        return Component.literal(elf.fill(Lines.get(elf.kind, elf.lineKey(key), elf.getRandom()), p));
    }

    private static MutableComponent gray(String s) {
        return Component.literal(s).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    private static Option opt(String s, String action) {
        return new Option(Component.literal(s), action);
    }

    private static long hours(long ticks) {
        return Math.max(1, ticks / 1000);
    }

    private static void show(ServerPlayer p, ElfEntity elf, Component text, List<Option> options) {
        List<String> labels = new ArrayList<>(), actions = new ArrayList<>();
        for (Option o : options) {
            labels.add(Component.Serializer.toJson(o.label()));
            actions.add(o.action());
        }
        SESSIONS.put(p.getUUID(), new Session(elf.getId(), actions));
        int qs = PlayerData.questActive(p, elf.kind) ? 1 : 0;
        Net.send(p, new Net.OpenDialogue(elf.getId(), elf.elfName(), elf.kind.title, Component.Serializer.toJson(text), labels,
                elf.kind.unique() ? PlayerData.friendship(p, elf.kind) : -1, qs, elf.kind.voice));
    }
}
