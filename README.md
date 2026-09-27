# Krolasyon Bosses (Forge 1.20.1)

## Aigoar — Derinliklerin Efendisi (dönüşüm)

**Aigoar'ın Gelgit Kılıcı** ile Aigoar'a dönüşürsün (Yaratıcı sekmesi "Krolasyon Bossları" veya tarif).

| Eylem | Tuş (Ayarlar > Kontroller'den değiştirilebilir) |
|---|---|
| Dönüş | Kılıçla **sağ tık** veya **H** |
| Geri dön | **Eğilip (Shift) sağ tık** veya **H** |
| 1 · Gelgit Yırtığı — su itişli atılma, çapraz + yükselen pençe | **R** |
| 2 · Girdap — düşmanları içine çeken, döndüren ve patlayan girdap | **G** |
| 3 · Derinlik Fışkırması — yere pençe darbesi, ileri doğru su gayzerleri | **V** |
| 4 · Basınç Işını — su küresi toplar, delici yüksek basınçlı su ışını | **Z** |
| 5 · Tsunami Çöküşü — takla atarak sıçrama, genişleyen dev dalga halkası | **B** |
| Çift zıplama (su itişi) | Havada **Boşluk** |

Pasifler: +30 can, +7 saldırı, +10 zırh, hız, erişim; suda nefes, su/yağmurda yenilenme,
düşme hasarı yok, suda hızlı yüzme. Kendine özel yürüme/koşma/zıplama/düşme/eğilme/yüzme/saldırı/
ölüm animasyonları ve tüm sesler (adım, hasar, pençe, yetenekler) moda özgüdür.

Tarif: `P H P / C S C / _ N _` — P prizmarin kırığı, H deniz kalbi, C prizmarin kristali,
S elmas kılıç, N nautilus kabuğu.

## Geliştirme
Model, doku, animasyon ve sesler `gen/` altındaki Python betikleriyle üretilir:
`python3 gen/build.py` (model/doku/animasyon), `python3 gen/sounds3.py` (Aigoar sesleri).
Jar, her push'ta GitHub Actions tarafından derlenir ve Releases'e eklenir.
