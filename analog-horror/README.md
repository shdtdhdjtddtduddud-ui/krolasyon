# Analog Korku: Şapkalı Adam (The Hatman)

Minecraft **Forge 1.20.1** ve **Forge 1.21.1** için analog-horror modu. Uzun, kuru, ıslak-siyah derili bir yaratık; yüzünü hiç tam göremediğin geniş hasır bir şapka. Ona bakarken kıpırdamaz, gözünü ayırdığında yaklaşır.

## Kurulum
| Minecraft | Forge | Gerekli |
|---|---|---|
| 1.20.1 | 47.2+ | GeckoLib 4.4+ (1.20.1) |
| 1.21.1 | 52.1+ | GeckoLib 4.5+ (1.21.1) |

Jar dosyaları her CI derlemesinde GitHub Releases'a (`horror-<no>-<sürüm>`) yüklenir.

## Yaratık
- 240 parçalı GeckoLib modeli. Kaburgalar, soluk göğüs kemiği, köprücük ve kürek kemikleri, boğumlu omurga, üç eklemli çatal parmaklar ve tırnaklar var. Şapka örgü dokulu, bantlı ve kenarı kıvrık; ayakta eski deri loafer'lar.
- Fotoğraftaki haliyle 4 kat yoğun (birim başına 4 piksel) yüzey dokusu: ıslak parlak deri, kir, damar ve kırışıklıklar.
- 19 animasyon:
  - **Hareket:** bekleme, yürüme, koşma, sürünme (hareketli ve durağan), sinsi takip, sıçrama.
  - **İzlenirken:** fotoğraftaki donma pozu, köşeden bakma.
  - **Saldırı:** sağ ve sol pençe, yakalayıp yere çarpma, çığlık.
  - **Ürkütücü hareketler:** kafayı 360° çeviren seğirme, şapka selamı, etrafı koklama, meşale söndürme.
  - **Doğuş ve ölüm:** yerden açılarak doğma, ölüm (şapka düşer).

## Yapay zeka
- **Sinsi takip:** Oyuncunun göremediği, tercihen arkasında ve karanlık yerlerden yol bulur. Saldırganlığı arttıkça mesafeyi kapatır.
- **İzlenince donar:** Bakış konisi ve görüş hattıyla kontrol edilir. Zifiri karanlıkta ve uzaktaysa "görülmüş" sayılmaz.
  - Gözünü kırpıp başka yere bakarsan bazen bir anda daha yakında belirir.
  - Çok uzun bakarsan şapkasını selamlar, kaybolur ya da çığlık atıp saldırır.
- **Av:** Koşar, 5-11 blok mesafeden sıçrar, iki pençeyle sırayla vurur. Bazen oyuncuyu yakalayıp havaya kaldırır ve yere çarpar (karanlık ve yavaşlık verir).
- **Takılırsa:** Alçak geçitlerde sürünmeye geçer, açıldığında tekrar ayağa kalkar. Hiç yol bulamazsa paraziti arkasında bırakıp gölgeye ışınlanır.
- **Arama:** Oyuncuyu kaybedince son görüldüğü yere gider ve etrafı koklar.
- **Duyma:** Koşan ya da çok yakındaki oyuncunun yerini görmeden bilir.
- **Ortam oyunları:** Oyuncunun arkasında kapı vurma, ayak sesi, fısıltı ve uzak çağrılar. Görülmediğinde meşale, fener ve mumları söndürür (mobGriefing açıksa).
- **Vur-kaç:** Yeterince kan döktüğünde karanlığa çekilir. Canı azalınca bir kez kaçıp iyileşir.
- **Gün ışığı:** Açık gündüz ışığında oyalanmaz.
- **Musallat sistemi:** Normal doğma yerine her oyuncu için bir "dehşet" sayacı tutulur. Gece ya da karanlık derinliklerde bir süre sessizlikten sonra, görüş alanı dışında 26-40 blok uzakta tek bir yaratık doğar. Bir oyuncunun yanında aynı anda en fazla bir tane olur.

## Ses ve ekran efektleri
- Prosedürel olarak üretilmiş 29 ses: nefes ve çene tıkırtısı, kemik çıtırtıları, çığlık, ölüm feryadı, kalp atışı, VHS parazit, jumpscare, kapı vurma, fısıltı, gerilim dronu.
- **VHS katmanı:** Yaratık yaklaştıkça kenarlar kararır, ardından tarama çizgileri, kayan iz bantları, renk yırtılmaları ve parazit gelir.
- Gerilim arttıkça hızlanan kalp atışı, kovalamaca dronu ve kamera titremesi.
- Yakından göz göze gelince tam ekran jumpscare karesi.

## Hikâye modu: ŞAPKALI ADAM: KİRA GÜNÜ / THE HATMAN: RENT DUE
Tek oyunculu, doğrusal bir korku hikâyesi. Tamamı Ardel evinde geçer. Başlatmak için `/hatman hikaye` (ya da `/hatman story`), çıkmak için `/hatman stop` yazılır. Hikâye kendi evini bulunduğun yerin önüne kurar, seni macera moduna alır ve geceyi, yağmuru sabitler.

**Konu:** 2 Kasım 2026. Bir emlakçı, 1998'den beri mühürlü Ardel evindeki aile kasetlerini dijitale aktarman için seni tutar. Kasetlerde "Bay Kenar'ın Komşuluk Saati" adında, hiçbir kanalın yayınlamadığı bir çocuk programı vardır. Sunucusu hasır şapkalı, çok uzun bir adamdır. Ev her 28 yılda bir "kira" toplar ve bu gece kira günüdür.

| Bölüm | İçerik |
|---|---|
| Giriş | Sesli mesaj, eve giriş sinematiği |
| 1 — Kasetler | Mira'nın doğum günü öncesi kaseti, koridordaki ilk görüntü, depo |
| 2 — Görgü Kuralları | 3 sigorta (balon, portre, kapı çalma bulmacası), Komşuluk Saati yayını, "gözünü ayırma" bölümü |
| 3 — Doğum Günü | Mira'nın odası, mum sırası bulmacası, 3. dakikada serbest kalan avcı, parti kaseti, çatıya kaçış |
| 4 — Ölü Kanal | Victor'un atölyesi, verici kadran şifresi, itiraf kaseti, aşağı kovalamaca |
| 5 — Kira Günü | Sürpriz gerçek, oturma odası stüdyoya dönüşür, son seçim |

- 20 okunabilir not ve 4 kaset (tam ekran VHS oynatıcı).
- 9 ara sahne: sinematik kamera, siyah bantlar ve harf harf yazılan altyazılar.
- Bölüm kartları ve görev göstergesi.
- 3 son: **Yeni Sunucu**, **Tahliye** ve en az 16 notla açılan gizli **Gönderene İade**.
- Yakalanınca ölmezsin: jumpscare, karartma ve son kayıt noktasına dönüş.
- Tüm metinler Türkçe ve İngilizce; oyunun dili ne ise o görünür.
- Tahmini oynanış süresi, keşfe ve notları okumaya göre yaklaşık 1,5–2 saattir.

## Geliştirme
- `gen/`: model (`hatman.py`), dokular (`paint_hatman.py`), animasyonlar (`hatman_anim.py`) ve sesler (`hsounds.py`). `python3 gen/build.py` ile varlıklar üretilir.
- `common/`: iki sürümün ortak Java kodu ve varlıkları.
- `forge-1.20.1/` ve `forge-1.21.1/`: sürüme özel ince uyumluluk katmanı (`compat/`) ve Gradle projeleri.
- CI (`.github/workflows/horror.yml`) iki sürümü derler. Başsız istemcide koridor sahnesini oynatıp ekran görüntüsü alır ve görüntüyü kaydeder.
- `trailer/trailer.py`: Bu kayıttan VHS "bulunan kaset" tanıtım videosunu kurgular ve sentezlenmiş sesle birleştirir.
