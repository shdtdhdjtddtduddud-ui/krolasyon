package com.krolasyon.elvenrealm.dialogue;

import com.krolasyon.elvenrealm.entity.ElfKind;
import net.minecraft.util.RandomSource;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Her elfin kendine has konuşma tarzıyla yazılmış repliklerİ. {p} = oyuncunun adı. */
public final class Lines {
    private Lines() {}

    private static final Map<ElfKind, Map<String, String[]>> L = new EnumMap<>(ElfKind.class);

    private static void put(ElfKind k, String key, String... lines) {
        L.computeIfAbsent(k, x -> new HashMap<>()).put(key, lines);
    }

    public static String get(ElfKind k, String key, RandomSource r) {
        String[] arr = L.getOrDefault(k, Map.of()).get(key);
        if (arr == null || arr.length == 0) arr = L.get(ElfKind.GUARD).getOrDefault(key, new String[]{"..."});
        return arr[r.nextInt(arr.length)];
    }

    public static boolean has(ElfKind k, String key) {
        return L.getOrDefault(k, Map.of()).containsKey(key);
    }

    static {
        // ------------------------------------------------------------------ LIRAEL: sıcak, esprili, biraz cilveli kılıç dansçısı
        ElfKind k = ElfKind.LIRAEL;
        put(k, "greet", "Ooo, yeni bir yüz! Sylvaren'e hoş geldin tatlım. Kılıcına mı bakıyorsun, yoksa bana mı? Ha!",
                "Gel gel, çekinme. Burada sadece ağaçlar ısırır, onlar da sadece dolunayda.",
                "{p}! Tam zamanında. Biri bana kahve getirseydi daha iyi olurdu ama sen de olursun.");
        put(k, "greet_friend", "{p}! Kalbim kılıcımdan hızlı çarptı, biliyor musun? Ne yapıyoruz bugün?",
                "Benim en sevdiğim insan geldi! Söyleme kimseye, Thalion kıskanır.");
        put(k, "about", "Ben Lirael. Sylvaren'in en hızlı kılıcı, en tatlı gülüşü ve en kötü aşçısı. Son ikisi tartışmaya açık. "
                + "Dans eder gibi dövüşürüm; düşmanlarım genelde müziği beğenmez.");
        put(k, "city", "Sylvaren, iki Kadim Ağaç'ın kökleri arasında kuruldu. Ağaçlar kenti korur, biz de ağaçları. "
                + "Bir de şelaleler var; geceleri sesleri ninni gibi gelir. Tabii Tavi horlamıyorsa.");
        put(k, "gossip", "Vaelor mu? Ah, o kara bulut. Aslında yumuşak kalpli, ama söylersen seni bir gölgeye gömer.",
                "Caelan bir kez bana şiir yazdı. Kafiyesi 'kılıç' ile 'ılıç'tı. Ne demek ılıç? Hâlâ bilmiyorum.",
                "Nimrae'nin yanında herkes sakinleşir. Ben hariç. Ben hiç sakinleşmem!");
        put(k, "gift_thanks", "Tatlı böğürtlen! Beni benden iyi tanıyorsun. Şimdi kılıcımla değil, kalbimle teşekkür ediyorum.");
        put(k, "gift_wrong", "Bu... düşünceli bir şey. Ama bana tatlı bir şey getirsen daha çok gülerdim, bilesin.");
        put(k, "gift_already", "Bugün beni yeterince şımarttın. Yarın yine dene, söz veriyorum yine sevineceğim.");
        put(k, "hire_refuse", "Seninle yola çıkmak mı? Önce dostluğumuzu kanıtla tatlım. Kılıcım herkese eşlik etmez.");
        put(k, "hire_accept", "Anlaştık! Önden git, ben arkanı kollarım. Ve evet, kafiyeli savaş naraları atacağım.");
        put(k, "hire_extend", "Bir gün daha mı? Beni bu kadar çok sevdiğini bilmiyordum. Peki peki, kalıyorum!");
        put(k, "hire_dismiss", "Pekâlâ, Sylvaren'e dönüyorum. Ama beni özleyeceksin, buna kılıcım üzerine yemin ederim.");
        put(k, "hire_end", "Anlaşmamızın süresi doldu tatlım. Beni yine istersen zümrütlerini parlat!");
        put(k, "recovering", "Ay, yaralarım hâlâ sızlıyor... Bugün dans edemem. Yarın gel, olur mu?");
        put(k, "farewell", "Görüşürüz! Kılıcını kuru tut, kalbini sıcak.", "Hoşça kal tatlım. Ağaçlar seninle olsun!");
        put(k, "chatter", "Şu çiçeğe bak! Ah, ezdin. Neyse, onu da severdim.", "Bir gün bir ejderhayı tek vuruşla kestim. Tamam, kertenkeleydi.",
                "Yürürken şarkı söyleyebilir miyim? Söylüyorum zaten, sormuyordum.", "Arkanı ben kollarım, sen de saçlarımı kolla. Rüzgar bugün çok edepsiz.",
                "Sence Vaelor gülse yüzü çatlar mı? Bir gün deneyeceğim.");
        put(k, "combat", "Dans vakti!", "Hadi bakalım, kim önce düşecek?");
        put(k, "hurt", "Ah! Bu iz kalacak... Neyse, iz yakışır bana.");
        put(k, "night", "Gece çöktü. Yıldızlar benim kadar parlak değil ama idare eder.");
        put(k, "approach", "Hey sen! Evet sen, yakışıklı yabancı!", "Bir dans ister misin? Kılıçlı olanından!");

        // ------------------------------------------------------------------ VAELOR: soğuk, kısa, kibirli kara elf
        k = ElfKind.VAELOR;
        put(k, "greet", "İnsan. Ne istiyorsun?", "Gölgemde durma. Konuş ya da git.", "Yine sen. Sabrım gölgem kadar kısa.");
        put(k, "greet_friend", "{p}. ...Gelmene sevindim. Kimseye söyleme.", "Sen. Bugün kimi ortadan kaldırıyoruz?");
        put(k, "about", "Vaelor. Kara elf. Sylvaren'in ışığı gözlerimi yakar ama burada borcum var. "
                + "Kimse gölgede ne olduğunu görmez. Ben gölgeyim.");
        put(k, "city", "Işık ve yaprak. Şarkılar ve şelaleler. Güzel. Kırılgan. Biri bu kenti korumalı ve bu iş temiz değil.");
        put(k, "gossip", "Lirael fazla konuşur. Ama kılıcı... konuşmaz. Saygı duyuyorum.",
                "Thalion beni izliyor. Güvenmiyor. Akıllıca.", "Tavi. Gürültülü çocuk. Onu korumak zorunda kalmam umarım.");
        put(k, "gift_thanks", "Ametist. Karanlıkta parlıyor. ...Teşekkürler. Bunu unutmayacağım.");
        put(k, "gift_wrong", "Bu nedir? Sakla. Bana ışık değil, karanlıkta parlayan bir şey getir.");
        put(k, "gift_already", "Bir hediye yeter. İkincisi şüphe uyandırır.");
        put(k, "hire_refuse", "Kılıcım ucuz değil, güvenim daha da pahalı. Önce kendini kanıtla.");
        put(k, "hire_accept", "Ödeme kabul edildi. Yürü. Arkanda olacağım, sen beni görmesen de.");
        put(k, "hire_extend", "Bir gün daha. Zümrütler gölgede de parlar.");
        put(k, "hire_dismiss", "Anlaşma bitti. Gölgeler beni çağırıyor.");
        put(k, "hire_end", "Süre doldu. Bedavaya çalışmam. Hoşça kal, insan.");
        put(k, "recovering", "Yaralıyım. Bir kara elf yarasını kimseye göstermez. Git.");
        put(k, "farewell", "Git.", "Gölgende dikkatli ol.");
        put(k, "chatter", "Sessiz ol. Bir şey duyuyorum.", "Güneş. Nefret ediyorum.", "Arkanı kolluyorum. Teşekkür etme.",
                "Ölmemeye çalış. Cesedini taşımak istemem.", "Bir zamanlar bir kral beni kiraladı. Artık kral değil.");
        put(k, "combat", "Ölüm sessizdir.", "Gölgeden...");
        put(k, "hurt", "Tch. Kan. Benimki.");
        put(k, "night", "Gece. Nihayet.");
        put(k, "approach", "...", "Bakma.");

        // ------------------------------------------------------------------ CAELAN: şiirsel, atasözleriyle konuşan bilge yazıcı
        k = ElfKind.CAELAN;
        put(k, "greet", "Ah, bir yolcu! Eski bir söz der ki: 'Kapıyı çalan, hikâyeyi de getirir.' Seninki nedir?",
                "Hoş geldin. Mürekkep kurumadan konuşalım; kelimeler kaçar.",
                "Selam olsun, {p}. Yaprakların fısıltısında adını duymuştum.");
        put(k, "greet_friend", "Dostum {p}! Kitaplarımın arasında en sevdiğim hikâye seninki oldu.",
                "Gel, otur. Bugün sana bir şiir okuyacağım. Kaçamazsın, kapıyı kilitledim. Şaka, şaka.");
        put(k, "about", "Adım Caelan, Sylvaren'in yazıcısı. Kayıp dilleri toplar, unutulmuş rünleri okurum. "
                + "Rün işlemeli bu tunik, ilk hocamın armağanıdır. Kılıcım yok; kalemim yeter. Çoğu zaman.");
        put(k, "city", "Kadim Ağaçlar bin yıldır ayakta. Kökleri, ilk elflerin mezarlarını sarar. "
                + "Bir rivayete göre ağaçlar uyandığında Sylvaren yürüyecek. Ben bunu bir mecaz sanıyorum. Umarım.");
        put(k, "gossip", "Nimrae'nin bilgeliği benimkinden derin; o kökleri dinler, ben sayfaları.",
                "Tavi bir keresinde kitabımı ok hedefi yaptı. Ok, tam 'sabır' kelimesine saplandı. İroni.",
                "Vaelor'a bir şiir okudum. Gözünü bile kırpmadı. Belki uyuyordu.");
        put(k, "gift_thanks", "Büyülü bir kitap! Sayfalarında kim bilir hangi sırlar uyuyor... Teşekkür ederim, dostum.");
        put(k, "gift_wrong", "Nazik bir jest. Ama bilge bir söz der ki: 'Yazıcıya kitap, ozana dize verilir.'");
        put(k, "gift_already", "Bugünlük cömertliğin bir destana yeter. Yarın yeni bir sayfa açalım.");
        put(k, "hire_refuse", "Seninle yola çıkmak isterim, ama bir yazıcı tanımadığı biriyle hikâye yazmaz. Önce bağımızı güçlendirelim.");
        put(k, "hire_accept", "Pekâlâ! Kalemim kılıcın, rünlerim kalkanın olsun. Bu yolculuğu kayda geçeceğim.");
        put(k, "hire_extend", "Bir gün daha? Destanımız uzuyor. Güzel.");
        put(k, "hire_dismiss", "Bu bölüm burada bitiyor. Kitaplarım beni bekler.");
        put(k, "hire_end", "Anlaşmamızın son sayfasına geldik. Hikâyemiz yine yazılabilir, bilirsin.");
        put(k, "recovering", "Mürekkebim de kanım da biraz eksik bugün. Yarın tekrar sor.");
        put(k, "farewell", "Yolun açık, sayfaların beyaz olsun.", "Eski bir söz der ki: 'Veda, bir sonraki selamın tohumudur.'");
        put(k, "chatter", "Şu bulutun şekli, kayıp bir rüne benziyor. Ah hayır, sadece bir koyun.",
                "Biliyor muydun? Elfçede 'dost' ve 'yol' aynı kökten gelir.", "Bu yürüyüşü bir şiire dökeceğim. 'Yürüdüler, yürüdüler...' Daha üzerinde çalışıyorum.",
                "Gözlerini kapat ve dinle: rüzgar bile bir şey anlatıyor.", "Kalemim düştü mü? Hayır, cebimde. İkisi de.");
        put(k, "combat", "Rünler, uyanın!", "Kelimeler keskindir!");
        put(k, "hurt", "Ah! Bu sahne hikâyede kesilecek.");
        put(k, "night", "Gece, yazıcıların en sadık dostudur.");
        put(k, "approach", "Merhaba, yolcu!", "Bir hikâye mi arıyorsun?");

        // ------------------------------------------------------------------ NIMRAE: dingin, mistik ay rahibesi
        k = ElfKind.NIMRAE;
        put(k, "greet", "Ay ışığı yolunu aydınlatsın, yolcu. Kökler geleceğini fısıldamıştı.",
                "Nefes al... Orman seninle birlikte nefes alıyor. Hoş geldin.",
                "{p}. Ruhun yorgun görünüyor. Otur, biraz dinlen.");
        put(k, "greet_friend", "Sevgili {p}, ışığın bugün daha parlak. Kökler seni tanıyor artık.",
                "Gel, yanımda dur. Kadim Ağaçlar sana selam yolluyor.");
        put(k, "about", "Ben Nimrae, Ay'ın rahibesi ve Kadim Ağaçların bekçisiyim. Yaraları sararım, ruhları dinlerim. "
                + "Gücüm benden değil; ormanın kalbinden gelir.");
        put(k, "city", "Sylvaren'in kalbi kalede değil, ağaçların köklerinde atar. Her şelale bir dua, her yaprak bir hatıradır.");
        put(k, "gossip", "Lirael'in ruhu bir kıvılcım gibi; sönmesin diye dua ediyorum.",
                "Vaelor'un içinde derin bir yara var. Bir gün onu da iyileştireceğim. Henüz istemiyor.",
                "Thalion herkesi korur, ama onu kim korur? Ay korur. Ve ben.");
        put(k, "gift_thanks", "Bir müge çiçeği... Ay'ın en sevdiği. Ruhuna teşekkür ederim, yolcu.");
        put(k, "gift_wrong", "Niyetin güzel. Ama Ay, beyaz çiçekleri sever. Bunu hatırla.");
        put(k, "gift_already", "Bugünün armağanı kabul edildi. Ay yarın yeniden doğacak.");
        put(k, "hire_refuse", "Kökler henüz seni tanımıyor. Ormana hizmet et, o zaman yolumuz birleşsin.");
        put(k, "hire_accept", "Işığım seninle olsun. Yaralarını ben sararım, sen yolunu yürü.");
        put(k, "hire_extend", "Bir gün daha... Ay da razı.");
        put(k, "hire_dismiss", "Ağaçlarıma dönüyorum. Ruhun hafif olsun.");
        put(k, "hire_end", "Ay döngüsü tamamlandı. Yollarımız şimdilik ayrılıyor.");
        put(k, "recovering", "Işığım sönük. Kökler beni iyileştirene kadar bekle.");
        put(k, "farewell", "Ay ışığı seninle olsun.", "Kökler seni korusun, yolcu.");
        put(k, "chatter", "Şşş... Bir kelebeğin kanat sesi. Duydun mu?", "Ruhun bugün daha hafif. Güzel.",
                "Toprak burada üzgün. Bir fidan dikmeliyiz.", "Gökyüzünü izle. Ay yakında doğacak.", "Nefes al... ve bırak.");
        put(k, "combat", "Ay, koru bizi!", "Kökler, bağlayın!");
        put(k, "hurt", "Işığım titriyor...");
        put(k, "night", "Ay doğdu. Gücüm tazelendi.");
        put(k, "approach", "Huzur seninle olsun.", "Hoş geldin, ruh.");

        // ------------------------------------------------------------------ TAVI: enerjik, şakacı genç okçu
        k = ElfKind.TAVI;
        put(k, "greet", "Heyy! Sen de kimsin? Pıtırcık seni sevdi bak, gagaladı!",
                "Vay be, bir insan! Gerçekten iki kulağınız da yuvarlak mı? Komik!",
                "Yo yo {p}! Ok atmayı bilir misin? Bilmezsen öğretirim, para yok, söz!");
        put(k, "greet_friend", "{p}! En iyi kankam! Pıtırcık da selam diyor, cik cik!",
                "Geldin geldin! Bugün bir maceraya çıkıyoruz değil mi? DEĞİL Mİ?");
        put(k, "about", "Ben Tavi! Sylvaren'in en genç okçusu, Pıtırcık'ın en iyi arkadaşı. Thalion'un çırağıyım ama bunu ona söyleme, şımarır. "
                + "Elma başından vururum! Tamam, bir kere Caelan'ın kitabını vurdum ama sayılmaz.");
        put(k, "city", "Sylvaren süper! En yüksek dala tırmanırsan bütün ormanı görürsün. Ben yasak olduğu için hiç tırmanmadım. Yüz kere.");
        put(k, "gossip", "Thalion çok ciddi ya! Bir kere güldüğünü gördüm, galiba hapşırıyordu.",
                "Vaelor korkunç ama Pıtırcık'a ekmek kırıntısı veriyor, gördüm! Sır, tamam mı?",
                "Lirael bana kılıç dersi verdi. Bacağıma vurdum. Kendi bacağıma.");
        put(k, "gift_thanks", "KURABİYE! Sen bir efsanesin! Pıtırcık'a da veriyorum... yarısını. Çeyreğini.");
        put(k, "gift_wrong", "Hmm, bu yenmiyor ki? Kurabiye getirsene, kurabiye!");
        put(k, "gift_already", "Bugün yeterince kurabiye yedim... şaka, asla yeterli değil. Ama Thalion kızar.");
        put(k, "hire_refuse", "Seninle maceraya mı? Süper olur! Ama Thalion önce bana kendini kanıtlaman gerektiğini söyledi. Kurallar işte, sıkıcı.");
        put(k, "hire_accept", "YAŞASIN! Macera! Pıtırcık, toplan, gidiyoruz! Oklarım hazır!");
        put(k, "hire_extend", "Bir gün daha mı? Evet evet evet!");
        put(k, "hire_dismiss", "Ohh, bitti mi? Peki... Ama yine çağır, tamam mı? TAMAM MI?");
        put(k, "hire_end", "Zaman doldu! Thalion beni arıyordur. Görüşürüz kanka!");
        put(k, "recovering", "Ayy, her yerim acıyor. Nimrae iksir içirdi, berbat tadı var. Yarın gel!");
        put(k, "farewell", "Görüşürüz! Pıtırcık da el sallıyor. Kanatla.", "Bay bay! Ok gibi git!");
        put(k, "chatter", "Şunu gördün mü? Bir sincap! Hayır, kaçtı.", "Bahse girerim şu ağacı buradan vururum. Bak! ...Iska. Rüzgar yüzünden.",
                "Pıtırcık acıktı. Ben de acıktım. Hepimiz acıktık.", "Thalion bana 'sabırlı ol' diyor. Ne kadar sabırlı? Ne zaman bitiyor sabır?",
                "Sen hiç ejderha gördün mü? Ben görmedim. Ama görsem vururdum!");
        put(k, "combat", "Hedefi gördüm!", "Bu benim, bu benim!");
        put(k, "hurt", "Ayy! Bu acıttı!");
        put(k, "night", "Karanlıkta da nişan alırım ama Pıtırcık uyumak istiyor.");
        put(k, "approach", "Heyy!", "Sen! Ok atmayı bilir misin?");

        // ------------------------------------------------------------------ THALION: askeri, kısa, otoriter korucu komutanı
        k = ElfKind.THALION;
        put(k, "greet", "Dur. Kimliğini söyle, yabancı.", "Sınırlar huzursuz. Kısa konuş.", "{p}. Rapor ver.");
        put(k, "greet_friend", "{p}. İyi iş çıkardın. Sylvaren sana güveniyor. Ben de.", "Asker. Hazır mısın?");
        put(k, "about", "Thalion. Sylvaren korucularının komutanı. Ormanın sınırlarını, yağmacılardan ve karanlıktan korurum. "
                + "Konuşmayı değil, nişan almayı severim.");
        put(k, "city", "Duvarlar sağlam, kapılar tutar. Ama asıl savunma ormanın kendisi. Her ağaç bir gözcü.");
        put(k, "gossip", "Tavi yetenekli. Ama sabırsız. Zamanla düzelir. Belki.", "Vaelor tehlikeli. Ama bizim tarafımızda. Şimdilik.",
                "Lirael iyi bir savaşçı. Fazla konuşuyor. Bunu yazma, Caelan.");
        put(k, "gift_thanks", "Pişmiş et. Sahra yemeği. ...Teşekkürler, asker.");
        put(k, "gift_wrong", "Bunu ne yapayım? Korucuya erzak getirilir.");
        put(k, "gift_already", "Bir kez yeter. Disiplin.");
        put(k, "hire_refuse", "Korucular yabancılarla yürümez. Görevlerini tamamla. Sonra konuşuruz.");
        put(k, "hire_accept", "Anlaşıldı. Öncü sensin, gözcü benim. Hareket.");
        put(k, "hire_extend", "Görev uzatıldı. Devam.");
        put(k, "hire_dismiss", "Görev tamamlandı. Karakola dönüyorum.");
        put(k, "hire_end", "Sözleşme sona erdi. Sınırlar beni bekliyor.");
        put(k, "recovering", "Yaralıyım. Korucu yarasıyla da nöbet tutar ama bugün değil.");
        put(k, "farewell", "Gözünü dört aç.", "Ayrılabilirsin.");
        put(k, "chatter", "İz var. Taze. Dikkat.", "Rüzgar yönü değişti.", "Sessiz yürü. Ormanın kulakları var.",
                "Oklarını say. Her zaman.", "Bu yol pusuya müsait. Sağ kanadı kolla.");
        put(k, "combat", "Hedef! Ateş!", "Yerinizi koruyun!");
        put(k, "hurt", "Yara aldım. Önemli değil.");
        put(k, "night", "Gece nöbeti başladı.");
        put(k, "approach", "Dur, yabancı.", "Kimsin?");

        // ------------------------------------------------------------------ MUHAFIZ
        k = ElfKind.GUARD;
        put(k, "greet", "Sylvaren'e hoş geldin, yabancı. Kurallara uy, ağaçlara saygı göster.",
                "Kent huzurlu. Böyle kalması için buradayız.", "Kadim Ağaçların gölgesinde barış hüküm sürer.");
        put(k, "greet_friend", "Dost {p}, Sylvaren seni hatırlıyor.");
        put(k, "about", "Ben Sylvaren muhafızıyım. Adımı bilmene gerek yok; kalkanımı bil yeter.");
        put(k, "city", "Komutanımız Thalion. Bilge Caelan kütüphanede, Rahibe Nimrae kale bahçesinde. Lirael'i bulmak kolay: kahkahayı takip et.");
        put(k, "gossip", "Kara elf Vaelor'dan uzak dur. Ya da durma. Ben uzak duruyorum.");
        put(k, "farewell", "Yolun açık olsun.");
        put(k, "chatter", "Nöbet.");
        put(k, "combat", "Kenti savunun!");
        put(k, "hurt", "Geri çekilmeyeceğim!");
        put(k, "night", "Meşaleleri yakın.");
        put(k, "approach", "Selam, yolcu.");
        put(k, "hire_refuse", "Muhafızlar kiralanmaz. Kenti korumakla görevliyiz.");
        put(k, "recovering", "...");
        put(k, "gift_thanks", "Teşekkürler.");
        put(k, "gift_wrong", "Teşekkürler.");
        put(k, "gift_already", "Teşekkürler.");
    }
}
