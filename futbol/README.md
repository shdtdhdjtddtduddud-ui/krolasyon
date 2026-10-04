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
| **Z** (basılı tut) | Şut — ne kadar tutarsan o kadar sert. Shift+Z: Plase, koşarken: Sert Şut. Top havadaysa otomatik Vole / Kafa |
| **R** | Kısa pas · Shift+R: Havadan pas · koşarken: Ara pas |
| **G** | Top çalma · Shift/koşarken: Kayarak müdahale |
| **V** | Favori çalımın (varsayılan: Gökkuşağı) |
| **B** | Süper yetenek (varsayılan: Ateş Şutu) |
| **H** | Gol sevinci |
| **Y** | Tüm hareketler menüsü (sol tık: yap, sağ tık: V/B/H tuşuna ata) |
| **J** | Maç / takım menüsü |

Animasyonları görmek için **F5** ile 3. şahıs kameraya geç.

## Hareketler (49)
- **Paslar:** Kısa Pas, Ara Pas, Havadan Pas, Topuk Pası, Rabona Pas, Bakmadan Pas, Orta
- **Şutlar:** Şut, Plase, Sert Şut, Rabona Şut, Aşırtma, Röveşata, Vole, Kafa Vuruşu, Dipten Vuruş (knuckleball), Akrep Vuruşu, Panenka, Trivela
- **Çalımlar:** Gökkuşağı, Makas, Elastico, Maradona Dönüşü, Cruyff Dönüşü, Bacak Arası, Taban Çekme, Fok Dribblingi, Top Sektirme, Şapka Çıkarma, Hız Patlaması, İlk Dokunuş
- **Savunma:** Top Çalma, Kayarak Müdahale, Omuz Omuza, Şut Bloğu
- **Kaleci:** Plonjon, Yumruklama, Kaleci Atışı
- **Süper yetenekler:** Ateş Şutu, Yıldırım Şutu, Kasırga Şutu, Kartal Pikesi, Hayalet Dribbling, Buz Pası
- **Gol sevinçleri:** Siuuu!, Diz Kayması, Uçak, Ters Takla, Zafer Dansı

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
