# Fjord Mist Shaders (Minecraft 1.20.1 Forge)

Yağmurlu, sisli, kasvetli bir İskandinav fiyordu havası için yapılmış shader paketi:
bulutların içinde kaybolan dağlar, yamaçlara tutunan sis, koyu yeşil-gri dalgalı su,
kıyıya vuran köpük ve rüzgarda sallanan ıslak çimenler.

## Kurulum

1. Forge 1.20.1 için **Oculus** (shader yükleyici) ve **Embeddium** (veya Rubidium) modlarını kur.
2. `FjordMist.zip` dosyasını `.minecraft/shaderpacks/` klasörüne kopyala (zip'i açma).
3. Oyunda `Seçenekler → Video Ayarları → Shader Paketleri` menüsünden **FjordMist.zip**'i seç.
4. Ayarlar düğmesinden profil seç: Düşük / Orta / Yüksek / Ultra.

İpucu: Referans fotoğraftaki havayı en iyi `/weather rain` ile, deniz kenarında,
sabah ya da öğleden sonra görürsün.

## Özellikler

| Bölüm | Neler var |
|---|---|
| Atmosfer | Yükseklikle yoğunlaşan pus, su üstünde alçak sis, dağ yamaçlarında sürüklenen sis kuşağı, gölgeli hacimsel ışık huzmeleri, sis için derinliğe duyarlı gürültü giderme |
| Bulutlar | Hacimsel bulutlar; yağmurda alçalıp kalınlaşıyor, yüksek dağların zirveleri bulutun içinde kayboluyor |
| Su | Fırtınada büyüyen, tepeleri köpüren dalgalar; rüzgar yönünde uzun dalgalar + kısa dalgacıklar, kırılma, derinliğe göre renk kaybı (koyu yeşil-gri fiyort suyu), ekran uzayı yansımaları (SSR), güneş parıltısı, kıyıya doğru yuvarlanan köpük şeritleri, hareketli su yüzeyi |
| Yağmur | Suya düşen her damla için sıçrama ve yayılıp sönen dalga halkaları, hareket bulanıklıklı yağmur çizgileri, uzakta yağmur perdeleri (yansımalarda ve siste bozulmaz), ıslanınca koyulaşan yüzeyler, düz zeminlerde su birikintileri ve yansımaları, gökyüzü ışığını yansıtan yağmur çizgileri, lense düşen yağmur damlaları |
| Bitkiler | Katmanlı rüzgar: araziyi dalga dalga geçen esintiler, ağaç taçlarının birlikte salınması, dalların hareketi ve yaprak titremesi; fırtınada hepsi şiddetlenir. Çimen, çiçek, ekin, uzun bitkiler ve sarmaşıklar rüzgar yönünde eğilir (gölgeleri de sallanır), yapraklardan ışık geçmesi |
| Işık | Elde tutulan meşale/fener/ışık taşı etrafı aydınlatır, yumuşak gölgeler, kapalı havada dağınık gri ışık, sıcak meşale ışığı, gündoğumu/günbatımı renkleri, ay ışığı |
| Gökyüzü | Fotoğraf gibi gece göğü: binlerce farklı parlaklık ve renkte parıldayan yıldız, mor-pembe-mavi tonlarda parlak çekirdekli Samanyolu (karanlık toz şeritleri ve pembe bulutsularla), ufukta pembe-turuncu gece parıltısı, gökyüzüyle birlikte dönen yıldızlar, yağmursuz gecelerde açılan bulutlar; evreleri olan, kraterli gerçekçi ay ve hilale göre değişen ay ışığı; ara sıra kayan yıldızlar; bazı açık gecelerde dalgalanan kuzey ışıkları (aurora, suda da yansır); yağmur dinince güneşin karşısında çift gökkuşağı; şimşek çakınca bulutlar ve dünya bir anlığına aydınlanır, şimşeğin kendisi göz kamaştırır |
| Gerçekçilik | Ortam gölgelendirmesi (köşeler, ağaç altları, çimen araları kararır), sığ suyun dibinde ve su altında dalgalanan ışık desenleri (kostik), su birikintilerinde de yağmur halkaları |
| Canlı dünya | Hareket eden bulutların araziye düşen gölgeleri ve bulut aralarından süzülen huzmeler; sakin gecelerde çimenlerin üstünde yanıp sönen ateşböcekleri; gece dalgalarda, köpükte ve yağmur damlalarında mavi parlayan plankton; su altında dalgaların odakladığı ışık huzmeleri ve süzülen parçacıklar; dalgaların ıslattığı kıyı şeridi; güneşte parıldayan ıslak taş ve yapraklar; karlı biyomlarda kar yağarken biriken kar ve yumuşak kar taneleri; alacakaranlıkta Dünya'nın gölgesi ve pembe Venüs kuşağı |
| Dünya değişimleri | Mevsimler (ilkbahar → yaz → sonbahar → kış; yapraklar kızarıp sararır, ladinler yeşil kalır, kışın her yer karla kaplanır ve yağmur kara döner); kışın aşağı inen karlı dağ zirveleri; soğuk gecelerde çimen ve yapraklarda sabah güneşiyle eriyen kırağı; nemli ve su kenarındaki taşlarda yosun; şelalelerde aşağı akan köpük; sudaki rüzgar izleri; titreyen meşale ve ateş ışığı; çimen, yaprak ve zeminde doğal renk farklılıkları; ışıkta parıldayan cevherler, ametist, kar ve buz; mağaralarda nemli duvarlar ve meşale ışığında parlayan damlalar |
| Kamera | Otomatik pozlama, bloom, ACES tonlama, güneşe bakınca lens parlaması, sudan çıkınca lensten süzülen damlalar, karanlıkta gerçek göz gibi renklerin solması (Purkinje), serin "fiyort" renk tonu, vinyet, film greni |

**Fiyort Havası (kapalı gök)** ayarı yağmur yokken bile gökyüzünün ne kadar gri ve
kasvetli olacağını belirler (`0` = açık hava, `1.0` = sürekli fırtına). Tüm ayarlar
Türkçe ve İngilizce menüde mevcut.

Nether ve End için de ayrı (sadeleştirilmiş) atmosfer var.

## Geliştirme

- `FjordMist/shaders/program/*.glsl` – asıl shader kodu, `lib/*.glsl` – ortak kütüphaneler.
- `tools/generate.py` – her program için `.vsh/.fsh` yükleyicilerini (Overworld, Nether, End)
  ve `textures/noise.png` dosyasını üretir. Program ekledikten sonra çalıştır.
- `tools/validate.py <glslangValidator>` – tüm programları derleyip hataları gösterir.
- `tools/preview.py` – sahte bir fiyort sahnesini tüm geçişlerden geçirip PNG üretir
  (GPU gerekmez, Mesa ile çalışır): `xvfb-run -a python3 tools/preview.py out.png rain`
- `tools/build_zip.py` – `FjordMist.zip` dosyasını yeniden oluşturur.

Geçiş sırası: `shadow` → `gbuffers_*` → `composite` (gökyüzü, bulutlar, su, yansımalar)
→ `composite1` (hacimsel sis/ışık) → `composite2` (sis gürültü giderme ve uygulama)
→ `final` (bloom, pozlama, tonlama, renk).
