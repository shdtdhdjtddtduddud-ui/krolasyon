package com.krolasyon.bosses.rpg.story;

import com.krolasyon.bosses.rpg.data.PlayerRpg;
import com.krolasyon.bosses.rpg.data.RpgWorldData;
import com.krolasyon.bosses.rpg.def.MonsterDef;
import com.krolasyon.bosses.rpg.def.RpgDefs;
import com.krolasyon.bosses.rpg.def.RpgDefs.RegionId;
import com.krolasyon.bosses.rpg.item.RpgItems;
import com.krolasyon.bosses.rpg.magic.PlayerMagic;
import com.krolasyon.bosses.rpg.npc.*;
import com.krolasyon.bosses.rpg.npc.NpcDialog.Opt;
import com.krolasyon.bosses.rpg.registry.RpgEntities;
import com.krolasyon.bosses.rpg.util.FX;
import com.krolasyon.bosses.rpg.world.Kingdom;
import com.krolasyon.bosses.rpg.world.Race;
import com.krolasyon.bosses.rpg.world.Site;
import com.krolasyon.bosses.rpg.world.WorldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The main storyline. A poor child of Solmere's slums rises through a world of kingdoms; choices change who stands
 * beside them at the end.
 */
public final class Story {
    private Story() {}

    public static final String[] CHAPTERS = {"Doğuş", "Sokakların Çocuğu", "Ateşli Hastalık", "Kara Yemin", "Elflerin Ormanı",
            "Demir ve Ateş", "Savaş Davulları", "Tahtın Bedeli", "Kül Tahtı", "Efsane"};

    public static String chapterTitle(PlayerRpg d) {
        int c = Math.min(d.chapter, CHAPTERS.length - 1);
        return c >= 9 ? "Sonsöz: " + CHAPTERS[9] : "Bölüm " + (c + 1) + ": " + CHAPTERS[c];
    }

    private static int counter(PlayerRpg d, String key) {
        for (String f : d.flags) if (f.startsWith(key + ":")) {
            try { return Integer.parseInt(f.substring(key.length() + 1)); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private static void setCounter(PlayerRpg d, String key, int v) {
        d.flags.removeIf(f -> f.startsWith(key + ":"));
        d.flags.add(key + ":" + v);
    }

    public static String objective(PlayerRpg d) {
        return switch (d.chapter) {
            case 0 -> switch (d.step) {
                case 0 -> "Annen Elira ile konuş.";
                case 1 -> "Pazardan 3 ekmek al ve annene getir. (Tüccarlar ekmek satar)";
                default -> "Babanla konuş.";
            };
            case 1 -> switch (d.step) {
                case 0 -> "Muhafız Yüzbaşısı Bran ile şehir meydanında konuş.";
                case 1 -> "Şehrin çevresindeki fareleri avla (" + counter(d, "rats") + "/6).";
                case 2 -> "Yüzbaşı Bran'a dön.";
                case 3 -> "Maceracılar Loncası'na katıl (Lonca Ustası ile konuş).";
                case 4 -> "Ara sokaktan gelen çığlığın kaynağını bul.";
                case 5 -> "Köle avcılarını yen!";
                default -> "Elf kızla konuş.";
            };
            case 2 -> switch (d.step) {
                case 0 -> "Annenle konuş, babanın durumu kötüleşti.";
                case 1 -> "Tapınaktaki Rahip Anselm ile konuş.";
                case 2 -> "3 Şifa Otu bul (orman canavarları düşürebilir, rahipler satar) — " + counter(d, "herbs_have") + "/3";
                default -> "Şifa otlarını annene götür.";
            };
            case 3 -> switch (d.step) {
                case 0 -> "Annenle konuş. Bir şeyler çok yanlış...";
                case 1 -> "Yüzbaşı Bran'dan yardım iste.";
                case 2 -> "Kan Tarikatı'nın inine git ve Mira'yı kurtar.";
                case 3 -> "Tarikat lideri Morvak'ı yen.";
                case 4 -> "Mira ile konuş.";
                default -> "Mira'yı eve, annene götür.";
            };
            case 4 -> switch (d.step) {
                case 0 -> "Kral Aldric'in huzuruna çık (Solmere sarayı).";
                case 1 -> "Lunareth'e git ve Kraliçe Aelindra'ya mektubu ver.";
                case 2 -> "Elflerin güvenini kazanmak için Ulu Çürük Ağaç'ı yen.";
                default -> "Kraliçe Aelindra'ya dön.";
            };
            case 5 -> switch (d.step) {
                case 0 -> "Karak Dûm'a git ve Dağ Kralı Durgrim ile konuş.";
                case 1 -> "Demir Yiyen Wurm'u yen.";
                default -> "Durgrim'e dön.";
            };
            case 6 -> switch (d.step) {
                case 0 -> "Solmere'ye dön! İblis ordusu başkente yürüyor.";
                case 1 -> "Kuşatmayı kır! (kalan düşman: " + counter(d, "siege") + ")";
                default -> "Yaralı Kral Aldric ile konuş.";
            };
            case 7 -> "Ölmek üzere olan Kral Aldric ile konuş ve krallığın geleceğine karar ver.";
            case 8 -> "Kül Tahtı'na git ve İblis Lordu Malakar'ı yen.";
            default -> "Dünya senin. Krallıkları gez, efsaneni büyüt.";
        };
    }

    // ------------------------------------------------------------------ helpers
    private static void advance(ServerPlayer p, PlayerRpg d, int chapter, int step) {
        boolean newChapter = chapter != d.chapter;
        d.chapter = chapter;
        d.step = step;
        if (newChapter) {
            title(p, "§6" + chapterTitle(d), "§7" + objective(d));
            p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, p.getSoundSource(), 0.8F, 1.0F);
            onChapterStart(p, d);
        } else {
            p.sendSystemMessage(Component.literal("§e➤ Yeni hedef: §f" + objective(d)));
        }
        PlayerMagic.sync(p);
    }

    public static void title(ServerPlayer p, String t, String sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t)));
    }

    private static String id(RpgNpc n) { return n.storyId; }

    private static boolean isKing(RpgNpc n, Kingdom k) { return n.role() == NpcRole.RULER && n.kingdomId() == k.ordinal(); }

    @Nullable
    private static BlockPos ground(ServerLevel l, BlockPos near, int spread) {
        for (int i = 0; i < 12; i++) {
            int x = near.getX() + l.random.nextInt(spread * 2 + 1) - spread, z = near.getZ() + l.random.nextInt(spread * 2 + 1) - spread;
            int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (l.getBlockState(p.below()).isSolid() && l.getBlockState(p).isAir() && l.getBlockState(p.above()).isAir()) return p;
        }
        return null;
    }

    // ------------------------------------------------------------------ dialogue hooks
    @Nullable
    public static String dialogText(ServerPlayer p, PlayerRpg d, RpgNpc npc) {
        String s = id(npc);
        int c = d.chapter, st = d.step;
        if (s.equals("mother")) {
            if (c == 0 && st == 0) return "Uyandın mı evladım? Babanın ateşi yine yükseldi... Bu gece hiç uyuyamadı.\nBugün çamaşırcı kadın bana ücretimi vermedi. Evde bir lokma ekmek kalmadı.";
            if (c == 0 && st == 1) return "Ekmekleri aldın mı? Mira açlıktan ağlıyor...";
            if (c == 2 && st == 0) return "Babana bir şey oldu! Ateşi düşmüyor, sayıklıyor... Rahip Anselm'e git, belki o bir çare bilir. Lütfen acele et!";
            if (c == 2 && st == 3) return d.flag("father_dead") ? "..." : "Otları getirdin mi? Çabuk, kaynatayım!";
            if (c == 3 && st == 0) return "Mira... Mira yok! Kan Tarikatı'nın adamları gece kapıyı kırıp onu kaçırdı! 'Kurban gerekiyor' dediler... Lütfen, Yüzbaşı Bran'a git!";
            if (c == 3 && st == 5) return "Mira! Kızım! ... Sen... sen bizim kahramanımızsın. Baban seninle gurur duyardı" + (d.flag("father_dead") ? "." : ", hayır, duyuyor!");
            if (c >= 4) return d.social >= 4 ? "Benim çocuğum bir şövalye! Komşular inanmıyor!" : "Kendine dikkat et evladım. Her gece senin için dua ediyorum.";
        }
        if (s.equals("father")) {
            if (d.flag("father_dead")) return null;
            if (c == 0 && st == 2) return "*öksürür* Gel buraya evlat... Ben artık kalkamıyorum. Bu aileye bakmak sana kaldı.\nŞu sandıktaki eski kılıcımı al. Paslı ama hâlâ keskin.";
            if (c >= 3 && d.flag("father_saved")) return "Hayatımı sana borçluyum. Muhafızlık günlerimde öğrendiğim her şeyi sana öğreteceğim.";
        }
        if (s.equals("captain")) {
            if (c == 1 && st == 0) return "Sen Doran'ın çocuğu değil misin? Baban iyi bir muhafızdı.\nŞehrin kenar mahallelerini dev fareler bastı. Vebalı olanlar da var. Altı tanesini temizlersen sana ücretini öderim.";
            if (c == 1 && st == 2) return "Fareleri temizledin demek! Fena değil. Al, bu senin. Gerçek iş istiyorsan Maceracılar Loncası'na yazıl. Onlar cesur gençlere iyi para öder.";
            if (c == 3 && st == 1) return "Kan Tarikatı mı? Lanet olsun... Haftalardır çocuk kaçırıyorlar. İnlerinin şehrin dışında, eski bir harabede olduğunu biliyoruz ama askerlerim korkudan gitmek istemiyor.";
        }
        if (s.equals("priest")) {
            if (c == 2 && st == 1) return "Doran'ın ateşi... Bu 'Kızıl Humma'. Tek ilacı Şifa Otu. Ormanın derinliklerinde, ağaç adamların ve yosunlu ayıların yaşadığı yerlerde bulunur.\nTapınağın deposunda biraz var ama... soylular için ayrıldı. Fiyatı bir aile için çok yüksek.";
        }
        if (s.equals("lyra")) {
            if (c == 1 && st == 4) return "*titrer* L-lütfen... beni onlara verme! Ben bir Gümüşyaprak'ım... Valdrenli köle avcıları beni sınırdan kaçırdı. Annem... annem beni arıyor olmalı.";
            if (c == 1 && st == 6) return "Hayatımı kurtardın, yabancı. Ben Lyra Gümüşyaprak. Bu kolyeyi al — elfler onu görürse sana yardım ederler.\nBir gün, bir yerde... bu iyiliğini ödeyeceğim. Yemin ederim.";
            if (d.flag("lyra_saved")) return "Seni yeniden görmek ne güzel, dostum. Annem senden çok bahsetti.";
        }
        if (s.equals("morvak") && d.chapter == 3 && d.step == 3 && !npc.flag(RpgNpc.F_HOSTILE))
            return "*kan tükürür* Dur! Teslim oluyorum! Beni öldürme... Kızın içeride, zarar görmedi! Bana hayatımı bağışla, karşılığında Tarikatın bütün sırlarını anlatırım!";
        if (s.equals("sister")) {
            if (c == 3 && st == 4) return "Abla! Abi! Geleceğini biliyordum! Onlar... onlar korkunçtu. Beni eve götür, lütfen!";
            return "Büyüyünce senin gibi olacağım!";
        }
        if (isKing(npc, Kingdom.ALDORIA)) {
            if (c == 4 && st == 0) return "Demek Kan Tarikatı'nı yok eden genç sensin. Krallığın sana ihtiyacı var.\nKül Topraklarından iblis ordusu toplanıyor. Elflerle ve cücelerle ittifak kurmalıyız. Bu mektubu Lunareth'teki Kraliçe Aelindra'ya götür.";
            if (c == 6 && st == 2) return "*ağır yaralı* Kuşatmayı kırdın... Ama ben... İblis Lordunun kara oku kalbime ulaştı. Vaktim az.";
            if (c == 7) return "Dinle beni... Kızım Elyse genç ve tecrübesiz. Valdren tahtı ele geçirmek için bekliyor. Aldoria'nın geleceği senin elinde.";
        }
        if (isKing(npc, Kingdom.SYLVARIEN)) {
            if (c == 4 && st == 1) return d.flag("lyra_saved") ? "Kızım Lyra'yı köle avcılarından kurtaran insan... Gümüşyaprak Hanedanı sana sonsuz minnet borçlu.\nAldric'in mektubunu okudum. Elfler, iblislere karşı Aldoria'nın yanında olacak."
                    : "Sen... kızımı köle avcılarına satan o hain insan! Seni burada öldürmememin tek sebebi Aldric'in mührü.\nGüvenimizi geri kazanmak istiyorsan, ormanımızı zehirleyen Ulu Çürük Ağaç'ı yok et.";
            if (c == 4 && st == 3) return "Ulu Çürük Ağaç'ı yendin... Kızım için yaptığını affetmeyeceğim. Ama halkımı kurtardın. Elfler savaşta yanınızda olacak.";
        }
        if (isKing(npc, Kingdom.KHAZDUR)) {
            if (c == 5 && st == 0) return "HAH! Bir insan elçi! Aldric de sonunda aklını başına toplamış.\nAma önce bir derdimiz var: Demir Yiyen Wurm madenlerimizi yiyor! Onu öldür, cüce baltaları senin olsun.";
            if (c == 5 && st == 2) return "Wurm öldü! Bu gece Karak Dûm'da şölen var! Bu rün kılıcını al, dostum. Cüceler borçlarını unutmaz.";
        }
        return null;
    }

    public static void dialogOptions(ServerPlayer p, PlayerRpg d, RpgNpc npc, List<Opt> o) {
        String s = id(npc);
        int c = d.chapter, st = d.step;
        if (s.equals("mother")) {
            if (c == 0 && st == 0) o.add(new Opt("story_bread", "★ Ne yapabilirim anne?"));
            if (c == 0 && st == 1 && NpcQuests.countItem(p, Items.BREAD) >= 3) o.add(new Opt("story_give_bread", "★ Ekmekleri ver"));
            if (c == 2 && st == 0) o.add(new Opt("story_father_sick", "★ Tapınağa gidiyorum!"));
            if (c == 2 && st == 3 && NpcQuests.countItem(p, RpgItems.HERB.get()) >= 3) o.add(new Opt("story_give_herbs", "★ Şifa otlarını ver"));
            if (c == 3 && st == 0) o.add(new Opt("story_mira_gone", "★ Mira'yı bulacağım, söz veriyorum!"));
            if (c == 3 && st == 5) o.add(new Opt("story_mira_home", "★ Mira artık güvende."));
        }
        if (s.equals("father") && c == 0 && st == 2) o.add(new Opt("story_sword", "★ Kılıcı al"));
        if (s.equals("captain")) {
            if (c == 1 && st == 0) o.add(new Opt("story_rats", "★ Fareleri temizlerim"));
            if (c == 1 && st == 2) o.add(new Opt("story_rats_done", "★ Fareler temizlendi"));
            if (c == 3 && st == 1) o.add(new Opt("story_cult", "★ İnin yerini söyle, ben giderim!"));
        }
        if (s.equals("priest") && c == 2 && st == 1) {
            o.add(new Opt("story_herb_quest", "★ Otları ormandan kendim bulacağım"));
            o.add(new Opt("story_herb_steal", "★ (Gizlice) Depodan otları çal"));
            o.add(new Opt("story_herb_buy", "★ Otları satın al (" + NpcQuests.coins(600) + ")"));
        }
        if (s.equals("lyra")) {
            if (c == 1 && st == 4) {
                o.add(new Opt("story_lyra_help", "★ Arkamda dur. Kimse sana dokunamayacak!"));
                o.add(new Opt("story_lyra_betray", "★ (Köle avcılarına seslen) Aradığınız elf burada!"));
            }
            if (c == 1 && st == 6) o.add(new Opt("story_lyra_thanks", "★ Kolyeyi al"));
        }
        if (s.equals("morvak") && c == 3 && st == 3 && !npc.flag(RpgNpc.F_HOSTILE)) {
            o.add(new Opt("story_morvak_spare", "★ Hayatını bağışla"));
            o.add(new Opt("story_morvak_kill", "★ Adalet yerini bulmalı"));
        }
        if (s.equals("sister") && c == 3 && st == 4) o.add(new Opt("story_mira_follow", "★ Hadi eve gidelim"));
        if (isKing(npc, Kingdom.ALDORIA)) {
            if (c == 4 && st == 0) o.add(new Opt("story_envoy", "★ Mektubu Lunareth'e götüreceğim"));
            if (c == 6 && st == 2) o.add(new Opt("story_king_wounded", "★ Majesteleri, dayanın!"));
            if (c == 7) {
                o.add(new Opt("story_throne_princess", "★ Prenses Elyse'nin yanında olacağım"));
                o.add(new Opt("story_throne_claim", "★ Tahtı ben alacağım (Baron+ ve 700 saygınlık)"));
                o.add(new Opt("story_throne_valdren", "★ (İhanet) Aldoria'yı Valdren'e teslim et"));
            }
        }
        if (isKing(npc, Kingdom.SYLVARIEN)) {
            if (c == 4 && st == 1) o.add(new Opt("story_elf_letter", "★ Aldric'in mektubunu sun"));
            if (c == 4 && st == 3) o.add(new Opt("story_elf_trial_done", "★ Ağaç yok edildi"));
        }
        if (isKing(npc, Kingdom.KHAZDUR)) {
            if (c == 5 && st == 0) o.add(new Opt("story_dwarf_help", "★ Wurm'u ben öldüreceğim"));
            if (c == 5 && st == 2) o.add(new Opt("story_dwarf_done", "★ Wurm öldü"));
        }
    }

    @Nullable
    public static String onChoice(ServerPlayer p, PlayerRpg d, RpgNpc npc, String id) {
        RpgWorldData w = RpgWorldData.get(p.server);
        ServerLevel level = p.serverLevel();
        switch (id) {
            case "story_bread" -> {
                NpcQuests.giveCoins(p, 6);
                advance(p, d, 0, 1);
                return "Al, son 6 bakırımız bu. Çarşıdaki tüccardan 3 ekmek al. Dikkatli ol, sokaklar tehlikeli.";
            }
            case "story_give_bread" -> {
                NpcQuests.takeItem(p, Items.BREAD, 3);
                npc.rel(p.getUUID()).add(10, 10);
                advance(p, d, 0, 2);
                return "Tanrılar seni korusun evladım. Git, babanla konuş. Sana söylemek istediği bir şey var.";
            }
            case "story_sword" -> {
                NpcQuests.give(p, new ItemStack(RpgItems.sword("rusty_shortsword")));
                d.addXp(50);
                advance(p, d, 1, 0);
                return "Bu kılıç beni otuz yıl korudu. Şimdi seni korusun. Şehir muhafızlarının yüzbaşısı Bran eski bir dostum. Ona git, belki sana iş verir.";
            }
            case "story_rats" -> {
                setCounter(d, "rats", 0);
                advance(p, d, 1, 1);
                return "Güzel. Şehrin dışındaki tarlalarda ve kanalizasyon ağızlarında ara. Altı tane yeter.";
            }
            case "story_rats_done" -> {
                NpcQuests.giveCoins(p, 40);
                d.addRep(Kingdom.ALDORIA.ordinal(), 30);
                d.addXp(80);
                advance(p, d, 1, 3);
                return "Kırk bakır, hak ettin. Lonca binası meydanın yakınında. Lonca Ustası'yla konuş.";
            }
            case "story_lyra_help" -> {
                for (RpgNpc s : level.getEntitiesOfClass(RpgNpc.class, npc.getBoundingBox().inflate(24), n -> n.storyId.startsWith("slaver"))) {
                    s.setFlag(RpgNpc.F_HOSTILE, true);
                    s.setTarget(p);
                }
                advance(p, d, 1, 5);
                p.sendSystemMessage(Component.literal("§c Köle Avcısı: \"Bu elf bizim malımız, velet! Çekil yoksa seni de zincire vururuz!\""));
                return "";
            }
            case "story_lyra_betray" -> {
                NpcQuests.giveCoins(p, 300);
                d.setFlag("lyra_betrayed");
                d.addRep(Kingdom.SYLVARIEN.ordinal(), -400);
                d.addRep(Kingdom.VALDREN.ordinal(), 80);
                Bonds.add(p, npc, "betrayed");
                npc.rel(p.getUUID()).add(-100, -100);
                npc.setFlag(RpgNpc.F_SLAVE, true);
                npc.leaveAt = level.getGameTime() + 100;
                for (RpgNpc s : level.getEntitiesOfClass(RpgNpc.class, npc.getBoundingBox().inflate(24), n -> n.storyId.startsWith("slaver"))) s.leaveAt = level.getGameTime() + 100;
                p.sendSystemMessage(Component.literal("§8Köle avcıları elf kızı zincire vurup götürdü. Gözlerindeki nefreti unutamayacaksın."));
                advance(p, d, 2, 0);
                return "§8Köle Avcısı: \"Akıllı velet. Al bakalım, otuz gümüş. Valdren'de iyi fiyata gider.\"";
            }
            case "story_lyra_thanks" -> {
                NpcQuests.give(p, new ItemStack(RpgItems.ELF_PENDANT.get()));
                d.setFlag("lyra_saved");
                Bonds.add(p, npc, "saved");
                npc.rel(p.getUUID()).add(80, 70);
                npc.rel(p.getUUID()).rescued = true;
                d.addRep(Kingdom.SYLVARIEN.ordinal(), 200);
                npc.leaveAt = level.getGameTime() + 20 * 15;
                advance(p, d, 2, 0);
                return "Elveda... şimdilik. Yollarımız yeniden kesişecek, hissediyorum.";
            }
            case "story_father_sick" -> { advance(p, d, 2, 1); return "Koş evladım, koş!"; }
            case "story_herb_quest" -> {
                setCounter(d, "herb_day", (int) (level.getDayTime() / 24000L));
                advance(p, d, 2, 2);
                return "Işık yolunu aydınlatsın. Ama acele et — Kızıl Humma on günden fazla beklemez.";
            }
            case "story_herb_steal" -> {
                NpcQuests.give(p, new ItemStack(RpgItems.HERB.get(), 3));
                d.setFlag("stole_herbs");
                d.bounty[Kingdom.ALDORIA.ordinal()] += 80;
                npc.rel(p.getUUID()).add(-30, -40);
                advance(p, d, 2, 3);
                return "§8(Rahip arkasını döndüğünde depodan üç demet ot aldın. Kimse görmedi... değil mi?)";
            }
            case "story_herb_buy" -> {
                if (!NpcQuests.pay(p, 600)) return "Üzgünüm evladım. Altı altın gerekiyor... Bunu bir yoksul nasıl öder, bilirim. Ama kurallar soyluların.";
                NpcQuests.give(p, new ItemStack(RpgItems.HERB.get(), 3));
                advance(p, d, 2, 3);
                return "Al. Işık babanı iyileştirsin.";
            }
            case "story_give_herbs" -> {
                NpcQuests.takeItem(p, RpgItems.HERB.get(), 3);
                int started = counter(d, "herb_day");
                long now = level.getDayTime() / 24000L;
                if (started > 0 && now - started > 10) {
                    d.setFlag("father_dead");
                    advance(p, d, 3, 0);
                    p.sendSystemMessage(Component.literal("§8✝ Çok geç kaldın. Baban Doran, ateşler içinde son nefesini verdi."));
                    return "Geç kaldın... O... o seni sordu son anında. 'Çocuğum nerede?' dedi...";
                }
                d.setFlag("father_saved");
                d.addXp(150);
                advance(p, d, 3, 0);
                return "Ateşi düşüyor! Tanrılara şükür... Ve sana şükür, evladım.";
            }
            case "story_mira_gone" -> { advance(p, d, 3, 1); return "Tanrılar seninle olsun..."; }
            case "story_cult" -> {
                Site cult = w.site("cult_hideout");
                if (cult == null) {
                    cult = new Site();
                    cult.id = "cult_hideout";
                    cult.name = "Kan Tarikatı İni";
                    cult.type = Site.Type.CAMP;
                    Site cap = WorldMap.capital(w, Kingdom.ALDORIA.ordinal());
                    double a = p.getRandom().nextDouble() * Math.PI * 2;
                    cult.x = (int) (cap.x + Math.cos(a) * 260);
                    cult.z = (int) (cap.z + Math.sin(a) * 260);
                    cult.seed = p.getRandom().nextLong();
                    w.sites.add(cult);
                    w.setDirty();
                }
                advance(p, d, 3, 2);
                return "Eski harabe, şehrin " + NpcDialog.direction(npc.getX(), npc.getZ(), cult.x, cult.z) + " tarafında, " + cult.x + ", " + cult.z + " civarında. Dikkatli ol... Morvak sıradan bir haydut değil, kan büyüsü kullanır.";
            }
            case "story_morvak_spare" -> {
                d.setFlag("morvak_spared");
                Bonds.add(p, npc, "saved");
                npc.leaveAt = level.getGameTime() + 20 * 10;
                npc.rel(p.getUUID()).add(60, 40);
                advance(p, d, 3, 4);
                return "Merhametin... beklemediğim bir şeydi. Tarikat daha büyük bir gücün piyonu: Kül Tahtı'nın efendisi Malakar. Bir gün bunu hatırla. Ve beni.";
            }
            case "story_morvak_kill" -> {
                npc.setFlag(RpgNpc.F_HOSTILE, true);
                npc.hurt(p.damageSources().playerAttack(p), 9999);
                d.setFlag("morvak_dead");
                advance(p, d, 3, 4);
                return "";
            }
            case "story_mira_follow" -> {
                npc.setLeader(p.getUUID());
                npc.setFlag(RpgNpc.F_FOLLOW, true);
                npc.setFlag(RpgNpc.F_FAMILY, true);
                advance(p, d, 3, 5);
                return "Elimi bırakma, tamam mı?";
            }
            case "story_mira_home" -> {
                RpgNpc mira = NpcFactory.findStory(level, npc.blockPosition(), "sister", 24);
                if (mira == null) return "Mira nerede? Onu buraya getir!";
                mira.setFlag(RpgNpc.F_FOLLOW, false);
                mira.setLeader(null);
                BlockPos home = w.anchor("family_home");
                mira.home = home;
                d.addXp(300);
                d.addRep(Kingdom.ALDORIA.ordinal(), 120);
                d.fame += 20;
                w.news("Solmere'de bir yoksul genç, Kan Tarikatı'nı tek başına çökertti!");
                advance(p, d, 4, 0);
                return dialogText(p, d, npc) == null ? "Teşekkürler..." : "Kahramanım... Kral bile senden bahsediyormuş! Saraydan bir haberci geldi, seni görmek istiyor.";
            }
            case "story_envoy" -> {
                NpcQuests.give(p, new ItemStack(RpgItems.LETTER.get()));
                NpcQuests.give(p, new ItemStack(RpgItems.ROYAL_SEAL.get()));
                d.addRep(Kingdom.ALDORIA.ordinal(), 50);
                Site lun = WorldMap.capital(w, Kingdom.SYLVARIEN.ordinal());
                advance(p, d, 4, 1);
                return "Lunareth, buradan " + NpcDialog.direction(p.getX(), p.getZ(), lun.x, lun.z) + " yönünde, " + (int) Math.sqrt(lun.distSq(p.getX(), p.getZ())) + " adım uzakta (" + lun.x + ", " + lun.z + "). Kraliyet mührü sana kapıları açacak.";
            }
            case "story_elf_letter" -> {
                NpcQuests.takeItem(p, RpgItems.LETTER.get(), 1);
                if (d.flag("lyra_saved")) {
                    d.addRep(Kingdom.SYLVARIEN.ordinal(), 300);
                    w.changeRelation(Kingdom.ALDORIA.ordinal(), Kingdom.SYLVARIEN.ordinal(), 40);
                    NpcQuests.give(p, new ItemStack(RpgItems.sword("elven_moonblade")));
                    d.setFlag("elf_alliance");
                    w.news("Aldoria ile Sylvarien elfleri iblislere karşı ittifak kurdu!");
                    advance(p, d, 5, 0);
                    return "Bu Ay Kılıcı'nı al. Ve Lyra'ya bir uğra; seni görmek isteyecek. Şimdi Karak Dûm'a, cücelere git.";
                }
                advance(p, d, 4, 2);
                return "Git ve ormanımızı temizle. Sonra konuşuruz.";
            }
            case "story_elf_trial_done" -> {
                d.addRep(Kingdom.SYLVARIEN.ordinal(), 150);
                w.changeRelation(Kingdom.ALDORIA.ordinal(), Kingdom.SYLVARIEN.ordinal(), 20);
                d.setFlag("elf_alliance");
                advance(p, d, 5, 0);
                return "Git artık. Cücelerin kralı Durgrim de bu mektubu bekliyor.";
            }
            case "story_dwarf_help" -> { advance(p, d, 5, 1); return "Wurm, Demirdağlar'ın derinliklerinde. Yerin titrediğini hissettiğin yerdedir!"; }
            case "story_dwarf_done" -> {
                NpcQuests.give(p, new ItemStack(RpgItems.sword("dwarven_runeblade")));
                d.addRep(Kingdom.KHAZDUR.ordinal(), 250);
                w.changeRelation(Kingdom.ALDORIA.ordinal(), Kingdom.KHAZDUR.ordinal(), 40);
                d.setFlag("dwarf_alliance");
                w.news("Cüceler Aldoria'ya asker göndermeye söz verdi.");
                advance(p, d, 6, 0);
                return "Ve şimdi... kötü haber. Kuzgunlar getirdi: İblis ordusu Solmere'ye yürüyor! Acele et!";
            }
            case "story_king_wounded" -> { advance(p, d, 7, 0); return "Dinle beni... vaktim az..."; }
            case "story_throne_princess" -> {
                d.social = Math.max(d.social, 7);
                d.addRep(Kingdom.ALDORIA.ordinal(), 200);
                d.setFlag("throne_princess");
                w.kingDead = true;
                w.news("Kral Aldric öldü. Kraliçe Elyse tahta çıktı; yanında yeni Dük " + p.getName().getString() + " var.");
                kingDies(p, npc);
                advance(p, d, 8, 0);
                return "Elyse... iyi bir kraliçe olacak... seninle... *son nefes*\n\n§6Dük unvanını aldın.";
            }
            case "story_throne_claim" -> {
                if (d.social < 5 || d.repWith(Kingdom.ALDORIA.ordinal()) < 700) return "Lordlar seni tanımaz... Önce Baron ol ve krallığın sevgisini kazan (700 saygınlık).";
                d.social = 8;
                d.allegiance = Kingdom.ALDORIA.ordinal();
                d.setFlag("throne_self");
                w.kingDead = true;
                w.news("Çamur Mahallesi'nin çocuğu " + p.getName().getString() + ", Aldoria'nın yeni hükümdarı oldu!");
                kingDies(p, npc);
                p.server.getPlayerList().broadcastSystemMessage(Component.literal("§6§l♛ " + p.getName().getString() + " Aldoria tahtına çıktı! Uzun yaşasın Hükümdar!"), false);
                advance(p, d, 8, 0);
                return "Sokakların çocuğu... tahtta... Belki de... krallığın ihtiyacı buydu... *son nefes*";
            }
            case "story_throne_valdren" -> {
                d.setFlag("traitor");
                d.addRep(Kingdom.VALDREN.ordinal(), 500);
                d.addRep(Kingdom.ALDORIA.ordinal(), -800);
                d.allegiance = Kingdom.VALDREN.ordinal();
                d.social = Math.max(d.social, 6);
                w.kingDead = true;
                w.news("İhanet! Aldoria'nın kapıları Valdren lejyonlarına açıldı.");
                kingDies(p, npc);
                advance(p, d, 8, 0);
                return "Sen... sen de mi... *son nefes*\n\n§8Valdren İmparatoru sana Kont unvanı verdi. Ama Aldoria'da artık bir hainsin.";
            }
            default -> {}
        }
        return null;
    }

    private static void kingDies(ServerPlayer p, RpgNpc king) {
        king.setCustomName(Component.literal("Kraliçe Elyse"));
        king.setLook(Race.HUMAN.ordinal(), true, 1);
        FX.column(p.level(), ParticleTypes.END_ROD, king.position(), 3, 1, 60);
    }

    // ------------------------------------------------------------------ world hooks
    public static void onEvent(ServerPlayer p, PlayerRpg d, String event) {
        if (event.equals("guild_join") && d.chapter == 1 && d.step == 3) {
            advance(p, d, 1, 4);
            spawnLyraScene(p);
        }
    }

    private static void spawnLyraScene(ServerPlayer p) {
        ServerLevel l = p.serverLevel();
        BlockPos at = ground(l, p.blockPosition().offset(10, 0, 10), 6);
        if (at == null) at = p.blockPosition().offset(4, 0, 4);
        NpcFactory.spawn(l, at, Race.ELF, true, NpcRole.PEASANT, Kingdom.SYLVARIEN.ordinal(), "Lyra Gümüşyaprak", "lyra");
        for (int i = 0; i < 2; i++) {
            RpgNpc s = NpcFactory.spawn(l, at.offset(2 + i, 0, -2), Race.HUMAN, false, NpcRole.SLAVER, Kingdom.VALDREN.ordinal(), null, "slaver" + i);
            if (s != null) s.setFlag(RpgNpc.F_HOSTILE, false);
        }
        p.sendSystemMessage(Component.literal("§c⚠ Yakındaki bir ara sokaktan bir kız çığlığı yükseliyor: \"Bırakın beni! Yardım edin!\""));
        l.playSound(null, at, SoundEvents.VILLAGER_HURT, p.getSoundSource(), 1.5F, 1.4F);
    }

    public static void onKill(ServerPlayer p, PlayerRpg d, @Nullable MonsterDef def, @Nullable RegionId region) {
        if (def == null) return;
        if (d.chapter == 1 && d.step == 1 && (def.id().equals("plague_rat") || def.id().equals("sewer_rat_king"))) {
            int n = counter(d, "rats") + 1;
            setCounter(d, "rats", n);
            if (n >= 6) advance(p, d, 1, 2);
            else p.displayClientMessage(Component.literal("§7Fareler: " + n + "/6"), true);
        }
        if (d.chapter == 2 && d.step == 2 && region != null && (region == RegionId.WOLF_FOREST || region == RegionId.SYLVARIEN || region == RegionId.FELARIS)
                && (def.arch() == RpgDefs.Archetype.TREANT || def.arch() == RpgDefs.Archetype.QUADRUPED || p.getRandom().nextInt(3) == 0)) {
            NpcQuests.give(p, new ItemStack(RpgItems.HERB.get()));
            p.displayClientMessage(Component.literal("§aBir Şifa Otu buldun!"), true);
        }
        if (d.chapter == 4 && d.step == 2 && def.id().equals("elder_rot_tree")) advance(p, d, 4, 3);
        if (d.chapter == 5 && d.step == 1 && def.id().equals("iron_eater_wurm")) advance(p, d, 5, 2);
        if (d.chapter == 6 && d.step == 1 && p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(80), e -> e.getPersistentData().getBoolean("Siege")).isEmpty()) {
            advance(p, d, 6, 2);
        }
        if (d.chapter == 8 && def.id().equals("demon_lord_malakar")) ending(p, d);
    }

    public static void onNpcDeath(ServerLevel level, RpgNpc npc, DamageSource src) {
        if (npc.storyId.startsWith("slaver")) {
            for (PlayerRpg d : RpgWorldData.get(level).players.values()) {
                if (d.chapter == 1 && d.step == 5) {
                    List<RpgNpc> rest = level.getEntitiesOfClass(RpgNpc.class, npc.getBoundingBox().inflate(40), n -> n != npc && n.isAlive() && n.storyId.startsWith("slaver"));
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
                    if (rest.isEmpty() && p != null) advance(p, d, 1, 6);
                }
            }
        }
        if (npc.storyId.equals("morvak")) {
            for (PlayerRpg d : RpgWorldData.get(level).players.values()) {
                if (d.chapter == 3 && d.step == 3) {
                    ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
                    d.setFlag("morvak_dead");
                    if (p != null) advance(p, d, 3, 4);
                }
            }
        }
        if (npc.getPersistentData().getBoolean("Siege")) {
            for (PlayerRpg d : RpgWorldData.get(level).players.values()) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(d.id);
                if (p != null && d.chapter == 6 && d.step == 1) {
                    int left = level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(96), e -> e != npc && e.isAlive() && e.getPersistentData().getBoolean("Siege")).size();
                    setCounter(d, "siege", left);
                    if (left == 0) advance(p, d, 6, 2);
                    else PlayerMagic.sync(p);
                }
            }
        }
    }

    private static void ending(ServerPlayer p, PlayerRpg d) {
        d.chapter = 9;
        d.step = 0;
        String ending;
        if (d.flag("traitor")) ending = "Gölgelerin Efendisi: Malakar düştü, ama Aldoria artık Valdren'in zinciri altında. Tarih seni hem kahraman hem hain olarak anacak.";
        else if (d.flag("throne_self")) ending = "Işığın Hükümdarı: Çamurdan doğan çocuk, iblisleri yenen kral oldu. Solmere'nin sokaklarında artık kimse aç uyumuyor.";
        else if (d.flag("throne_princess")) ending = "Tahtın Kalkanı: Kraliçe Elyse'nin yanında, Dük olarak krallığı korudun. Halk senin adını şarkılarla anıyor.";
        else ending = "Gezgin Efsane: Malakar'ı yendin ve yoluna devam ettin. Hikayen her handa anlatılıyor.";
        if (d.flag("lyra_saved")) ending += "\nLyra Gümüşyaprak, elflerin yeni kraliçesi olarak sana sonsuz dostluk yemini etti.";
        if (d.flag("lyra_betrayed")) ending += "\nElfler adını hâlâ lanetle anıyor.";
        if (d.flag("morvak_spared")) ending += "\nMorvak, Tarikat'tan kalanları senin adına avlıyor.";
        if (d.flag("father_saved")) ending += "\nBaban Doran, torunlarına senin hikayeni anlatıyor.";
        if (d.spouse != null) ending += "\n" + d.spouseName + " ve çocukların seni evde bekliyor.";
        title(p, "§6§lEFSANE", "§eİblis Lordu Malakar yenildi!");
        p.sendSystemMessage(Component.literal("§6§l════ SONSÖZ ════\n§f" + ending + "\n§7(Hikaye bitti ama dünya yaşamaya devam ediyor.)"));
        p.server.getPlayerList().broadcastSystemMessage(Component.literal("§6§l" + p.getName().getString() + " İblis Lordu Malakar'ı yendi! Dünya kurtuldu!"), false);
        d.fame += 200;
        PlayerMagic.sync(p);
    }

    private static void onChapterStart(ServerPlayer p, PlayerRpg d) {
        if (d.chapter == 3) {
            RpgWorldData w = RpgWorldData.get(p.server);
            BlockPos home = w.anchor("family_home");
            if (home != null) {
                RpgNpc mira = NpcFactory.findStory(p.serverLevel(), home, "sister", 64);
                if (mira != null) mira.discard();
            }
            d.setFlag("mira_taken");
        }
        if (d.chapter == 6) RpgWorldData.get(p.server).news("§cİblis Lordu Malakar'ın ordusu Aldoria sınırını geçti!");
    }

    // ------------------------------------------------------------------ ticking (once a second per player)
    public static void tick(ServerPlayer p, PlayerRpg d) {
        RpgWorldData w = RpgWorldData.get(p.server);
        ServerLevel level = p.serverLevel();
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        BlockPos home = w.anchor("family_home");
        if (!d.originDone) {
            if (home == null) {
                if (p.tickCount % 100 == 0) p.displayClientMessage(Component.literal("§7Dünya şekilleniyor... Solmere inşa ediliyor."), true);
                return;
            }
            origin(p, d, home);
            return;
        }
        // keep the family in their hovel
        if (home != null && p.blockPosition().distSqr(home) < 72 * 72) {
            ensure(level, home, "mother", Race.HUMAN, true, "Elira", true);
            if (!d.flag("father_dead")) ensure(level, home, "father", Race.HUMAN, false, "Doran", true);
            if (!d.flag("mira_taken") || d.chapter >= 4) ensure(level, home, "sister", Race.HUMAN, true, "Mira", true);
        }
        BlockPos square = w.anchor("square");
        if (square != null && p.blockPosition().distSqr(square) < 72 * 72) ensure(level, square, "captain", Race.HUMAN, false, "Yüzbaşı Bran", false);
        BlockPos temple = w.anchor("temple");
        if (temple != null && p.blockPosition().distSqr(temple) < 72 * 72) ensure(level, temple, "priest", Race.HUMAN, false, "Rahip Anselm", false);
        // herbs counter for the objective line
        if (d.chapter == 2 && d.step == 2) {
            int have = NpcQuests.countItem(p, RpgItems.HERB.get());
            if (have != counter(d, "herbs_have")) { setCounter(d, "herbs_have", have); PlayerMagic.sync(p); }
            if (have >= 3) advance(p, d, 2, 3);
        }
        // the cult hideout
        if (d.chapter == 3 && d.step == 2) {
            Site cult = w.site("cult_hideout");
            if (cult != null && cult.distSq(p.getX(), p.getZ()) < 30 * 30) {
                BlockPos c = ground(level, cult.center(), 3);
                if (c == null) c = p.blockPosition();
                RpgNpc mor = NpcFactory.spawn(level, c, Race.HUMAN, false, NpcRole.MAGE, Kingdom.INFERNAX.ordinal(), "Morvak, Kan Rahibi", "morvak");
                if (mor != null) {
                    mor.setFlag(RpgNpc.F_HOSTILE, true);
                    mor.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(90);
                    mor.setHealth(90);
                    mor.setTarget(p);
                }
                EntityType<?> zealot = RpgEntities.typeOf("blood_zealot");
                for (int i = 0; i < 4 && zealot != null; i++) {
                    Entity z = zealot.spawn(level, c.offset(i * 2 - 3, 0, 3), MobSpawnType.EVENT);
                    if (z instanceof Mob m) m.setTarget(p);
                }
                RpgNpc mira = NpcFactory.spawn(level, c.offset(0, 0, -4), Race.HUMAN, true, NpcRole.CHILD, Kingdom.ALDORIA.ordinal(), "Mira", "sister");
                if (mira != null) { mira.setAge(RpgNpc.CHILD_TICKS * 2); mira.setFlag(RpgNpc.F_STAY, true); }
                p.sendSystemMessage(Component.literal("§4Morvak: \"Kurbanımı almaya mı geldin? Kanın, Efendimiz Malakar'a armağan olacak!\""));
                advance(p, d, 3, 3);
            }
        }
        if (d.chapter == 3 && d.step == 3) {
            RpgNpc mor = NpcFactory.findStory(level, p.blockPosition(), "morvak", 48);
            if (mor != null && mor.flag(RpgNpc.F_HOSTILE) && mor.getHealth() < mor.getMaxHealth() * 0.3F && !d.flag("morvak_asked")) {
                d.setFlag("morvak_asked");
                mor.setFlag(RpgNpc.F_HOSTILE, false);
                mor.setTarget(null);
                mor.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.krolasyon.bosses.rpg.registry.RpgEffects.STUN.get(), 20 * 60, 0));
                p.sendSystemMessage(Component.literal("§eMorvak dizlerinin üstüne çöktü ve silahını bıraktı. Onunla konuş."));
            }
        }
        if (d.chapter == 3 && d.step == 5 && home != null && p.blockPosition().distSqr(home) < 12 * 12) {
            RpgNpc mira = NpcFactory.findStory(level, home, "sister", 16);
            if (mira != null) mira.home = home;
        }
        // the siege of Solmere
        if (d.chapter == 6 && d.step == 0) {
            Site cap = WorldMap.capital(w, Kingdom.ALDORIA.ordinal());
            if (cap.distSq(p.getX(), p.getZ()) < 90 * 90) siege(p, d, cap);
        }
        // allies gather before the final battle
        if (d.chapter == 8 && !d.flag("final_rally")) {
            Site lair = w.site("lair_demon_lord_malakar");
            if (lair != null && lair.distSq(p.getX(), p.getZ()) < 80 * 80) {
                d.setFlag("final_rally");
                rally(p, d, true);
            }
        }
    }

    private static void ensure(ServerLevel level, BlockPos anchor, String storyId, Race race, boolean female, String name, boolean family) {
        if (NpcFactory.findStory(level, anchor, storyId, 64) != null) return;
        if (!level.isLoaded(anchor)) return;
        RpgNpc n = NpcFactory.spawn(level, anchor, race, female, family ? NpcRole.FAMILY : storyId.equals("captain") ? NpcRole.GUARD : NpcRole.PRIEST,
                Kingdom.ALDORIA.ordinal(), name, storyId);
        if (n == null) return;
        if (storyId.equals("sister")) n.setAge(RpgNpc.CHILD_TICKS * 3);
        if (storyId.equals("captain")) {
            n.setRole(NpcRole.KNIGHT);
            n.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(80);
            n.setHealth(80);
        }
        n.setFlag(RpgNpc.F_STAY, family && storyId.equals("father"));
    }

    private static void origin(ServerPlayer p, PlayerRpg d, BlockPos home) {
        d.originDone = true;
        p.teleportTo(p.serverLevel(), home.getX() + 0.5, home.getY(), home.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.setRespawnPosition(p.serverLevel().dimension(), home, 0, true, false);
        NpcQuests.giveCoins(p, 5);
        NpcQuests.give(p, new ItemStack(Items.BREAD, 2));
        title(p, "§6§lKROLASYON", "§eBir hayat, bin hikaye");
        p.sendSystemMessage(Component.literal("""
                §6§l═══════ Çamurdan Doğanlar ═══════§r
                §fSolmere — Aldoria Krallığı'nın kadim başkenti. Taş surların gölgesindeki §7Çamur Mahallesi§f'nde, yoksul bir ailenin çocuğu olarak dünyaya geldin.
                §fBaban §eDoran§f bir zamanlar şehir muhafızıydı; şimdi ateşli bir hastalıkla yatağa bağlı. Annen §eElira§f çamaşır yıkayarak ailenin karnını doyurmaya çalışıyor. Küçük kız kardeşin §eMira§f ise senin bir gün kahraman olacağına inanıyor.
                §fBu dünyada §e10 krallık§f, insanlar, elfler, cüceler, iblisler, devler, orklar ve daha nicesi yaşıyor. Her seçimin bir bedeli, her iyiliğin bir karşılığı var.
                §7Tuşlar: §fK§7 karakter • §fJ§7 görevler & hikaye • §fM§7 krallıklar • §fR§7 büyü yap • §fZ/X§7 büyü değiştir • §fNPC'ye sağ tık§7 konuş"""));
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, p.getSoundSource(), 1.0F, 0.8F);
        PlayerMagic.sync(p);
    }

    private static void siege(ServerPlayer p, PlayerRpg d, Site cap) {
        ServerLevel level = p.serverLevel();
        int n = 0;
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI * 2 / 10;
            BlockPos at = ground(level, new BlockPos((int) (cap.x + Math.cos(a) * (cap.type.radius - 10)), 0, (int) (cap.z + Math.sin(a) * (cap.type.radius - 10))), 4);
            if (at == null) continue;
            Mob m;
            if (i % 3 == 0) {
                EntityType<?> t = RpgEntities.typeOf(i % 2 == 0 ? "imp" : "hellhound");
                Entity e = t == null ? null : t.spawn(level, at, MobSpawnType.EVENT);
                m = e instanceof Mob mm ? mm : null;
            } else {
                m = NpcFactory.spawn(level, at, i % 4 == 0 ? Race.ORC : Race.DEMON, false, i == 1 ? NpcRole.KNIGHT : NpcRole.SOLDIER, Kingdom.INFERNAX.ordinal(), null, null);
                if (m instanceof RpgNpc rn) rn.setFlag(RpgNpc.F_HOSTILE, true);
            }
            if (m != null) {
                m.getPersistentData().putBoolean("Siege", true);
                m.setTarget(p);
                n++;
            }
        }
        EntityType<?> fiend = RpgEntities.typeOf("pit_fiend");
        if (fiend != null) {
            Entity e = fiend.spawn(level, cap.center().offset(0, 0, cap.type.radius / 2), MobSpawnType.EVENT);
            if (e instanceof Mob m) { m.getPersistentData().putBoolean("Siege", true); n++; }
        }
        setCounter(d, "siege", n);
        title(p, "§4§lKUŞATMA!", "§cİblis ordusu Solmere'nin kapılarında!");
        level.playSound(null, p.blockPosition(), SoundEvents.RAID_HORN.value(), p.getSoundSource(), 3.0F, 0.8F);
        advance(p, d, 6, 1);
        rally(p, d, false);
    }

    /** everyone the player once helped comes back to fight beside them */
    public static void rally(ServerPlayer p, PlayerRpg d, boolean finalBattle) {
        ServerLevel level = p.serverLevel();
        int came = 0;
        for (PlayerRpg.Bond b : d.bonds) {
            if (b.dead || !(b.kind.equals("saved") || b.kind.equals("freed") || b.kind.equals("friend"))) continue;
            if (com.krolasyon.bosses.rpg.npc.Fate.summonHelper(p, b, "Seni duydum! Bir zamanlar beni kurtarmıştın. Şimdi yanındayım!") != null) came++;
            if (came >= 6) break;
        }
        if (finalBattle) {
            if (d.flag("elf_alliance")) for (int i = 0; i < 3; i++) helper(p, Race.ELF, NpcRole.KNIGHT, Kingdom.SYLVARIEN);
            if (d.flag("dwarf_alliance")) for (int i = 0; i < 3; i++) helper(p, Race.DWARF, NpcRole.SOLDIER, Kingdom.KHAZDUR);
            if (d.flag("throne_self") || d.flag("throne_princess")) for (int i = 0; i < 3; i++) helper(p, Race.HUMAN, NpcRole.KNIGHT, Kingdom.ALDORIA);
        }
        if (came > 0) p.sendSystemMessage(Component.literal("§a§l" + came + " eski dostun yardımına koştu!"));
    }

    private static void helper(ServerPlayer p, Race race, NpcRole role, Kingdom k) {
        BlockPos at = ground(p.serverLevel(), p.blockPosition(), 5);
        if (at == null) return;
        RpgNpc n = NpcFactory.spawn(p.serverLevel(), at, race, p.getRandom().nextInt(3) == 0, role, k.ordinal(), null, null);
        if (n == null) return;
        n.setLeader(p.getUUID());
        n.setFlag(RpgNpc.F_HELPER, true);
        n.setFlag(RpgNpc.F_FOLLOW, true);
        n.leaveAt = p.level().getGameTime() + 20 * 60 * 6;
        FX.column(p.level(), FX.dust(k.color, 1.5F), n.position(), 2.5, 0.6, 30);
    }
}
