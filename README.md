# Solo Leveling: Arise (Forge 1.20.1)

Solo Leveling animesinden ilham alan, sıfırdan yazılmış büyük bir Forge 1.20.1 (47.2.0) modu.
*A from-scratch Solo Leveling mod for Minecraft Forge 1.20.1.*

## Kurulum / Install
1. Forge **1.20.1-47.2.0** (veya üstü 47.x) kur.
2. `sololeveling-arise-1.20.1-forge-1.0.0.jar` dosyasını `mods` klasörüne at. (Releases sayfasından indir.)
3. Oyun dili Türkçe veya İngilizce olabilir (ikisi de tam çevrildi).

## Oynanış özeti
* **Sistem (K tuşu)** – Seviye, EXP, GÜÇ / ÇEVİKLİK / DAYANIKLILIK / ZEKA / ALGI puanları, mana, yorgunluk, rütbe (E→D→C→B→A→S→Ulusal), meslek (Nekromancer), ünvanlar.
  * Sekmeler: **Durum, Yetenek, Görev, Harita, Haber, Lonca**.
  * Günlük Görev ("Güçlenmeye Hazırlık"), Görev Panosu (bounty), ödüller.
* **13 yetenek** (R ile kullan, Z/X ile seç): Gölge Çıkarma (**ARISE**, G), Atılma, Hançer Atılışı, Şiddetli Kesik, Gizlilik, Kan Susuzluğu, Gölge Adımı, Parçalama, Hükümdarın Otoritesi, Cıva Hızı, Gölge Değişimi, Ejderhanın Korkusu, Hükümdarın Alanı.
* **Gölge Ordusu** – Öldürdüğün canavarların cesetlerinden gölge askerler çıkar (Gölge Asker, Büyücü, Kurt, Tank, **Igris**, **Beru**). Sahibinin seviyesiyle güçlenirler, Kapılara seninle girerler.
* **Kapılar & Zindanlar** – Dünyada rastgele Kapılar açılır (E→Ulusal, Kırmızı Kapılar). İçeri girince seviyene uygun, prosedürel üretilen zindana (8 tema) girersin. Boss'u yen → ödül, EXP, haber. Kapıyı temizlemezsen **Zindan Kırılması** olur ve canavarlar dışarı çıkar.
  * Temalar: Goblin Mağarası, Tanrı Tapınağı (Çifte Zindan), Kasaka'nın Bataklığı, Buz Mağarası (Kırmızı Kapı), Cehennem İni, Cheju Karınca Yuvası, Şeytan Kalesi, Kamish'in İni.
  * Bosslar: Goblin Savaş Lordu, Tanrı Heykeli, **Kasaka**, **Baruka**, **Kerberos**, **Karınca Kral**, **Kızıl Şövalye Igris**, **Ejderha Kamish**.
* **Canlı Dünya: "Avcı Dünyası"** (özel boyut) – Seul (Avcılar Birliği merkezi, gökdelenler, loncalar, kapı meydanı), Gangnam, Busan, Incheon ve Cheju Adası. Şehirler tamamen prosedürel: yollar, parklar, otoparklar, binalar, ladder'lı kuleler, NPC'ler.
  * NPC'ler: Go Gun-Hee, Cha Hae-In, Baek Yoon-Ho, görevliler, tüccar, şifacı, gazeteci, avcılar, vatandaşlar.
  * **Işınlanma Platformu** (şehir meydanı) veya **Dünya Haritası** ile bölgeler arası seyahat.
  * **Haber Panosu / Avcı Ağı**: Kapı açılışları, temizlemeler, kırılmalar, rütbe atlamaları canlı haber olarak yayınlanır.
* **Loncalar** – Avcılar Loncası, Ahjin Loncası, Beyaz Kaplan Loncası (avantajlar).
* **Eşyalar** – 12 silah + 2 asa (hepsi ayrı 3B model ve doku), 4 zırh seti (Avcı, Kızıl Şövalye, Buz Elfi, Gölge Hükümdarı), 9 iksir, 6 rütbe mana kristali, anahtarlar, parşömen, lisans, harita, gazete, 16 blok.

## Kontroller
| Tuş | İşlev |
|---|---|
| K | Sistem penceresi |
| R | Seçili yeteneği kullan |
| Z / X | Önceki / sonraki yetenek |
| G | ARISE (gölge çıkar) |
| V | Gölgeleri yanına çağır |

## Komutlar (OP)
`/sl gate <rütbe> [tema] [red]`, `/sl dungeon <tema> <rütbe>`, `/sl bossroom`, `/sl region <seoul|gangnam|busan|incheon|jeju>`,
`/sl level <n>`, `/sl skills`, `/sl gold <n>`, `/sl cast <yetenek>`

## Geliştirme
* Tüm içerik `gen/content.py` içinde tanımlıdır; `python3 gen/build_assets.py` doku, 3B model, dil dosyaları, tarifler ve Java içerik tablolarını üretir.
* CI (GitHub Actions) jar'ı derler, sunucu self-test'i çalıştırır (dünya üretimi, zindanlar, entity'ler, yetenekler) ve `[vtest]` commit'lerinde başsız istemciyle ekran görüntüleri alır.
