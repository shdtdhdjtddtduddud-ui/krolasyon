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
| Su | Rüzgar yönünde uzun dalgalar + kısa dalgacıklar, kırılma, derinliğe göre renk kaybı (koyu yeşil-gri fiyort suyu), ekran uzayı yansımaları (SSR), güneş parıltısı, kıyıya doğru yuvarlanan köpük şeritleri, yağmur halkaları, hareketli su yüzeyi |
| Yağmur | Islanınca koyulaşan yüzeyler, düz zeminlerde su birikintileri ve yansımaları, gökyüzü ışığını yansıtan yağmur çizgileri, lense düşen yağmur damlaları |
| Bitkiler | Rüzgarda sallanan çimen, çiçek, ekin, uzun bitkiler, yapraklar ve sarmaşıklar (gölgeleri de sallanır), yapraklardan ışık geçmesi |
| Işık | Yumuşak gölgeler, kapalı havada dağınık gri ışık, sıcak meşale ışığı, gündoğumu/günbatımı renkleri, ay ışığı |
| Kamera | Otomatik pozlama, bloom, ACES tonlama, serin "fiyort" renk tonu, vinyet, film greni |

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
