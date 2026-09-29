# HULK MOD — Minecraft Bedrock 1.21.62

Radyoaktif Elma'yı ye → dönüşüm sekansı → **HULK**.

## Kurulum
1. `dist/HulkMod.mcaddon` dosyasını telefonda/PC'de aç (Minecraft otomatik içe aktarır).
2. Dünya ayarları → **Behavior Packs** ve **Resource Packs**: "Hulk Mod" paketlerini etkinleştir (BP etkinleşince RP de gelir).
3. Ek deney (Experiments) açmaya gerek yok.

## Kullanım
| Ne | Nasıl |
|---|---|
| Radyoaktif Elma | Yaratıcı envanter (Items → Yemekler) veya `/give @s hulk:radioactive_apple` veya craft: elma + 2 glowstone tozu + 2 slime topu (şekilsiz) |
| Sakinleştirici Hap (geri dön) | `/give @s hulk:calm_pill` veya craft: lapis + şeker + kemik tozu (2 adet verir) |
| Zorla dönüş / geri dönüş | `/scriptevent hulk:transform` · `/scriptevent hulk:revert` |

Elmayı **hayatta kalma (Survival) modunda** yiyebilirsin; **Creative**'de yemek mümkün olmadığından elmaya sağ tık yeterli. Dönüşüm için üstünde 3 blok boşluk olmalı; süre 5 dk, süre bitince/hap yiyince/ölünce Bruce'a dönersin.

### Yetenekler
* **Dönüşüm animasyonu (3.6 sn):** Bruce dokusu → gömlek yırtılır → yeşile döner → damarlar belirir → kükreme + şok dalgası (5 aşamalı texture, büzülmeden büyümeye ölçek animasyonu, titreme, kas şişmesi, kumaş parçacıkları, kamera sarsıntısı)
* **Süper zıplama**, **Sprint + zıpla = Hulk Leap**
* **Yüksekten iniş = Yer Sarsıntısı** (düşme hasarı yok)
* **Havada eğil (sneak) = Meteor Dalışı** → inişte dev şok dalgası
* **Eğilerek vur = HULK SMASH** (alan hasarı, geri itme, yumuşak blokları kırar)
* **Yerde eğil + zıpla = Thunderclap** (çift el alkışı şok dalgası)
* **Yerde 1.5 sn eğil = Kükreme** (düşmanları iter ve korkutur)
* **Öfke barı:** vurdukça/vuruldukça dolar → dolunca **ÖFKE MODU** (parlayan damarlar, daha güçlü)
* 40 can, direnç, hız, kazma hızı, yenilenme, ağır adım sesleri/titreşim
* Modelde: koşu / yürüyüş / durma / zıplama / düşme / çömelme / yüzme / yumruk animasyonları ve birinci şahıs Hulk kolları

## Teknik
* `HulkMod_BP` — `player.json` (özellikler: `hulk:active`, `hulk:stage`, `hulk:anim`; hitbox 1.4×3.0), eşyalar, tarifler, `scripts/main.js` (`@minecraft/server 1.16.0`)
* `HulkMod_RP` — `player.entity.json` (vanilla 1.21.60 dosyasından türetildi; Hulk açıkken Hulk render controller'ı devreye girer), `hulk.geo.json` (142 kutu, 18 kemik), 1024×1024 5 aşamalı texture, animasyonlar, animasyon/render controller'lar, parçacıklar
* Her şey `tools/` altındaki Python ile üretilir: `pip install pillow numpy && python3 tools/build.py` (model, texture, animasyon ve `.mcaddon` yeniden oluşur). `tools/preview.py` oyunsuz önizleme çizer.
* `tools/vanilla/` — Mojang bedrock-samples v1.21.60.10'dan referans oyuncu dosyaları.

## Bilinen sınırlar / dürüst not
* Bu paket **oyunda test edilemeden** üretildi (ortamda Minecraft yok); model/texture/pozlar yazılım önizlemesiyle kontrol edildi, JSON'lar ve script sözdizimi doğrulandı. Oyunda ilk denemede küçük ince ayar gerekebilir (ör. hitbox/kamera yüksekliği, animasyon hızları, ses adları).
* Model kutulardan oluşur (Minecraft sınırı); referans görseldeki gibi yuvarlak kaslar kutu + gölgeleme ile taklit edilir, birebir pürüzsüz mesh mümkün değildir.
* Oyuncu modelini değiştiren `player.entity.json` / `player.json` diğer oyuncu-modeli paketleriyle çakışır.
* Zırh/pelerin Hulk modundayken gösterilmez.
