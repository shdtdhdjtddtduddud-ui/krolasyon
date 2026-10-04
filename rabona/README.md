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
| **K** | Hareketler menüsü (3B önizleme, yuvalara atama) |
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
- **Şut (8):** Sert Şut, Plase, Aşırtma, Trivela, Knuckleball, Rabona Şut, Panenka, Burun Vuruşu
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
`cd rabona && gradle build` → `build/libs/rabona_arena-1.20.1-forge-1.0.0.jar`.
Dokular `gen/textures.py`, sesler `gen/sounds.py`, modeller `gen/assets.py`, dil dosyaları `gen/lang.py` ile üretilir.

## Çok oyunculu
Arkadaşınla oynamak için: tek oyunculu dünyada *LAN'a Aç* ya da bir Forge 1.20.1 sunucusuna modu kur.
Her oyuncu `M` menüsünden takımını ve mevkisini seçer; boş yerleri botlar doldurur.
