# Itachi Arena – Roblox arena dövüş oyunu

Roblox Studio'da doğrudan açılan bir arena dövüş oyunu (Battlegrounds tarzı).
İlk karakter: **Itachi** (Akatsuki pelerini, Sharingan/Mangekyō, 4 jutsu).
Hiçbir dış asset/animasyon ID'si gerekmez: model, animasyonlar ve efektler tamamen kodla üretilir.

## Studio'da açma

1. `ItachiArena.rbxlx` dosyasını indir.
2. Çift tıkla veya Roblox Studio → **File → Open from File** ile aç.
3. **Play (F5)** tuşuna bas.

Arena, karakter modeli, kuklalar ve sparring botu oyun başlarken kodla inşa edilir.
Modeli/arenayı elle düzenlemek istersen: Play'e bas → Explorer'da `Workspace.Arena`
(veya kendi karakterini) kopyala → Stop → Workspace'e yapıştır.

## Kontroller

| Tuş | Eylem |
|---|---|
| Sol tık | M1 kombo (4 vuruş, son vuruş fırlatır) |
| F (basılı tut) | Blok – vuruştan hemen önce basarsan **PARRY** |
| Q | Dash (W/A/S/D yönüne, kısa dokunulmazlık) |
| 1 | **Katon: Gōkakyū** – el mühürleri + dev ateş topu, patlama + yanma |
| 2 | **Karasu Bunshin** – kargalara dönüşüp düşmanın arkasında belirir, tekme |
| 3 | **Amaterasu** – nişan alınan yerde sönmeyen siyah alevler (bloklanamaz) |
| 4 | **Tsukuyomi** – göz teması: hedefi genjutsuya hapseder (ekran kızıl ay efekti) |
| Shift | Shift Lock |

Mobilde dokunmatik butonlar otomatik oluşur, gamepad de desteklenir (R2/L2/B).

## Dövüş mekanikleri

- **Blok:** önden gelen hasarın %85'ini engeller, hareket yavaşlar.
- **Posture:** blokladığın hasar posture barını düşürür; biterse **GUARD BREAK** (sersemleme).
- **Parry:** bloğa vuruştan 0.22 sn önce basarsan saldırgan sersemler.
- **Dash:** i-frame verir, afterimage + toz efektleri.
- **Stun / knockback / hasar sayıları / kamera sarsıntısı.**
- Tüm sayılar `src/ReplicatedStorage/Shared/Config.lua` içinde.

## Arenada neler var

- Meşaleli sütunlar, kızıl ay, Akatsuki bulutu zemin, siper kayaları
- 2 antrenman kuklası, 1 sürekli blok yapan kukla, 1 **Sparring Itachi** botu
  (yaklaşır, kombo atar, bloklar, jutsu kullanır – `Config.SpawnSparringBot` ile kapatılabilir)
- Modeli incelemek için kaide üzerinde Itachi heykeli

## Proje yapısı

```
src/
  ReplicatedStorage/Shared/
    Config.lua          denge ayarları, ses ID'leri
    ItachiBuilder.lua   karakter modeli (R6 rig + kıyafet + yüz)
    Poses.lua           prosedürel animasyon pozları / keyframe'ler
    VFX.lua             tüm görsel efektler (istemci)
  ServerScriptService/
    ArenaBuilder        arena + ışık
    CombatServer/       ana sunucu + CombatCore (hasar/blok/parry) + ItachiKit (4 yetenek)
  StarterPlayer/
    StarterPlayerScripts/CombatClient         girdi, dash, HUD
    StarterPlayerScripts/AnimationController  tüm karakterleri canlandırır
    StarterCharacterScripts/Animate           varsayılan animasyonları kapatır
```

`.rbxlx` dosyasını yeniden üretmek için: `python3 build.py`
Rojo kullanıyorsan `default.project.json` hazır (`rojo serve`).

## Yeni karakter eklemek

1. `Shared/` içine `XBuilder.lua` (model) yaz.
2. `CombatServer/` içine `XKit.lua` (yetenekler) yaz – `ItachiKit.lua` örnek.
3. `Poses.Actions` içine yeteneklerin pozlarını, `VFX.lua` içine efektlerini ekle.

## Notlar

- Sesler Roblox'un dahili sesleridir; daha iyi sesler için `Config.Sounds` içine
  Creator Store'dan ses ID'leri koy.
- Itachi / Naruto bir telif hakkı korumalı karakterdir. Oyunu herkese açık yayınlayacaksan
  isim ve görünümü özgün bir karaktere dönüştürmen önerilir (Roblox topluluk kuralları).
