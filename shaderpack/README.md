# Krolasyon Fantasy Shaders

Minecraft **1.20.1 Forge** için fantastik temalı shader paketi
(Oculus ile çalışır; Fabric'te Iris ile de çalışır).

Rüya gibi pembe-lavanta gün batımları, halkalı dev bir gezegen ve nebulalarla dolu
geceler, gece ışıldayan ormanlar ve ateş böcekleri.

## Özellikler

**Gökyüzü**
- Temaya göre değişen gökyüzü degradesi; pembe/mor gün batımı ve karşı ufukta "venüs kuşağı"
- Halkalı dev gezegen ve iki küçük ay (gündüz soluk, gece net görünür)
- Galaksi/nebula bandı, iki katmanlı ve göz kırpan yıldızlar, kutup ışıkları (aurora)
- Evreleri olan prosedürel ay, parlak güneş diski
- Kendi gölgesini hesaplayan, gün batımında pembeleşen prosedürel bulutlar

**Fantastik efektler**
- **Biyolüminesans:** gece yapraklarda parlayan damar ağı ve göz kırpan ışık noktaları,
  çiçek taç yaprakları kendi renginde ışıldar, mantarlar parlar
- **Işıklı ağaç damarları:** kütüklerde yukarı doğru akan ışık
- **Ateş böcekleri** (gece), **büyülü toz** (gündüz), **közler** (Nether)
- Ametist ve değerli cevherlerde **kristal parıltısı**
- Gece su yüzeyinde **ışıldayan plankton**

**Işık ve atmosfer**
- Yumuşak, renkli gölgeler (vitray rengi gölgeye geçer)
- Hacimsel ışık huzmeleri (güneş, ay, su altı)
- Ekran uzayı ortam kapatma (SSAO), elde tutulan ışık
- Yükseklik ve günün saatine bağlı sis, görüş mesafesi kenarında gökyüzüyle kaynaşma

**Su**
- Dalgalı yüzey, ekran uzayı yansımaları + gökyüzü yansıması, Fresnel
- Derinliğe bağlı turkuaz renk emilimi, kostikler, su altı sisi ve ışık huzmeleri

**Kamera**
- Çok katmanlı bloom, otomatik pozlama, ACES ton eşleme
- Fantastik renk derecelendirme (mor gölgeler, sıcak pembe-altın parlak tonlar)
- FXAA, keskinleştirme, vinyet

**Boyutlar:** Overworld, Nether (kızıl sis, uçuşan közler) ve End (mor gökyüzü, nebula,
uçuşan ışıklar) için ayrı ayarlar.

## Temalar

| Tema | Açıklama |
|---|---|
| Rüya (varsayılan) | Lavanta gündüzler, pembe gün batımları, mor-mavi nebula |
| Pandora | Turkuaz geceler, zümrüt tonları |
| Altın Çağ | Altın gün batımları, amber geceler |

Tema, shader ayarları ekranının en üstündeki **Gökyüzü Teması** seçeneğinden değiştirilir.

## Kurulum (Forge 1.20.1)

1. Forge 1.20.1'i kurun.
2. `mods` klasörüne **Oculus** ve onun gerektirdiği **Embeddium** (ya da Rubidium) modunu koyun.
3. Bu depodaki GitHub sürümlerinden (Releases) `KrolasyonFantasy.zip` dosyasını indirin
   ya da `shaderpack/tools/package.sh` ile kendiniz paketleyin.
4. Zip dosyasını açmadan `.minecraft/shaderpacks/` klasörüne kopyalayın.
5. Oyunda **Seçenekler → Görüntü Ayarları → Shader Paketleri** menüsünden
   *KrolasyonFantasy* paketini seçin.

Ayarlar menüsünde Düşük / Orta / Yüksek / Ultra kalite profilleri vardır. Düşük
sistemlerde **Orta** profili önerilir.

## Geliştirme

```
shaderpack/
  KrolasyonFantasy/shaders/
    lib/        ortak kod (ayarlar, gökyüzü, gölge, aydınlatma, dalga)
    program/    asıl program kaynakları (gbuffers, shadow, deferred, composite*, final)
    *.vsh/*.fsh program giriş dosyaları (tools/gen_stubs.py üretir)
    world-1/    Nether, world1/ End
  tools/
    gen_stubs.py  giriş dosyalarını üretir
    validate.py   tüm programları Mesa ile derler/bağlar (4 farklı ayar kombinasyonu)
    preview.py    gökyüzü ve son işlem zincirini sentetik sahnede çizer
    package.sh    zip oluşturur
```

Yerelde doğrulama:

```
pip install moderngl numpy pillow
xvfb-run -a env MESA_GL_VERSION_OVERRIDE=4.5COMPAT python3 shaderpack/tools/validate.py
xvfb-run -a env MESA_GL_VERSION_OVERRIDE=4.5COMPAT python3 shaderpack/tools/preview.py out/
```
