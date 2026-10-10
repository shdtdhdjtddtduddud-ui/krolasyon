# Boşluk Yetenek Ağacı (Void Skill Tree) — Forge 1.20.1

İkinci referans görseldeki mor boşluk/gölge yetenek ağacının oynanabilir hali. 14 ikonun tamamı referans görselden
piksel piksel kırpıldı (`gen/reference.jpg`); ağaç ekranı aynı görselin çizgileri üzerine kurulu ve bağlantılar
görseldeki hatlarla aynı mantıkta çalışır (bir üst yetenekten herhangi biri açıksa alttaki açılabilir).

## Kullanım
- **J**: boşluk ağacını aç (ya da **Boşluk Grimuarı**'na sağ tık)
- Yetenekler **XP seviyesi** ile açılır (kademe 1: 2, kademe 2: 5, kademe 3: 8, kademe 4: 12 seviye).
- Aktif yetenekleri 4 slota ata, **H / B / N / M** ile kullan (Yıldırım modu ile çakışmaz, ikisi birlikte kurulabilir).
- Yetenekler **Boşluk Özü** harcar (hotbarın sağındaki mor çubuk).
- **Boşluk Grimuarı** tarifi: üstte ender gözü, ortada ağlayan obsidyen-kitap-ağlayan obsidyen, altta ametist kırığı.
  Taşınırken öz bedelleri %20 düşer.
- Operatör komutları: `/void unlockall`, `/void reset`, `/void refill`, `/void cast <id>`.

## Yetenekler
| Kademe | Yetenek | Etki |
|---|---|---|
| 1 | Gölge Atılımı | 9 blok gölge atılımı, yoldakilere hasar + körlük, kısa dokunulmazlık |
| 1 | Hayalet Formu | 6 sn görünmezlik, canavarlar hedefleyemez, ilk vuruş 1.5x +6 |
| 1 | Boşluk Peleriği (pasif) | +50 öz, karanlıkta +%20 hasar, %15 yakın saldırıdan kaçma |
| 2 | Boşluk Girdabı | 5 sn düşmanları merkeze çeken girdap |
| 2 | Ruh Hortumu | İlerleyen hortum, düşmanları kaldırıp döndürür |
| 2 | Gölge Pençeleri | 3 hızlı pençe (5+5+8), solma |
| 2 | Ruh Hapishanesi | Hedefi 4 sn yıldızlı küreye hapseder |
| 3 | Gölge Yağmuru | 16 gölge bıçağı yağdırır |
| 3 | Kara Delik | Düşman/eşya/mermi yutan kara delik, sonunda 15 hasarlık çöküş |
| 3 | Boyut Yırtığı | Yırtık fırlat, düştüğü yere ışınlan, 8 hasarlık patlama |
| 4 | Yükseliş | 15 sn uçuş, Direnç II, yarı bedel, kanat + hale |
| 4 | Boşluk Mührü | 8 blok mühür: yavaşlatma, zayıflatma, düşman mermisi yok etme, enderman ışınlanamaz |
| 4 | Gölge Klonları | 20 sn savaşan 3 gölge klonu |
| 4 | Kozmik Çöküş | Gökyüzünden yıldız yağmuru + 18 hasarlık çöküş |

## Varlıklar
- Yetenek ikonları ve ağaç arka planı: referans görselden birebir.
- Kara delik, boyut yırtığı, yıldız alanı, girdap, pençe izi, parıltı yıldızı, gölge duman parçacıkları ve Boşluk Grimuarı ikonu
  **Higgsfield MCP** (`gpt_image_2_5`) ile üretildi; ham çıktılar `gen/higgsfield_raw/`, dönüştürme `gen/process_higgsfield.py`.
- Gölge klonu derisi ve sesler prosedürel (`gen/sounds.py`).
- Animasyonlar: Mixin ile oyuncu modeline anahtar kareli pozlar (atılım, üçlü pençe, çağırma, yükseliş...), 1. şahıs el hareketleri,
  kamera sarsıntısı; karanlık duman + mor parıltı katmanlı prosedürel efektler.

## Derleme
`cd void && gradle build` → `build/libs/void_skill_tree-1.20.1-forge-1.0.0.jar`. GitHub Actions (`void-build.yml`) her push'ta derler.
