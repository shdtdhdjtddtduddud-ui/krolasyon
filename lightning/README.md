# Fırtına Yetenek Ağacı (Storm Skill Tree) — Forge 1.20.1

Referans görseldeki yıldırım yetenek ağacının oynanabilir hali. 14 ikonun tamamı referans görselden piksel piksel
kırpıldı (`gen/reference.jpg`), ağaç ekranı da aynı görselin çizgileri üzerine kurulu.

## Kullanım
- **K**: yetenek ağacını aç (ya da **Fırtına Kitabı**'na sağ tık)
- Yetenekler **XP seviyesi** ile açılır (kademe 1: 2, kademe 2: 5, kademe 3: 8, kademe 4: 12 seviye).
  Kademe 4 için herhangi iki kademe 3 yeteneği gerekir.
- Açılan aktif yetenek boş slota otomatik yerleşir. Ağaçta yeteneğe tıkla → alttaki slota tıkla (ya da üzerindeyken 1-4).
- **Z / R / G / V**: slot 1-4'teki yeteneği kullan (Ayarlar → Kontroller → "Fırtına Yetenekleri"nden değiştirilebilir).
- Yetenekler **Fırtına Enerjisi** harcar (hotbarın sağındaki mavi çubuk), zamanla dolar.
- **Fırtına Kitabı** (tarif: üstte paratoner, ortada elmas-kitap-elmas, altta ametist kırığı) hotbarda/sol elde taşınırken enerji bedelleri %20 düşer.
- Operatör komutları: `/storm unlockall`, `/storm reset` (seviyeleri iade eder), `/storm refill`, `/storm cast <id>`.

## Yetenekler
| Kademe | Yetenek | Etki |
|---|---|---|
| 1 | Yıldırım Çarpması | Nişan alınan yere yıldırım: 8 hasar, yakar, yavaşlatır |
| 1 | Statik Çekirdek (pasif) | +50 enerji, hızlı yenilenme, yakın dövüşte %30 şok, yıldırım bağışıklığı |
| 2 | Şimşek Oku | Delip geçen hızlı şimşek, 3 hedefe 7 hasar |
| 2 | Kıvılcım Patlaması | 6 blok AoE 6 hasar + geri savurma |
| 2 | Fırtına Kubbesi | 8 sn: %60 hasar azaltma, mermi yansıtma, saldıranı çarpma |
| 2 | Elektrik Halkası | 10 sn dönen halka, yakındaki düşmanlara sürekli hasar |
| 3 | Yıldırım Yağmuru | Bölgeye 3 sn'de 10 yıldırım |
| 3 | Fırtına Bedeni | 15 sn: hız/güç/acele, 2x enerji, vuruşlar sekiyor |
| 3 | Şok Pençesi | Hedefi çekip 2.5 sn kilitler |
| 3 | Top Yıldırım | Yol boyunca çarpan, sonunda patlayan küre |
| 4 | Zincir Yıldırım | 10 hasar, 7 kez sıçrar |
| 4 | Yıldırım Novası | Havalanıp 12 blok nova: 16 hasar, sersemletme |
| 4 | Yıldırım Mızrağı | 64 blok delici ışın, 20 hasar |
| 4 | Fırtına Azraili (pasif) | %20 can altı hedefleri infaz, öldürmeler enerji ve bekleme süresi kazandırır |

## Varlıklar
- Yetenek ikonları ve ağaç arka planı: referans görselden birebir.
- Top yıldırım, kıvılcım parçacıkları, kalkan petek dokusu, kafatası işareti ve Fırtına Kitabı ikonu
  **Higgsfield MCP** (`gpt_image_2_5`) ile üretildi; ham çıktılar `gen/higgsfield_raw/`, dönüştürme `gen/process_higgsfield.py`.
- Sesler `gen/sounds.py` ile prosedürel üretildi.
- Animasyonlar: oyuncu modeline Mixin ile anahtar kareli büyü pozları (3. şahıs) + 1. şahıs el hareketleri, kamera sarsıntısı,
  zikzak şimşek arkları, kubbe, halka, nova şok dalgası, mızrak ışını gibi prosedürel efektler.

## Derleme
`cd lightning && gradle build` → `build/libs/storm_skill_tree-1.20.1-forge-1.0.0.jar`.
GitHub Actions (`lightning-build.yml`) her push'ta jar'ı derleyip bir release'e koyar.
