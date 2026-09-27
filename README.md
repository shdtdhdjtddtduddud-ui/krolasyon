# Krolasyon Bosses (Forge 1.20.1)

## Boss dönüşümleri

Her formun kendi **dönüşüm silahı** vardır (Yaratıcı sekmesi "Krolasyon Bossları" veya tarif).
Silahla **sağ tık** veya **H** → dönüşüm. **Eğilip sağ tık** veya **H** → geri dönüş.
Yetenek tuşları (Ayarlar > Kontroller'den değiştirilebilir): **R, G, V, Z, B**. Havada **Boşluk**: çift zıplama.

| Form (silah) | R | G | V | Z | B |
|---|---|---|---|---|---|
| Aigoar (Gelgit Kılıcı) | Gelgit Yırtığı | Girdap | Derinlik Fışkırması | Basınç Işını | Tsunami Çöküşü |
| Güneş Alevi Hükümdarı (Güneş Büyük Kılıcı) | Güneş Yarığı | Alev Kasırgası | Güneş Mührü | Anka Atılışı | Güneş Düşüşü (göktaşı) |
| Ejder Muhafızı (Ejder Palası) | Ejder Nefesi | İkiz Ejder Başı | Pul Kalkanı | Ejder Dalışı | Kızıl Ejder |
| Alev Ruhu (Kor Kılıcı) | Alev Adımı | Kor Yağmuru | Cehennem Novası | Alev Hilalleri | Kül Anka |
| Kızıl Gölge (Gölge Kılıcı) | Gölge Adımı | Kan Fırtınası | Gölge Kuyrukları | Kara Dikenler | İblis Uyanışı |
| Cehennem Mızrakçısı (Cehennem Mızrağı) | Cehennem Mızrağı | Delici Hücum | Kuyruk Kasırgası | Lav Sütunları | Cehennem Kapısı |

Tüm formlar: +30 can, +7 saldırı, +10 zırh, hız, erişim, düşme hasarı yok, çift zıplama; kendine özel
yürüme/koşma/zıplama/düşme/eğilme/yüzme/saldırı/ölüm animasyonları, sesler ve 1. şahıs kol/silah görünümü.
Ateş formları ateşe bağışıktır ve ateşte iyileşir; Kızıl Gölge karanlıkta görür ve iyileşir; Aigoar suda nefes alır.

## Geliştirme
Model, doku, animasyon ve sesler `gen/` altındaki Python betikleriyle üretilir:
`python3 gen/build.py` (modeller/dokular/animasyonlar), `python3 gen/sounds3.py` ve `gen/sounds4.py` (sesler).
Jar, her push'ta GitHub Actions tarafından derlenir ve Releases'e eklenir.
