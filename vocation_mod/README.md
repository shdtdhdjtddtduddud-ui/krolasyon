# Krolasyon Vocations (Forge 1.20.1)

Four vocation sets: **Elite Knight**, **Royal Paladin**, **Elder Druid**, **Master Sorcerer**.
Each set has:

- Helmet, armor, legs and boots (vanilla armor slots). Wearing all four pieces of one set gives its set effect.
- A weapon: Spiked Mace (sword), Royal Crossbow, Druid Staff and Sorcerer Staff (magic bolts, 1 durability per cast).
- An amulet and a ring. Right-click to wear; the bonus stays while worn. Sneak + right-click with an empty hand takes them all off. They survive relog and death.
- Ammunition (arrows with extra base damage).

## Build

```
cd vocation_mod
gradle build
```

The jar is written to `build/libs/krolasyon_vocations-1.20.1-forge-1.0.0.jar`.
The GitHub workflow `.github/workflows/vocation-build.yml` builds the same jar and uploads it as an artifact.

## Assets

`gen/make_assets.py <reference.jpg>` cuts the 32 item icons out of the reference sheet and paints the armor layer textures (`textures/models/armor/*_layer_{1,2}.png`) from per-set palettes.

## Not yet included

- Custom 3D armor geometry and animated cloak/robe models (armor currently uses the vanilla humanoid armor shape with the new textures).
- Backpack and shield items from the reference sheet.
- Crafting recipes and loot tables (items are obtained from the creative tab / `/give`).
