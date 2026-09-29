# Chainsaw Man (Denji) Add-on – Minecraft Bedrock / PE 1.21.62

Pochita'nın Kalbi eşyasıyla **Chainsaw Man'e dönüşürsün**: kafada testere + kol bıçakları,
gömlek/kravat/pantolon/spor ayakkabı modeli, dönüşüm animasyonu, sprint / zıplama / düşme
pozları ve 5 özel yetenek.

## Kurulum (telefon / PE)
1. `dist/ChainsawMan.mcaddon` dosyasını telefona indir ve aç (Minecraft otomatik içe aktarır).
2. Dünya oluştur/düzenle → **Davranış Paketleri**'nden `Chainsaw Man BP`, **Kaynak Paketleri**'nden `Chainsaw Man RP` etkinleştir
   (BP, RP'ye bağlıdır; biri açılınca diğeri de istenir).
3. **Deneysel özellik gerekmez.** Script API sabit sürüm (`@minecraft/server 1.15.0`) kullanır.
4. Dünyaya ilk girişte envantere **Pochita'nın Kalbi** gelir. Gelmezse: `/scriptevent cm:heart`
   (ya da `/give @s chainsaw_man:pochita_heart`).

## Kullanım
| Ne | Nasıl |
|---|---|
| Dönüş / insana geri dönüş | Pochita'nın Kalbi'ne sağ tık (`/scriptevent cm:toggle`) |
| Dönüş sonrası | Hotbar 5-9'a kilitli yetenek eşyaları gelir |
| **Kesik Serisi** | 3 hamlelik kesik combo (7/7/11 hasar), can çalar |
| **Kasırga** | Kollar yana açılıp 360° dönüş, çevredekileri parçalar |
| **Atılış** | Bıçaklar önde ileri hücum (10 hasar + savurma) |
| **Kükreme** | Korku dalgası (yavaşlık+zayıflık) + 15 sn **Kan Çılgınlığı** (güç/hız/yenilenme, çift can çalma) |
| **Kan Püskürtme** | Baştaki testereden koni şeklinde kan (körlük + wither) |
| Pasifler | Hız II, Güç II, Zıplama III, Direnç I, Yenilenme, +16 can; yumruk/el vuruşlarına kanlı ekstra hasar + can çalma; öldürünce can; canın ≤3 kalpken **Kan İçme** ile ölümden dönüş (2 dk'da 1) |
| Koşma / zıplama | Ayaklarda kıvılcım + motor sesi, koşarken öne eğilme, havada kollar açılır |
| Öl / respawn | Otomatik insana döner |

## Yapı
```
ChainsawMan_BP/   davranış: player özellikleri (cm:form, cm:anim), eşyalar, tarif, scripts/main.js
ChainsawMan_RP/   kaynak: player client entity, geometry.chainsawman, 128x128 doku, animasyonlar,
                  animasyon kontrolcüsü, parçacıklar, sesler (.wav)
tools/            generate_assets.py (model+doku+ikon+ses), generate_animations.py, build_mcaddon.py
dist/             hazır .mcaddon ve .mcpack dosyaları
```
Model/doku/animasyonu değiştirmek için `tools/*.py` dosyalarını düzenleyip
`python3 tools/generate_assets.py && python3 tools/generate_animations.py && python3 tools/build_mcaddon.py`
çalıştır (pillow + numpy gerekir).

## Bilinen sınırlar (dürüst not)
- Kod ve asset'ler oyun **içinde test edilmedi** (üretim ortamında Minecraft yok). JSON/JS sözdizimi ve kemik/UV tutarlılığı
  doğrulandı; ama animasyon açıları (kol/bıçak pozları) ve doku oranları ilk denemede ince ayar isteyebilir.
- Model, oyunun 64x64 oyuncu iskeletine bağlı küp tabanlı bir yorumdur; "birebir" anime görünümü küp modellemeyle
  yaklaşık olarak yakalanır (turuncu-siyah testere kafa, arka tutamak, alından çıkan bıçak, kol bıçakları, kravat, kemer, spor ayakkabı).
- Sesler sentetik `.wav`; oyun bunları oynatmazsa yanına konan vanilla sesler çalar.
- Zırh, dönüş sırasında görünmez (model tamamen değişir). Cape/elytra desteklenmez.
- İnsan formunda oyuncunun kendi skini korunur.
