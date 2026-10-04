package com.krolasyon.bosses.rpg.npc;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.Quest;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.net.RpgNet;
import com.krolasyon.bosses.rpg.story.Story;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Server side conversation logic. Builds a page (text + options) and reacts to the player's choice. */
public final class NpcDialog {
    private NpcDialog() {}

    public record Opt(String id, String label) {}

    public static long day(ServerPlayer p) { return p.level().getDayTime() / 24000L; }

    private static String pick(RandomSource r, String... lines) { return lines[r.nextInt(lines.length)]; }

    public static void send(ServerPlayer p, RpgNpc npc, String text, List<Opt> opts) {
        Relation rel = npc.rel(p.getUUID());
        CompoundTag t = new CompoundTag();
        t.putInt("Npc", npc.getId());
        t.putString("Name", npc.npcName());
        t.putString("Title", npc.race().title + " " + (npc.isChildNpc() ? "Çocuk" : npc.role().title) + " • " + Kingdom.of(npc.kingdomId()).title);
        t.putString("Rel", rel.affinityTitle() + " (" + rel.affinity + ")  •  Güven: " + rel.trustTitle() + " (" + rel.trust + ")");
        t.putString("Text", text);
        t.putInt("Race", npc.race().ordinal());
        t.putBoolean("Female", npc.female());
        ListTag l = new ListTag();
        for (Opt o : opts) {
            CompoundTag x = new CompoundTag();
            x.putString("Id", o.id);
            x.putString("Label", o.label);
            l.add(x);
        }
        t.put("Options", l);
        RpgNet.send(p, RpgNet.DIALOG, t);
    }

    public static void close(ServerPlayer p) { RpgNet.send(p, RpgNet.CLOSE, new CompoundTag()); }

    // ------------------------------------------------------------------ greeting
    private static String greeting(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d) {
        RandomSource r = p.getRandom();
        String you = d.social >= 4 ? (d.social >= 8 ? "Majesteleri" : d.social >= 5 ? "Lordum" : "Sör") : "";
        if (npc.flag(RpgNpc.F_SLAVE)) {
            if (npc.leader() != null && npc.leader().equals(p.getUUID())) return pick(r, "E-efendim? Ne emredersiniz?", "Sizin için ne yapabilirim, efendim?", "Zincirlerim... ağır, efendim. Ama emrinizdeyim.");
            return pick(r, "Lütfen... beni buradan kurtarın.", "Ben satılık bir maldan ibaretim artık...", "Bir zamanlar özgürdüm. Sen de benim gibi olma.");
        }
        if (npc.isChildNpc()) {
            if (npc.flag(RpgNpc.F_FAMILY)) return pick(r, "Anne! Baba! Bana bir hikaye anlatır mısın?", "Büyüyünce senin gibi bir kahraman olacağım!", "Bugün bir kurbağa yakaladım!");
            return pick(r, "Sen bir maceracı mısın? Kılıcın gerçek mi?", "Annem yabancılarla konuşmamamı söyledi...", "Hey! Saklambaç oynar mısın?");
        }
        if (rel.married) return pick(r, "Hoş geldin sevgilim. Seni özledim.", "Bugün nasıldı? Yaralanmadın değil mi?", "Seninle evlendiğim gün hayatımın en güzel günüydü.");
        if (rel.enemy) return pick(r, "Defol git gözümün önünden!", "Senin yüzünü görmek bile midemi bulandırıyor.", "Yaptıklarını unutmadım. Asla da unutmayacağım.");
        if (rel.rescued && rel.affinity > 40) return pick(r, "Hayat kurtarıcım! Sana olan borcumu asla ödeyemem.", "Seni görmek ne güzel! O gün olmasaydın şimdi burada olmazdım.");
        StringBuilder b = new StringBuilder();
        switch (npc.role()) {
            case BEGGAR -> b.append(pick(r, "Bir parça ekmek... ne olur...", "Merhametli yolcu, birkaç bakır?", "Eskiden ben de bir tüccardım, inanır mısın?"));
            case GUARD, SOLDIER -> b.append(d.bounty[npc.kingdomId()] > 0 ? "Seni tanıyorum! Kanun önünde hesabın var." : pick(r, "Kanun burada geçerlidir, vatandaş.", "Sorun çıkarma, yeter.", "Devriyedeyim. Ne istiyorsun?"));
            case NOBLE -> b.append(d.social <= 1 && rel.affinity < 30 ? "Bir köylü... Benimle konuşmaya nasıl cüret edersin? Çabuk söyle." : pick(r, "Ah, " + (you.isEmpty() ? "saygın misafir" : you) + ". Saraydaki dedikodulardan sıkıldım, sen ne anlatacaksın?", "İyi günler. Soyluların işleri bitmez, kısa kes."));
            case RULER -> b.append(Kingdom.of(npc.kingdomId()).rulerTitle).append(" ").append(npc.npcName()).append(" seni dinliyor. ")
                    .append(d.allegiance == npc.kingdomId() ? "Sadık hizmetkârım, hoş geldin." : "Huzuruma neden çıktın?");
            case MERCHANT -> b.append(pick(r, "En kaliteli mallar, en uygun fiyatlar!", "Altın konuşur dostum, ben de dinlerim.", "Bir göz at, belki aradığın bendedir."));
            case BLACKSMITH -> b.append(pick(r, "Örsün sesi müziktir. Silah mı arıyorsun?", "İyi bir kılıç, iyi bir dosttan daha sadıktır.", "Demir sıcakken dövülür, çabuk söyle."));
            case MAGE -> b.append(pick(r, "Büyünün kokusunu üzerinde alıyorum... ya da almıyorum.", "Bilgi güçtür. Ve bilgi pahalıdır.", "Kitaplarıma dokunma. Ne istiyorsun?"));
            case PRIEST -> b.append(pick(r, "Işık seninle olsun, evladım.", "Tanrılar her şeyi görür. Kalbinde ne var?", "Yaralı mısın? Tapınağın kapısı herkese açık."));
            case INNKEEPER -> b.append(pick(r, "Hoş geldin yolcu! Sıcak yemek, soğuk bira ve taze dedikodu!", "Odalarımız temiz, biramız köpüklü!"));
            case GUILD_MASTER -> b.append(d.guild < 0 ? "Maceracılar Loncası'na hoş geldin. Kayıt mı yaptıracaksın?" : "Hey, " + d.guildRank() + " sınıfı! Panoda yeni işler var.");
            case SLAVER -> b.append(pick(r, "Sağlam kollar, itaatkâr ruhlar. Fiyatlarım makul.", "Bak bak... Belki sen de iyi bir mal olurdun. Şaka! Alışveriş mi?"));
            case ADVENTURER -> b.append(pick(r, "Bir sonraki maceraya hazır mısın? Ben hep hazırım!", "Ejderha avına çıkmak isteyen var mı? Şaka şaka... ya da değil.", "Kılıcım paslanıyor, iş var mı?"));
            case FAMILY -> b.append(pick(r, "Evladım...", "Gel, otur yanıma."));
            default -> b.append(raceLine(r, npc.race(), rel));
        }
        if (rel.affinity >= 60 && npc.role() != NpcRole.RULER) b.append(" ").append(pick(r, "Seni görmek her zaman güzel.", "Dostum!", "Bugün yüzün gülüyor."));
        return b.toString();
    }

    private static String raceLine(RandomSource r, Race race, Relation rel) {
        if (rel.affinity >= 35) return pick(r, "Ah, sen misin! Seni görmek ne güzel.", "Hoş geldin dostum.", "Gel, biraz soluklan.");
        return switch (race) {
            case ELF -> pick(r, "Bir yabancı... Ağaçlar senin geldiğini fısıldadı.", "İnsanların ömrü kısa, sözleri daha da kısa. Ne istiyorsun?");
            case DWARF -> pick(r, "Hah! Sakalımın ucundan bile uzun değilsin. Ne var?", "Cüce birası içmeden konuşmam. Şaka. Söyle.");
            case DEMON -> pick(r, "Kokundan zayıf olduğunu anlıyorum. Neden buradasın?", "Ruhun ilginç... Sat bana! Hahaha.");
            case GIANT -> pick(r, "Küçük şey konuşuyor. Ne istersin küçük şey?", "HMM. Yavaş konuş. Devler hızlı düşünmez.");
            case ORC -> pick(r, "Güçlü müsün, zayıf mı? Konuş!", "Ork kanı sıcaktır, sözünü dikkatli seç.");
            case BEASTKIN -> pick(r, "Kokunu tanımıyorum. Dost musun, av mı?", "Kulaklarım seni uzaktan duydu. Merhaba.");
            case HALFLING -> pick(r, "Merhaba merhaba! İkinci kahvaltıya kalır mısın?", "Ayakların çok büyük! Yani... merhaba!");
            case DARK_ELF -> pick(r, "Gölgelerde dolaşan bir yüzey sakini... ilginç.", "Ne istediğini söyle ve git. Burada gözler çoktur.");
            default -> pick(r, "Merhaba yabancı.", "Seni buralarda daha önce görmedim.", "Güzel bir gün, değil mi?", "Ne istiyorsun?");
        };
    }

    // ------------------------------------------------------------------ pages
    public static void open(ServerPlayer p, RpgNpc npc, String node) {
        page(p, npc, null);
    }

    private static boolean ownedBy(RpgNpc npc, ServerPlayer p) { return npc.leader() != null && npc.leader().equals(p.getUUID()); }

    public static void page(ServerPlayer p, RpgNpc npc, @Nullable String response) {
        PlayerRpg d = RpgWorldData.player(p);
        Relation rel = npc.rel(p.getUUID());
        if (!rel.met) { rel.met = true; d.fame++; }
        List<Opt> o = new ArrayList<>();
        String text = response != null ? response : greeting(p, npc, rel, d);
        String storyText = Story.dialogText(p, d, npc);
        if (storyText != null && response == null) text = storyText;
        Story.dialogOptions(p, d, npc, o);
        boolean owned = ownedBy(npc, p);
        NpcRole role = npc.role();
        if (rel.enemy && !owned) {
            o.add(new Opt("apologize", "Özür dile (" + NpcQuests.coins(50) + " öde)"));
            o.add(new Opt("threaten", "Tehdit et"));
            o.add(new Opt("bye", "Ayrıl"));
            send(p, npc, text, o);
            return;
        }
        if (!npc.flag(RpgNpc.F_SLAVE) || owned) o.add(new Opt("talk", "Sohbet et"));
        if (role == NpcRole.INNKEEPER || role == NpcRole.PEASANT || role == NpcRole.MERCHANT || role == NpcRole.BEGGAR || role == NpcRole.ADVENTURER || role == NpcRole.GUARD)
            o.add(new Opt("rumor", "Dedikodu ve haber sor"));
        if (!p.getMainHandItem().isEmpty() && !npc.flag(RpgNpc.F_SLAVE)) o.add(new Opt("gift", "Elimdekini hediye et: " + p.getMainHandItem().getHoverName().getString()));
        if (role.trades() && !npc.isChildNpc()) o.add(new Opt("trade", "Ticaret yap"));
        // quests
        Quest mine = null;
        for (Quest q : d.quests) if (npc.getUUID().equals(q.giver)) mine = q;
        Quest deliver = null;
        Site here = WorldMap.siteAt(RpgWorldData.get(p.server), npc.getX(), npc.getZ());
        for (Quest q : d.quests) if (q.type == Quest.Type.DELIVER && here != null && here.id.equals(q.target)) deliver = q;
        if (deliver != null) o.add(new Opt("deliver", "Mektubu teslim et (" + deliver.giverName + " yolladı)"));
        if (mine != null) o.add(new Opt("turnin", (mine.done() || mine.type == Quest.Type.FETCH ? "Görevi teslim et: " : "Görev durumu: ") + mine.title));
        else if (!npc.isChildNpc() && !npc.flag(RpgNpc.F_SLAVE) && role != NpcRole.SLAVER && role != NpcRole.FAMILY && !owned && role != NpcRole.GUILD_MASTER && d.quests.size() < NpcQuests.MAX_ACTIVE)
            o.add(new Opt("quest", "Senin için yapabileceğim bir iş var mı?"));
        // guild
        if (role == NpcRole.GUILD_MASTER) {
            if (d.guild < 0) o.add(new Opt("guild_join", "Loncaya katıl (" + NpcQuests.coins(20) + ")"));
            else if (d.quests.size() < NpcQuests.MAX_ACTIVE) o.add(new Opt("guild_quest", "Lonca panosundan iş al"));
            if (d.guild >= 0) o.add(new Opt("guild_info", "Lonca rütbem nedir?"));
        }
        // priest
        if (role == NpcRole.PRIEST) o.add(new Opt("heal", "Yaralarımı iyileştir (" + NpcQuests.coins(rel.affinity > 40 ? 0 : 15) + ")"));
        // law
        if (npc.isLawEnforcer() && d.bounty[npc.kingdomId()] > 0) o.add(new Opt("bounty", "Cezamı öde (" + NpcQuests.coins(d.bounty[npc.kingdomId()] * 2) + ")"));
        // ruler & nobles
        if (role == NpcRole.RULER || role == NpcRole.NOBLE && d.social < 4) o.add(new Opt("rank", "Unvan / rütbe talep et"));
        if (role == NpcRole.RULER) {
            if (d.allegiance != npc.kingdomId()) o.add(new Opt("swear", "Bu krallığa bağlılık yemini et"));
            o.add(new Opt("war", "Krallığın savaş durumu nedir?"));
        }
        // slaver
        if (role == NpcRole.SLAVER) o.add(new Opt("slaves", "Kölelerini göster"));
        // party / companions
        if (owned) {
            o.add(new Opt(npc.flag(RpgNpc.F_FOLLOW) ? "stay" : "follow", npc.flag(RpgNpc.F_FOLLOW) ? "Burada bekle" : "Beni takip et"));
            if (npc.flag(RpgNpc.F_PARTY)) o.add(new Opt("dismiss", "Ekipten ayrıl"));
            if (p.getMainHandItem().getItem() instanceof TieredItem && npc.flag(RpgNpc.F_PARTY)) o.add(new Opt("equip", "Bu silahı al ve kullan"));
            if (npc.flag(RpgNpc.F_SLAVE)) o.add(new Opt("free", "Seni özgür bırakıyorum"));
            if (rel.married && d.home != null) o.add(new Opt("home", "Eve git ve beni bekle"));
            if (rel.married && !npc.isChildNpc()) o.add(new Opt("child", "Bir çocuğumuz olsun mu?"));
        } else if (!npc.flag(RpgNpc.F_SLAVE) && !npc.isChildNpc() && role != NpcRole.RULER && role != NpcRole.FAMILY) {
            if (role == NpcRole.ADVENTURER || role.fighter && rel.affinity >= 40 || rel.affinity >= 55) o.add(new Opt("recruit", "Ekibime katıl"));
            if (role == NpcRole.ADVENTURER) o.add(new Opt("hire", "Paralı asker olarak tut (" + NpcQuests.coins(hirePrice(npc)) + ")"));
        }
        if (!npc.isChildNpc() && !rel.married && d.spouse == null && countRing(p) && !npc.flag(RpgNpc.F_SLAVE) && npc.role() != NpcRole.FAMILY)
            o.add(new Opt("propose", "♥ Evlenme teklif et"));
        if (!owned && !npc.flag(RpgNpc.F_SLAVE)) {
            o.add(new Opt("insult", "Hakaret et"));
            o.add(new Opt("threaten", npc.isLawEnforcer() ? "Tehdit et" : "Tehdit et ve parasını iste"));
        }
        o.add(new Opt("bye", "Hoşça kal"));
        send(p, npc, text, o);
    }

    private static boolean countRing(ServerPlayer p) { return NpcQuests.countItem(p, RpgItems.RING.get()) > 0; }

    public static int hirePrice(RpgNpc npc) { return 150 + (int) npc.getMaxHealth() * 4; }

    // ------------------------------------------------------------------ choices
    public static void choose(ServerPlayer p, int entityId, String id) {
        Entity e = p.level().getEntity(entityId);
        if (!(e instanceof RpgNpc npc) || !npc.isAlive() || npc.distanceToSqr(p) > 12 * 12) { close(p); return; }
        npc.talkTicks = 200;
        npc.talkingTo = p.getUUID();
        PlayerRpg d = RpgWorldData.player(p);
        Relation rel = npc.rel(p.getUUID());
        RandomSource r = p.getRandom();
        RpgWorldData w = RpgWorldData.get(p.server);
        if (id.startsWith("story")) {
            String resp = Story.onChoice(p, d, npc, id);
            if (resp != null && !resp.isEmpty() && npc.isAlive()) page(p, npc, resp);
            else close(p);
            PlayerMagic.sync(p);
            return;
        }
        if (id.startsWith("buy:")) { buySlave(p, npc, id.substring(4)); return; }
        String resp;
        switch (id) {
            case "bye" -> { close(p); return; }
            case "talk" -> {
                long day = day(p);
                if (rel.lastChatDay != day) {
                    rel.lastChatDay = day;
                    rel.chats++;
                    if (rel.affinity < 40) rel.add(3, 1);
                    else rel.add(1, 1);
                }
                resp = smallTalk(p, npc, rel, d, w);
            }
            case "rumor" -> resp = rumor(p, npc, w);
            case "gift" -> resp = gift(p, npc, rel);
            case "trade" -> { close(p); npc.openTrade(p); return; }
            case "quest", "guild_quest" -> {
                Quest q = NpcQuests.make(p, npc, id.equals("guild_quest"));
                d.quests.add(q);
                rel.questGiven = true;
                resp = pick(r, "Aslında evet, bir konuda yardımın lazım.", "Güvenebileceğim birine ihtiyacım vardı.", "İşte sana bir iş:") + "\n\n" + NpcQuests.brief(q);
                PlayerMagic.sync(p);
            }
            case "turnin" -> resp = turnIn(p, npc, rel, d);
            case "deliver" -> {
                Quest q = null;
                for (Quest x : d.quests) if (x.type == Quest.Type.DELIVER) q = x;
                if (q != null && NpcQuests.countItem(p, RpgItems.LETTER.get()) > 0) {
                    NpcQuests.takeItem(p, RpgItems.LETTER.get(), 1);
                    q.progress = q.count;
                    NpcQuests.complete(p, q);
                    rel.add(5, 5);
                    resp = "Bir mektup mu? Bu kadar yolu benim için mi geldin? Teşekkürler, ulak!";
                } else resp = "Mektup? Hangi mektup? Elinde bir şey göremiyorum.";
            }
            case "guild_join" -> {
                if (NpcQuests.pay(p, 20)) {
                    d.guild = 0;
                    d.guildXp = 0;
                    resp = "Kaydın tamam! Artık F sınıfı bir maceracısın. Görevleri tamamla, canavar avla, rütben yükselsin. S sınıfına çıkan efsane olur!";
                    Story.onEvent(p, d, "guild_join");
                } else resp = "Kayıt ücreti 20 bakır. Cebin boş görünüyor evlat.";
            }
            case "guild_info" -> {
                int next = d.guild + 1 < PlayerRpg.GUILD.length ? PlayerRpg.GUILD_XP[d.guild + 1] : -1;
                resp = "Sınıfın: " + d.guildRank() + ". Lonca puanın: " + d.guildXp + (next > 0 ? ". Bir sonraki sınıf için " + next + " puan gerekiyor." : ". Daha yükseğe çıkamazsın, sen bir efsanesin!")
                        + "\nTamamlanan görev: " + d.questsDone + " • Yenilen boss: " + d.bossKills;
            }
            case "heal" -> {
                int cost = rel.affinity > 40 ? 0 : 15;
                if (NpcQuests.pay(p, cost)) {
                    p.heal(p.getMaxHealth());
                    p.removeAllEffects();
                    FX.spiral(p.level(), ParticleTypes.END_ROD, p.position(), 2.4, 0.8, 3, 40);
                    p.playNotifySound(SoundEvents.BEACON_POWER_SELECT, p.getSoundSource(), 0.8F, 1.6F);
                    resp = "Işık yaralarını kapatsın. Git ve iyilik yap.";
                } else resp = "Tapınağın da masrafları var evladım. 15 bakır yeterli.";
            }
            case "bounty" -> {
                int k = npc.kingdomId();
                int cost = d.bounty[k] * 2;
                if (NpcQuests.pay(p, cost)) {
                    d.bounty[k] = 0;
                    d.addRep(k, 20);
                    resp = "Borcunu ödedin. Bu sefer affedildin, ama gözüm üstünde.";
                } else resp = "Paran yetmiyor. Ya öde ya da zindanda çürü!";
            }
            case "rank" -> resp = RankLogic.request(p, d, npc);
            case "swear" -> {
                int k = npc.kingdomId();
                if (d.repWith(k) < 100) resp = "Sana henüz güvenmiyorum. Önce bu krallığa hizmet et (saygınlık en az 100 olmalı).";
                else {
                    int old = d.allegiance;
                    d.allegiance = k;
                    d.addRep(k, 50);
                    if (old >= 0 && old != k) { d.addRep(old, -200); }
                    resp = "Diz çök. ... Kalk, " + Kingdom.of(k).title + " sancağı altında artık sen de varsın. Düşmanlarımız senin de düşmanındır.";
                    p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, p.getSoundSource(), 0.8F, 1.0F);
                }
            }
            case "war" -> resp = WarLogic.status(w, npc.kingdomId());
            case "slaves" -> { slaveList(p, npc); return; }
            case "follow" -> { npc.setFlag(RpgNpc.F_FOLLOW, true); npc.setFlag(RpgNpc.F_STAY, false); resp = pick(r, "Peşindeyim!", "Arkandayım.", "Nereye gidersen oraya."); }
            case "stay" -> { npc.setFlag(RpgNpc.F_FOLLOW, false); npc.setFlag(RpgNpc.F_STAY, true); npc.home = npc.blockPosition(); resp = "Burada bekliyorum."; }
            case "home" -> {
                npc.setFlag(RpgNpc.F_FOLLOW, false);
                npc.setFlag(RpgNpc.F_STAY, false);
                if (d.home != null) {
                    npc.home = d.home;
                    npc.teleportTo(d.home.getX() + 0.5, d.home.getY(), d.home.getZ() + 0.5);
                }
                resp = "Evde görüşürüz. Kendine dikkat et!";
            }
            case "dismiss" -> {
                PartyLogic.leave(p, npc);
                resp = rel.affinity > 50 ? "Yollarımız ayrılıyor demek... Ama ihtiyacın olursa beni bul." : "Peki. Kendi yoluma giderim.";
            }
            case "recruit" -> resp = PartyLogic.recruit(p, npc, rel, false);
            case "hire" -> resp = PartyLogic.recruit(p, npc, rel, true);
            case "equip" -> {
                ItemStack st = p.getMainHandItem().copy();
                ItemStack old = npc.getMainHandItem();
                npc.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, st);
                npc.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 1.0F);
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, old.isEmpty() || !(old.getItem() instanceof SwordItem) ? ItemStack.EMPTY : old);
                resp = "Güzel bir silah! Onu onurla taşıyacağım.";
            }
            case "free" -> resp = PartyLogic.free(p, npc, rel, d);
            case "propose" -> resp = FamilyLogic.propose(p, npc, rel, d);
            case "child" -> resp = FamilyLogic.child(p, npc, rel, d);
            case "insult" -> {
                rel.add(-15, -5);
                resp = npc.traits[3] > 60 ? "Benimle böyle konuşamazsın! Bunu unutmayacağım." : pick(r, "...Neden böyle söylüyorsun?", "Kaba birisin.", "Hah! Annen sana terbiye vermemiş.");
                if (npc.isFighter() && npc.traits[1] > 70 && rel.affinity < -40) { npc.setTarget(p); close(p); return; }
            }
            case "threaten" -> resp = threaten(p, npc, rel, d);
            case "apologize" -> {
                if (NpcQuests.pay(p, 50)) {
                    rel.add(30, 5);
                    rel.enemy = rel.affinity <= -60;
                    resp = rel.enemy ? "Paranı alırım ama seni affetmem." : "Hmm... Peki. Bu seferlik kabul ediyorum.";
                } else resp = "Sözler ucuz. Cebin daha da ucuz.";
            }
            default -> resp = "...";
        }
        if (resp.isEmpty()) return;
        page(p, npc, resp);
        PlayerMagic.sync(p);
    }

    private static String smallTalk(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d, RpgWorldData w) {
        RandomSource r = p.getRandom();
        Kingdom k = Kingdom.of(npc.kingdomId());
        List<String> lines = new ArrayList<>();
        lines.add(k.lore);
        lines.add("Burası " + k.title + ". " + k.rulerTitle + " " + k.ruler + " hüküm sürüyor. " + (npc.traits[0] > 60 ? "İyi bir hükümdar, Tanrı korusun." : "Vergiler her yıl artıyor..."));
        if (npc.race() == Race.ELF) lines.add("Ormanın şarkısını duyabiliyor musun? Biz elfler ağaçlarla konuşuruz.");
        if (npc.race() == Race.DWARF) lines.add("Karak Dûm'un ocakları hiç sönmez. Bir gün görmelisin.");
        if (npc.race() == Race.GIANT) lines.add("Buzhisar'da devler kar fırtınasında bile şarkı söyler. HMM. Güzel şarkılar.");
        if (npc.race() == Race.ORC) lines.add("Gorthak Han'ın davulları her dolunayda çalar. Savaş kanımızdadır.");
        if (npc.race() == Race.DEMON) lines.add("Malakar'ın tahtı küllerden yapılmıştır. Her kül, bir ruhtur.");
        if (npc.race() == Race.BEASTKIN) lines.add("Sürü Annesi Rakşa doğayla dengede yaşamamızı öğretir.");
        if (npc.race() == Race.HALFLING) lines.add("Altınliman'da her şeyin bir fiyatı var. Dostluğun bile! Ama benimki bedava.");
        if (npc.race() == Race.DARK_ELF) lines.add("Gecegölge'de güvendiğin tek şey kendi gölgen olmalı.");
        if (npc.role() == NpcRole.BEGGAR) lines.add("Sokaklar acımasız. Kral sarayında altın yiyor, biz çöpten ekmek arıyoruz.");
        if (npc.role() == NpcRole.GUARD) lines.add("Geceleri sur dışına çıkma. Canavarlar karanlıkta daha güçlü olur.");
        if (d.spouse != null) lines.add("Evlendiğini duydum! " + d.spouseName + " şanslı biri.");
        if (d.bossKills > 0) lines.add("Bir canavar avcısı olduğunu duydum! Hikayeler senden bahsediyor.");
        if (d.social >= 4) lines.add("Bir şövalyeyle konuştuğuma inanamıyorum! Çocuklarıma anlatacağım.");
        if (!w.news.isEmpty()) lines.add("Duydun mu? " + w.news.get(r.nextInt(Math.min(5, w.news.size()))));
        String line = lines.get(r.nextInt(lines.size()));
        return line + "\n\n§8(" + rel.affinityTitle() + " • " + rel.trustTitle() + ")";
    }

    private static String rumor(ServerPlayer p, RpgNpc npc, RpgWorldData w) {
        RandomSource r = p.getRandom();
        List<String> l = new ArrayList<>();
        for (Site s : w.sites) {
            if (s.type == Site.Type.LAIR && s.bossAlive && s.distSq(npc.getX(), npc.getZ()) < 2200 * 2200) {
                MonsterDef b = RpgDefs.BY_ID.get(s.boss);
                if (b != null) l.add("Yaşlılar " + b.name() + " adında bir canavarın " + direction(npc.getX(), npc.getZ(), s.x, s.z) + " yönünde, yaklaşık " +
                        (int) Math.sqrt(s.distSq(npc.getX(), npc.getZ())) + " adım ötede yaşadığını söylüyor. Ödülü büyük olur ama geri dönen olmamış.");
            }
            if ((s.type == Site.Type.CAMP) && s.distSq(npc.getX(), npc.getZ()) < 1200 * 1200)
                l.add(s.name + " " + direction(npc.getX(), npc.getZ(), s.x, s.z) + " tarafında. Haydutlar kervanları soyuyor, muhafızlar ise korkudan ses çıkarmıyor.");
            if (s.type == Site.Type.RUIN && s.distSq(npc.getX(), npc.getZ()) < 1500 * 1500)
                l.add(s.name + " hakkında hikayeler var. Hazinesi hâlâ orada olabilir...");
        }
        for (String n : w.news) l.add(n);
        if (l.isEmpty()) return "Bugünlerde pek bir şey olmuyor. Sakin günler... şimdilik.";
        return "Kulağını yaklaştır...\n\n" + l.get(r.nextInt(l.size()));
    }

    public static String direction(double fx, double fz, double tx, double tz) {
        double a = Math.toDegrees(Math.atan2(tz - fz, tx - fx));
        String[] dirs = {"doğu", "güneydoğu", "güney", "güneybatı", "batı", "kuzeybatı", "kuzey", "kuzeydoğu"};
        int i = (int) Math.round(((a % 360) + 360) % 360 / 45.0) % 8;
        return dirs[i];
    }

    private static String gift(ServerPlayer p, RpgNpc npc, Relation rel) {
        ItemStack st = p.getMainHandItem();
        if (st.isEmpty()) return "Elin boş.";
        long day = day(p);
        float taste = Gifts.taste(npc.race(), st);
        int value = Gifts.value(st);
        String name = st.getHoverName().getString();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        npc.wealth += value;
        if (taste < 0) {
            rel.add(-8, -2);
            return "Bu... " + name + " mi? Bize hakaret mi ediyorsun? " + npc.race().title + "ler böyle şeylerden hoşlanmaz.";
        }
        int gain = (int) Math.min(25, Math.max(1, Math.sqrt(value) * taste));
        if (rel.lastGiftDay == day) gain = Math.max(1, gain / 3);
        rel.lastGiftDay = day;
        rel.add(gain, gain / 3 + 1);
        FX.send(p.level(), ParticleTypes.HEART, npc.position().add(0, npc.getBbHeight() + 0.3, 0), 3 + gain / 5, 0.4, 0.05);
        if (taste >= 2) return "Bu " + name + " tam istediğim şey! Nereden bildin? Teşekkür ederim! (+" + gain + ")";
        if (gain >= 15) return "Vay! Bu çok değerli bir hediye. Sana borçlandım. (+" + gain + ")";
        if (gain >= 5) return "Ne kadar naziksin, teşekkürler. (+" + gain + ")";
        return "Hmm, teşekkürler... sanırım. (+" + gain + ")";
    }

    private static String turnIn(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d) {
        Quest q = null;
        for (Quest x : d.quests) if (npc.getUUID().equals(x.giver)) q = x;
        if (q == null) return "Senden bir şey beklemiyordum.";
        if (q.type == Quest.Type.FETCH) {
            var it = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(q.target));
            if (it != null && NpcQuests.countItem(p, it) >= q.count) {
                NpcQuests.takeItem(p, it, q.count);
                q.progress = q.count;
            } else return "Daha " + q.count + " " + q.targetName + " getirmedin. Elinde " + (it == null ? 0 : NpcQuests.countItem(p, it)) + " tane var.";
        }
        if (!q.done()) return "Henüz bitmedi: " + q.describe() + ". Sana güveniyorum.";
        NpcQuests.complete(p, q);
        rel.add(12, 12);
        return pick(p.getRandom(), "Harika iş çıkardın! İşte ödülün.", "Sözünü tuttun. Bunu unutmayacağım.", "Biliyordum, sana güvenilir!");
    }

    private static String threaten(ServerPlayer p, RpgNpc npc, Relation rel, PlayerRpg d) {
        RandomSource r = p.getRandom();
        rel.add(-20, -15);
        if (npc.isLawEnforcer() || npc.traits[1] > 65 && npc.isFighter()) {
            npc.setTarget(p);
            close(p);
            p.sendSystemMessage(Component.literal("§c" + npc.npcName() + ": \"Demek kavga istiyorsun? Gel o zaman!\""));
            return "";
        }
        if (npc.wealth > 0 && npc.traits[1] < 50) {
            int take = Math.min(npc.wealth, 10 + r.nextInt(30));
            npc.wealth -= take;
            NpcQuests.giveCoins(p, take);
            d.bounty[npc.kingdomId()] += 40;
            rel.enemy = true;
            rel.add(-30, -40);
            d.addRep(npc.kingdomId(), -25);
            return "T-tamam! Al, al şunu! (" + NpcQuests.coins(take) + ") ... Muhafızlar bunu duyacak!";
        }
        return pick(r, "Benim hiçbir şeyim yok! Git başımdan!", "Beni korkutamazsın.", "Muhafızlar! Muhafızlar!");
    }

    private static void slaveList(ServerPlayer p, RpgNpc slaver) {
        List<RpgNpc> slaves = p.level().getEntitiesOfClass(RpgNpc.class, slaver.getBoundingBox().inflate(16), n -> n.flag(RpgNpc.F_SLAVE) && n.leader() == null);
        List<Opt> o = new ArrayList<>();
        StringBuilder b = new StringBuilder("Mallarıma bak. Hepsi sağlıklı, hepsi itaatkâr.");
        if (slaves.isEmpty()) b = new StringBuilder("Şu an elimde mal kalmadı. Kervan gelince uğra.");
        for (RpgNpc s : slaves) {
            int price = 200 + (int) s.getMaxHealth() * 6;
            o.add(new Opt("buy:" + s.getId(), s.npcName() + " (" + s.race().title + ") — " + NpcQuests.coins(price)));
        }
        if (Kingdom.of(slaver.kingdomId()) == Kingdom.ALDORIA) b.append("\n§8(Aldoria'da kölelik yasaktır. Bu adam kaçakçı!)");
        o.add(new Opt("bye", "Vazgeç"));
        send(p, slaver, b.toString(), o);
    }

    private static void buySlave(ServerPlayer p, RpgNpc slaver, String idStr) {
        Entity e;
        try { e = p.level().getEntity(Integer.parseInt(idStr)); } catch (NumberFormatException ex) { e = null; }
        if (!(e instanceof RpgNpc s) || !s.flag(RpgNpc.F_SLAVE) || s.leader() != null) { page(p, slaver, "O artık satılık değil."); return; }
        int price = 200 + (int) s.getMaxHealth() * 6;
        if (!NpcQuests.pay(p, price)) { page(p, slaver, "Paran yetmez. " + NpcQuests.coins(price) + " lazım."); return; }
        s.setLeader(p.getUUID());
        s.setFlag(RpgNpc.F_FOLLOW, true);
        s.siteId = "";
        Relation rel = s.rel(p.getUUID());
        rel.add(-5, -10);
        PlayerRpg d = RpgWorldData.player(p);
        if (!d.party.contains(s.getUUID())) d.party.add(s.getUUID());
        if (!Kingdom.of(slaver.kingdomId()).slavery) d.bounty[slaver.kingdomId()] += 60;
        page(p, slaver, "Anlaştık. " + s.npcName() + " artık senin. Zincirlerini sen çöz ya da çözme, beni ilgilendirmez.");
        p.sendSystemMessage(Component.literal("§7" + s.npcName() + " seni takip etmeye başladı. Onunla konuşarak §aözgür bırakabilirsin§7."));
    }
}
