# ⚽ Rabona Arena — Minecraft 1.20.1 Forge Futbol Modu

Minecraft'a tam bir futbol deneyimi getirir: kendi fizik motoruyla çalışan top, **57 farklı hareket**
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
| **R** (basılı tut) | Şut — basılı tuttukça güç dolar, kırmızı bölgede kontrol bozulur. Top havadaysa otomatik olarak kafa / vole / rövaşata / akrep vuruşu yapılır |
| **V** | Kısa pas (Shift: ara pas) |
| **B** | Havadan pas (Shift: orta) |
| **Z** | Top çalma (koşarken veya Shift: kayarak müdahale) |
| **G H J N** | Çalım yuvaları (Hareketler menüsünden istediğiniz hareketi atayın) |
| **U** | Özel yetenek |
| **Y** | Şut stilini değiştir (Sert, Plase, Aşırtma, Trivela, Knuckleball, Rabona, Panenka, Burun) |
| **I** | Gol sevinci |
| **K** | Hareketler menüsü (3B önizleme, tuşa atama, deneme) |
| **M** | Maç menüsü |
| A / D | Plase, elastiko, croqueta gibi hareketlerin yönünü seçer |

Topa sol tıklamak hızlı vuruş, sağ tıklamak sektirme, Shift + sağ tık topu almaktır.

## Hareketler (57)
- **Pas (7):** Kısa Pas, Ara Pas, Havadan Pas, Orta, Topuk Pası, Bakmadan Pas, Rabona Orta
- **Şut (8):** Sert Şut, Plase, Aşırtma, Trivela, Knuckleball, Rabona Şut, Panenka, Burun Vuruşu
- **Hava (6):** Kafa, Balıklama Kafa, Vole, Rövaşata, Akrep Vuruşu, Göğüsle Kontrol
- **Çalım (15):** Gökkuşağı, Elastico, Marsilya Dönüşü, Makas, Cruyff Dönüşü, Topu Geri Çekme, La Croqueta,
  Sombrero, Bacak Arası, Ronaldo Kesişi, Topuk Aşırtma, Fok Çalımı, Dünya Turu, Hız Patlaması, Sahte Şut
- **Savunma (5):** Top Çalma, Kayarak Müdahale, Omuz Omuza, Şut Bloğu, Pres Koşusu
- **Kaleci (4):** Sol/Sağ Plonjon, Degaj, Elle Oyun Kurma
- **Gol Sevinci (6):** Siuuu!, Diz Kayma, Ters Takla, Uçak, Zafer Dansı, Sus İşareti
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
