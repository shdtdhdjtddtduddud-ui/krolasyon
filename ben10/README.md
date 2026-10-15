# Ben 10 Omnitrix (Minecraft Forge 1.20.1)

Omnitrix ile 9 uzaylıya dönüş. Her uzaylının 4 özel yeteneği, kendine özgü bir bloklaması, pasif güçleri, modeli, animasyonları ve efektleri var.

**Kurulum:** `ben10_omnitrix-1.20.1-forge-1.0.0.jar` dosyasını `.minecraft/mods` klasörüne at (Forge 1.20.1, 47.x).
Omnitrix'i yaratıcı moddaki **Ben 10 Omnitrix** sekmesinden alabilirsin ya da `/give @s krolben10:omnitrix` komutunu kullanabilirsin.

| Tuş | Ne yapar |
|---|---|
| Omnitrix'e **sağ tık** | Saati bileğine tak |
| **G** | Kolunu kaldır: kadran açılır ve yeşil hologram belirir. **Tekerlekle** çevir, **sol tıkla** saate vurup dönüş |
| Uzaylıyken **G** | İnsana dön |
| **Eğil + G** | Saati çıkar |
| **H** | Uzaylı listesi menüsü |
| **Z / X / C / V** | 1. / 2. / 3. / 4. yetenek |
| **R** (basılı tut) | Bloklama |
| Sol tık | Uzaylıya özel yakın dövüş (yakar, dondurur, zehirler, savurur...) |

Tuşlar Ayarlar → Kontroller → "Ben 10 Omnitrix" bölümünden değiştirilebilir.
Dönüşüm varsayılan olarak 5 dakika sürer. Süre dolunca Omnitrix 20 saniye şarj olur. Bu değerler `config/krolben10-common.toml` dosyasından ayarlanabilir (süre, şarj, hasar ve bekleme çarpanları, dünyaya etki aç/kapa).

| Uzaylı | 1 | 2 | 3 | 4 | Bloklama | Pasif |
|---|---|---|---|---|---|---|
| Heatblast | Ateş Topu | Alev Püskürtücü (buz/kar eritir, yakar) | Süpernova (kumu cama çevirir) | Ateş Sütunları | Ateş Kalkanı (mermileri yakar) | Ateşe bağışık, uçar, su zarar verir |
| Four Arms | Yer Sarsıntısı (toprak parçaları uçar) | Gök Gürültüsü Alkışı (camları kırar) | Dev Sıçrayış + iniş darbesi | Kavra ve Fırlat (yaratık yoksa yerden kaya söker) | Dörtlü Kol Savunması | Dev güç, yüksek zıplama |
| Diamondhead | Kristal Mermiler | Kristal Dikenler | Kristal Duvar (geçici bloklar) | Kristal Yağmuru | Kristal Kalkan (okları geri yansıtır) | Çok yüksek zırh |
| XLR8 | Hız Atılımı | Kasırga (çeker, savurur) | Şimşek Kombosu (7 hedef) | Aşırı Hız (etraf yavaşlar) | Kaçınma | Süper hız, suda koşar |
| Goop | Asit Tükürüğü (asit birikintisi) | Sümük Kırbacı (çeker) | Yutma | Sümük Dalgası | Sıvı Beden (mermiler geçer) | Düşme/zehir bağışıklığı |
| Blowpoke | Yuvarlanma | Gülle Atışı | Sekme Bombası (3 zıplama) | Bilardo Sekmesi | Zırhlı Kabuk (%90) | Ağır zırh |
| Big Chill | Buz Nefesi (suyu dondurur, lavı obsidyen yapar) | Buz Hapsi | Buz Mızrağı | Kar Fırtınası | Kanat Pelerini (saldıranı dondurur) | Uçar, donmaya bağışık |
| Ultimate Echo Echo | Ses Diskleri (yapışıp titreşir) | Sonik Işın | Disk Yağmuru (güdümlü) | Sonik Kıyamet | Disk Duvarı | Süzülür/uçar |
| Ghostfreak | Gölge Pençe | Korku Bakışı (yaratıklar kaçar) | Ele Geçirme (yaratığı müttefiklerine saldırtır) | Hayalet Formu (hasar almaz) | Hayalet Kaçış (%50) | Uçar, gece görür |

`gen/` klasöründeki Python betikleri model/texture/animasyon/ses dosyalarını üretir (`python3 gen/build.py`).
