package com.krolasyon.elvenrealm.dialogue;

import com.krolasyon.elvenrealm.entity.ElfKind;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** Her karakterin iki görevi: toplama ya da avlama. Görevler sırayla verilir, ikisi de bitince karakter "kalbini açar". */
public final class Quests {
    public record Need(Item item, int count, boolean saplings) {
        public boolean matches(ItemStack s) {
            return saplings ? s.is(ItemTags.SAPLINGS) : s.is(item);
        }
    }

    public record Quest(String title, String offer, String progress, String done, List<Need> items, Set<EntityType<?>> targets,
                        int kills, String targetName, Supplier<List<ItemStack>> rewards) {
        public boolean hunt() {
            return kills > 0;
        }
    }

    private static final Map<ElfKind, List<Quest>> Q = new EnumMap<>(ElfKind.class);

    public static List<Quest> of(ElfKind k) {
        return Q.getOrDefault(k, List.of());
    }

    private static Need n(Item i, int c) {
        return new Need(i, c, false);
    }

    private static Supplier<List<ItemStack>> r(ItemStack... st) {
        return () -> {
            List<ItemStack> out = new ArrayList<>();
            for (ItemStack s : st) out.add(s.copy());
            return out;
        };
    }

    private static ItemStack ench(Item item, net.minecraft.world.item.enchantment.Enchantment e, int lvl) {
        ItemStack s = new ItemStack(item);
        EnchantmentHelper.setEnchantments(Map.of(e, lvl), s);
        return s;
    }

    private static ItemStack potion(net.minecraft.world.item.alchemy.Potion p) {
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), p);
    }

    private static void collect(ElfKind k, String title, String offer, String progress, String done, List<Need> needs, Supplier<List<ItemStack>> rw) {
        Q.computeIfAbsent(k, x -> new ArrayList<>()).add(new Quest(title, offer, progress, done, needs, Set.of(), 0, "", rw));
    }

    private static void hunt(ElfKind k, String title, String offer, String progress, String done, Set<EntityType<?>> targets, int count, String name,
                             Supplier<List<ItemStack>> rw) {
        Q.computeIfAbsent(k, x -> new ArrayList<>()).add(new Quest(title, offer, progress, done, List.of(), targets, count, name, rw));
    }

    static {
        collect(ElfKind.LIRAEL, "Dansçının Kılıcı",
                "Kılıcımın ağzı köreldi tatlım. Demirci Haldir yeni bir çelik döver ama malzemeyi benden ister. "
                        + "Bana altı demir külçe ve bir elmas getirirsen, sana dansçıların gizli bir hilesini öğretirim. Ve zümrüt de var, merak etme.",
                "Demirler, elmas... hâlâ bekliyorum! Ama bekleyişim bile zarif, değil mi?",
                "Oooh, bak şu parıltıya! Haldir bayılacak. Al bakalım, hak ettin. Artık sana 'tatlım' demeyi resmileştirdim.",
                List.of(n(Items.IRON_INGOT, 6), n(Items.DIAMOND, 1)), r(new ItemStack(Items.EMERALD, 18), ench(Items.IRON_SWORD, Enchantments.SWEEPING_EDGE, 3)));
        hunt(ElfKind.LIRAEL, "Ağların Ardında",
                "Batıdaki korulukta örümcekler ağ örmüş, çiçek toplayan çocuklar korkuyor. On tanesini temizle. "
                        + "Ben gelirdim ama saçıma ağ yapışıyor, anlarsın.",
                "Kaç örümcek kaldı? Saymayı unuttum, sen saydın değil mi?",
                "Koruluk yeniden güvende! Çocuklar sana çiçek taçları yapıyor. Ben de bir tane istedim, vermediler. Al ödülünü.",
                Set.of(EntityType.SPIDER, EntityType.CAVE_SPIDER), 10, "örümcek", r(new ItemStack(Items.EMERALD, 22), new ItemStack(Items.GOLDEN_APPLE, 2)));

        hunt(ElfKind.VAELOR, "Kemik Avı",
                "Sınırda kemikler yürüyor. Sekiz iskelet. Kafataslarını say, bana anlatma.",
                "Hâlâ yürüyorlar.",
                "...Yeterli. Belki işe yararsın. Al.",
                Set.of(EntityType.SKELETON, EntityType.STRAY, EntityType.WITHER_SKELETON), 8, "iskelet",
                r(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.ARROW, 32), new ItemStack(Items.ENDER_PEARL, 1)));
        collect(ElfKind.VAELOR, "Hiçliğin Gözleri",
                "Gölgeler arasında yürümek için Son'un gözleri gerekir. Dört ender incisi. Sorma neden.",
                "Dört. İnci. Sayı mı bilmiyorsun?",
                "Hm. Gözlerin keskinmiş. Bunu tut. Gece seni korur. Kimseye söyleme benden aldığını.",
                List.of(n(Items.ENDER_PEARL, 4)), r(new ItemStack(Items.EMERALD, 30), new ItemStack(Items.NETHERITE_SCRAP),
                        potion(Potions.LONG_INVISIBILITY), potion(Potions.LONG_INVISIBILITY)));

        collect(ElfKind.CAELAN, "Mürekkep ve Tüy",
                "Eski bir söz der ki: 'Yazılmayan bilgi, rüzgara söylenmiş sırdır.' Kadim Ağaç'ın tarihini yazıyorum ama "
                        + "kağıdım, tüyüm, mürekkebim bitti. Üç kitap, iki tüy ve bir mürekkep kesesi... ilham perisinin bedeli bu kadar.",
                "Sayfalar boş, tüyler uçmuş... Bekliyorum, sabırla, bir yaprağın düşüşünü bekler gibi.",
                "Ah! Mürekkebin kokusu, yeni bir sabahın kokusudur. Bu bilgi kitabını al, sana yeni yollar açsın.",
                List.of(n(Items.BOOK, 3), n(Items.FEATHER, 2), n(Items.INK_SAC, 1)),
                r(new ItemStack(Items.EMERALD, 14), new ItemStack(Items.EXPERIENCE_BOTTLE, 8), new ItemStack(Items.ENCHANTED_BOOK)));
        collect(ElfKind.CAELAN, "Derinliklerin Yankısı",
                "Derin Karanlık'ta, sessizliğin dahi uyuduğu yerde, yankı parçaları vardır. Birini bana getir. "
                        + "Bilirim, tehlikeli. Ama en güzel sözler hep tehlikeli yerlerde yazılır.",
                "Yankı parçası... Gardiyan'ın uykusunu kaçırma sakın.",
                "Duyuyor musun? Parça hâlâ fısıldıyor. Bunu ölümsüzleştireceğim. Sana bir şükran ve bu tılsımlar.",
                List.of(n(Items.ECHO_SHARD, 1)), r(new ItemStack(Items.EMERALD, 32), new ItemStack(Items.AMETHYST_SHARD, 8), new ItemStack(Items.EXPERIENCE_BOTTLE, 16)));

        collect(ElfKind.NIMRAE, "Ormanın Çocukları",
                "Kökler bana ağladıklarını söyledi; ormanın kenarı kesilmiş. On iki fidan ve sekiz kemik tozu getir. "
                        + "Onları ay ışığında toprağa vereceğiz.",
                "Fidanlar hâlâ yolda mı? Toprak sabırlıdır, ben de onun kızıyım.",
                "Toprak şükran duyuyor. Hissediyor musun? Bu kutsanmış elmalar senin olsun.",
                List.of(new Need(Items.OAK_SAPLING, 12, true), n(Items.BONE_MEAL, 8)),
                r(new ItemStack(Items.EMERALD, 12), new ItemStack(Items.GOLDEN_APPLE, 3), new ItemStack(Items.GLOW_BERRIES, 16)));
        hunt(ElfKind.NIMRAE, "Huzursuz Ölüler",
                "Ölüler toprağa dönmeli, ama bazıları dönmeyi reddediyor. On iki çürümüş ruhu huzura kavuştur.",
                "Toprak hâlâ huzursuz. Ölüler hâlâ yürüyor.",
                "Artık uyuyorlar. Ay seni korusun. Al, bu kolye karanlıkta yolunu bulsun diye.",
                Set.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.DROWNED, EntityType.ZOMBIE_VILLAGER), 12, "zombi",
                r(new ItemStack(Items.EMERALD, 24), potion(Potions.STRONG_REGENERATION), potion(Potions.STRONG_REGENERATION), new ItemStack(Items.GLOW_INK_SAC, 6)));

        collect(ElfKind.TAVI, "Ok Yağmuru",
                "Thalion bana yine okları bitirdin dedi! Ama ben sadece... çok attım. Otuz iki ok ve dört tavşan postu getirir misin? "
                        + "Postlarla yeni bir sadak yapacağım, Pıtırcık da içinde uyusun diye!",
                "Daha gelmedi mi? Pıtırcık da sabırsızlanıyor, bak gagasını takırdatıyor!",
                "YAŞASIN! Çok tatlısın! Al, bu benim ikinci en iyi yayım. Birincisini veremem, o Pıtırcık'ın tüneği.",
                List.of(n(Items.ARROW, 32), n(Items.RABBIT_HIDE, 4)),
                r(new ItemStack(Items.EMERALD, 10), ench(Items.BOW, Enchantments.POWER_ARROWS, 3)));
        hunt(ElfKind.TAVI, "Patlayan Yeşiller",
                "Şu yeşil patlayan şeyler var ya? Pıtırcık onlardan korkuyor. Altı tanesini yok edebilir misin? Uzaktan tabii, yakından değil!",
                "Patlama sesi duymadım daha... iyi bir şey mi bu yoksa kötü mü?",
                "Pıtırcık artık uçmaya korkmuyor! Al, bu barut ödülü. Biraz ironik, biliyorum, hihi.",
                Set.of(EntityType.CREEPER), 6, "creeper", r(new ItemStack(Items.EMERALD, 20), new ItemStack(Items.SPECTRAL_ARROW, 32)));

        hunt(ElfKind.THALION, "Haydut Temizliği",
                "Rapor: kuzeyde yağmacılar görüldü. Görev: on yağmacıyı etkisiz hale getir. Yağmacı, savunucu, büyücü fark etmez. Dağıl.",
                "Durum raporu yetersiz. Görev devam ediyor.",
                "Görev başarılı. Kayıplar? Yok. Güzel. Bu kalkanı al; benim birliğimde taşınır.",
                Set.of(EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER, EntityType.ILLUSIONER), 10, "yağmacı",
                r(new ItemStack(Items.EMERALD, 28), ench(Items.SHIELD, Enchantments.UNBREAKING, 3), new ItemStack(Items.COOKED_BEEF, 16)));
        collect(ElfKind.THALION, "Ölümsüzlük Mührü",
                "Korucularım için bir ölümsüzlük tılsımı gerek. Bir tane. Büyücülerden düşer. Kolay değil. O yüzden sana veriyorum.",
                "Tılsım yok. Sorun yok. Henüz.",
                "...Teşekkürler, asker. Ciddiyim. Korucular seni artık kardeş sayar. Bu zırh ve madalya senin.",
                List.of(n(Items.TOTEM_OF_UNDYING, 1)), r(new ItemStack(Items.EMERALD, 40), ench(Items.DIAMOND_CHESTPLATE, Enchantments.ALL_DAMAGE_PROTECTION, 2)));
    }

    private Quests() {}
}
