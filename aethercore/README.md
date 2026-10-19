# Krolasyon Eterik Çekirdek (Forge 1.20.1)

Kadim bir medeniyetten kalma, havada süzülen bir enerji çekirdeği. Plazma dolu cam bir kürenin etrafında
altın-inci renkli, mavi kanallı ve mercekli beş eğri zırh plakası, dönen ekvator bandı ve kutup kapakları var.

**Animasyonlar**
- Uykuda: plakalar küreyi sarar, yavaşça döner; çekirdek sönük atar.
- Aktif: plakalar dışarı açılır, farklı hızlarda yörüngede döner ve dalgalanır; çekirdek nabız gibi parlar;
  etrafta altıgen kalkan panelleri süzülür; 16 blokluk altıgen desenli kalkan kubbesi büyüyerek açılır.
- Kalkana bir mermi çarptığında kubbe parlar.

**İşlevi** (aktifken, 16 blok yarıçap)
- Düşman yaratıkları kubbenin dışına iter ve büyü hasarı verir
- Oyuncu olmayanların attığı okları, ateş toplarını, iksirleri vb. yok eder
- Kubbe içinde doğal canavar doğmasını engeller
- İçerideki oyunculara Yenilenme I ve Direnç I verir
- 15 seviye ışık yayar

**Yakıt** (sağ tık): Ametist Parçası 2 dk · Elmas 10 dk · Yankı Parçası 25 dk · Nether Yıldızı 4 saat (en fazla 8 saat).
Boş elle sağ tık: aç/kapat. Eğilerek sağ tık: durum ve kalan enerji.

**Nasıl bulunur**
- Overworld'de nadir **Kadim Sunaklar**: kuvars-kalsit platform, ametist taçlı sütunlar, kaide üzerinde
  uyuyan bir çekirdek ve ganimet sandığı.
- Tarif: `altın-ametist-altın / elmas-ender gözü-elmas / altın-ametist-altın`.

Derleme: `cd aethercore && gradle build`. Dokular `tools/make_textures.py` ile üretilir.
