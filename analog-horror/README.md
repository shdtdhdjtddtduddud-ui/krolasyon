# Analog Korku: Şapkalı Adam (The Hatman)

Minecraft **Forge 1.20.1** ve **Forge 1.21.1** için analog-horror modu. Uzun, kuru, ıslak-siyah derili bir yaratık; yüzünü hiç tam göremediğin geniş hasır bir şapka. Ona bakarken kıpırdamaz, gözünü ayırdığında yaklaşır.

## Kurulum
| Minecraft | Forge | Gerekli |
|---|---|---|
| 1.20.1 | 47.2+ | GeckoLib 4.4+ (1.20.1) |
| 1.21.1 | 52.1+ | GeckoLib 4.5+ (1.21.1) |

Jar dosyaları her CI derlemesinde GitHub Releases'a (`horror-<no>-<sürüm>`) yüklenir.

## Yaratık
- 240 parçalı GeckoLib modeli. Kaburgalar, soluk göğüs kemiği, köprücük ve kürek kemikleri, boğumlu omurga, üç eklemli çatal parmaklar ve tırnaklar var. Şapka örgü dokulu, bantlı ve kenarı kıvrık; ayakta eski deri loafer'lar.
- Fotoğraftaki haliyle 4 kat yoğun (birim başına 4 piksel) yüzey dokusu: ıslak parlak deri, kir, damar ve kırışıklıklar.
- 19 animasyon:
  - **Hareket:** bekleme, yürüme, koşma, sürünme (hareketli ve durağan), sinsi takip, sıçrama.
  - **İzlenirken:** fotoğraftaki donma pozu, köşeden bakma.
  - **Saldırı:** sağ ve sol pençe, yakalayıp yere çarpma, çığlık.
  - **Ürkütücü hareketler:** kafayı 360° çeviren seğirme, şapka selamı, etrafı koklama, meşale söndürme.
  - **Doğuş ve ölüm:** yerden açılarak doğma, ölüm (şapka düşer).

## Yapay zeka
- **Sinsi takip:** Oyuncunun göremediği, tercihen arkasında ve karanlık yerlerden yol bulur. Saldırganlığı arttıkça mesafeyi kapatır.
- **İzlenince donar:** Bakış konisi ve görüş hattıyla kontrol edilir. Zifiri karanlıkta ve uzaktaysa "görülmüş" sayılmaz.
  - Gözünü kırpıp başka yere bakarsan bazen bir anda daha yakında belirir.
  - Çok uzun bakarsan şapkasını selamlar, kaybolur ya da çığlık atıp saldırır.
- **Av:** Koşar, 5-11 blok mesafeden sıçrar, iki pençeyle sırayla vurur. Bazen oyuncuyu yakalayıp havaya kaldırır ve yere çarpar (karanlık ve yavaşlık verir).
- **Takılırsa:** Alçak geçitlerde sürünmeye geçer, açıldığında tekrar ayağa kalkar. Hiç yol bulamazsa paraziti arkasında bırakıp gölgeye ışınlanır.
- **Arama:** Oyuncuyu kaybedince son görüldüğü yere gider ve etrafı koklar.
- **Duyma:** Koşan ya da çok yakındaki oyuncunun yerini görmeden bilir.
- **Ortam oyunları:** Oyuncunun arkasında kapı vurma, ayak sesi, fısıltı ve uzak çağrılar. Görülmediğinde meşale, fener ve mumları söndürür (mobGriefing açıksa).
- **Vur-kaç:** Yeterince kan döktüğünde karanlığa çekilir. Canı azalınca bir kez kaçıp iyileşir.
- **Gün ışığı:** Açık gündüz ışığında oyalanmaz.
- **Musallat sistemi:** Normal doğma yerine her oyuncu için bir "dehşet" sayacı tutulur. Gece ya da karanlık derinliklerde bir süre sessizlikten sonra, görüş alanı dışında 26-40 blok uzakta tek bir yaratık doğar. Bir oyuncunun yanında aynı anda en fazla bir tane olur.

## Ses ve ekran efektleri
- Prosedürel olarak üretilmiş 29 ses: nefes ve çene tıkırtısı, kemik çıtırtıları, çığlık, ölüm feryadı, kalp atışı, VHS parazit, jumpscare, kapı vurma, fısıltı, gerilim dronu.
- **VHS katmanı:** Yaratık yaklaştıkça kenarlar kararır, ardından tarama çizgileri, kayan iz bantları, renk yırtılmaları ve parazit gelir.
- Gerilim arttıkça hızlanan kalp atışı, kovalamaca dronu ve kamera titremesi.
- Yakından göz göze gelince tam ekran jumpscare karesi.

## Geliştirme
- `gen/`: model (`hatman.py`), dokular (`paint_hatman.py`), animasyonlar (`hatman_anim.py`) ve sesler (`hsounds.py`). `python3 gen/build.py` ile varlıklar üretilir.
- `common/`: iki sürümün ortak Java kodu ve varlıkları.
- `forge-1.20.1/` ve `forge-1.21.1/`: sürüme özel ince uyumluluk katmanı (`compat/`) ve Gradle projeleri.
- CI (`.github/workflows/horror.yml`) iki sürümü derler. Başsız istemcide koridor sahnesini oynatıp ekran görüntüsü alır ve görüntüyü kaydeder.
- `trailer/trailer.py`: Bu kayıttan VHS "bulunan kaset" tanıtım videosunu kurgular ve sentezlenmiş sesle birleştirir.
