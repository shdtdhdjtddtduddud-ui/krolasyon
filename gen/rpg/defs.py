"""Single source of truth for the RPG content: monsters, bosses, spells and swords.

gen/rpg/build.py turns this into textures (PNG) and the generated Java table RpgDefs.java.
"""

# ---------------------------------------------------------------- regions
# id, Turkish name, kingdom index or -1 (wild), danger 1..5
REGIONS = [
    ('ALDORIA', 'Aldoria Ovaları', 0, 1),
    ('SYLVARIEN', 'Gümüşyaprak Ormanı', 1, 2),
    ('KHAZDUR', 'Demirdağlar', 2, 3),
    ('INFERNAX', 'Kül Toprakları', 3, 5),
    ('YMIRHEIM', 'Donuk Zirveler', 4, 4),
    ('GORMASH', 'Kızıl Bozkır', 5, 3),
    ('FELARIS', 'Pençe Vadisi', 6, 2),
    ('VALDREN', 'Kara Sınır', 7, 3),
    ('MERIDIA', 'Altın Kıyılar', 8, 2),
    ('NOCTHERA', 'Gecegölge Vadisi', 9, 4),
    ('WOLF_FOREST', 'Kurtların Ormanı', -1, 2),
    ('DEAD_MARSH', 'Ölüler Bataklığı', -1, 3),
    ('DRAGON_TEETH', 'Ejderdişi Dağları', -1, 4),
    ('GLASS_DESERT', 'Cam Çölü', -1, 3),
    ('WHISPER_STEPPE', 'Fısıltı Bozkırı', -1, 2),
    ('CRYSTAL_HOLLOW', 'Kristal Çukur', -1, 4),
    ('CURSED_RUINS', 'Lanetli Harabeler', -1, 4),
    ('STORM_COAST', 'Fırtına Kıyısı', -1, 3),
]

ARCHETYPES = ['HUMANOID', 'BRUTE', 'QUADRUPED', 'ARACHNID', 'INSECT', 'FLYER', 'SLIME', 'SERPENT',
              'GOLEM', 'FLOATER', 'TREANT', 'CRUSTACEAN']

PARTS = ['HORNS', 'EARS', 'TUSKS', 'SPIKES', 'TAIL', 'WINGS', 'CLAWS', 'MANE', 'CROWN', 'EYES', 'SHELL', 'HOOD']

ABILITIES = [
    # projectiles
    'FIREBALL', 'ICE_SHARD', 'POISON_SPIT', 'ACID_SPIT', 'SHADOW_BOLT', 'LIGHTNING_ORB', 'ROCK_THROW', 'BONE_SPEAR',
    'WATER_JET', 'ARCANE_MISSILES', 'BLOOD_BOLT', 'SPORE_SHOT', 'WEB_SHOT', 'HOLY_BOLT', 'SAND_BLAST',
    # areas
    'FIRE_NOVA', 'FROST_NOVA', 'SHOCKWAVE', 'POISON_CLOUD', 'QUAKE', 'SPORE_BURST', 'SANDSTORM', 'THUNDERSTORM',
    'METEOR', 'ROOTS', 'VORTEX_PULL', 'SONIC_SCREAM', 'FEAR_ROAR', 'BLIND_FLASH', 'CURSE', 'DARK_PULSE',
    # movement
    'LEAP_SLAM', 'CHARGE', 'BLINK_BEHIND', 'BURROW', 'DIVE_BOMB', 'SHADOW_STEP', 'DODGE',
    # self / support
    'HEAL', 'ENRAGE', 'STONE_SKIN', 'INVISIBILITY', 'REGEN_AURA', 'SUMMON',
    # melee specials
    'TAIL_SWEEP', 'SPIN_ATTACK', 'LIFE_DRAIN', 'STEAL', 'GRAB_THROW', 'BITE_BLEED', 'FIRE_BREATH', 'FROST_BREATH',
    'POISON_BREATH',
]

TRAITS = ['FIRE_IMMUNE', 'SUN_BURN', 'FLYING', 'FLOATING', 'HEAVY', 'THORNS', 'REGEN', 'POISON_TOUCH', 'FIRE_TOUCH',
          'FROST_TOUCH', 'WITHER_TOUCH', 'LIFESTEAL', 'EXPLODE_DEATH', 'SPLIT_DEATH', 'PACK', 'NIGHT_ONLY', 'UNDEAD',
          'ARMORED', 'FAST', 'EVASIVE', 'SHOCK_TOUCH', 'AQUATIC']

PATTERNS = ['fur', 'scales', 'spots', 'stripes', 'bone', 'rock', 'crystal', 'slime', 'bark', 'cloth', 'metal', 'ghost',
            'magma', 'feather', 'chitin', 'flesh', 'sand', 'ice', 'void', 'coral', 'mushroom', 'rune']


class Mon:
    def __init__(self, id, name, arch, scale, hp, atk, spd, colors, pattern, parts, abilities, traits, regions,
                 armor=0, summon=None, glow=False, desc=''):
        assert arch in ARCHETYPES, (id, arch)
        for p in parts: assert p in PARTS, (id, p)
        for a in abilities: assert a in ABILITIES, (id, a)
        for t in traits: assert t in TRAITS, (id, t)
        for r in regions: assert r in [x[0] for x in REGIONS], (id, r)
        assert pattern in PATTERNS, (id, pattern)
        self.id, self.name, self.arch, self.scale = id, name, arch, scale
        self.hp, self.atk, self.spd, self.colors, self.pattern = hp, atk, spd, colors, pattern
        self.parts, self.abilities, self.traits, self.regions = parts, abilities, traits, regions
        self.armor, self.summon, self.glow, self.desc = armor, summon, glow, desc
        self.boss = False


def M(*a, **k):
    return Mon(*a, **k)


#       id                  name                         arch         scale  hp   atk  spd   (base, secondary, accent, eye)              pattern     parts                  abilities                          traits                         regions
MONSTERS = [
    # ---------------------------------------------------------------- Kurtların Ormanı
    M('dire_wolf', 'Vahşi Boz Kurt', 'QUADRUPED', 1.1, 26, 5, 0.33, (0x6b6560, 0x3a3533, 0xe8e0d0, 0xffd040), 'fur', ['EARS', 'TAIL'], ['BITE_BLEED', 'FEAR_ROAR'], ['PACK', 'FAST'], ['WOLF_FOREST', 'ALDORIA']),
    M('shadow_wolf', 'Gölge Kurdu', 'QUADRUPED', 1.15, 30, 6, 0.34, (0x2a2038, 0x120c1c, 0x7a50c0, 0xc070ff), 'void', ['EARS', 'TAIL', 'SPIKES'], ['SHADOW_STEP', 'BITE_BLEED'], ['PACK', 'NIGHT_ONLY'], ['WOLF_FOREST', 'NOCTHERA'], glow=True),
    M('thornback_boar', 'Dikensırt Yaban Domuzu', 'QUADRUPED', 1.0, 30, 6, 0.30, (0x5a3a26, 0x2e1e14, 0xd8c8a0, 0xff5030), 'fur', ['TUSKS', 'SPIKES'], ['CHARGE'], ['THORNS'], ['WOLF_FOREST', 'ALDORIA', 'FELARIS']),
    M('moss_bear', 'Yosunlu Ayı', 'QUADRUPED', 1.5, 55, 9, 0.27, (0x4a3a28, 0x3f6a2a, 0x8aa060, 0xffb030), 'fur', ['EARS'], ['GRAB_THROW', 'ENRAGE'], ['HEAVY'], ['WOLF_FOREST', 'SYLVARIEN']),
    M('goblin_raider', 'Goblin Yağmacı', 'HUMANOID', 0.7, 18, 4, 0.33, (0x5d8f3a, 0x4a3020, 0xa09080, 0xffe030), 'spots', ['EARS'], ['STEAL', 'DODGE'], ['PACK', 'FAST'], ['WOLF_FOREST', 'ALDORIA', 'WHISPER_STEPPE']),
    M('goblin_shaman', 'Goblin Şaman', 'HUMANOID', 0.72, 22, 3, 0.28, (0x4f8a4a, 0x7a2a6a, 0xe8e0c8, 0x60ff90), 'rune', ['EARS', 'HORNS'], ['SPORE_SHOT', 'REGEN_AURA'], ['PACK'], ['WOLF_FOREST', 'GORMASH'], glow=True),
    M('werewolf', 'Kurtadam', 'BRUTE', 1.2, 60, 10, 0.32, (0x5a5048, 0x2c2622, 0xe0d8c8, 0xffe040), 'fur', ['EARS', 'MANE', 'TAIL'], ['LEAP_SLAM', 'BITE_BLEED'], ['REGEN', 'NIGHT_ONLY'], ['WOLF_FOREST', 'VALDREN']),
    M('angry_sapling', 'Öfkeli Fidan', 'TREANT', 0.8, 28, 4, 0.22, (0x5a4028, 0x58a038, 0x90c050, 0xffe080), 'bark', [], ['ROOTS', 'SPORE_SHOT'], [], ['WOLF_FOREST', 'SYLVARIEN']),
    M('venom_spider', 'Zehir Örümceği', 'ARACHNID', 1.0, 24, 5, 0.32, (0x1e2a18, 0x5ac030, 0xd0ff60, 0xff2020), 'chitin', ['EYES'], ['WEB_SHOT', 'POISON_SPIT'], ['POISON_TOUCH'], ['WOLF_FOREST', 'DEAD_MARSH']),
    M('owlbear', 'Baykuşayı', 'QUADRUPED', 1.4, 50, 9, 0.28, (0x7a5a3a, 0xc8b090, 0xf0c040, 0xffa020), 'feather', ['EARS', 'MANE'], ['SONIC_SCREAM', 'GRAB_THROW'], ['HEAVY'], ['WOLF_FOREST', 'FELARIS']),
    # ---------------------------------------------------------------- Ölüler Bataklığı
    M('bog_corpse', 'Bataklık Cesedi', 'HUMANOID', 1.0, 26, 5, 0.23, (0x4a5a38, 0x2a3020, 0x8a7a50, 0xb0ff40), 'flesh', [], ['POISON_CLOUD'], ['UNDEAD', 'SUN_BURN'], ['DEAD_MARSH', 'CURSED_RUINS']),
    M('swamp_hag', 'Bataklık Cadısı', 'HUMANOID', 0.95, 32, 4, 0.25, (0x6a7a50, 0x2a2030, 0xa0a080, 0x80ff40), 'cloth', ['HOOD'], ['CURSE', 'POISON_SPIT', 'SUMMON'], [], ['DEAD_MARSH'], summon='bog_corpse', glow=True),
    M('mire_slime', 'Çamur Balçığı', 'SLIME', 1.0, 22, 4, 0.26, (0x5a4a30, 0x3a2e1c, 0x8a7040, 0xffe080), 'slime', [], ['ACID_SPIT'], ['SPLIT_DEATH'], ['DEAD_MARSH']),
    M('leech_serpent', 'Sülük Yılan', 'SERPENT', 1.0, 30, 5, 0.28, (0x3a2a30, 0x8a3040, 0xd06070, 0xff3040), 'flesh', [], ['LIFE_DRAIN', 'BITE_BLEED'], ['LIFESTEAL', 'AQUATIC'], ['DEAD_MARSH', 'STORM_COAST']),
    M('marsh_troll', 'Bataklık Trolü', 'BRUTE', 1.5, 70, 10, 0.25, (0x4a6a5a, 0x2a3a30, 0x9a8a60, 0xffc030), 'spots', ['TUSKS'], ['ROCK_THROW', 'HEAL'], ['REGEN', 'HEAVY'], ['DEAD_MARSH']),
    M('will_o_wisp', 'Fener Ruhu', 'FLOATER', 0.5, 16, 4, 0.30, (0x60e0ff, 0x2080c0, 0xc0ffff, 0xffffff), 'ghost', [], ['BLINK_BEHIND', 'ARCANE_MISSILES'], ['FLOATING', 'EVASIVE'], ['DEAD_MARSH', 'SYLVARIEN'], glow=True),
    M('bone_crawler', 'Kemik Sürüngen', 'ARACHNID', 0.85, 24, 5, 0.30, (0xd8d0b8, 0x8a8070, 0xf0e8d0, 0x40ffe0), 'bone', [], ['BONE_SPEAR'], ['UNDEAD'], ['DEAD_MARSH', 'CURSED_RUINS'], glow=True),
    M('plague_toad', 'Veba Kurbağası', 'QUADRUPED', 0.9, 22, 4, 0.27, (0x8a9a30, 0x4a5a18, 0xe0e060, 0xff8000), 'spots', [], ['POISON_SPIT', 'LEAP_SLAM'], ['EXPLODE_DEATH'], ['DEAD_MARSH']),
    M('ghoul', 'Gulyabani', 'HUMANOID', 1.05, 30, 6, 0.32, (0x7a8a78, 0x3a403a, 0xc0b8a0, 0xff3030), 'flesh', ['CLAWS'], ['BITE_BLEED', 'LEAP_SLAM'], ['UNDEAD', 'FAST'], ['DEAD_MARSH', 'CURSED_RUINS', 'VALDREN']),
    M('drowned_knight', 'Boğulmuş Şövalye', 'HUMANOID', 1.1, 40, 7, 0.24, (0x4a6a6a, 0x3a3020, 0x8a9a90, 0x40ffd0), 'metal', ['HORNS'], ['WATER_JET', 'SPIN_ATTACK'], ['UNDEAD', 'ARMORED', 'AQUATIC'], ['DEAD_MARSH', 'STORM_COAST'], armor=6, glow=True),
    # ---------------------------------------------------------------- Ejderdişi Dağları
    M('rock_golem', 'Kaya Golemi', 'GOLEM', 1.3, 70, 10, 0.20, (0x7a7670, 0x4a4844, 0xa0d060, 0xffa020), 'rock', ['SPIKES'], ['QUAKE', 'ROCK_THROW'], ['HEAVY', 'ARMORED'], ['DRAGON_TEETH', 'KHAZDUR'], armor=8),
    M('harpy', 'Harpi', 'FLYER', 1.0, 24, 5, 0.30, (0xa07850, 0xe0c8a0, 0x504030, 0xff4040), 'feather', ['CLAWS'], ['DIVE_BOMB', 'SONIC_SCREAM'], ['FLYING'], ['DRAGON_TEETH', 'STORM_COAST']),
    M('wyvern', 'Wyvern', 'FLYER', 1.6, 50, 8, 0.30, (0x3a5a30, 0x8a9a50, 0xe0d0a0, 0xffe020), 'scales', ['HORNS', 'TAIL'], ['POISON_SPIT', 'DIVE_BOMB'], ['FLYING'], ['DRAGON_TEETH']),
    M('mountain_ogre', 'Dağ Ogresi', 'BRUTE', 1.6, 75, 11, 0.24, (0xb09070, 0x5a4030, 0x403028, 0xffd000), 'spots', ['TUSKS'], ['GRAB_THROW', 'QUAKE'], ['HEAVY'], ['DRAGON_TEETH', 'GORMASH']),
    M('drake_whelp', 'Ejder Yavrusu', 'QUADRUPED', 1.0, 40, 7, 0.30, (0xa02818, 0xe08030, 0xf0d070, 0xffe000), 'scales', ['HORNS', 'WINGS', 'TAIL', 'SPIKES'], ['FIRE_BREATH', 'LEAP_SLAM'], ['FIRE_IMMUNE'], ['DRAGON_TEETH', 'INFERNAX'], glow=True),
    M('storm_eagle', 'Fırtına Kartalı', 'FLYER', 1.3, 30, 6, 0.34, (0x3a5a9a, 0xd0e0ff, 0xf0c040, 0x80e0ff), 'feather', [], ['LIGHTNING_ORB', 'DIVE_BOMB'], ['FLYING'], ['DRAGON_TEETH', 'WHISPER_STEPPE', 'STORM_COAST'], glow=True),
    M('cliff_spider', 'Uçurum Örümceği', 'ARACHNID', 1.2, 30, 6, 0.33, (0x6a6460, 0x3a3634, 0xc0b0a0, 0xff6000), 'rock', ['EYES'], ['WEB_SHOT', 'LEAP_SLAM'], [], ['DRAGON_TEETH']),
    M('minotaur', 'Minotor', 'BRUTE', 1.5, 80, 12, 0.27, (0x5a3a28, 0x2a1a12, 0xe8e0d0, 0xff2010), 'fur', ['HORNS', 'TAIL'], ['CHARGE', 'SPIN_ATTACK'], ['HEAVY'], ['DRAGON_TEETH', 'CURSED_RUINS']),
    M('griffon', 'Grifon', 'QUADRUPED', 1.4, 45, 8, 0.32, (0xc8a050, 0xf0ece0, 0x403020, 0xffc000), 'feather', ['WINGS', 'TAIL'], ['DIVE_BOMB', 'SONIC_SCREAM'], ['FLYING'], ['DRAGON_TEETH', 'MERIDIA']),
    M('stone_basilisk', 'Taş Bazilisk', 'QUADRUPED', 1.2, 48, 8, 0.24, (0x5a6a50, 0x8a9070, 0xd0c060, 0xff00ff), 'scales', ['SPIKES', 'TAIL', 'CROWN'], ['CURSE', 'BITE_BLEED'], ['ARMORED'], ['DRAGON_TEETH', 'GLASS_DESERT'], armor=6, glow=True),
    # ---------------------------------------------------------------- Cam Çölü
    M('sand_scorpion', 'Kum Akrebi', 'CRUSTACEAN', 1.1, 32, 6, 0.30, (0xc0a060, 0x7a5a30, 0x402818, 0x200000), 'chitin', ['TAIL', 'CLAWS'], ['BITE_BLEED', 'BURROW'], ['POISON_TOUCH'], ['GLASS_DESERT']),
    M('dune_worm', 'Kum Solucanı', 'SERPENT', 2.0, 60, 9, 0.28, (0xd0b078, 0x8a6a40, 0xf0e0c0, 0x301008), 'sand', ['SPIKES'], ['BURROW', 'QUAKE'], ['HEAVY'], ['GLASS_DESERT']),
    M('mummy', 'Mumya', 'HUMANOID', 1.0, 34, 6, 0.22, (0xd8ccaa, 0x8a7a5a, 0x4a3a2a, 0x40a0ff), 'cloth', [], ['CURSE', 'SANDSTORM'], ['UNDEAD'], ['GLASS_DESERT', 'CURSED_RUINS'], glow=True),
    M('glass_golem', 'Cam Golem', 'GOLEM', 1.3, 55, 8, 0.22, (0xa0e0e0, 0x60a0b0, 0xffffff, 0xff60ff), 'crystal', ['SPIKES'], ['ARCANE_MISSILES', 'STONE_SKIN'], ['ARMORED'], ['GLASS_DESERT'], armor=10, glow=True),
    M('sand_wraith', 'Kum Hayaleti', 'FLOATER', 1.0, 28, 6, 0.30, (0xd8b880, 0x8a6a40, 0xfff0c0, 0xff8020), 'ghost', ['HOOD'], ['SANDSTORM', 'BLINK_BEHIND'], ['FLOATING', 'UNDEAD'], ['GLASS_DESERT'], glow=True),
    M('scarab', 'Kutsal Bok Böceği', 'INSECT', 0.55, 12, 3, 0.34, (0x206060, 0x40a080, 0xe0c040, 0xff2000), 'chitin', ['HORNS'], ['BITE_BLEED'], ['PACK', 'FAST'], ['GLASS_DESERT']),
    M('sun_lizard', 'Güneş Kertenkelesi', 'QUADRUPED', 0.9, 26, 5, 0.33, (0xe08020, 0xf0d060, 0xa02010, 0xffff00), 'scales', ['SPIKES', 'TAIL'], ['FIRE_BREATH', 'DODGE'], ['FIRE_IMMUNE'], ['GLASS_DESERT', 'MERIDIA']),
    M('desert_jackal', 'Çöl Çakalı', 'QUADRUPED', 0.85, 18, 4, 0.36, (0xc09a60, 0x5a4028, 0xf0e0c0, 0xffd040), 'fur', ['EARS', 'TAIL'], ['BITE_BLEED', 'DODGE'], ['PACK', 'FAST'], ['GLASS_DESERT', 'WHISPER_STEPPE']),
    M('sphinx_cat', 'Sfenks Kedisi', 'QUADRUPED', 1.2, 40, 7, 0.32, (0xd0b070, 0x3050a0, 0xf0d040, 0x40c0ff), 'stripes', ['EARS', 'WINGS', 'TAIL', 'CROWN'], ['BLIND_FLASH', 'LEAP_SLAM'], [], ['GLASS_DESERT'], glow=True),
    M('sand_djinn', 'Kum Cini', 'HUMANOID', 1.2, 45, 7, 0.28, (0x3060c0, 0xe0c060, 0xffe080, 0xffffff), 'rune', ['HORNS', 'TAIL'], ['SANDSTORM', 'FIREBALL'], ['FLOATING'], ['GLASS_DESERT', 'MERIDIA'], glow=True),
    # ---------------------------------------------------------------- Fısıltı Bozkırı
    M('steppe_hyena', 'Bozkır Sırtlanı', 'QUADRUPED', 0.95, 22, 5, 0.34, (0xa08a60, 0x3a3020, 0x5a4a30, 0xffe020), 'spots', ['EARS', 'MANE'], ['BITE_BLEED', 'FEAR_ROAR'], ['PACK'], ['WHISPER_STEPPE', 'GORMASH']),
    M('dust_devil', 'Toz Şeytanı', 'FLOATER', 1.0, 24, 5, 0.32, (0xa09070, 0x605040, 0xe0d0b0, 0xffe0a0), 'sand', [], ['VORTEX_PULL', 'SANDSTORM'], ['FLOATING', 'EVASIVE'], ['WHISPER_STEPPE', 'GLASS_DESERT']),
    M('thunder_bison', 'Gök Bizonu', 'QUADRUPED', 1.6, 65, 10, 0.27, (0x4a3a30, 0x2a2018, 0xa0d0ff, 0x80d0ff), 'fur', ['HORNS', 'MANE'], ['CHARGE', 'SHOCKWAVE'], ['HEAVY', 'SHOCK_TOUCH'], ['WHISPER_STEPPE'], glow=True),
    M('grass_mantis', 'Çayır Peygamber Devesi', 'INSECT', 1.1, 26, 7, 0.33, (0x60a040, 0xa0d060, 0x305020, 0xffe040), 'chitin', ['CLAWS', 'WINGS'], ['SPIN_ATTACK', 'LEAP_SLAM'], ['FAST'], ['WHISPER_STEPPE', 'FELARIS']),
    M('skyray', 'Gök Vatozu', 'FLYER', 1.4, 30, 6, 0.30, (0x305080, 0xa0c0e0, 0xf0f0ff, 0x80ffff), 'spots', ['TAIL'], ['LIGHTNING_ORB'], ['FLYING', 'SHOCK_TOUCH'], ['WHISPER_STEPPE', 'STORM_COAST'], glow=True),
    M('bone_vulture', 'Kemik Akbaba', 'FLYER', 1.1, 24, 5, 0.32, (0xd8d0c0, 0x2a2420, 0x8a2020, 0x40ff40), 'bone', [], ['DIVE_BOMB', 'BONE_SPEAR'], ['FLYING', 'UNDEAD'], ['WHISPER_STEPPE', 'CURSED_RUINS'], glow=True),
    M('wind_elemental', 'Rüzgar Elementali', 'FLOATER', 1.2, 32, 6, 0.34, (0xd0f0ff, 0x80c0e0, 0xffffff, 0x40a0ff), 'ghost', ['CROWN'], ['VORTEX_PULL', 'SHOCKWAVE'], ['FLOATING', 'EVASIVE'], ['WHISPER_STEPPE', 'STORM_COAST'], glow=True),
    M('stonehorn_rhino', 'Taşboynuz Gergedan', 'QUADRUPED', 1.5, 70, 10, 0.26, (0x8a8a88, 0x5a5a58, 0xe0d8c0, 0xff8000), 'rock', ['HORNS', 'SHELL'], ['CHARGE', 'STONE_SKIN'], ['HEAVY', 'ARMORED'], ['WHISPER_STEPPE', 'GORMASH'], armor=8),
    M('plague_rat', 'Veba Faresi', 'QUADRUPED', 0.55, 10, 3, 0.35, (0x5a5048, 0x3a3028, 0xe0a0a0, 0xff2020), 'fur', ['EARS', 'TAIL'], ['BITE_BLEED'], ['PACK', 'POISON_TOUCH'], ['WHISPER_STEPPE', 'ALDORIA', 'DEAD_MARSH']),
    M('scarecrow', 'Uğursuz Korkuluk', 'HUMANOID', 1.1, 28, 5, 0.26, (0xc0a050, 0x6a4a2a, 0xe07010, 0xff8000), 'cloth', ['HOOD'], ['FEAR_ROAR', 'BLIND_FLASH'], ['NIGHT_ONLY'], ['WHISPER_STEPPE', 'ALDORIA'], glow=True),
    # ---------------------------------------------------------------- Kristal Çukur
    M('crystal_spider', 'Kristal Örümcek', 'ARACHNID', 1.1, 30, 6, 0.32, (0x5a3a8a, 0xc080ff, 0xffd0ff, 0xff60ff), 'crystal', ['SPIKES', 'EYES'], ['ARCANE_MISSILES', 'WEB_SHOT'], [], ['CRYSTAL_HOLLOW'], glow=True),
    M('gem_golem', 'Mücevher Golemi', 'GOLEM', 1.4, 75, 10, 0.21, (0x3a3448, 0x60e0a0, 0xff60a0, 0x60ffff), 'crystal', ['SPIKES', 'CROWN'], ['SHOCKWAVE', 'STONE_SKIN'], ['HEAVY', 'ARMORED'], ['CRYSTAL_HOLLOW', 'KHAZDUR'], armor=10, glow=True),
    M('cave_troll', 'Mağara Trolü', 'BRUTE', 1.5, 75, 11, 0.24, (0x6a7068, 0x3a3e38, 0xb0a890, 0xffe040), 'rock', ['TUSKS', 'SPIKES'], ['GRAB_THROW', 'ROCK_THROW'], ['REGEN', 'HEAVY', 'SUN_BURN'], ['CRYSTAL_HOLLOW', 'KHAZDUR']),
    M('deep_watcher', 'Derinlik Gözcüsü', 'FLOATER', 1.1, 34, 6, 0.24, (0x4a2a3a, 0x8a3a5a, 0xf0e0e0, 0xffd000), 'flesh', ['EYES'], ['BLIND_FLASH', 'SHADOW_BOLT'], ['FLOATING'], ['CRYSTAL_HOLLOW', 'NOCTHERA'], glow=True),
    M('crystal_slime', 'Kristal Balçık', 'SLIME', 1.0, 26, 5, 0.26, (0xff80c0, 0xa040a0, 0xffe0ff, 0xffffff), 'crystal', [], ['ARCANE_MISSILES'], ['SPLIT_DEATH'], ['CRYSTAL_HOLLOW'], glow=True),
    M('echo_bat', 'Yankı Yarasası', 'FLYER', 0.6, 12, 3, 0.36, (0x3a3040, 0x6a5a70, 0xc0a0d0, 0x60ffff), 'fur', ['EARS'], ['SONIC_SCREAM'], ['FLYING', 'PACK'], ['CRYSTAL_HOLLOW', 'CURSED_RUINS'], glow=True),
    M('mushroom_brute', 'Mantar Devi', 'BRUTE', 1.4, 60, 8, 0.22, (0xd8c8b0, 0xc02820, 0xf0f0e0, 0x60ff60), 'mushroom', ['HOOD'], ['SPORE_BURST', 'REGEN_AURA'], ['REGEN'], ['CRYSTAL_HOLLOW', 'SYLVARIEN'], glow=True),
    M('lava_salamander', 'Lav Semenderi', 'QUADRUPED', 1.1, 36, 7, 0.30, (0x3a1a10, 0xff6010, 0xffd040, 0xffff60), 'magma', ['SPIKES', 'TAIL'], ['FIRE_BREATH', 'FIRE_NOVA'], ['FIRE_IMMUNE', 'FIRE_TOUCH'], ['CRYSTAL_HOLLOW', 'INFERNAX'], glow=True),
    M('tunnel_worm', 'Tünel Kurdu', 'SERPENT', 1.3, 40, 7, 0.26, (0xc0a090, 0x6a4a40, 0xf0e0d0, 0x200000), 'flesh', ['SPIKES'], ['BURROW', 'ACID_SPIT'], [], ['CRYSTAL_HOLLOW', 'KHAZDUR']),
    M('kobold_miner', 'Kobold Madenci', 'HUMANOID', 0.65, 16, 4, 0.32, (0xb04020, 0x5a4a3a, 0xe0d0b0, 0xffe000), 'scales', ['HORNS', 'TAIL'], ['ROCK_THROW', 'STEAL'], ['PACK'], ['CRYSTAL_HOLLOW', 'KHAZDUR']),
    # ---------------------------------------------------------------- Lanetli Harabeler
    M('skeleton_legionary', 'İskelet Lejyoner', 'HUMANOID', 1.05, 32, 7, 0.26, (0xe0d8c0, 0x8a2020, 0xc0a040, 0x40ffff), 'bone', ['CROWN'], ['SPIN_ATTACK', 'BONE_SPEAR'], ['UNDEAD', 'ARMORED'], ['CURSED_RUINS', 'VALDREN'], armor=6, glow=True),
    M('wraith', 'Hortlak', 'FLOATER', 1.1, 30, 7, 0.30, (0x3a4048, 0x101418, 0x80a0b0, 0x80ffff), 'ghost', ['HOOD'], ['LIFE_DRAIN', 'BLINK_BEHIND'], ['UNDEAD', 'FLOATING', 'EVASIVE'], ['CURSED_RUINS', 'NOCTHERA'], glow=True),
    M('lich_acolyte', 'Lich Çırağı', 'HUMANOID', 1.0, 34, 5, 0.25, (0x2a1a3a, 0xc0b8a0, 0x7040c0, 0xa060ff), 'rune', ['HOOD'], ['SHADOW_BOLT', 'SUMMON'], ['UNDEAD'], ['CURSED_RUINS'], summon='skeleton_legionary', glow=True),
    M('banshee', 'Feryatçı Ruh', 'FLOATER', 1.0, 28, 6, 0.32, (0xc0d8e0, 0x6a8090, 0xffffff, 0x40e0ff), 'ghost', ['HOOD'], ['SONIC_SCREAM', 'FEAR_ROAR'], ['UNDEAD', 'FLOATING'], ['CURSED_RUINS', 'DEAD_MARSH'], glow=True),
    M('cursed_armor', 'Lanetli Zırh', 'HUMANOID', 1.15, 50, 9, 0.24, (0x3a3a42, 0x1a1a20, 0x6a0a10, 0xff2020), 'metal', ['HORNS', 'SPIKES'], ['CHARGE', 'DARK_PULSE'], ['ARMORED', 'HEAVY'], ['CURSED_RUINS', 'VALDREN'], armor=12, glow=True),
    M('gargoyle', 'Gargoyl', 'FLYER', 1.2, 45, 8, 0.28, (0x6a6a70, 0x3a3a40, 0x9a9aa0, 0xff4000), 'rock', ['HORNS', 'TAIL', 'CLAWS'], ['DIVE_BOMB', 'STONE_SKIN'], ['FLYING', 'ARMORED'], ['CURSED_RUINS', 'VALDREN'], armor=8, glow=True),
    M('bone_hound', 'Kemik Tazısı', 'QUADRUPED', 1.0, 26, 6, 0.34, (0xe0d8c0, 0x3a3430, 0x8a1010, 0x40ff80), 'bone', ['SPIKES', 'TAIL'], ['BITE_BLEED', 'DARK_PULSE'], ['UNDEAD', 'PACK'], ['CURSED_RUINS'], glow=True),
    M('vampire_bat', 'Vampir Yarasa', 'FLYER', 0.8, 18, 4, 0.36, (0x3a1018, 0x8a1020, 0xe0c0c0, 0xff2020), 'fur', ['EARS'], ['LIFE_DRAIN'], ['FLYING', 'LIFESTEAL', 'PACK'], ['CURSED_RUINS', 'NOCTHERA'], glow=True),
    M('grave_digger', 'Mezar Kazıcı', 'BRUTE', 1.2, 50, 8, 0.24, (0x5a5a50, 0x2a2420, 0x8a7a60, 0xffe080), 'flesh', ['HOOD'], ['BURROW', 'QUAKE'], ['UNDEAD'], ['CURSED_RUINS', 'DEAD_MARSH']),
    M('shadow_stalker', 'Gölge Avcı', 'HUMANOID', 1.05, 30, 8, 0.34, (0x14101c, 0x2a2038, 0x6040a0, 0xff40ff), 'void', ['HOOD', 'CLAWS'], ['SHADOW_STEP', 'INVISIBILITY'], ['NIGHT_ONLY', 'FAST'], ['CURSED_RUINS', 'NOCTHERA'], glow=True),
    M('blood_zealot', 'Kan Tarikatı Fanatiği', 'HUMANOID', 1.0, 30, 6, 0.28, (0x6a1018, 0x2a0a0c, 0xc0a040, 0xff2020), 'cloth', ['HOOD'], ['BLOOD_BOLT', 'ENRAGE'], [], ['CURSED_RUINS', 'ALDORIA', 'INFERNAX'], glow=True),
    M('flesh_golem', 'Et Golemi', 'GOLEM', 1.3, 70, 10, 0.22, (0xc08a80, 0x6a3a3a, 0x3a3a3a, 0xffe060), 'flesh', ['SPIKES'], ['GRAB_THROW', 'POISON_CLOUD'], ['REGEN', 'HEAVY'], ['CURSED_RUINS']),
    # ---------------------------------------------------------------- Fırtına Kıyısı
    M('kraken_spawn', 'Kraken Yavrusu', 'FLOATER', 1.1, 36, 6, 0.28, (0x6a2a5a, 0xc06090, 0xf0d0e0, 0xffe000), 'spots', ['EYES'], ['WATER_JET', 'VORTEX_PULL'], ['FLOATING', 'AQUATIC'], ['STORM_COAST']),
    M('siren', 'Siren', 'HUMANOID', 1.0, 30, 5, 0.28, (0x60a0a0, 0x206060, 0xf0e0c0, 0x80ffff), 'scales', ['EARS', 'TAIL'], ['SONIC_SCREAM', 'CURSE'], ['AQUATIC'], ['STORM_COAST', 'MERIDIA'], glow=True),
    M('giant_crab', 'Dev Yengeç', 'CRUSTACEAN', 1.3, 50, 9, 0.24, (0xc04020, 0x8a2010, 0xf0d0a0, 0x101010), 'chitin', ['CLAWS', 'SHELL', 'EYES'], ['GRAB_THROW', 'STONE_SKIN'], ['ARMORED', 'AQUATIC'], ['STORM_COAST', 'MERIDIA'], armor=10),
    M('sea_serpent', 'Deniz Yılanı', 'SERPENT', 1.7, 55, 9, 0.30, (0x205a8a, 0x60c0c0, 0xf0f0d0, 0xffe000), 'scales', ['HORNS', 'SPIKES'], ['WATER_JET', 'TAIL_SWEEP'], ['AQUATIC'], ['STORM_COAST']),
    M('storm_elemental', 'Fırtına Elementali', 'FLOATER', 1.3, 40, 7, 0.30, (0x30305a, 0x6a6aa0, 0xa0e0ff, 0xffffff), 'rune', ['CROWN', 'SPIKES'], ['THUNDERSTORM', 'LIGHTNING_ORB'], ['FLOATING', 'SHOCK_TOUCH'], ['STORM_COAST'], glow=True),
    M('fishman', 'Balık Adam', 'HUMANOID', 0.95, 22, 5, 0.30, (0x4a8a6a, 0xc0d8a0, 0xe08040, 0xffff40), 'scales', ['SPIKES', 'EARS'], ['WATER_JET', 'SPIN_ATTACK'], ['PACK', 'AQUATIC'], ['STORM_COAST', 'MERIDIA']),
    M('coral_golem', 'Mercan Golemi', 'GOLEM', 1.3, 60, 9, 0.21, (0xe06080, 0xf0a0a0, 0x60c0e0, 0x80ffff), 'coral', ['SPIKES'], ['SHOCKWAVE', 'WATER_JET'], ['THORNS', 'ARMORED', 'AQUATIC'], ['STORM_COAST'], armor=6, glow=True),
    M('sea_witch', 'Deniz Cadısı', 'HUMANOID', 1.0, 32, 5, 0.26, (0x305a6a, 0x102a30, 0xa0ffe0, 0x40ffc0), 'scales', ['HOOD', 'TAIL'], ['FROST_NOVA', 'CURSE'], ['AQUATIC'], ['STORM_COAST'], glow=True),
    M('thunder_jelly', 'Şimşek Denizanası', 'FLOATER', 0.9, 20, 5, 0.24, (0xa0c0ff, 0x6080ff, 0xffffff, 0xffffff), 'ghost', [], ['LIGHTNING_ORB', 'SHOCKWAVE'], ['FLOATING', 'SHOCK_TOUCH'], ['STORM_COAST'], glow=True),
    M('pirate_ghost', 'Korsan Hayaleti', 'HUMANOID', 1.0, 32, 7, 0.30, (0x40a0a0, 0x1a3a3a, 0xe0c040, 0x80ffff), 'ghost', ['HOOD'], ['SPIN_ATTACK', 'BLINK_BEHIND'], ['UNDEAD'], ['STORM_COAST', 'MERIDIA'], glow=True),
    # ---------------------------------------------------------------- Donuk Zirveler
    M('frost_wolf', 'Ayaz Kurdu', 'QUADRUPED', 1.15, 30, 6, 0.34, (0xe8f0f8, 0xa0c0e0, 0x4080c0, 0x40c0ff), 'fur', ['EARS', 'TAIL', 'MANE'], ['FROST_BREATH', 'BITE_BLEED'], ['PACK', 'FROST_TOUCH'], ['YMIRHEIM'], glow=True),
    M('yeti', 'Yeti', 'BRUTE', 1.6, 80, 11, 0.26, (0xf0f0f0, 0xa0b0c0, 0x3a4a5a, 0x40a0ff), 'fur', ['HORNS', 'MANE'], ['ROCK_THROW', 'FEAR_ROAR'], ['HEAVY', 'FROST_TOUCH'], ['YMIRHEIM']),
    M('ice_wraith', 'Buz Hayaleti', 'FLOATER', 1.1, 30, 6, 0.30, (0xc0e8ff, 0x6090c0, 0xffffff, 0x80ffff), 'ice', ['HOOD', 'CROWN'], ['ICE_SHARD', 'FROST_NOVA'], ['FLOATING', 'UNDEAD'], ['YMIRHEIM'], glow=True),
    M('frost_troll', 'Buz Trolü', 'BRUTE', 1.5, 75, 11, 0.24, (0x7aa0c0, 0x3a5a7a, 0xe0f0ff, 0x80e0ff), 'ice', ['TUSKS', 'SPIKES'], ['QUAKE', 'FROST_NOVA'], ['REGEN', 'HEAVY'], ['YMIRHEIM']),
    M('snow_owl', 'Kar Baykuşu', 'FLYER', 0.9, 20, 5, 0.34, (0xf8f8f8, 0xd0d8e0, 0x303030, 0xffd000), 'feather', ['EARS'], ['ICE_SHARD', 'DIVE_BOMB'], ['FLYING'], ['YMIRHEIM']),
    M('ice_golem', 'Buz Golemi', 'GOLEM', 1.4, 70, 10, 0.20, (0xa0d8ff, 0x6090c0, 0xffffff, 0x2060ff), 'ice', ['SPIKES', 'CROWN'], ['FROST_NOVA', 'STONE_SKIN'], ['HEAVY', 'ARMORED', 'FROST_TOUCH'], ['YMIRHEIM'], armor=8, glow=True),
    M('woolly_mammoth', 'Yünlü Mamut', 'QUADRUPED', 2.0, 100, 12, 0.24, (0x6a4a30, 0x3a2818, 0xf0e8d8, 0x101010), 'fur', ['TUSKS', 'MANE'], ['CHARGE', 'QUAKE'], ['HEAVY'], ['YMIRHEIM']),
    # ---------------------------------------------------------------- Kül Toprakları
    M('imp', 'İmp', 'HUMANOID', 0.6, 16, 4, 0.34, (0xc02818, 0x3a0a08, 0xf0c040, 0xffff00), 'scales', ['HORNS', 'WINGS', 'TAIL'], ['FIREBALL', 'BLINK_BEHIND'], ['FIRE_IMMUNE', 'FLYING', 'PACK'], ['INFERNAX'], glow=True),
    M('hellhound', 'Cehennem Tazısı', 'QUADRUPED', 1.15, 34, 7, 0.34, (0x1a1010, 0xa01808, 0xff8020, 0xffa000), 'magma', ['SPIKES', 'TAIL', 'EARS'], ['FIRE_BREATH', 'BITE_BLEED'], ['FIRE_IMMUNE', 'PACK', 'FIRE_TOUCH'], ['INFERNAX'], glow=True),
    M('magma_slime', 'Magma Balçığı', 'SLIME', 1.1, 28, 6, 0.26, (0x5a1a0a, 0xff6010, 0xffe060, 0xffff80), 'magma', [], ['FIRE_NOVA'], ['SPLIT_DEATH', 'FIRE_IMMUNE', 'FIRE_TOUCH'], ['INFERNAX'], glow=True),
    M('ash_golem', 'Kül Golemi', 'GOLEM', 1.5, 80, 11, 0.20, (0x3a3434, 0x1a1414, 0xff5010, 0xffa020), 'magma', ['SPIKES', 'HORNS'], ['METEOR', 'FIRE_NOVA'], ['FIRE_IMMUNE', 'HEAVY'], ['INFERNAX'], armor=6, glow=True),
    M('succubus', 'Sukkubus', 'HUMANOID', 1.0, 36, 6, 0.30, (0xd06070, 0x2a0a18, 0x1a1a1a, 0xff40a0), 'scales', ['HORNS', 'WINGS', 'TAIL'], ['LIFE_DRAIN', 'CURSE'], ['FIRE_IMMUNE', 'LIFESTEAL'], ['INFERNAX', 'NOCTHERA'], glow=True),
    M('bone_drake', 'Kemik Ejderi', 'QUADRUPED', 1.5, 60, 10, 0.30, (0xd8d0b8, 0x3a3a30, 0x40a040, 0x40ff40), 'bone', ['HORNS', 'WINGS', 'TAIL', 'SPIKES'], ['POISON_BREATH', 'TAIL_SWEEP'], ['UNDEAD', 'FLYING'], ['INFERNAX', 'CURSED_RUINS'], glow=True),
    M('fire_beetle', 'Ateş Böceği', 'INSECT', 0.8, 18, 4, 0.30, (0x3a1a0a, 0xff6010, 0xffc040, 0xffff40), 'chitin', ['HORNS', 'SHELL'], ['FIRE_NOVA'], ['EXPLODE_DEATH', 'FIRE_IMMUNE'], ['INFERNAX', 'GLASS_DESERT'], glow=True),
    M('pit_fiend', 'Çukur Zebanisi', 'BRUTE', 1.7, 90, 13, 0.25, (0x8a1010, 0x2a0606, 0x1a1a1a, 0xffd000), 'magma', ['HORNS', 'WINGS', 'TAIL'], ['GRAB_THROW', 'FIRE_NOVA'], ['FIRE_IMMUNE', 'HEAVY'], ['INFERNAX'], armor=6, glow=True),
    M('lava_serpent', 'Lav Yılanı', 'SERPENT', 1.5, 50, 9, 0.28, (0x2a0a06, 0xff4010, 0xffe060, 0xffff60), 'magma', ['SPIKES', 'HORNS'], ['FIREBALL', 'BURROW'], ['FIRE_IMMUNE'], ['INFERNAX'], glow=True),
    # ---------------------------------------------------------------- Gecegölge Vadisi
    M('night_weaver', 'Gece Dokuyucu', 'ARACHNID', 1.3, 40, 7, 0.32, (0x1a1428, 0x4a3a7a, 0xc0a0ff, 0xc040ff), 'void', ['SPIKES', 'EYES'], ['WEB_SHOT', 'SHADOW_STEP'], ['NIGHT_ONLY'], ['NOCTHERA'], glow=True),
    M('void_eye', 'Boşluk Gözü', 'FLOATER', 1.2, 40, 7, 0.24, (0x1a0a2a, 0x5a2a8a, 0xe0d0ff, 0xff00ff), 'void', ['EYES', 'SPIKES'], ['DARK_PULSE', 'VORTEX_PULL'], ['FLOATING'], ['NOCTHERA', 'CURSED_RUINS'], glow=True),
    M('nightmare_steed', 'Kabus Atı', 'QUADRUPED', 1.4, 50, 9, 0.36, (0x101010, 0x2a0a3a, 0xa040ff, 0xff40ff), 'void', ['MANE', 'TAIL', 'HORNS'], ['CHARGE', 'FEAR_ROAR'], ['FAST'], ['NOCTHERA'], glow=True),
    M('mind_flayer', 'Zihin Yiyici', 'HUMANOID', 1.05, 40, 6, 0.26, (0x8a6a9a, 0x3a2a4a, 0xd0a0d0, 0xffffff), 'flesh', ['HOOD', 'TUSKS'], ['CURSE', 'SHADOW_BOLT'], [], ['NOCTHERA'], glow=True),
    M('umbral_moth', 'Gölge Güvesi', 'INSECT', 1.0, 22, 4, 0.30, (0x3a3050, 0x8070c0, 0xf0e0ff, 0x80ffff), 'spots', ['WINGS', 'EYES'], ['BLIND_FLASH', 'SPORE_BURST'], ['FLYING'], ['NOCTHERA', 'SYLVARIEN'], glow=True),
    M('dark_dryad', 'Kara Dryad', 'TREANT', 1.0, 36, 6, 0.26, (0x2a2030, 0x6a2a6a, 0xff60c0, 0xff40ff), 'bark', ['CROWN'], ['ROOTS', 'LIFE_DRAIN'], ['REGEN'], ['NOCTHERA'], glow=True),
    # ---------------------------------------------------------------- Gümüşyaprak / Demirdağlar / Pençe Vadisi / ...
    M('corrupted_unicorn', 'Kirlenmiş Tekboynuz', 'QUADRUPED', 1.3, 50, 8, 0.36, (0xe0d8f0, 0x4a2a6a, 0xc040ff, 0xff40ff), 'rune', ['HORNS', 'MANE', 'TAIL'], ['CHARGE', 'DARK_PULSE'], ['FAST'], ['SYLVARIEN', 'NOCTHERA'], glow=True),
    M('wicked_pixie', 'Kötücül Peri', 'FLYER', 0.4, 10, 3, 0.36, (0xa0ff80, 0xffa0f0, 0xffffff, 0xffffff), 'rune', ['EARS', 'WINGS'], ['ARCANE_MISSILES', 'BLINK_BEHIND'], ['FLYING', 'EVASIVE', 'PACK'], ['SYLVARIEN'], glow=True),
    M('thorn_treant', 'Diken Ağaç Adam', 'TREANT', 1.5, 80, 11, 0.20, (0x4a3020, 0x2a6a20, 0xa0e040, 0xff8000), 'bark', ['SPIKES'], ['ROOTS', 'SHOCKWAVE'], ['THORNS', 'HEAVY'], ['SYLVARIEN', 'FELARIS'], glow=True),
    M('moss_slime', 'Yosun Balçığı', 'SLIME', 0.9, 18, 3, 0.26, (0x60b040, 0x306020, 0xc0f080, 0xffff80), 'slime', [], ['SPORE_SHOT'], ['SPLIT_DEATH'], ['SYLVARIEN', 'FELARIS']),
    M('rust_monster', 'Pas Canavarı', 'INSECT', 1.1, 32, 5, 0.28, (0xa04a20, 0x5a2a10, 0xe0a040, 0x60ff60), 'chitin', ['HORNS', 'TAIL'], ['ACID_SPIT', 'CHARGE'], ['ARMORED'], ['KHAZDUR'], armor=6),
    M('clockwork_sentinel', 'Kurmalı Bekçi', 'GOLEM', 1.2, 60, 9, 0.24, (0xb08a40, 0x5a4a30, 0x40c0ff, 0x40e0ff), 'metal', ['CROWN', 'EYES'], ['LIGHTNING_ORB', 'CHARGE'], ['ARMORED', 'HEAVY', 'SHOCK_TOUCH'], ['KHAZDUR', 'MERIDIA'], armor=10, glow=True),
    M('cave_bear', 'Mağara Ayısı', 'QUADRUPED', 1.5, 60, 10, 0.27, (0x3a2a20, 0x1a1410, 0xa09080, 0xff6000), 'fur', ['EARS'], ['GRAB_THROW', 'ENRAGE'], ['HEAVY'], ['KHAZDUR', 'YMIRHEIM']),
    M('warg', 'Varg', 'QUADRUPED', 1.35, 40, 8, 0.35, (0x2a2420, 0x5a3020, 0xe0d0c0, 0xff2000), 'fur', ['EARS', 'MANE', 'TAIL'], ['CHARGE', 'BITE_BLEED'], ['PACK'], ['GORMASH', 'VALDREN']),
    M('red_vulture', 'Kızıl Akbaba', 'FLYER', 1.0, 20, 5, 0.33, (0x8a2a1a, 0x3a1a10, 0xe0c0a0, 0xffe000), 'feather', [], ['DIVE_BOMB', 'BITE_BLEED'], ['FLYING', 'PACK'], ['GORMASH']),
    M('saber_tiger', 'Kılıçdiş Kaplan', 'QUADRUPED', 1.3, 45, 9, 0.36, (0xe08a20, 0x1a1410, 0xf8f0e0, 0x80ff40), 'stripes', ['TUSKS', 'TAIL', 'EARS'], ['LEAP_SLAM', 'BITE_BLEED'], ['FAST'], ['FELARIS']),
    M('jungle_python', 'Dev Piton', 'SERPENT', 1.6, 50, 8, 0.27, (0x5a6a20, 0xc0a040, 0x2a3010, 0xffd000), 'spots', [], ['ROOTS', 'BITE_BLEED'], [], ['FELARIS']),
    M('stone_ape', 'Taş Goril', 'BRUTE', 1.4, 70, 10, 0.28, (0x3a3a3a, 0x6a6a68, 0xc0b8a8, 0xff8000), 'rock', ['MANE'], ['QUAKE', 'ROCK_THROW'], ['HEAVY'], ['FELARIS', 'DRAGON_TEETH']),
    M('carrion_crow', 'Leş Kargası', 'FLYER', 0.6, 12, 3, 0.36, (0x14141a, 0x2a2a3a, 0x6a6a80, 0xff2020), 'feather', [], ['DIVE_BOMB', 'BLIND_FLASH'], ['FLYING', 'PACK'], ['VALDREN', 'CURSED_RUINS']),
    M('war_ghoul', 'Savaş Leşçisi', 'BRUTE', 1.3, 55, 9, 0.28, (0x5a6050, 0x3a2a20, 0x8a8a8a, 0xff4040), 'flesh', ['SPIKES', 'CLAWS'], ['LEAP_SLAM', 'BITE_BLEED'], ['UNDEAD'], ['VALDREN']),
    M('sewer_rat_king', 'Lağım Faresi', 'QUADRUPED', 0.7, 14, 3, 0.34, (0x4a4038, 0x6a5a50, 0xe0b0b0, 0xff4040), 'fur', ['EARS', 'TAIL'], ['BITE_BLEED', 'DODGE'], ['PACK'], ['ALDORIA', 'MERIDIA']),
    M('plains_slime', 'Ova Balçığı', 'SLIME', 0.9, 16, 3, 0.26, (0x60a0ff, 0x3060c0, 0xc0e0ff, 0xffffff), 'slime', [], ['WATER_JET'], ['SPLIT_DEATH'], ['ALDORIA', 'MERIDIA']),
    M('highway_ghost', 'Yol Hayaleti', 'FLOATER', 1.0, 22, 5, 0.30, (0xa0b8c0, 0x506068, 0xe0f0ff, 0x80ffff), 'ghost', ['HOOD'], ['FEAR_ROAR', 'LIFE_DRAIN'], ['FLOATING', 'UNDEAD', 'NIGHT_ONLY'], ['ALDORIA', 'VALDREN'], glow=True),
    M('golden_mimic', 'Altın Taklitçi', 'GOLEM', 0.9, 40, 8, 0.28, (0xa07030, 0xf0c040, 0x5a3010, 0xff2020), 'metal', ['TUSKS'], ['STEAL', 'LEAP_SLAM'], ['ARMORED'], ['MERIDIA', 'CRYSTAL_HOLLOW'], armor=8),
]

# ---------------------------------------------------------------- bosses
B = M
BOSSES = [
    B('wolf_king_fenrath', 'Kurt Kral Fenrath', 'QUADRUPED', 3.0, 450, 18, 0.33, (0x4a4a50, 0x1a1a20, 0xf0f0f0, 0x40c0ff), 'fur', ['EARS', 'MANE', 'TAIL', 'SPIKES', 'CROWN'], ['FEAR_ROAR', 'SUMMON', 'LEAP_SLAM', 'BITE_BLEED', 'SHADOW_STEP'], ['FAST'], ['WOLF_FOREST'], summon='dire_wolf', glow=True),
    B('mother_morgra', 'Bataklık Anası Morgra', 'BRUTE', 2.5, 500, 16, 0.24, (0x4a6a40, 0x2a2030, 0xa0a070, 0xa0ff40), 'spots', ['HOOD', 'TUSKS', 'SPIKES'], ['POISON_CLOUD', 'SUMMON', 'CURSE', 'ACID_SPIT'], ['REGEN'], ['DEAD_MARSH'], summon='bog_corpse', glow=True),
    B('dragon_queen_vaelith', 'Ejder Kraliçesi Vaelith', 'QUADRUPED', 4.0, 900, 24, 0.30, (0x8a1010, 0xf0a030, 0xffe070, 0xffff40), 'scales', ['HORNS', 'WINGS', 'TAIL', 'SPIKES', 'CROWN'], ['FIRE_BREATH', 'METEOR', 'TAIL_SWEEP', 'FIRE_NOVA', 'DIVE_BOMB'], ['FIRE_IMMUNE', 'FLYING'], ['DRAGON_TEETH'], glow=True),
    B('pharaoh_sekhmar', 'Kum Firavunu Sekhmar', 'HUMANOID', 2.6, 550, 18, 0.26, (0xe0c060, 0x2050a0, 0xfff0a0, 0x40c0ff), 'cloth', ['CROWN', 'HOOD'], ['SANDSTORM', 'SUMMON', 'CURSE', 'BLINK_BEHIND', 'SAND_BLAST'], ['UNDEAD'], ['GLASS_DESERT'], summon='mummy', glow=True),
    B('storm_khan_tengrak', 'Fırtına Hanı Tengrak', 'FLYER', 3.2, 600, 18, 0.32, (0x2a3a7a, 0xe0f0ff, 0xffe040, 0x80ffff), 'feather', ['CROWN', 'TAIL', 'CLAWS'], ['THUNDERSTORM', 'LIGHTNING_ORB', 'DIVE_BOMB', 'VORTEX_PULL'], ['FLYING', 'SHOCK_TOUCH'], ['WHISPER_STEPPE'], glow=True),
    B('crystal_heart_guardian', 'Kristal Kalbin Bekçisi', 'GOLEM', 3.5, 750, 20, 0.20, (0x4a2a7a, 0xc080ff, 0xffd0ff, 0xff60ff), 'crystal', ['SPIKES', 'CROWN', 'EYES'], ['ARCANE_MISSILES', 'SHOCKWAVE', 'STONE_SKIN', 'QUAKE'], ['HEAVY', 'ARMORED'], ['CRYSTAL_HOLLOW'], armor=14, glow=True),
    B('lich_king_valdemar', 'Lich Kral Valdemar', 'HUMANOID', 2.5, 650, 20, 0.25, (0x1a1028, 0xd8d0b8, 0x40ffc0, 0x40ffff), 'rune', ['CROWN', 'HOOD'], ['SHADOW_BOLT', 'SUMMON', 'DARK_PULSE', 'LIFE_DRAIN', 'BLINK_BEHIND'], ['UNDEAD'], ['CURSED_RUINS'], summon='skeleton_legionary', glow=True),
    B('deep_kraken', 'Derinlerin Krakeni', 'FLOATER', 4.0, 800, 20, 0.24, (0x5a1a4a, 0xc05080, 0xf0d0e0, 0xffe000), 'spots', ['EYES', 'SPIKES', 'CROWN'], ['WATER_JET', 'VORTEX_PULL', 'THUNDERSTORM', 'GRAB_THROW'], ['FLOATING', 'AQUATIC'], ['STORM_COAST'], glow=True),
    B('frost_giant_shade', "Ymir'in Gölgesi", 'BRUTE', 3.6, 800, 24, 0.24, (0x8ab0d0, 0x2a4a6a, 0xffffff, 0x40c0ff), 'ice', ['HORNS', 'MANE', 'SPIKES', 'CROWN'], ['FROST_NOVA', 'ICE_SHARD', 'QUAKE', 'FROST_BREATH'], ['HEAVY', 'FROST_TOUCH'], ['YMIRHEIM'], glow=True),
    B('ash_lord_azgaroth', 'Kül Lordu Azgaroth', 'BRUTE', 3.5, 850, 26, 0.26, (0x3a0a06, 0xff4010, 0x1a1a1a, 0xffd000), 'magma', ['HORNS', 'WINGS', 'TAIL', 'SPIKES'], ['METEOR', 'FIRE_NOVA', 'GRAB_THROW', 'SUMMON', 'ENRAGE'], ['FIRE_IMMUNE', 'HEAVY'], ['INFERNAX'], summon='imp', glow=True),
    B('night_mother_shaelra', 'Gece Anası Shaelra', 'ARACHNID', 3.5, 700, 20, 0.30, (0x1a1028, 0x5a3a9a, 0xe0c0ff, 0xd040ff), 'void', ['SPIKES', 'EYES', 'CROWN'], ['WEB_SHOT', 'SUMMON', 'POISON_SPIT', 'SHADOW_STEP'], ['NIGHT_ONLY'], ['NOCTHERA'], summon='night_weaver', glow=True),
    B('elder_rot_tree', 'Ulu Çürük Ağaç', 'TREANT', 4.0, 800, 20, 0.18, (0x3a2a1a, 0x5a7a20, 0xd0a040, 0xff8000), 'bark', ['SPIKES', 'CROWN'], ['ROOTS', 'SPORE_BURST', 'SUMMON', 'SHOCKWAVE'], ['REGEN', 'HEAVY', 'THORNS'], ['SYLVARIEN'], summon='angry_sapling', glow=True),
    B('iron_eater_wurm', 'Demir Yiyen Wurm', 'SERPENT', 4.0, 800, 22, 0.26, (0x5a5050, 0xa06040, 0xe0d0c0, 0xff2000), 'rock', ['SPIKES', 'HORNS'], ['BURROW', 'ACID_SPIT', 'QUAKE', 'TAIL_SWEEP'], ['HEAVY', 'ARMORED'], ['KHAZDUR'], armor=10),
    B('blood_bull_behemoth', 'Kan Boğası Behemot', 'QUADRUPED', 3.5, 750, 24, 0.28, (0x6a1a10, 0x2a0a06, 0xf0e0d0, 0xff2000), 'fur', ['HORNS', 'MANE', 'SPIKES', 'TAIL'], ['CHARGE', 'QUAKE', 'ENRAGE', 'FEAR_ROAR'], ['HEAVY'], ['GORMASH']),
    B('spirit_kalkara', 'Orman Ruhu Kalkara', 'QUADRUPED', 3.0, 600, 20, 0.36, (0xf0a020, 0x1a1a10, 0xffffff, 0x80ff40), 'stripes', ['TUSKS', 'TAIL', 'EARS', 'CROWN'], ['LEAP_SLAM', 'BITE_BLEED', 'SUMMON', 'INVISIBILITY'], ['FAST'], ['FELARIS'], summon='saber_tiger', glow=True),
    B('black_knight_mordred', 'Kara Şövalye Mordred', 'HUMANOID', 2.2, 650, 24, 0.28, (0x1a1a20, 0x4a0a10, 0xc0a040, 0xff2020), 'metal', ['HORNS', 'SPIKES', 'CROWN'], ['CHARGE', 'SPIN_ATTACK', 'DARK_PULSE', 'ENRAGE'], ['ARMORED', 'HEAVY'], ['VALDREN'], armor=16, glow=True),
    B('golden_dragon_aurelion', 'Altın Ejder Aurelion', 'QUADRUPED', 3.8, 850, 22, 0.30, (0xe0a020, 0xfff0a0, 0xffffff, 0x40e0ff), 'scales', ['HORNS', 'WINGS', 'TAIL', 'SPIKES', 'CROWN'], ['FIRE_BREATH', 'BLIND_FLASH', 'METEOR', 'HOLY_BOLT'], ['FIRE_IMMUNE', 'FLYING'], ['MERIDIA'], glow=True),
    B('plague_rat_king', 'Veba Kralı Sıçanyüz', 'BRUTE', 2.4, 450, 16, 0.28, (0x4a4038, 0x7a6a50, 0xe0a0a0, 0xa0ff20), 'fur', ['EARS', 'TAIL', 'CROWN'], ['SUMMON', 'POISON_CLOUD', 'BITE_BLEED', 'DODGE'], ['POISON_TOUCH'], ['ALDORIA'], summon='plague_rat', glow=True),
    B('bone_dragon_nekrath', 'Kemik Ejder Nekrath', 'QUADRUPED', 4.0, 850, 22, 0.28, (0xd8d0b8, 0x2a2a20, 0x40ff80, 0x40ff80), 'bone', ['HORNS', 'WINGS', 'TAIL', 'SPIKES'], ['POISON_BREATH', 'SUMMON', 'DARK_PULSE', 'DIVE_BOMB'], ['UNDEAD', 'FLYING'], ['CURSED_RUINS'], summon='bone_hound', glow=True),
    B('thunder_colossus_thoran', 'Yıldırım Kolosu Thoran', 'GOLEM', 3.5, 750, 22, 0.22, (0x4a4a6a, 0x2a2a3a, 0x80c0ff, 0xa0e0ff), 'metal', ['SPIKES', 'CROWN'], ['THUNDERSTORM', 'CHARGE', 'SHOCKWAVE', 'LIGHTNING_ORB'], ['HEAVY', 'ARMORED', 'SHOCK_TOUCH'], ['STORM_COAST'], armor=12, glow=True),
    B('nightmare_rider', 'Kabus Süvarisi', 'QUADRUPED', 3.0, 600, 20, 0.36, (0x101010, 0x3a0a4a, 0xc040ff, 0xff40ff), 'void', ['HORNS', 'MANE', 'TAIL', 'CROWN'], ['CHARGE', 'FIRE_NOVA', 'FEAR_ROAR', 'BLINK_BEHIND'], ['FAST', 'FIRE_IMMUNE'], ['NOCTHERA'], glow=True),
    B('sand_mother_shai', 'Kum Anası Shai-Hulud', 'SERPENT', 5.0, 900, 24, 0.28, (0xd0a868, 0x7a5a30, 0xfff0d0, 0x401000), 'sand', ['SPIKES', 'HORNS'], ['BURROW', 'QUAKE', 'SANDSTORM', 'SUMMON'], ['HEAVY'], ['GLASS_DESERT'], summon='scarab'),
    B('crystal_spider_queen', 'Kristal Örümcek Kraliçesi', 'ARACHNID', 3.0, 600, 18, 0.30, (0x7a3aaa, 0xff90ff, 0xffffff, 0xffffff), 'crystal', ['SPIKES', 'EYES', 'CROWN'], ['ARCANE_MISSILES', 'WEB_SHOT', 'SUMMON', 'BLINK_BEHIND'], [], ['CRYSTAL_HOLLOW'], summon='crystal_spider', glow=True),
    B('void_watcher_xalthor', "Boşluk Gözcüsü Xal'Thor", 'FLOATER', 3.5, 700, 20, 0.22, (0x14082a, 0x6a2aaa, 0xffffff, 0xff00ff), 'void', ['EYES', 'SPIKES', 'CROWN'], ['DARK_PULSE', 'VORTEX_PULL', 'BLIND_FLASH', 'ARCANE_MISSILES', 'BLINK_BEHIND'], ['FLOATING'], ['NOCTHERA'], glow=True),
    B('stone_titan_golgoth', 'Taş Titan Golgoth', 'GOLEM', 4.5, 1000, 26, 0.18, (0x6a6660, 0x3a3834, 0x90d040, 0xffa000), 'rock', ['SPIKES', 'CROWN'], ['QUAKE', 'ROCK_THROW', 'SHOCKWAVE', 'STONE_SKIN'], ['HEAVY', 'ARMORED'], ['DRAGON_TEETH'], armor=16),
    B('blood_queen_elizabet', 'Kanlı Kraliçe Elizabet', 'HUMANOID', 2.2, 600, 20, 0.30, (0xe0d0d0, 0x6a0a14, 0x1a0a0a, 0xff2020), 'cloth', ['WINGS', 'CROWN'], ['LIFE_DRAIN', 'BLOOD_BOLT', 'SUMMON', 'SHADOW_STEP'], ['LIFESTEAL', 'FLYING'], ['CURSED_RUINS', 'VALDREN'], summon='vampire_bat', glow=True),
    B('phoenix_anka', 'Ateş Kuşu Anka', 'FLYER', 3.0, 650, 20, 0.34, (0xff6010, 0xffd040, 0xffffff, 0xffff80), 'feather', ['CROWN', 'TAIL'], ['FIRE_NOVA', 'FIREBALL', 'DIVE_BOMB', 'HEAL'], ['FIRE_IMMUNE', 'FLYING', 'FIRE_TOUCH'], ['INFERNAX', 'GLASS_DESERT'], glow=True),
    B('marsh_hydra', 'Bataklık Hidrası', 'SERPENT', 3.5, 750, 20, 0.26, (0x2a5a3a, 0x8aa040, 0xf0f0a0, 0xffe000), 'scales', ['HORNS', 'SPIKES', 'CROWN'], ['POISON_SPIT', 'ACID_SPIT', 'TAIL_SWEEP', 'POISON_BREATH'], ['REGEN', 'AQUATIC'], ['DEAD_MARSH'], glow=True),
    B('sky_leviathan', 'Gök Leviathanı', 'FLYER', 4.5, 850, 20, 0.26, (0x20406a, 0x80b0e0, 0xffffff, 0x80ffff), 'spots', ['TAIL', 'SPIKES', 'CROWN'], ['WATER_JET', 'VORTEX_PULL', 'SONIC_SCREAM', 'THUNDERSTORM'], ['FLYING'], ['STORM_COAST', 'WHISPER_STEPPE'], glow=True),
    B('demon_lord_malakar', 'İblis Lordu Malakar', 'BRUTE', 4.2, 1500, 30, 0.28, (0x2a0606, 0xa01010, 0xffd040, 0xff4000), 'rune', ['HORNS', 'WINGS', 'TAIL', 'SPIKES', 'CROWN'], ['METEOR', 'DARK_PULSE', 'SUMMON', 'FIRE_NOVA', 'BLINK_BEHIND', 'ENRAGE'], ['FIRE_IMMUNE', 'HEAVY', 'ARMORED'], ['INFERNAX'], summon='pit_fiend', armor=14, glow=True),
    B('mushroom_king_myco', 'Mantar Kralı Myco', 'BRUTE', 2.8, 550, 16, 0.22, (0xe0d0b8, 0xa02090, 0xfff0ff, 0x60ff60), 'mushroom', ['HOOD', 'CROWN'], ['SPORE_BURST', 'SUMMON', 'REGEN_AURA', 'POISON_CLOUD'], ['REGEN'], ['CRYSTAL_HOLLOW'], summon='mushroom_brute', glow=True),
    B('clockwork_colossus', 'Kurmalı Kolos', 'GOLEM', 4.0, 850, 24, 0.22, (0xb08a30, 0x4a3a20, 0x40e0ff, 0x40e0ff), 'metal', ['SPIKES', 'CROWN', 'EYES'], ['LIGHTNING_ORB', 'CHARGE', 'FIRE_NOVA', 'SHOCKWAVE'], ['HEAVY', 'ARMORED'], ['KHAZDUR'], armor=16, glow=True),
]
for b in BOSSES:
    b.boss = True

# ---------------------------------------------------------------- spells
SCHOOLS = [
    # id, name, color, secondary color
    ('FIRE', 'Ateş', 0xff6a1a, 0xffd040),
    ('ICE', 'Buz', 0x80d8ff, 0xffffff),
    ('LIGHTNING', 'Yıldırım', 0xa0c0ff, 0xffffff),
    ('EARTH', 'Toprak', 0x8a6a40, 0xc0a070),
    ('WIND', 'Rüzgar', 0xd0f0e0, 0x80c0a0),
    ('WATER', 'Su', 0x3080ff, 0x80d0ff),
    ('LIGHT', 'Işık', 0xfff0a0, 0xffffff),
    ('DARK', 'Karanlık', 0x5a2a8a, 0x1a0a2a),
    ('NATURE', 'Doğa', 0x50c040, 0xc0ff80),
    ('ARCANE', 'Gizem', 0xc060ff, 0xff90ff),
    ('BLOOD', 'Kan', 0xc01020, 0x5a0008),
    ('SPIRIT', 'Ruh', 0x60ffe0, 0xd0fff8),
]
SHAPES = ['BOLT', 'BALL', 'BEAM', 'CONE', 'NOVA', 'RAIN', 'WALL', 'BUFF', 'HEAL', 'SUMMON', 'TELEPORT', 'CHAIN',
          'AURA', 'TRAP', 'PULL', 'STRIKE']

# id, name, school, shape, power, mana, cooldown(ticks), tier(1..5)
SPELLS = [
    # FIRE
    ('fire_bolt', 'Ateş Oku', 'FIRE', 'BOLT', 6, 8, 12, 1),
    ('fireball', 'Ateş Topu', 'FIRE', 'BALL', 10, 18, 30, 2),
    ('flame_breath', 'Alev Nefesi', 'FIRE', 'CONE', 4, 3, 2, 2),
    ('fire_nova', 'Ateş Halkası', 'FIRE', 'NOVA', 9, 22, 60, 2),
    ('meteor_shower', 'Meteor Yağmuru', 'FIRE', 'RAIN', 12, 45, 200, 4),
    ('fire_wall', 'Alev Duvarı', 'FIRE', 'WALL', 5, 25, 120, 3),
    ('phoenix_blessing', 'Anka Kutsaması', 'FIRE', 'BUFF', 0, 30, 600, 4),
    ('inferno_pillar', 'Cehennem Sütunu', 'FIRE', 'STRIKE', 16, 35, 140, 4),
    ('sun_lance', 'Güneş Mızrağı', 'FIRE', 'BEAM', 3, 4, 2, 5),
    # ICE
    ('ice_shard', 'Buz Kıymığı', 'ICE', 'BOLT', 5, 7, 10, 1),
    ('frost_orb', 'Ayaz Küresi', 'ICE', 'BALL', 8, 16, 30, 2),
    ('cone_of_cold', 'Soğuk Konisi', 'ICE', 'CONE', 3, 3, 2, 2),
    ('frost_nova', 'Ayaz Patlaması', 'ICE', 'NOVA', 7, 20, 60, 2),
    ('blizzard', 'Kar Fırtınası', 'ICE', 'RAIN', 9, 40, 200, 4),
    ('ice_wall', 'Buz Duvarı', 'ICE', 'WALL', 0, 20, 160, 3),
    ('ice_armor', 'Buz Zırhı', 'ICE', 'BUFF', 0, 25, 500, 3),
    ('glacial_spike', 'Buzul Dikeni', 'ICE', 'STRIKE', 14, 30, 120, 4),
    ('absolute_zero', 'Mutlak Sıfır', 'ICE', 'TRAP', 6, 35, 300, 5),
    # LIGHTNING
    ('spark', 'Kıvılcım', 'LIGHTNING', 'BOLT', 5, 6, 8, 1),
    ('ball_lightning', 'Yumak Şimşek', 'LIGHTNING', 'BALL', 9, 18, 40, 2),
    ('lightning_beam', 'Şimşek Işını', 'LIGHTNING', 'BEAM', 3, 4, 2, 3),
    ('chain_lightning', 'Zincir Şimşek', 'LIGHTNING', 'CHAIN', 9, 24, 60, 3),
    ('thunderclap', 'Gök Gürültüsü', 'LIGHTNING', 'NOVA', 8, 22, 70, 2),
    ('thunderstorm', 'Şimşek Fırtınası', 'LIGHTNING', 'RAIN', 11, 45, 220, 4),
    ('static_field', 'Statik Alan', 'LIGHTNING', 'AURA', 3, 30, 400, 3),
    ('lightning_step', 'Şimşek Adımı', 'LIGHTNING', 'TELEPORT', 4, 15, 60, 3),
    ('wrath_of_sky', 'Göğün Gazabı', 'LIGHTNING', 'STRIKE', 18, 40, 180, 5),
    # EARTH
    ('stone_bullet', 'Taş Mermisi', 'EARTH', 'BOLT', 6, 7, 12, 1),
    ('boulder', 'Kaya Fırlatma', 'EARTH', 'BALL', 11, 18, 40, 2),
    ('earthquake', 'Deprem', 'EARTH', 'NOVA', 9, 28, 100, 3),
    ('stone_wall', 'Taş Duvar', 'EARTH', 'WALL', 0, 15, 140, 2),
    ('stone_skin', 'Taş Deri', 'EARTH', 'BUFF', 0, 25, 500, 2),
    ('earth_spikes', 'Toprak Dikenleri', 'EARTH', 'STRIKE', 12, 24, 80, 3),
    ('quicksand', 'Bataklık Tuzağı', 'EARTH', 'TRAP', 3, 22, 200, 3),
    ('gravity_well', 'Yerçekimi Kuyusu', 'EARTH', 'PULL', 4, 26, 160, 4),
    ('rock_rain', 'Kaya Yağmuru', 'EARTH', 'RAIN', 10, 40, 200, 4),
    # WIND
    ('wind_blade', 'Rüzgar Kesiği', 'WIND', 'BOLT', 5, 6, 8, 1),
    ('gust', 'Kasırga Darbesi', 'WIND', 'CONE', 2, 10, 30, 1),
    ('tornado', 'Hortum', 'WIND', 'PULL', 5, 30, 160, 3),
    ('wind_walk', 'Rüzgar Yürüyüşü', 'WIND', 'BUFF', 0, 15, 400, 2),
    ('feather_fall', 'Tüy Düşüşü', 'WIND', 'AURA', 0, 10, 300, 1),
    ('air_dash', 'Hava Atılımı', 'WIND', 'TELEPORT', 0, 10, 40, 2),
    ('cyclone_nova', 'Siklon', 'WIND', 'NOVA', 7, 22, 80, 3),
    ('storm_arrows', 'Fırtına Okları', 'WIND', 'RAIN', 8, 35, 180, 4),
    ('vacuum_blade', 'Boşluk Kılıcı', 'WIND', 'BEAM', 3, 4, 2, 5),
    # WATER
    ('water_bolt', 'Su Oku', 'WATER', 'BOLT', 5, 6, 10, 1),
    ('bubble_burst', 'Kabarcık Patlaması', 'WATER', 'BALL', 7, 14, 30, 1),
    ('tidal_wave', 'Gelgit Dalgası', 'WATER', 'CONE', 5, 20, 80, 3),
    ('healing_rain', 'Şifa Yağmuru', 'WATER', 'HEAL', 10, 30, 300, 3),
    ('whirlpool', 'Girdap', 'WATER', 'PULL', 5, 26, 160, 3),
    ('water_breath', 'Su Solukluğu', 'WATER', 'BUFF', 0, 10, 600, 1),
    ('hydro_cannon', 'Su Topu', 'WATER', 'BEAM', 3, 4, 2, 4),
    ('drowning_prison', 'Boğucu Hapis', 'WATER', 'TRAP', 6, 30, 240, 4),
    ('purify', 'Arındırma', 'WATER', 'HEAL', 4, 15, 200, 2),
    # LIGHT
    ('holy_light', 'Kutsal Işık', 'LIGHT', 'BOLT', 6, 8, 12, 1),
    ('heal', 'İyileştirme', 'LIGHT', 'HEAL', 8, 15, 100, 1),
    ('greater_heal', 'Büyük Şifa', 'LIGHT', 'HEAL', 20, 40, 400, 4),
    ('divine_shield', 'İlahi Kalkan', 'LIGHT', 'BUFF', 0, 35, 700, 4),
    ('radiance', 'Nur Saçılımı', 'LIGHT', 'NOVA', 9, 24, 80, 3),
    ('judgement', 'Hüküm', 'LIGHT', 'STRIKE', 16, 35, 160, 4),
    ('blinding_light', 'Kör Edici Işık', 'LIGHT', 'CONE', 2, 15, 100, 2),
    ('sacred_ground', 'Kutsal Toprak', 'LIGHT', 'AURA', 2, 30, 400, 3),
    ('angel_summon', 'Melek Çağrısı', 'LIGHT', 'SUMMON', 0, 50, 900, 5),
    # DARK
    ('shadow_bolt', 'Gölge Oku', 'DARK', 'BOLT', 7, 8, 12, 1),
    ('dark_orb', 'Kara Küre', 'DARK', 'BALL', 10, 18, 40, 2),
    ('life_drain', 'Can Emme', 'DARK', 'BEAM', 2, 3, 2, 2),
    ('curse_of_weakness', 'Zayıflık Laneti', 'DARK', 'CONE', 1, 15, 100, 2),
    ('raise_dead', 'Ölüleri Diriltme', 'DARK', 'SUMMON', 0, 40, 600, 3),
    ('shadow_step', 'Gölge Adımı', 'DARK', 'TELEPORT', 0, 12, 50, 2),
    ('void_nova', 'Boşluk Patlaması', 'DARK', 'NOVA', 10, 28, 100, 4),
    ('black_hole', 'Kara Delik', 'DARK', 'PULL', 6, 40, 240, 5),
    ('cloak_of_shadows', 'Gölge Pelerini', 'DARK', 'BUFF', 0, 20, 500, 3),
    # NATURE
    ('thorn_shot', 'Diken Atışı', 'NATURE', 'BOLT', 5, 6, 10, 1),
    ('entangle', 'Sarmaşık Tuzağı', 'NATURE', 'TRAP', 3, 18, 160, 2),
    ('regrowth', 'Yeniden Filizlenme', 'NATURE', 'HEAL', 6, 15, 160, 1),
    ('poison_cloud', 'Zehir Bulutu', 'NATURE', 'BALL', 4, 18, 80, 2),
    ('summon_wolves', 'Kurt Çağırma', 'NATURE', 'SUMMON', 0, 35, 600, 3),
    ('barkskin', 'Ağaç Kabuğu', 'NATURE', 'BUFF', 0, 20, 500, 2),
    ('natures_wrath', 'Doğanın Gazabı', 'NATURE', 'RAIN', 9, 38, 200, 4),
    ('spore_nova', 'Spor Patlaması', 'NATURE', 'NOVA', 6, 20, 80, 2),
    ('treant_call', 'Ağaç Adam Çağrısı', 'NATURE', 'SUMMON', 0, 50, 900, 5),
    # ARCANE
    ('magic_missile', 'Büyü Füzesi', 'ARCANE', 'BOLT', 4, 4, 6, 1),
    ('arcane_orb', 'Gizem Küresi', 'ARCANE', 'BALL', 9, 18, 35, 2),
    ('blink', 'Işınlanma', 'ARCANE', 'TELEPORT', 0, 12, 40, 2),
    ('arcane_beam', 'Gizem Işını', 'ARCANE', 'BEAM', 3, 4, 2, 3),
    ('mana_shield', 'Mana Kalkanı', 'ARCANE', 'BUFF', 0, 25, 500, 3),
    ('arcane_explosion', 'Gizem Patlaması', 'ARCANE', 'NOVA', 9, 24, 70, 3),
    ('time_slow', 'Zamanı Yavaşlatma', 'ARCANE', 'AURA', 0, 40, 600, 4),
    ('arcane_barrage', 'Gizem Yağmuru', 'ARCANE', 'RAIN', 10, 40, 200, 4),
    ('telekinesis', 'Telekinezi', 'ARCANE', 'PULL', 2, 14, 80, 2),
    ('starfall', 'Yıldız Düşüşü', 'ARCANE', 'STRIKE', 20, 50, 240, 5),
    # BLOOD
    ('blood_spear', 'Kan Mızrağı', 'BLOOD', 'BOLT', 9, 4, 14, 2),
    ('blood_boil', 'Kan Kaynatma', 'BLOOD', 'NOVA', 10, 10, 80, 3),
    ('sanguine_pact', 'Kan Antlaşması', 'BLOOD', 'BUFF', 0, 5, 600, 3),
    ('hemorrhage', 'Kanama', 'BLOOD', 'CONE', 3, 12, 80, 2),
    ('blood_ritual', 'Kan Ayini', 'BLOOD', 'HEAL', 12, 25, 400, 4),
    ('crimson_rain', 'Kızıl Yağmur', 'BLOOD', 'RAIN', 10, 30, 200, 4),
    ('vein_chain', 'Damar Zinciri', 'BLOOD', 'CHAIN', 8, 18, 60, 3),
    ('blood_golem', 'Kan Golemi', 'BLOOD', 'SUMMON', 0, 40, 900, 5),
    # SPIRIT
    ('spirit_arrow', 'Ruh Oku', 'SPIRIT', 'BOLT', 6, 7, 10, 1),
    ('soul_orb', 'Ruh Küresi', 'SPIRIT', 'BALL', 9, 16, 35, 2),
    ('ancestral_guard', 'Ataların Koruması', 'SPIRIT', 'BUFF', 0, 30, 600, 3),
    ('spirit_wolves', 'Ruh Kurtları', 'SPIRIT', 'SUMMON', 0, 30, 500, 3),
    ('soul_link', 'Ruh Bağı', 'SPIRIT', 'CHAIN', 7, 20, 60, 3),
    ('banish', 'Sürgün', 'SPIRIT', 'STRIKE', 14, 30, 140, 4),
    ('ghost_walk', 'Hayalet Yürüyüşü', 'SPIRIT', 'TELEPORT', 0, 15, 80, 3),
    ('wail_of_souls', 'Ruhların Feryadı', 'SPIRIT', 'NOVA', 9, 26, 90, 4),
    ('spirit_beam', 'Ruh Işını', 'SPIRIT', 'BEAM', 3, 4, 2, 4),
]

# ---------------------------------------------------------------- swords
# id, name, damage bonus over tier, speed, tier(0 wood..4 netherite,5 mythic), blade color, guard color, grip color, gem color,
# blade shape, passive, skill, description
SWORD_SHAPES = ['straight', 'broad', 'curved', 'rapier', 'serrated', 'flame', 'great', 'dagger', 'crystal', 'bone', 'katana', 'cleaver']
SWORD_PASSIVES = ['NONE', 'BURN', 'FREEZE', 'SHOCK', 'POISON', 'LIFESTEAL', 'WITHER', 'BLEED', 'KNOCKUP', 'HOLY', 'EXECUTE',
                  'CRIT', 'MANA_STEAL', 'SLOW', 'BLIND', 'WEAKEN', 'GIANT_SLAYER', 'DEMON_SLAYER', 'UNDEAD_SLAYER', 'DRAGON_SLAYER']
SWORD_SKILLS = ['FLAME_WAVE', 'FROST_NOVA', 'THUNDER_STRIKE', 'POISON_FAN', 'BLOOD_FRENZY', 'SHADOW_DASH', 'WHIRLWIND',
                'GROUND_SLAM', 'HOLY_SMITE', 'CRESCENT_SLASH', 'BLADE_STORM', 'DRAGON_BREATH', 'SOUL_REAP', 'WIND_CUTTER',
                'EARTH_SPLITTER', 'STAR_FALL', 'VOID_RIFT', 'MIRROR_CLONES', 'BERSERK', 'WAR_CRY', 'TIDE_SLASH', 'METEOR_DROP',
                'PHANTOM_BLADES', 'ICE_PRISON', 'CHAIN_SHOCK', 'LIFE_BLOOM', 'EXECUTION', 'ASH_CYCLONE', 'GRAVITY_CRUSH',
                'SPIRIT_ARMY', 'SUN_BURST', 'ROYAL_DECREE']
SWORDS = [
    ('rusty_shortsword', 'Paslı Kısa Kılıç', 1, -2.2, 0, 0x8a6a50, 0x5a4a3a, 0x4a3020, None, 'straight', 'NONE', 'CRESCENT_SLASH'),
    ('militia_sword', 'Milis Kılıcı', 1, -2.4, 1, 0xc0c0c0, 0x7a6a50, 0x5a3a20, None, 'straight', 'NONE', 'WAR_CRY'),
    ('aldorian_longsword', 'Aldoria Uzun Kılıcı', 2, -2.4, 2, 0xe0e0e8, 0xe0b040, 0x2a3a8a, 0x3060ff, 'straight', 'CRIT', 'CRESCENT_SLASH'),
    ('knight_oath', 'Şövalye Yemini', 3, -2.4, 3, 0xf0f0f8, 0xf0c040, 0x8a1a1a, 0xff3030, 'broad', 'HOLY', 'ROYAL_DECREE'),
    ('elven_moonblade', 'Elf Ay Kılıcı', 3, -2.0, 3, 0xd0f0ff, 0xc0e0d0, 0x2a5a4a, 0x80ffff, 'curved', 'MANA_STEAL', 'WIND_CUTTER'),
    ('dwarven_runeblade', 'Cüce Rün Kılıcı', 4, -2.6, 3, 0xa0a0a8, 0xc08030, 0x4a2a10, 0xff8000, 'broad', 'KNOCKUP', 'EARTH_SPLITTER'),
    ('orc_cleaver', 'Ork Satırı', 5, -3.0, 2, 0x7a7a70, 0x4a3020, 0x2a1a10, None, 'cleaver', 'BLEED', 'BERSERK'),
    ('giant_greatsword', 'Dev Kılıcı', 7, -3.2, 3, 0xb0b8c0, 0x6a7080, 0x3a3a3a, 0x60a0ff, 'great', 'GIANT_SLAYER', 'GROUND_SLAM'),
    ('demon_fang', 'İblis Dişi', 4, -2.2, 4, 0x3a0a0a, 0xa01010, 0x1a1a1a, 0xff4000, 'serrated', 'WITHER', 'VOID_RIFT'),
    ('flame_tongue', 'Alev Dili', 3, -2.4, 3, 0xff6010, 0x4a1a0a, 0x2a0a06, 0xffd040, 'flame', 'BURN', 'FLAME_WAVE'),
    ('frostmourne', 'Ayaz Yası', 4, -2.4, 4, 0x90d0ff, 0x30405a, 0x1a2030, 0x60ffff, 'broad', 'FREEZE', 'ICE_PRISON'),
    ('stormcaller', 'Fırtına Çağıran', 4, -2.2, 4, 0xa0c0ff, 0x3a4a8a, 0x1a2040, 0xffffff, 'straight', 'SHOCK', 'THUNDER_STRIKE'),
    ('viper_fang', 'Engerek Dişi', 2, -1.8, 2, 0x60c040, 0x2a4a20, 0x1a2a10, 0xa0ff40, 'dagger', 'POISON', 'POISON_FAN'),
    ('bloodthirster', 'Kan Emici', 4, -2.2, 4, 0xa01020, 0x2a0a0a, 0x1a0a0a, 0xff2040, 'serrated', 'LIFESTEAL', 'BLOOD_FRENZY'),
    ('shadow_edge', 'Gölge Kenarı', 3, -1.9, 3, 0x2a1a3a, 0x5a3a8a, 0x101018, 0xc040ff, 'katana', 'BLIND', 'SHADOW_DASH'),
    ('dawnbreaker', 'Şafak Kıran', 5, -2.4, 4, 0xfff0c0, 0xffd040, 0xe0e0e0, 0xffffa0, 'broad', 'UNDEAD_SLAYER', 'SUN_BURST'),
    ('dragonbane', 'Ejderha Kıran', 6, -2.8, 5, 0xe0d0b0, 0x8a1a10, 0x3a1a10, 0xff6000, 'great', 'DRAGON_SLAYER', 'DRAGON_BREATH'),
    ('soul_reaper', 'Ruh Biçen', 5, -2.6, 4, 0x60ffe0, 0x1a3a3a, 0x0a1a1a, 0xd0fff8, 'curved', 'EXECUTE', 'SOUL_REAP'),
    ('tempest_katana', 'Kasırga Katanası', 3, -1.8, 4, 0xe0f0f0, 0x2a2a2a, 0x8a1a1a, 0x80ffd0, 'katana', 'CRIT', 'BLADE_STORM'),
    ('whirlwind_saber', 'Kasırga Palası', 3, -2.0, 3, 0xd0e0e0, 0x6a8a8a, 0x2a3a3a, 0xa0ffff, 'curved', 'SLOW', 'WHIRLWIND'),
    ('crystal_sword', 'Kristal Kılıç', 4, -2.2, 4, 0xe0a0ff, 0x8040c0, 0x3a1a5a, 0xffffff, 'crystal', 'MANA_STEAL', 'STAR_FALL'),
    ('bone_blade', 'Kemik Kılıç', 3, -2.4, 2, 0xe8e0c8, 0x5a5040, 0x2a2a20, 0x40ff80, 'bone', 'WITHER', 'SPIRIT_ARMY'),
    ('tide_blade', 'Gelgit Kılıcı', 3, -2.2, 3, 0x40a0ff, 0x206080, 0x103040, 0x80ffff, 'curved', 'SLOW', 'TIDE_SLASH'),
    ('meteor_hammer_blade', 'Meteor Kılıcı', 6, -3.0, 4, 0x5a3a2a, 0xff6010, 0x2a1a10, 0xffa040, 'great', 'BURN', 'METEOR_DROP'),
    ('phantom_rapier', 'Hayalet Meç', 2, -1.6, 3, 0xc0e0ff, 0xa0a0c0, 0x3a3a5a, 0x80c0ff, 'rapier', 'CRIT', 'PHANTOM_BLADES'),
    ('mirror_blade', 'Ayna Kılıcı', 3, -2.0, 4, 0xf0f0ff, 0xc0c0d0, 0x5a5a6a, 0xffffff, 'rapier', 'BLIND', 'MIRROR_CLONES'),
    ('thunder_fang', 'Gök Dişi', 3, -2.0, 3, 0xffe040, 0x3a3a6a, 0x1a1a2a, 0xa0e0ff, 'serrated', 'SHOCK', 'CHAIN_SHOCK'),
    ('life_bloom', 'Hayat Çiçeği', 2, -2.2, 3, 0x80e060, 0xf0a0c0, 0x3a6a2a, 0xffc0e0, 'curved', 'NONE', 'LIFE_BLOOM'),
    ('executioner', 'Cellat Kılıcı', 7, -3.2, 4, 0x6a6a70, 0x2a2a2a, 0x3a0a0a, 0xff0000, 'cleaver', 'EXECUTE', 'EXECUTION'),
    ('ashbringer', 'Kül Getiren', 5, -2.6, 5, 0xd0c8c0, 0x4a3a3a, 0x2a1a1a, 0xff8040, 'broad', 'DEMON_SLAYER', 'ASH_CYCLONE'),
    ('gravity_edge', 'Yerçekimi Kılıcı', 5, -2.8, 5, 0x3a2a5a, 0x1a1a2a, 0x0a0a1a, 0xa060ff, 'great', 'WEAKEN', 'GRAVITY_CRUSH'),
    ('crown_of_kings', 'Kralların Kılıcı', 6, -2.4, 5, 0xfff0d0, 0xffd040, 0x5a1a8a, 0xff40ff, 'broad', 'HOLY', 'ROYAL_DECREE'),
    ('starfall_blade', 'Yıldız Kılıcı', 5, -2.2, 5, 0x2a3a8a, 0xe0e0ff, 0x1a1a3a, 0xffffff, 'straight', 'MANA_STEAL', 'STAR_FALL'),
    ('wind_cutter', 'Rüzgar Kesen', 3, -1.8, 3, 0xe0fff0, 0x80c0a0, 0x2a4a3a, 0xc0ffe0, 'katana', 'KNOCKUP', 'WIND_CUTTER'),
    ('void_reaver', 'Boşluk Yağmacısı', 6, -2.6, 5, 0x1a0a2a, 0x6a2aaa, 0x0a0a0a, 0xff00ff, 'serrated', 'WITHER', 'VOID_RIFT'),
]
