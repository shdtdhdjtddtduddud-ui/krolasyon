# Alien: Isolation — Sevastopol (Forge 1.20.1)

Hazir jar: GitHub **Releases** sayfasindaki en son `alien-build-*` surumu (`alienisolation-1.20.1-forge-1.0.0.jar`).
`mods` klasorune at, Forge 1.20.1 (47.x) ile ac. Iki oyunculu oynamak icin ikinizin de modu kurmasi yeterli (LAN veya sunucu).

## Hizli baslangic
1. Yaratici sekmesinden **Sevastopol Cekirdegi**ni al (ya da `/sevastopol olustur`).
   Duz (superflat) bir dunyada kullan: etrafina 4x4 odalik bir istasyon kurar, sizi (en fazla 2 kisi) baslangic odasina koyar,
   uzayliyi en uzak odadaki havalandirmaya salar.
2. Baslangic odasindaki sandikta hareket sensoru, el feneri, gurultu yapici, isaret fisegi ve ilk yardim kiti var.
   Alev silahi baska bir odadaki sandikta.
3. Hayatta kalma modunda oyna. Blok kirma kapali (ayarlardan acilabilir).

## Uzayli nasil davranir
- **Gorme:** isik, egilme, hareket ve mesafe onemli. Karanlikta egilerek duran oyuncuyu zor fark eder; el feneri seni ele verir.
- **Duyma:** kosmak, kapi acmak, alev silahi, dusme, nefes nefese kalmak... hepsi sesi yayar. Egilerek yurumek sessizdir.
- **Havalandirma:** gorunmeden kanallarda dolasir; hareket sensorunde gorunur, sesi duyulur, tavandan iner (isiklar titrer).
- **Arama:** sesin geldigi yere gelir, bolgeyi tarar, dolaplari koklar.
- **Dolap:** SHIFT ile nefesini tut, BOSLUK ile cik. Uzayli dolabi koklarken nefes alirsan seni ceker cikarir.
  Seni dolaba girerken gorduyse dogrudan oraya gelir.
- **Ogrenme:** alevle kac kez kovuldugunu, dolap numaralarini ve gurultu yapicilari hatirlar; giderek daha zorlasir.
- **Yonetmen:** cok uzun sure baskida kaldiysan bir sure geri cekilir; uzaklastiysan sana dogru gelir.
- Oldurulemez; yalnizca ates onu kacirir. Yakaladiginda oldurur (kolay modda agir hasar verip kacar).

## Komutlar (OP)
- `/sevastopol olustur` — istasyonu kur ve uzayliyi sal
- `/sevastopol uzayli` — yakina uzayli cagir
- `/sevastopol temizle` — uzaylilari kaldir

## Ayarlar
`config/alienisolation-common.toml`: aninda oldurme, duyma carpani, gorus mesafesi, blok kirma korumasi.

## Gelistirme
Tum modeller, dokular, animasyonlar ve sesler `gen/build.py` ile uretilir (Python + numpy/scipy/Pillow + ffmpeg).
Modeller, dokular ve sesler orijinal oyundan alinmamistir; oyundan esinlenerek sifirdan uretilmistir.
