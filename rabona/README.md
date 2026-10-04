# ⚽ Rabona Arena — Minecraft 1.20.1 Forge Futbol Modu

Minecraft'a tam bir futbol deneyimi getirir: kendi fizik motoruyla çalışan top, **60 farklı hareket**
(pas, şut, hava topu, çalım, savunma, kaleci, gol sevinci, özel yetenek), her biri için ayrı animasyon,
iki takım, akıllı botlar, tek tıkla kurulan stadyum ve maç arayüzü.

**Gereksinim:** Minecraft 1.20.1 + Forge 47.x. JAR dosyasını `.minecraft/mods` klasörüne atın.
Mod hem tek oyunculu hem sunucuda çalışır. Sunucuda modun sunucuya ve oyunculara kurulu olması gerekir.

## Hızlı başlangıç
1. Yaratıcı menüde **Rabona Arena** sekmesini açın.
2. **Stadyum Kurucu** ile düz bir zemine sağ tıklayın (ya da `M` → *Stadyum ↔ / ↕*). Çizgili çim, saha çizgileri,
   ceza sahaları, fileli kaleler, korner bayrakları, renkli tribünler, LED panolar ve ışık kuleleri kurulur.
3. `M` ile **Maç Menüsü**nü açın → takımınızı seçin → *Botlarla doldur* → *Başlat*.
4. Başka bir oyuncu sunucuya girdiğinde o da `M` menüsünden **diğer takımı** seçebilir. Boş yerleri botlar doldurur.

## Kontroller (Ayarlar → Kontroller → *Rabona Arena* altından değiştirilebilir)
| Tuş | İşlev |
|---|---|
| **R / C / X** (basılı tut) | 3 şut yuvası (varsayılan: Sert, Plase, Trivela). Basılı tuttukça güç dolar. Top havadaysa otomatik kafa / vole / rövaşata / akrep |
| **V** | Kısa pas (Shift: ara pas) |
| **B** | Havadan pas (Shift: orta) |
| **Z** | Top çalma (koşarken veya Shift: kayarak müdahale — 6 sn bekleme süresi) |
| **G / H / J** | 3 hareket yuvası (varsayılan: Vücut Çalımı, Elastico, Gökkuşağı) |
| **N** | Pas iste — topu süren takım arkadaşın (bot) sana pas atar |
| **U** | Özel yetenek |
| **I** | Gol sevinci (Forma çıkarma dahil) |
| **O** | Kamera modu: Normal / TV Yayını / Üstten Takip / Kuşbakışı (isteğe bağlı) |
| **L** | Son golü tekrar izle |
| **Y** | Oyuncu kartları (mağaza, paket açma, kadro) |
| **K** | Hareketler menüsü (3B önizleme, yuvalara atama; 2. oyuncu kendi hareketlerini ayrı seçer) |
| **P** | Oyuncu değişikliği (kumandada **R3** — sağ çubuğa bas) |
| **M** | Maç menüsü (takım, mevki, botlar, süre, zorluk, stadyum) |

Futbol kameralarında hareket kameraya göredir (W = ekranın yukarısı), oyuncun koştuğu yöne döner.

## Mevkiler ve yapay zekâ
Maç menüsünden mevkini seç (Kaleci, Bek, Stoper, DOS, Orta Saha, Kanat, OOS, Açık, Santrafor). Botlar
takım büyüklüğüne göre gerçek dizilişle (ör. 11 kişide 4-3-3) yerleşir ve mevkisine göre oynar:
paslaşarak oyun kurar, boş adamı arar, forvetler derin koşu yapar, bekler bindirir, savunmada bir oyuncu
pres yapıp diğeri kapatırken kalanlar bölge savunması ve markaj yapar. Botlar oyuncudan hızlı koşamaz.

## Jeton ve oyuncu kartları
Gol +50, asist +30, galibiyet +150, beraberlik +70, maçın yıldızı +60 jeton. `Y` menüsünden Bronz / Gümüş /
Altın / Efsane paketleri aç; kartlarda Güç, Mevki, HIZ-ŞUT-PAS-DRİ-DEF-FİZ değerleri var. Kadrona aldığın
kartlar (en fazla 11) takımındaki botlar olur — isimleri, güçleri ve istatistikleri karttan gelir.

## Gol tekrarı ve taraftarlar
Her golden sonra son anlar sinematik kamerayla, sonu ağır çekimle tekrar oynatılır (Maç menüsünden kapatılabilir,
`L` ile istediğin zaman tekrar izle). Tribünler taraftarla dolu: tezahürat, Meksika dalgası, gollerde zıplayan taraftarlar.

## Hareketler (59 + pas isteme)
- **Pas (7):** Kısa Pas, Ara Pas, Havadan Pas, Orta, Topuk Pası, Bakmadan Pas, Rabona Orta
- **Şut (9):** Sert Şut, Plase, Aşırtma, Trivela (dış küple, ters falso), Ölü Yaprak (düşen serbest vuruş), Knuckleball, Rabona Şut, Panenka, Burun Vuruşu
- **Hava (6):** Kafa, Balıklama Kafa, Vole, Rövaşata, Akrep Vuruşu, Göğüsle Kontrol
- **Çalım (16):** Gökkuşağı, Elastico, Marsilya Dönüşü, Makas, Cruyff Dönüşü, Topu Geri Çekme, La Croqueta,
  Sombrero, Bacak Arası, Ronaldo Kesişi, Topuk Aşırtma, Fok Çalımı, Dünya Turu, Hız Patlaması, Sahte Şut, Vücut Çalımı
- **Savunma (5):** Top Çalma, Kayarak Müdahale, Omuz Omuza, Şut Bloğu, Pres Koşusu
- **Kaleci (4):** Sol/Sağ Plonjon, Degaj, Elle Oyun Kurma
- **Gol Sevinci (7):** Siuuu!, Diz Kayma, Ters Takla, Uçak, Zafer Dansı, Sus İşareti, Formayı Çıkar
- **Özel Yetenek (6):** Alev Vuruşu, Yıldırım Koşusu, Kasırga Şutu, Hayalet Çalım, Mıknatıs Ayak, Buz Duvarı

Çalımlar rakibin dengesini bozar (sersemletir), dayanıklılık harcar ve özel enerji kazandırır.
Özel enerji; gol, asist, başarılı çalım, top çalma ve kurtarışlarla dolar.

## Top fiziği
Yerçekimi, hava direnci, **Magnus etkisi** (falsolu şutlar gerçekten kıvrılır), topspin/backspin ile zıplama,
çim sürtünmesi, direkten metalik sekme, fileye çarpınca yavaşlama, oyuncu gövdesinden sekme ve top sürme.

## Maç sistemi
Süre, devre arası ve taraf değişimi, santra, taç, korner, kale vuruşu, gol tespiti, gol afişi, havai fişekler,
skor tabelası, mini saha radarı, maçın yıldızı. Botlar diziliş tutar, pres yapar, çalım atar, pas/şut seçer;
kaleci şutun nereye gideceğini tahmin edip plonjona atlar. Zorluk: Kolay / Orta / Zor.

## Komutlar
`/rabona takim <kirmizi|mavi|izleyici>` · `/rabona stadyum` · `/rabona basla [dakika]` · `/rabona bitir` ·
`/rabona bot doldur [kişi]` · `/rabona bot ekle <takım>` · `/rabona bot temizle` · `/rabona zorluk <0-2>` · `/rabona skor`

## Eşyalar
6 farklı top (Klasik, Altın, Cehennem, Neon, Galaksi, Rabona Pro — parlayan desenler), Stadyum Kurucu,
Hakem Düdüğü, Bot Kartları, Altın Krampon (+hız, +şut gücü) ve tüm stadyum blokları (tariflerle üretilebilir).

## Derleme
`cd rabona && gradle build` → `build/libs/rabona_arena-1.20.1-forge-2.3.0.jar`.
Dokular `gen/textures.py`, sesler `gen/sounds.py`, modeller `gen/assets.py`, dil dosyaları `gen/lang.py` ile üretilir.

## Çok oyunculu
Arkadaşınla oynamak için: tek oyunculu dünyada *LAN'a Aç* ya da bir Forge 1.20.1 sunucusuna modu kur.
Her oyuncu `M` menüsünden takımını ve mevkisini seçer; boş yerleri botlar doldurur.

## Tek bilgisayarda 2 kişilik (kumanda)
Bilgisayara 1 ya da 2 oyun kumandası (Xbox / PlayStation / genel) tak. `M` menüsünde **2. Oyuncu (kumanda)**
düğmesiyle *Rakip takım* (birbirinize karşı) ya da *Benim takımım* (birlikte) seç. Kamera otomatik olarak TV
yayınına geçer ki ikiniz de ekranda olun. İki kumanda varsa: 1. kumanda = sen, 2. kumanda = 2. oyuncu.
Tek kumanda varsa: klavye/fare = sen, kumanda = 2. oyuncu. Başlarınızın üstünde **P1 / P2** işareti çıkar.

| Kumanda | Top sendeyken | Top sende değilken |
|---|---|---|
| Sol çubuk | Koş (ekrana göre) | Koş |
| RT | Depar | Depar |
| A | Kısa pas | Pas iste |
| B (basılı tut) | Şut — RB ile Plase, LT ile Trivela | Top çalma |
| X | Havadan pas (RB: orta) | Kayarak müdahale |
| Y | Ara pas | — |
| LB | Kontrol edilen oyuncuyu değiştir | Kontrol edilen oyuncuyu değiştir |
| R3 (sağ çubuğa bas) | Oyuncu değişikliği (yedek kulübesi) | Oyuncu değişikliği |
| Sağ çubuk | **Kombo çalımlar** | — |
| D-pad ↑ / ↓ / ← / → | Yetenek / Sevinç / Hareket 1 / Hareket 2 | |
| Start / Back | Maç menüsü / Kamera | |

## Kombo çalımlar (FIFA tarzı)
Kumandada sağ çubuk, klavyede **yön okları** (↑ ileri, ↓ geri, ←/→ yanlar; ekrana göre):

| Kombo | Hareket | Kombo | Hareket |
|---|---|---|---|
| ← veya → | Vücut Çalımı | ↓ | Topu Geri Çekme |
| ↑ | Hız Patlaması | ↓ ↑ ↑ | Gökkuşağı |
| ↑ ↑ | Sombrero | ↓ ↓ | Cruyff Dönüşü |
| ← ← / → → | La Croqueta | ← → / → ← | Makas |
| ↑ ↓ | Bacak Arası | ↓ ↑ | Topuk Aşırtma |
| → ↓ / ← ↓ | Ronaldo Kesişi | ↑ ← / ↑ → | Sahte Şut |
| → ↓ ← / ← ↓ → | Elastico | ↑ → ↓ ← (tam tur) | Marsilya Dönüşü |

## Pas
Pas attığında top, hedeflenen takım arkadaşına doğru yön düzeltir ve ona ulaşacak hızda gider
(arkadaşın koşuyorsa önüne). Pası alacak bot topu karşılamaya gelir; FIFA kontrolü açıksa top ona ulaştığı an
o oyuncuyu sen yönetirsin.

## Faul, kart, serbest vuruş ve penaltı
Arkadan müdahale, topsuz adama kayarak girme ve sert omuz faul sayılır; hakem sarı ya da kırmızı kart gösterebilir
(2 sarı = kırmızı, oyuncu oyundan atılır). Faul ceza sahası dışındaysa **serbest vuruş**, içindeyse **penaltı**:
top kilitlenir, rakipler uzaklaşır, serbest vuruşta baraj kurulur. Atıcı sensen kamera FIFA'daki gibi topun
arkasına geçer, **fareyle nişan alırsın** ve seçili şut tipinin **yörünge çizgisi** (falso, ölü yaprak düşüşü dahil)
ekranda görünür. Şut tuşunu basılı tutup güç ayarla. Botlar serbest vuruşta plase, ölü yaprak, knuckleball, trivela
ya da orta seçer; kaleci penaltıda köşe tahmin eder.

## Kadro, menajer ve taktik
`Y` → **Kadro** sekmesi: sahada ilk 11'i (menajerin dizilişine göre) ve 7 yedeği görürsün. Bir mevkiye tıkla, sonra başka
mevkiye (yer değiştir) ya da sağdaki karta (kadroya al) tıkla; sağ tık çıkarır. *Otomatik kadro* en iyi 11'i kurar.
**Menajer** sekmesinden menajer paketi al (⛁350). Her menajerin bir taktiği ve dizilişi var; botlar buna göre oynar:

| Taktik | Diziliş | Oyun planı |
|---|---|---|
| Dengeli | 4-3-3 | Karışık pas, orta hat |
| Tiki-Taka | 4-3-3 | Kısa paslar, topa sahip olma, ikili pres |
| Gegenpress | 4-2-3-1 | 3 kişiyle anında pres, çok önde savunma |
| Kontra Atak | 4-4-1-1 | Derinde bekle, hızlı dikine oyun, forvet önde |
| Otobüsü Çek | 5-4-1 | Dar, kompakt, derin savunma |
| Kanat Oyunu | 4-4-2 | Geniş saha, kanatlar çizgiye iner, orta |
| Uzun Top | 4-4-2 | Uzun toplar, orta, uzaktan şut |

Menajer puanı takım gücüne bonus verir. Botlardan oluşan rakip takımın da rastgele bir menajeri/taktiği olur (maç başında yazılır).

## Oyuncu değişikliği
`P` (kumandada **R3**) yedek kulübesini açar: solda sahadakiler ve kondisyonları, sağda yedekler. Çıkacak oyuncuyu sonra
gireni seç (maç başına 5 hak). Yedekler kadrondaki 7 yedekten gelir; yoksa kulüp yedekleri kullanılır. Bot takımları
devre arasında yorgun oyuncularını değiştirir.

## Akıcılık ve görünüm
Maç sırasında saha ve tribünlerde blok kırılamaz/koyulamaz. Taraftarlar kamera arkasındaysa ya da çok uzaktaysa çizilmez.
Futbolcular daha uzun ve ince (atletik) vücutla çizilir; bot isimleri sadece bakınca görünür, P1/P2 işaretleri küçüktür.
