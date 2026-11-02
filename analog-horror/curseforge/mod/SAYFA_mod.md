# CurseForge — "The Hatman" mod sayfası (kapaktan galeriye)

Bu dosyayı yukarıdan aşağıya takip et. Her bölümde CurseForge'daki alanın adı ve kopyalayıp yapıştıracağın metin var.

---

## 1. Proje oluşturma (Create Project)

| Alan | Değer |
|---|---|
| **Project Type** | Mods (Minecraft: Java Edition) |
| **Project Name** | `The Hatman: Analog Horror` |
| **Project Avatar / Logo** | `logo_400.png` (400×400) |
| **Primary Category** | Mobs |
| **Additional Categories** | Adventure and RPG · Structures · Cosmetic |
| **License** | All Rights Reserved (mods.toml ile aynı) |
| **Source / Issues URL** | GitHub repo bağlantın (isteğe bağlı) |

### Summary (kısa özet, en fazla ~250 karakter)
```
An analog-horror stalker that freezes when you watch it and creeps closer when you look away — plus a haunted house and a 5-chapter story with cutscenes, tapes and notes. English & Turkish.
```

---

## 2. Description (açıklama) — CurseForge editöründe "Markdown" seç ve yapıştır

> Önce `banner_1200x400.png` dosyasını projenin **Images** sekmesine yükle, çıkan bağlantıyı aşağıdaki `BANNER_URL` yerine koy.

```markdown
![The Hatman](BANNER_URL)

## He freezes when you look at him. He gets closer when you don't.

**The Hatman** is an analog-horror mod for **Forge 1.20.1 and 1.21.1**. A tall, emaciated figure with fork-like fingers and a wide straw hat over a face you never quite see. He does not run at you in the open. He waits behind corners, snuffs out your torches, knocks on doors behind you — and the moment you turn around, he is standing still. Exactly like the photograph.

---

### 🎩 The creature
- Highly detailed **GeckoLib** model (240 parts): ribs, collarbones, knobby spine, three-jointed claws, woven straw hat, old leather loafers
- **19 animations**: stalking, sprinting, spider-crawl, the frozen "photograph" pose, peeking around corners, leaps, claw swipes, grab-and-slam, a scream, a 360° head twitch, a polite hat tip, unfolding from the floor…
- **Stalker AI**
  - Freezes while observed (view cone + line of sight), creeps closer when you look away and may even appear closer after you blink
  - Hides behind cover, snuffs out torches, knocks and whispers behind you
  - Hunts, leaps and grabs; crawls through low gaps; searches your last known position
- **Haunting director**: he doesn't spawn like a normal mob. After a quiet build-up at night or deep underground, **one** Hatman unfolds somewhere out of sight.
- **Analog dread**: VHS static, a heartbeat that speeds up, camera shake and a jumpscare when you meet him face to face — all generated sound design.

### 🏚️ The Ardel house
`/hatman ev` (or `/hatman house`) builds a two-storey haunted house:
- floral-wallpapered hallways, a living room with a CRT that never stops showing static
- a stair landing with balloons, an upstairs hall with candle sconces
- Victor's attic workshop
- a wrap-around porch, a turret, chimneys and an overgrown yard

**23 furniture blocks** are included in their own creative tab.

### 📼 Story mode — *The Hatman: Rent Due*
`/hatman story` (or `/hatman hikaye`) starts a **linear five-chapter horror story** set entirely inside the house:
- **20 readable notes**, **4 VHS tapes** with their own footage, **9 cinematic cutscenes** with subtitles
- puzzles: a knock code, fuses, candle order, transmitter dials
- timed appearances, a "never look away" corridor, a free-roaming hunter and two chases
- **3 endings** — one of them secret
- Being caught never kills you: jumpscare, fade to black, back to the last checkpoint
- Items for the next objective only appear when the current one is finished

> Singleplayer only. Estimated play time: 1–2 hours.

### 🌐 Languages
Everything — subtitles, notes, tapes, objectives — follows your game language: **English** or **Türkçe**.

---

### ⌨️ Commands
| Command | |
|---|---|
| `/hatman story` · `/hatman hikaye` | Start the story (singleplayer) |
| `/hatman stop` | Leave the story |
| `/hatman durum` · `/hatman status` | Show story progress (if you're stuck) |
| `/hatman ev` · `/hatman house` | Build the haunted house (operator) |
| `/summon krolasyonhorror:hatman` | Summon the Hatman |

### 📦 Requirements
- Minecraft Forge **1.20.1** (47.2+) or **1.21.1** (52.1+)
- **GeckoLib** (4.4+ for 1.20.1, 4.5+ for 1.21.1)

### 🎨 Recommended shaderpack
**Hatman Analog** — made for this mod: candle flicker, ground mist, glowing sconces and a Hatman that swallows light. *(link to the shaderpack page)*

---

*Please don't look away.*
```

---

## 3. Açıklamanın Türkçe sürümü (isterseniz İngilizcenin altına ekleyin)

```markdown
---
## 🇹🇷 Türkçe

**Şapkalı Adam**, Forge 1.20.1 ve 1.21.1 için bir analog korku modu.

Uzun, kupkuru, çatal parmaklı bir yaratık; yüzü geniş bir hasır şapkanın altında hep gizli. Ona bakarken kıpırdamaz, gözünü ayırdığında yaklaşır.

**Yaratık**
- 240 parçalı detaylı model ve 19 animasyon
- Gelişmiş takipçi yapay zekası: izlenince donar, bakmayınca yaklaşır, meşale söndürür, arkanda kapı çalar, koşar, sıçrar, yakalar
- Gece ya da karanlıkta tek bir Şapkalı Adam'ın beliriverdiği "musallat" sistemi

**Ev**
- `/hatman ev` ile iki katlı lanetli Ardel evi kurulur
- 23 dekor bloğu kendi yaratıcı sekmesinde

**Hikâye modu (Kira Günü)**
- `/hatman hikaye` ile başlar
- 5 bölüm, 20 not, 4 kaset, 9 sinematik ara sahne
- Bulmacalar ve kovalamacalar
- 3 son (biri gizli)
- Yakalanınca ölmezsin, son kayıt noktasına dönersin
- Bir sonraki görevin eşyaları ancak mevcut görev bitince açılır

**Dil:** Tüm metinler Türkçe ve İngilizce.

**Gereken:** GeckoLib. **Önerilen:** Hatman Analog shaderpack.
```

---

## 4. Images (galeri) — `gallery/` klasöründeki dosyalar

| Dosya | Title (başlık) | Description |
|---|---|---|
| 01_ardel_house_night.png | The Ardel house | 13 Ashgrove Lane, the night the rent is due. |
| 02_porch_intro.png | The porch | The opening cutscene: a voicemail and a locked door. |
| 03_hatman_model.png | The Hatman | 240-part GeckoLib model. |
| 04_frozen_like_the_photo.png | Don't look away | Watched, he freezes in his photograph pose. |
| 05_waiting_in_the_doorway.png | The doorway | He waits where the light ends. |
| 06_upstairs_hallway.png | Upstairs hallway | Candle sconces, pictures, a runner rug. |
| 07_living_room_tv.png | The television | It never stops showing static. |
| 08_cutscene_subtitles.png | Cutscenes | Cinematic camera, letterbox and subtitles. |
| 09_vhs_tape_player.png | Channel 13 | "Mr. Brim's Neighborly Hour". |
| 10_notes.png | 20 notes | Every page is part of the mystery. |
| 11_chapter_card.png | Five chapters | A linear story inside one house. |
| 12_turkish_language.png | Türkçe | The whole story follows your game language. |
| 13_studio_finale.png | Rent due | The finale on Mr. Brim's set. |
| 14_scream.png | Too long | Stare too long and he screams. |

İlk görseli (01) **Featured** olarak işaretle.

---

## 5. Dosya yükleme (Upload File) — her sürüm için ayrı

| Alan | 1.20.1 dosyası | 1.21.1 dosyası |
|---|---|---|
| **File** | `krolasyon_horror-1.20.1-forge-2.0.0.jar` | `krolasyon_horror-1.21.1-forge-2.0.0.jar` |
| **Display Name** | `The Hatman 2.0.0 (Forge 1.20.1)` | `The Hatman 2.0.0 (Forge 1.21.1)` |
| **Release Type** | Release | Release |
| **Game Version** | 1.20.1 | 1.21.1 |
| **Mod Loader** | Forge | Forge |
| **Java** | Java 17 | Java 21 |
| **Related Projects** | GeckoLib → *Required Dependency* | GeckoLib → *Required Dependency* |

### Changelog (Markdown)
```markdown
## 2.0.0 — Rent Due
- New: story mode *The Hatman: Rent Due* — 5 chapters, 20 notes, 4 tapes, 9 cutscenes, 3 endings (EN/TR)
- New: grand haunted house exterior (porch, turret, chimneys, yard) and 23 furniture blocks
- New: `/hatman durum` / `/hatman status` progress command
- Objectives unlock in order: items for the next objective appear only after the current one
- Much less screen static in the VHS overlay
- Fix: tapes that were closed unusually could stop the story
- Fix: transmitter dials accept the code from both sides of the desk
```
