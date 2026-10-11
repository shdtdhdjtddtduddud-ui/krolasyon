# Omnitrix (Ben 10) — Forge 1.20.1

Bileğine tak, kadranı çevir, dönüş. 9 uzaylı, her birinde 4 yetenek, bloklama ve pasif özellikler.

## Kullanım
- **Omnitrix** eşyasına sağ tık: saat bileğine kilitlenir (tarif: netherit-zümrüt-netherit / demir blok-nether yıldızı-demir blok / netherit-zümrüt-netherit; yaratıcı sekmede de var).
- **Y**: saati etkinleştir → kadran açılır. Fare yönü / tekerlek / ok tuşlarıyla uzaylıyı seç, **sol tık / Enter** ile dönüş.
  Dönüşmüşken **Y**: insana geri dön.
- **Z / X / C / V**: 4 yetenek. **Sol Alt** (basılı tut): bloklama. Tuşlar Ayarlar → Kontroller → "Omnitrix"ten değişir.
- Dönüşüm 3 dakika sürer; süre bitince saat zaman aşımına uğrar, 20 saniye şarj olur (son 10 saniyede bip sesi).
- Operatör komutları: `/omnitrix transform <uzaylı>`, `/omnitrix revert`, `/omnitrix ability <0-3>`, `/omnitrix recharge`, `/omnitrix remove`.

| Uzaylı | Z | X | C | V | Blok | Pasif |
|---|---|---|---|---|---|---|
| Heatblast | Ateş Topu | Alev Püskürtücü | Süpernova | Alev Jeti | Alev kalkanı | Ateş/lav bağışıklığı, yumruklar yakar |
| Terraspin | Rüzgâr Darbesi | Kasırga | Dönerek Uçuş | Kabuk Darbesi | Kabuğa çekilir | Düşme hasarı yok, süzülme |
| Diamondhead | Kristal Parçaları | Kristal Kılıçlar | Diken Dalgası | Kristal Hapis | Kristal kalkan (yansıtır) | Direnç, %25 mermi yansıtma |
| Goop | Asit Tükürüğü | Uzayan Kol | Birikinti | Asit Bombası | Mermiler içinden geçer | Zehir bağışıklığı |
| XLR8 | Hız Atılımı | Yıldırım Saldırısı | Girdap Koşusu | Aşırı Hız | %75 kaçınma | Hız, suyun üstünde koşma |
| Four Arms | Gök Gürültüsü Alkışı | Yer Sarsıntısı | Dörtlü Kombo | Kaya Fırlatma | Dört kolla siper | Süper güç, yüksek zıplama |
| Big Chill | Dondurucu Nefes | Hayalet Geçiş | Buz Hapsi | Buz Kanatları | Buz duvarı | Suyu dondurarak yürür |
| Ultimate Echo Echo | Ses Diskleri | Sonik Kıyamet | Sonik Çığlık | Disk Sürüşü | Ses bariyeri | Yankıyla canavar görme |
| Ghostfreak | Maddesizleşme | Ele Geçirme | Dokunaçlar | Korku Dalgası | Yakın saldırılar geçer | Gece görüşü, yavaş düşme |

## Modeller ve varlıklar
- 9 uzaylı ve saatin 3D modelleri, verdiğin çizimlerden **Higgsfield MCP** ile üretildi (`sam_3_3d` uzaylılar, `tripo_h3_1_image_to_3d` saat).
  Ham GLB'ler CI'da indirilip (`gen/inspect_models.py`, `gen/export_meshes.py`) `gen/mesh/` altına alınır,
  `gen/build_models.py` + `gen/rigs.py` iskelet kurup deri ağırlıklarını hesaplar ve `.omx` olarak moda yazar.
- Mod modelleri kendi skinning renderer'ıyla çizer: yürüme/koşma/zıplama/yüzme, yumruk, blok duruşları, 36 yetenek animasyonu,
  çizgi film tarzı siyah dış hat, dönüşüm (yeşil ışık sütunu + DNA sarmalı + büyüme) ve geri dönüş/zaman aşımı animasyonları.
- Efekt dokuları (ateş, kristal, balçık, buz, ses halkası, ektoplazma, rüzgâr, kadran hologramı) Higgsfield ile üretildi; sesler `gen/sounds.py`.

Fan yapımıdır; Ben 10 ve karakterleri Cartoon Network'e aittir.
