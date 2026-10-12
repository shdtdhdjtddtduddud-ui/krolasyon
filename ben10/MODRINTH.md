# Modrinth listing - Ben 10 Omnitrix

## Project settings

- **Project type:** Mod
- **Name:** Ben 10: Omnitrix Unleashed
- **URL slug:** `ben10-omnitrix-unleashed`
- **Summary (short description):**
  Slam the Omnitrix and transform into 9 iconic aliens - each with 4 unique abilities, a special guard, passive powers, custom models, animations and world-shaping effects.
- **Categories:** Adventure, Game Mechanics, Equipment
- **Environment:** Client: Required - Server: Required
- **License:** All Rights Reserved (or pick MIT / CC BY-NC 4.0 if you want others to reuse it)
- **Source code (optional):** link to your GitHub repo if you want to share it

## Version upload

- **File:** `ben10_omnitrix-1.20.1-forge-1.0.0.jar`
- **Version name:** Ben 10 Omnitrix 1.0.0
- **Version number:** `1.0.0`
- **Release channel:** Beta (recommended for the first public version) or Release
- **Loaders:** Forge
- **Game versions:** 1.20.1
- **Dependencies:** none (only Forge 47.x)
- **Changelog:**

```
Initial release
- Omnitrix item with an animated selection dial (G / right-click)
- 9 aliens: Heatblast, Four Arms, Diamondhead, XLR8, Goop, Cannonbolt, Big Chill, Ultimate Echo Echo, Ghostfreak
- 36 unique abilities (4 per alien) + 9 unique guards
- Custom models, textures, glowing details and keyframe animations for every alien
- Procedural walk / run / fly / float / roll animations
- Abilities affect the world: melting, freezing, crystal walls, ice prisons, shattering glass, flying debris
- Omnitrix timer, recharge and HUD with ability cooldowns
- Configurable duration, recharge, damage, cooldowns and world griefing
- English and Turkish translations
```

## Description (paste into the "Description" tab)

```markdown
# Ben 10: Omnitrix Unleashed

**It's hero time!** Strap on the Omnitrix, spin the dial and slam it down to become one of **9 iconic aliens**. Every alien has its own body, size, stats and fighting style, plus **4 special abilities**, a **unique guard**, and passive powers. Nothing here is decorative: every power does real damage and many of them change the world around you.

---

## Features

- **Omnitrix dial** - a rotating selection wheel with alien portraits, ability previews and a green transformation flash
- **9 playable aliens** with custom models, textures, glowing eyes and details
- **36 abilities + 9 guards** with keyframe animations, particles, sounds and camera shake
- **Procedural animations** - walking, sprinting, flying, ghostly floating, rolling, punching
- **World interaction** - melt ice, freeze lakes, turn lava to obsidian, bake sand into glass, raise crystal walls, rip boulders out of the ground, shatter glass with sound waves
- **Alien stats** - extra health, armour, speed, strength, step height, reach and hitbox size per alien
- **Omnitrix timer** - transformations time out, the watch beeps and needs to recharge (fully configurable)
- **HUD** - alien portrait, time bar, ability list with cooldowns and the guard key
- **Multiplayer synced** - other players see your alien form, animations and effects

---

## Controls

| Key | Action |
|---|---|
| **G** or right-click the Omnitrix | Open the dial - scroll/click to choose, click again (or Enter) to transform. "Return to Human" button |
| **Z / X / C / V** | Ability 1 / 2 / 3 / 4 |
| **R** (hold) | Guard |
| **Left click** | Alien melee (burns, freezes, poisons, stuns, knocks back...) |

All keys can be rebound in *Options → Controls → Ben 10 Omnitrix*.

---

## The Aliens

### 🔥 Heatblast
**Fireball** · **Flamethrower** · **Supernova** · **Fire Geysers**
Guard: *Fire Shield* - burns attackers and destroys incoming arrows.
Passive: immune to fire and lava, can fly, punches ignite, melts snow and ice. Water hurts him!

### 💪 Four Arms
**Ground Slam** · **Thunder Clap** · **Mighty Leap** · **Grab & Throw**
Guard: *Four-Arm Block*.
Passive: huge health and strength, very high jumps, long reach. No mob nearby? Grab & Throw rips a boulder out of the ground instead!

### 💎 Diamondhead
**Crystal Shards** · **Crystal Spikes** · **Crystal Wall** · **Crystal Storm**
Guard: *Crystal Shield* - reflects projectiles back at the shooter.
Passive: very high armour. Crystal walls are temporary and shatter by themselves.

### ⚡ XLR8
**Speed Dash** · **Cyclone** · **Blitz Combo** · **Overdrive**
Guard: *Evasion* - dodges every projectile and often melee hits.
Passive: super speed, runs across water, steps up blocks. Overdrive slows down everything around you.

### 🟢 Goop
**Acid Spit** · **Slime Whip** · **Engulf** · **Goo Wave**
Guard: *Liquid Body* - projectiles pass right through.
Passive: no fall damage, immune to poison, punches poison, floats with his anti-gravity UFO. Acid leaves damaging puddles and dissolves plants.

### 🛡️ Cannonbolt
**Rolling Charge** · **Cannonball Launch** · **Bounce Bomb** · **Ricochet**
Guard: *Armoured Shell* - curls into a ball and blocks 90% of damage.
Passive: heavy armour, hard to knock back, no fall damage. Crushes everything in his path while rolling.

### ❄️ Big Chill
**Ice Breath** · **Ice Prison** · **Ice Spear** · **Blizzard**
Guard: *Wing Cloak* - freezes attackers.
Passive: flies, immune to freezing, freezes water beneath him. Ice breath turns lava into obsidian.

### 🔊 Ultimate Echo Echo
**Sonic Discs** · **Sonic Beam** · **Disc Barrage** · **Sonic Doom**
Guard: *Disc Wall* - a spinning wall of discs blocks projectiles.
Passive: hovers and flies, punches stun and shatter glass. Discs stick to enemies and pulse with sound.

### 👻 Ghostfreak
**Shadow Claw** · **Fear Gaze** · **Possession** · **Phantom Form**
Guard: *Ghostly Evasion* - 50% chance to avoid any attack.
Passive: flies, night vision, no fall damage, claws inflict wither. Possess a mob and turn it against its allies!

---

## Getting the Omnitrix

Find it in the **Ben 10 Omnitrix** creative tab or use:
`/give @s krolben10:omnitrix`

---

## Configuration

`config/krolben10-common.toml`

| Option | Default | Description |
|---|---|---|
| `durationSeconds` | 300 | Transformation time (0 = unlimited) |
| `rechargeSeconds` | 20 | Recharge time after timing out |
| `abilityGriefing` | true | Abilities change blocks (melt, freeze, walls, fire) |
| `damageMultiplier` | 1.0 | Multiplier for all ability damage |
| `cooldownMultiplier` | 1.0 | Multiplier for all ability cooldowns |

> Heatblast can set grass and leaves on fire. Turn off `abilityGriefing` or use `/gamerule doFireTick false` if you don't want fire spreading.

---

## Requirements

- Minecraft **1.20.1**
- **Forge 47.x**
- Install on **both client and server**

---

*This is an unofficial fan-made mod. It is not affiliated with, endorsed by or sponsored by Cartoon Network or Warner Bros. Discovery. Ben 10 and all related characters are trademarks of their respective owners.*
```

## Gallery suggestions

Use in-game screenshots (F2), one per alien plus a few ability shots:

1. **Omnitrix Dial** - "Spin the dial and choose your alien"
2. **All 9 aliens** - "Nine playable alien forms"
3. **Heatblast - Supernova** - "Burn everything around you"
4. **Four Arms - Ground Slam** - "Shake the earth"
5. **Diamondhead - Crystal Wall** - "Raise crystal barriers"
6. **Big Chill - Ice Prison** - "Freeze your enemies solid"
7. **Ultimate Echo Echo - Sonic Doom** - "Sonic destruction"
8. **HUD** - "Timer, abilities and cooldowns at a glance"

Featured image: the screenshot with all 9 aliens.
Icon: the Omnitrix (`src/main/resources/assets/krolben10/textures/item/omnitrix.png`, upscaled to 512x512 with nearest-neighbour).
