# Labirent: Ölümcül Kaçış (Forge 1.20.1)

Kitaptan uyarlanmış, hikayeli, çok oyunculu labirent hayatta kalma modu.

- Oyuna giren ilk oyuncu **Kaptan** olur (Kaptan Asası + `K` ekranı ile emir ve görev verir).
- Herkes Ayrık'a **Kutu** (asansör) ile gelir; her Minecraft ayında (varsayılan 8 gün) Kutu yeni bir Ayrıklı ve erzak getirir.
- Akşamları dev **kapılar kapanır**, geceleri labirentin bölümleri **yer değiştirir**.
- Gezdiğin her yer ortak **haritaya** otomatik işlenir (`M` tam harita, sağ üstte mini harita).
- Görevler: Koşucu, Aşçı, Bahçıvan, Kasap, Şifacı, Bekçi — oyuncular ve NPC Ayrıklılar için.
- Kederciler, böcek-bıçaklar, Keder Serumu, Değişim, Şifre Taşları, Uçurum ve Kederci Deliği.
- 9 bölümlük hikaye: `J` ile Ayrık Günlüğü.

Komutlar: `/labirent durum | rol <oyuncu> <rol> | emir <metin> | kaptan <oyuncu>`; yöneticiler için `git | kutu | bolum <n> | sifirla`.

Varlıklar `gen/` içindeki Python betikleriyle üretilir (`build.py` model+animasyon, `textures.py`, `models.py`, `sounds.py`, `lang.py`).
