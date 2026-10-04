# Krolasyon Futbol — Minecraft 1.20.1 Forge futbol modu

Fizikli top, 49 farklı hareket (her biri kendi animasyonuyla), tribünlü stadyum, çok oyunculu takım seçimi ve yapay zekalı bot oyuncular.

**Gereken:** Minecraft 1.20.1 + Forge 47.x. Jar dosyasını `mods` klasörüne at.
Hazır jar dosyası GitHub Releases sayfasında `futbol-build-N` etiketiyle yayınlanır (`krolasyon_futbol-1.20.1-forge-1.0.0.jar`).

## Hızlı başlangıç
1. Yaratıcı moddaki **Krolasyon Futbol** sekmesinden **Saha Kurucu**'yu al.
2. Düz bir alanda **Shift + sağ tık**: bulunduğun yer orta nokta olacak şekilde stadyum kurulur (65x41 saha, kaleler + file, korner bayrakları, tribünler, projektörler).
3. **J** ile maç menüsünü aç: takımını seç (Kızıl Aslanlar / Mavi Kartallar / İzleyici), takım başına oyuncu sayısını (1–11) ve süreyi ayarla, **Maçı Başlat**.
4. Eksik oyuncuları botlar tamamlar. Sunucuya giren her oyuncu kendi takımını seçebilir (girişte menü ve sohbette tıklanabilir takım butonları çıkar).

## Kontroller (Ayarlar > Kontroller'den değiştirilebilir)
| Tuş | Hareket |
|---|---|
| Topun yanına koş | Top ayağına yapışır (dribbling). **Shift**: topu ayağının altına al |
| Sol tık (topa) | Hızlı vuruş |
| **Z** (basılı tut) | Şut stili 1 · **Shift+Z** stil 2 · **koşarken Z** stil 3 (3 stil de seçilebilir) |
| **R** | Kısa pas · Shift+R: Havadan pas · koşarken: Ara pas |
| **X** | Pas iste (bot takım arkadaşı sana pas atar) |
| **G** | Top çalma · Shift/koşarken: Kayarak müdahale (5 sn bekleme süresi) |
| **V / N / M** | Seçtiğin 3 çalım hareketi |
| **B** | Süper yetenek · **H** gol sevinci |
| **K** | Kamera: Normal / TV Yayını / Üstten (FIFA). Futbol kameralarında WASD ekrana göre hareket eder |
| **O** | Son golün tekrarını izle (gol sonrası otomatik oynar, O ile geçilir) |
| **Y** | Tüm hareketler + tuş/şut stili atama (üstte slotu seç, karta sağ tıkla) |
| **J** | Maç menüsü: takım, **mevki** (KL/DEF/OS/KNT/FV), oyuncu sayısı, süre |
| **U** | Kulüp: jeton, kart paketleri, kadro |

## Mevkiler ve yapay zeka
Botlar mevkisine göre oynar: defans hattı korur ve rakibi markajlar, orta saha pas açısı yaratır, kanatlar çizgiye açılıp orta yapar, forvetler son defansın arkasına koşu yapar. Topu alan oyuncu şut, pas (ilerleme + boşluk + pas yolu açık mı) ve top sürme arasında karar verir; takımlar oyun kurar. Top rakipteyken bir oyuncu pres yapar, biri arkasını kollar, diğerleri derli toplu kalır.
Sen de mevkini seçebilirsin; **kaleci** seçersen kendi ceza sahanda topu elle tutarsın (R: at, Z: uzun vur).

## Jeton ve oyuncu kartları
Gol: +30 jeton (takımına +10), galibiyet +150, beraberlik +70, mağlubiyet +40. Kulüp ekranında Bronz (100), Altın (300) ve Efsane (800) paket açılır. Kartın mevkisi, genel gücü ve 6 özelliği (HIZ, ŞUT, PAS, DRİ, DEF, KAL) vardır. Kadroya aldığın en fazla 10 kart, takımındaki botların yerine kendi isim ve özellikleriyle oynar. İstemediğin kartı Shift+tık ile satabilirsin.

## Çok oyunculu
Mod hem istemcide hem sunucuda olmalı (aynı jar). Tek oyunculu dünyada "LAN'a Aç" diyerek ya da Forge 1.20.1 sunucusuna jar'ı `mods` klasörüne koyarak arkadaşlarınla oynayabilirsin. Giren herkes kendi takımını ve mevkisini seçer, eksik yerleri botlar doldurur.

## Hareketler (51)
- **Paslar:** Kısa Pas, Ara Pas, Havadan Pas, Topuk Pası, Rabona Pas, Bakmadan Pas, Orta
- **Şutlar:** Şut, Plase, Sert Şut, Rabona Şut, Aşırtma, Röveşata, Vole, Kafa Vuruşu, Dipten Vuruş (knuckleball), Akrep Vuruşu, Panenka, Trivela
- **Çalımlar:** Gökkuşağı, Makas, Elastico, Maradona Dönüşü, Cruyff Dönüşü, Bacak Arası, Taban Çekme, Fok Dribblingi, Top Sektirme, Şapka Çıkarma, Hız Patlaması, İlk Dokunuş, Vücut Çalımı
- **Savunma:** Top Çalma, Kayarak Müdahale, Omuz Omuza, Şut Bloğu
- **Kaleci:** Plonjon, Yumruklama, Kaleci Atışı
- **Süper yetenekler:** Ateş Şutu, Yıldırım Şutu, Kasırga Şutu, Kartal Pikesi, Hayalet Dribbling, Buz Pası
- **Gol sevinçleri:** Siuuu!, Diz Kayması, Uçak, Ters Takla, Zafer Dansı, Formayı Çıkar

Çalım hareketleri kısa bir süre "sıyrılma" penceresi açar: bu sırada yapılan müdahalelerin başarı şansı çok düşer.

## Top fiziği
Yerçekimi, hava sürtünmesi, çimde yuvarlanma sürtünmesi, zıplama (enerji kaybıyla), falso (Magnus etkisi — plase/trivela/rabona topları kıvrılır), topspin/backspin, dönmeyen titreyen knuckleball, direk ve fileye çarpma tepkileri (direkte metal sesi, filede top ölür), oyunculara çarpıp sekme.

## Maç kuralları
Başlama vuruşu geri sayımı, gol (kendi kalesine gol algılama), taç, korner ve aut atışları, skor tabelası ve süre, gol sonrası havai fişek + seyirci sesi + "GOOOL!" ekranı, maç sonu düdüğü.

## Komutlar
`/futbol saha` (op) · `/futbol baslat [dakika] [oyuncu]` · `/futbol bitir` · `/futbol takim <kirmizi|mavi|izleyici>` · `/futbol top` · `/futbol ortala` · `/futbol hareket <isim>` · `/futbol bot <takim> <adet>` (op) · `/futbol botlarisil` (op)

## Kaynaktan derleme
```
cd futbol
gradle build        # build/libs/krolasyon_futbol-1.20.1-forge-1.0.0.jar
```
Dokular `gen/textures.py`, model/dil dosyaları `gen/assets.py`, sesler `gen/sounds.py` ile üretilir.
