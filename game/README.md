# Krolasyon: Kayıp Kristaller

Tarayıcıda çalışan, üçüncü şahıs kameralı 3D aksiyon RPG. Three.js ile yazıldı. Modeller, texture'lar, animasyonlar, sesler ve müziğin tamamı kodla (prosedürel olarak) üretilir. Harici hiçbir görsel ya da ses dosyası kullanılmaz.

## Çalıştırma

ES modülleri kullandığı için bir yerel sunucu gerekir:

```bash
cd game
python3 -m http.server 8080
# tarayıcıda http://localhost:8080
```

## İçerik

- **Dünya:** 1200×1200 m prosedürel arazi ve 5 biyom: Yeşil Vadi, Karanlık Orman, Kızıl Çöl, Buzul Zirveleri, Kül Diyarı. Göller, lav gölleri, yollar ve gece-gündüz döngüsü var. Her biyomun kendi hava partikülleri (kar, kül, kum, ateş böcekleri) ve sis tonu bulunur.
- **Yapılar:** Köy (evler, yel değirmeni, demirci, pazar, kuyu, kamp ateşi), ork kampı, orman harabeleri, piramit, buz kalesi, ejderha ini, ışınlanma taşları ve hazine sandıkları.
- **Sınıflar:**
  - Savaşçı: 3 vuruşluk kombo, Kasırga, Yer Sarsıntısı, Hücum, Savaş Narası.
  - Büyücü: Ateş Topu, Buz Novası, Zincir Şimşek, Meteor Yağmuru. Kaçınmak için ışınlanır.
  - Okçu: Çoklu Atış, Zehirli Ok, Geri Sıçrama ve tuzak, Ok Yağmuru.
- **Yaratıklar:** 18 düşman türü. Balçık, kurt, goblin, goblin şaman, dev örümcek, iskelet, ork, akrep, kum/buz/magma golemi, mumya, yeti, buz kurdu, ateş iblisi ve cehennem tazısı bunlardan bazıları.
- **Bosslar:** Ork Şefi Grol, Çürük Kök (ağaç lordu), Akrep Kral, Buz Devi Hrimgar ve Kızıl Ejderha Ignarok. Her boss'un yere işaretlenen özel saldırıları ve öfke fazı var.
- **Sistemler:** Seviye (en fazla 20), 4 nadirlikte rastgele eşya, çanta ve ekipman, dükkan, silah güçlendirme, ana ve yan görevler, ışınlanma ve otomatik kayıt (localStorage).
- **Vuruş hissi:** Hit-stop, kamera sarsıntısı, kılıç izi, kıvılcım ve kan partikülleri, hasar sayıları, geri savrulma, bloom ve renk düzeltme.
- **Kontroller:** Klavye ve fare (işaretçi kilidi). Telefon ve tablette sanal joystick ile dokunmatik butonlar.

## Kontroller

| Tuş | İşlev |
| --- | --- |
| WASD | Hareket (Shift ile koşu) |
| Fare | Kamera |
| Sol tık | Saldırı / kombo |
| Sağ tık / F | Takla (büyücüde ışınlanma) |
| Boşluk | Zıplama |
| 1–4 | Yetenekler |
| Q / R | Can / mana iksiri |
| E | Konuşma, sandık, ışınlanma |
| Tab | Hedef kilitleme |
| I · J · M | Çanta, görevler, harita |
| Esc | Menü / ayarlar |
