# Krolasyon: Kızıl Taht (Forge 1.20.1)

Kızıl Diyar'a açılan portallar, beş krallık, 45 yaratık ve tahtı ele geçirme hikâyesi.

## Kızıl Diyar'a geçiş
- **Kendiliğinden oluşan portallar:** Normal dünyada nadiren *Kızıl Geçit harabeleri* doğar (çoğu zaten aktif).
  Ayrıca geceleri oyuncuların yakınında **kızıl yarıklar** kendiliğinden açılabilir (`config/krolasyon-realm.toml`).
- **Kendi portalın:** Lanetli Obsidyen'den (obsidyen + blaze tozu + magma kremi) 2x3 – 21x21 çerçeve kur,
  çakmak / ateş topu / Kor Anahtarı ile yak.

## Krallıklar ve siyaset
| Krallık | Biyom | Lord | Dostu | Düşmanı |
|---|---|---|---|---|
| Kül Krallığı | Kül Çölleri | Kül Kralı Varkhas | Demir Lejyonu | Ruh Tarikatı, Gölge Konseyi |
| Kan Hanedanı | Kan Bataklığı | Kan Kraliçesi Serathis | Gölge Konseyi | Ruh Tarikatı, Demir Lejyonu |
| Gölge Konseyi | Obsidyen Ormanı | Gölge Veziri Nyx'ar | Kan Hanedanı | Demir Lejyonu, Kül Krallığı |
| Demir Lejyonu | Bazalt Savaş Alanları | Savaş Lordu Grommak | Kül Krallığı | Gölge Konseyi, Kan Hanedanı |
| Ruh Tarikatı | Ruh Vadisi | Ruh Kâhini Ilvaine | — | Kül Krallığı, Kan Hanedanı |

- Düşman krallıkların askerleri birbirleriyle savaşır.
- Elçilerle konuş (sağ tık): **haraç** sun (altın, elmas, düşmanlarının özleri), **görev** al, 50 itibarda **ittifak** kur.
- Bir krallıkla ittifak, düşmanlarını sana kızdırır. Lordunu yenersen krallık **fethedilir**.
- Her ittifak / fetih bir **Mühür** ve bir **Lütuf** verir (ateş bağışıklığı, can çalma, karanlıkta görme, +zırh/+saldırı, yenilenme).
- Beş Mühür + altın blok = **Taht Anahtarı** → Taht Harabeleri'nde kullan → **Tiran Azgaroth** → Hükümdar ol.

## İçerik
- 34 canavar + 5 elçi + 5 krallık lordu + Tiran (her birinin kendi modeli, dokusu, animasyonları ve yetenekleri)
- 6 efsanevi silah (sağ tık güçleri), 10 büyü kitabı, mana çubuğu, Kızıl Atılım (`G`), Kızıl Çelik zırh, Hükümdar Tacı
- Kızıl Kronik: hikâye, krallıklar, itibar ve lütuflar
- Eski bosslar: Mühür Bekçisi, Kızıl Cehennem Kurdu, İntikam, Kalp Kırıcı İblis

## Derleme
`gradle build` → `build/libs/krolasyon_bosses-1.20.1-forge-2.0.0.jar`.
Modeller, dokular, animasyonlar ve tüm veri dosyaları `python3 gen/realm_build.py` ile üretilir.
