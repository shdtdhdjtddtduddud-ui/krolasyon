# Köy Hayatı

Tarayıcıda çalışan, birinci şahıs bakışlı 3D köy ve çiftlik oyunu. Kurulum gerektirmez; Three.js tarayıcıya CDN'den yüklenir.

## Çalıştırma

Klasörü herhangi bir statik sunucuyla açın (ES modülleri `file://` üzerinden çalışmaz):

```sh
cd koy-hayati
python3 -m http.server 8000
# tarayıcıda: http://localhost:8000
```

## Neler var

- **Dünya:** dağlar, Gümüş Şelale, Ayna Göl ve adası, nehir, Çam Ormanı, Çiçekli Yayla, Kartal Tepesi zirvesi, kasaba
- **Mevsimler:** her mevsim 7 gün; ilkbahar hızlı büyüme ve yağmur, yaz uzun gün ama kuraklık, sonbahar hasat bereketi ve mantar, kış donmuş toprak, buz balıkçılığı, saman ihtiyacı
- **Gün/gece döngüsü ve hava:** yağmur, fırtına (şimşek), kar, tipi, sıcak hava
- **Çiftçilik:** 8 ekin (havuç, patates, buğday, domates, lahana, mısır, karpuz, bal kabağı), çapa, sulama, gübre, sera
- **Hayvanlar:** tavuk, horoz, kaz, tavşan, inek, boğa, at, koyun, keçi, domuz; alım-satım, sevgi, ürün toplama, ilkbaharda yavru, ata binme
- **Araçlar:** traktör (tarlayı çapalar), kamyonet, yelkenli kayık, at arabası
- **Çalışma hayatı:** iş ilanları (siparişler), yevmiye, fırın ve değirmen vardiyaları, pazar ve çiftlik tezgâhı
- **Aktiviteler:** balık tutma (göl, nehir, şelale, kışın buzda), odun kesme, böğürtlen/mantar/kekik toplama, mutfakta peynir, ekmek, reçel, çorba
- **Ev:** girilebilir iç mekân, yatak, soba, mutfak, alınabilir saksı bitkileri

Bütün modeller, dokular, sesler ve animasyonlar kodla üretilir; harici görsel/ses dosyası yoktur. Kayıt tarayıcının yerel deposunda tutulur.

## Kontroller

Bilgisayar: WASD yürü, Shift koş, Boşluk zıpla, fare bak, sol tık alet kullan, E etkileşim/araca bin, 1–6 alet, Q tohum değiştir, I çanta, M harita, R ye, H rehber.

Telefon: sol daire yürü, sağ taraf kaydır/bak, Kullan ve Etkileş düğmeleri, alttaki çubuktan alet seç.

## Kod yapısı

`src/` altında: `terrain.js` (arazi, su, şelale), `sky.js` (gökyüzü, ışık, hava), `buildings.js`, `vehicles.js`, `animals.js`, `nature.js`, `plants.js` (modeller), `farming.js`, `game.js` (kurallar, ekonomi, kayıt), `world.js` (yerleşim), `player.js`, `input.js`, `ui.js`, `audio.js`, `main.js` (ana döngü).
