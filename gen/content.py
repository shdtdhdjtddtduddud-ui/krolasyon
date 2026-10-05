# -*- coding: utf-8 -*-
"""Single source of truth for all content ids / stats / names. build_assets.py exports Java + lang from this."""

RANKS = ['E', 'D', 'C', 'B', 'A', 'S', 'N']
RANK_NAMES_TR = {'E': 'E-Rütbe', 'D': 'D-Rütbe', 'C': 'C-Rütbe', 'B': 'B-Rütbe', 'A': 'A-Rütbe', 'S': 'S-Rütbe', 'N': 'Ulusal Seviye'}

# id, rank, level_req, damage, attack_speed_mod, durability, special, special_power, tr, en, desc_tr, desc_en
WEAPONS = [
    ('hunter_dagger', 'E', 1, 5, -1.6, 450, 'none', 0, 'Avcı Hançeri', "Hunter's Dagger", 'Yeni uyanmış avcıların hançeri.', "A dagger for freshly awakened hunters."),
    ('iron_blade', 'D', 5, 7, -2.4, 650, 'none', 0, 'Demir Kılıç', 'Iron Blade', 'Sağlam bir D-Rütbe kılıcı.', 'A sturdy D-rank blade.'),
    ('knight_killer', 'C', 15, 9, -1.8, 900, 'knight', 6, 'Şövalye Katili', 'Knight Killer', 'Zırhlı düşmanlara ekstra hasar verir.', 'Deals bonus damage to armored foes.'),
    ('mithril_sword', 'B', 20, 10, -2.2, 1200, 'frost', 1, 'Mithril Kılıç', 'Mithril Sword', 'Düşmanları yavaşlatan hafif bir kılıç.', 'A light blade that slows enemies.'),
    ('kasaka_venom_fang', 'B', 25, 9, -1.6, 1000, 'poison', 3, "Kasaka'nın Zehirli Dişi", "Kasaka's Venom Fang", 'Düşmanları zehirler.', 'Poisons its targets.'),
    ('baruka_dagger', 'A', 35, 12, -1.5, 1500, 'frost', 3, "Baruka'nın Hançeri", "Baruka's Dagger", 'Buz Elfi Kralının dondurucu hançeri.', "The Ice Elf King's freezing dagger."),
    ('obsidian_greatsword', 'A', 40, 17, -3.0, 2000, 'knockback', 2, 'Obsidyen Büyük Kılıç', 'Obsidian Greatsword', 'Düşmanları savuran ağır bir kılıç.', 'A heavy blade that launches foes.'),
    ('igris_blade', 'A', 45, 16, -2.6, 2200, 'bleed', 4, "Igris'in Kılıcı", "Igris's Blade", 'Kırmızı Şövalye\'nin kanatan kılıcı.', "The Red Knight's bleeding blade."),
    ('demon_king_dagger', 'S', 60, 14, -1.2, 2800, 'lifesteal', 20, 'Şeytan Kralının Hançeri', "Demon King's Daggers", 'Verilen hasarın bir kısmını can olarak geri verir.', 'Steals life from its targets.'),
    ('demon_monarch_longsword', 'S', 70, 22, -2.6, 3200, 'fire', 8, 'Şeytan Hükümdarının Uzun Kılıcı', "Demon Monarch's Longsword", 'Cehennem ateşiyle yanar.', 'Burns with hellfire.'),
    ('kamish_wrath', 'S', 80, 17, -1.4, 3600, 'inferno', 10, "Kamish'in Gazabı", "Kamish's Wrath", 'Ejderha Kamish\'in ateşini taşır.', "Carries the fire of the dragon Kamish."),
    ('shadow_monarch_sword', 'N', 100, 26, -2.0, 5000, 'shadow', 12, 'Gölge Hükümdarının Kılıcı', "Shadow Monarch's Blade", 'Vuruşlarda gölge kesikleri yaratır.', 'Strikes spawn shadow slashes.'),
]

# staves: id, rank, level, damage, mana_cost, kind, tr, en, desc
STAVES = [
    ('ice_elf_staff', 'A', 35, 9, 12, 'ice', 'Buz Elfi Asası', "Ice Elf's Staff", 'Sağ tık: buz mermisi fırlatır.', 'Right-click: fires ice bolts.'),
    ('necromancer_staff', 'S', 60, 12, 20, 'shadow', 'Nekromancer Asası', "Necromancer's Staff", 'Sağ tık: gölge mermisi; gölgeleri iyileştirir.', 'Right-click: shadow bolt; heals shadows.'),
]

# id, kind, amount(percent or flat), tr, en, desc
POTIONS = [
    ('hp_potion_small', 'hp', 25, 'Düşük Seviye Can İksiri', 'Low-Grade Healing Potion', 'Canın %25\'ini yeniler.', 'Restores 25% HP.'),
    ('hp_potion_medium', 'hp', 50, 'Orta Seviye Can İksiri', 'Mid-Grade Healing Potion', 'Canın %50\'sini yeniler.', 'Restores 50% HP.'),
    ('hp_potion_large', 'hp', 100, 'Yüksek Seviye Can İksiri', 'High-Grade Healing Potion', 'Canı tamamen yeniler.', 'Fully restores HP.'),
    ('mp_potion_small', 'mp', 30, 'Düşük Seviye Mana İksiri', 'Low-Grade Mana Potion', 'Mananın %30\'unu yeniler.', 'Restores 30% MP.'),
    ('mp_potion_large', 'mp', 100, 'Yüksek Seviye Mana İksiri', 'High-Grade Mana Potion', 'Manayı tamamen yeniler.', 'Fully restores MP.'),
    ('antidote', 'antidote', 0, 'Panzehir', 'Antidote', 'Zehir ve kötü etkileri temizler.', 'Cures poison and bad effects.'),
    ('stamina_potion', 'stamina', 0, 'Dayanıklılık İksiri', 'Stamina Potion', 'Yorgunluğu giderir, kısa süre hız verir.', 'Removes fatigue, grants brief speed.'),
    ('elixir_of_life', 'elixir', 0, 'Yaşam İksiri', 'Elixir of Life', 'Can, mana ve yorgunluğu tamamen yeniler.', 'Fully restores HP, MP and fatigue.'),
    ('rebirth_elixir', 'rebirth', 0, 'Yeniden Doğuş İksiri', 'Elixir of Rebirth', 'Tüm istatistik puanlarını sıfırlar.', 'Resets all stat points.'),
]

# crystals: id, rank, xp/gold value, tr, en
CRYSTALS = [
    ('mana_crystal_e', 'E', 10, 'E-Rütbe Mana Kristali', 'E-Rank Mana Crystal'),
    ('mana_crystal_d', 'D', 40, 'D-Rütbe Mana Kristali', 'D-Rank Mana Crystal'),
    ('mana_crystal_c', 'C', 120, 'C-Rütbe Mana Kristali', 'C-Rank Mana Crystal'),
    ('mana_crystal_b', 'B', 400, 'B-Rütbe Mana Kristali', 'B-Rank Mana Crystal'),
    ('mana_crystal_a', 'A', 1200, 'A-Rütbe Mana Kristali', 'A-Rank Mana Crystal'),
    ('mana_crystal_s', 'S', 4000, 'S-Rütbe Mana Kristali', 'S-Rank Mana Crystal'),
]

MISC_ITEMS = [
    ('gold_coin', 'Altın Sikke', 'Gold Coin', 'Sağ tık: bakiyene eklenir.', 'Right-click: adds to your balance.'),
    ('dungeon_key', 'Zindan Anahtarı', 'Dungeon Key', 'Sağ tık: seviyene uygun bir Kapı açar.', 'Right-click: opens a Gate fitting your level.'),
    ('red_gate_key', 'Kırmızı Kapı Anahtarı', 'Red Gate Key', 'Sağ tık: tehlikeli bir Kırmızı Kapı açar.', 'Right-click: opens a dangerous Red Gate.'),
    ('skill_scroll', 'Yetenek Parşömeni', 'Skill Scroll', 'Sağ tık: rastgele bir yetenek öğretir.', 'Right-click: teaches a random skill.'),
    ('hunter_license', 'Avcı Lisansı', 'Hunter License', 'Sağ tık: Sistem penceresini açar.', 'Right-click: opens the System window.'),
    ('world_map', 'Dünya Haritası', 'World Map', 'Sağ tık: Bölge haritasını ve hızlı seyahati açar.', 'Right-click: opens the region map and fast travel.'),
    ('hunter_news', 'Avcı Haberleri', 'Hunter News', 'Sağ tık: güncel haberleri okur.', 'Right-click: read the latest news.'),
    ('rune_stone', 'Rün Taşı', 'Rune Stone', 'Sağ tık: 1 istatistik puanı verir.', 'Right-click: grants 1 stat point.'),
    ('shadow_essence', 'Gölge Özü', 'Shadow Essence', 'Gölge çıkarmada başarı şansını artırır.', 'Improves shadow extraction chance.'),
]

# armor sets: id, tr, en, level, defense(h,c,l,b), toughness, kb_res
ARMOR = [
    ('hunter', 'Avcı', 'Hunter', 8, (2, 6, 5, 2), 1.0, 0.0),
    ('knight', 'Kızıl Şövalye', 'Red Knight', 40, (3, 8, 6, 3), 3.0, 0.1),
    ('ice', 'Buz Elfi', 'Ice Elf', 35, (3, 7, 6, 3), 2.0, 0.0),
    ('monarch', 'Gölge Hükümdarı', 'Shadow Monarch', 80, (4, 9, 7, 4), 4.0, 0.2),
]
ARMOR_PIECES_TR = {'helmet': 'Miğfer', 'chestplate': 'Zırh', 'leggings': 'Pantolon', 'boots': 'Çizme'}
ARMOR_PIECES_EN = {'helmet': 'Helmet', 'chestplate': 'Chestplate', 'leggings': 'Leggings', 'boots': 'Boots'}

# monsters: id, model, rank, hp, dmg, speed, armor, xp, scale, width, height, boss, abilities, tr, en, egg1, egg2, shadow
MONSTERS = [
    ('goblin', 'goblin', 'E', 16, 3, 0.30, 0, 12, 0.85, 0.6, 1.55, False, [], 'Goblin', 'Goblin', 0x4C8A34, 0x2A1A10, 'soldier'),
    ('orc', 'orc', 'D', 50, 7, 0.26, 2, 45, 1.2, 0.95, 2.3, False, ['slam'], 'Ork', 'Orc', 0x5A7A48, 0x7A5A2A, 'tank'),
    ('goblin_warlord', 'orc', 'D', 260, 11, 0.27, 4, 260, 1.65, 1.3, 3.2, True, ['slam', 'summon_goblin', 'charge'], 'Goblin Savaş Lordu', 'Goblin Warlord', 0x3A5A2A, 0xC0A040, 'tank'),
    ('stone_soldier', 'stone_soldier', 'C', 80, 10, 0.24, 6, 80, 1.15, 0.8, 2.3, False, [], 'Taş Asker', 'Stone Soldier', 0x8A8A96, 0x58C0FF, 'tank'),
    ('statue_of_god', 'statue_of_god', 'C', 520, 16, 0.22, 8, 600, 2.1, 1.7, 4.8, True, ['slam', 'sword_beam', 'summon_stone'], 'Tanrı Heykeli', 'Statue of God', 0xE6E2D2, 0xFFD25A, 'tank'),
    ('venom_ant', 'venom_ant', 'B', 70, 11, 0.33, 2, 110, 1.1, 1.3, 1.0, False, ['spit'], 'Zehirli Karınca', 'Venom Ant', 0x2A6A34, 0xDDE070, 'soldier'),
    ('kasaka', 'kasaka', 'B', 700, 18, 0.30, 4, 1100, 1.7, 1.6, 2.0, True, ['spit', 'lunge', 'poison_cloud'], 'Mavi Zehirli Dişli Kasaka', 'Kasaka, the Venom-Fanged', 0x1E7A44, 0xFFF03C, 'soldier'),
    ('ice_elf', 'ice_elf', 'A', 110, 14, 0.30, 3, 220, 1.0, 0.6, 1.95, False, ['frost_bolt'], 'Buz Elfi', 'Ice Elf', 0xB8DCFF, 0x5090E0, 'mage'),
    ('baruka', 'baruka', 'A', 1100, 24, 0.32, 6, 2400, 1.35, 0.7, 2.7, True, ['frost_bolt', 'ice_nova', 'summon_elf', 'blink'], "Buz Elfi Kralı Baruka", 'Baruka, the Ice Elf King', 0xF0F8FF, 0x60C8FF, 'mage'),
    ('hell_hound', 'hell_hound', 'A', 120, 15, 0.38, 2, 240, 1.0, 1.1, 1.2, False, ['leap'], 'Cehennem Köpeği', 'Hell Hound', 0x6A2A24, 0xFF7A30, 'wolf'),
    ('cerberus', 'cerberus', 'A', 1000, 22, 0.34, 5, 2200, 1.7, 1.8, 2.6, True, ['leap', 'fire_breath', 'howl'], 'Kerberos', 'Cerberus', 0x7A2A20, 0xFF5A18, 'wolf'),
    ('giant_ant', 'giant_ant', 'S', 160, 20, 0.34, 4, 420, 1.2, 1.4, 1.1, False, ['leap'], 'Dev Karınca', 'Giant Ant', 0x7A2E1E, 0xFF6A38, 'soldier'),
    ('ant_king', 'ant_king', 'S', 2400, 34, 0.36, 8, 6000, 1.55, 1.1, 3.4, True, ['charge', 'slam', 'summon_ant', 'blink'], 'Karınca Kral', 'The Ant King', 0x7A1E18, 0xFF5A30, 'beru'),
    ('demon_knight', 'demon_knight', 'S', 220, 24, 0.28, 8, 520, 1.15, 0.8, 2.4, False, ['charge'], 'Şeytan Şövalye', 'Demon Knight', 0x4A2A5A, 0xFF3A5A, 'igris'),
    ('igris', 'igris', 'S', 2000, 30, 0.31, 10, 5000, 1.35, 0.9, 2.9, True, ['sword_beam', 'charge', 'slam', 'blink'], 'Kızıl Şövalye Igris', 'Igris, the Red Knight', 0xB0202E, 0xFFC050, 'igris'),
    ('kamish', 'kamish', 'N', 6000, 55, 0.30, 15, 20000, 3.0, 3.2, 3.6, True, ['fire_breath', 'slam', 'charge', 'howl'], 'Ejderha Kamish', 'Kamish, the Dragon', 0x9A1A10, 0xFFB030, None),
]

# shadow ids: id, model, tr, en, hp, dmg, speed, scale, width, height, ranged
SHADOWS = [
    ('shadow_soldier', 'shadow_soldier', 'Gölge Asker', 'Shadow Soldier', 30, 5, 0.32, 1.0, 0.6, 1.9, False),
    ('shadow_mage', 'shadow_mage', 'Gölge Büyücü', 'Shadow Mage', 24, 6, 0.30, 1.0, 0.6, 1.9, True),
    ('shadow_wolf', 'shadow_wolf', 'Gölge Kurt', 'Shadow Wolf', 28, 6, 0.40, 1.0, 0.9, 1.0, False),
    ('shadow_tank', 'shadow_tank', 'Gölge Tank', 'Shadow Tank', 80, 8, 0.26, 1.3, 1.5, 1.6, False),
    ('shadow_igris', 'shadow_igris', 'Gölge Igris', 'Shadow Igris', 120, 14, 0.34, 1.35, 0.8, 2.9, False),
    ('shadow_beru', 'shadow_beru', 'Gölge Beru', 'Shadow Beru', 140, 16, 0.38, 1.5, 1.0, 3.3, False),
]

# NPC roles: id, tr, en
NPC_ROLES = [
    ('receptionist', 'Birlik Görevlisi', 'Association Clerk'),
    ('guild_master', 'Lonca Lideri', 'Guild Master'),
    ('merchant', 'Tüccar', 'Merchant'),
    ('journalist', 'Gazeteci', 'Journalist'),
    ('healer', 'Şifacı', 'Healer'),
    ('hunter', 'Avcı', 'Hunter'),
    ('citizen', 'Vatandaş', 'Citizen'),
]

# skills: id, tr, en, mana, cooldown_s, min_level, desc_tr, desc_en
SKILLS = [
    ('shadow_extraction', 'Gölge Çıkarma', 'Shadow Extraction', 0, 3, 1, 'Yakındaki bir cesetten gölge askeri uyandırır ("ARISE").', 'Raises a shadow soldier from a nearby corpse ("ARISE").'),
    ('sprint', 'Atılma', 'Sprint', 10, 8, 1, 'Kısa süre çok hızlı koşarsın.', 'Run extremely fast for a short time.'),
    ('dagger_rush', 'Hançer Atılışı', 'Dagger Rush', 14, 6, 5, 'İleri atılır ve yoldaki düşmanlara hasar verirsin.', 'Dash forward, damaging enemies in your path.'),
    ('violent_slash', 'Şiddetli Kesik', 'Violent Slash', 20, 8, 10, 'Önündeki düşmanlara geniş bir kesik atar.', 'A wide slash hitting enemies in front.'),
    ('stealth', 'Gizlilik', 'Stealth', 18, 20, 12, 'Görünmez olur ve canavarların dikkatini dağıtırsın.', 'Turn invisible and lose monster aggro.'),
    ('bloodlust', 'Kan Susuzluğu', 'Bloodlust', 25, 25, 20, 'Yakındaki düşmanları korkutup yavaşlatır.', 'Terrifies and slows nearby enemies.'),
    ('shadow_step', 'Gölge Adımı', 'Shadow Step', 22, 10, 25, 'Baktığın yere ışınlanırsın.', 'Teleport to where you look.'),
    ('mutilation', 'Parçalama', 'Mutilation', 30, 12, 30, 'Hedefe ardı ardına hızlı vuruşlar.', 'A flurry of rapid strikes on the target.'),
    ('rulers_authority', 'Hükümdarın Otoritesi', "Ruler's Authority", 35, 14, 40, 'Yakındaki düşmanları havaya kaldırıp fırlatır.', 'Telekinetically lifts and hurls nearby enemies.'),
    ('quicksilver', 'Cıva Hızı', 'Quicksilver', 40, 40, 50, 'Hız ve saldırı hızını muazzam artırır.', 'Hugely boosts speed and attack speed.'),
    ('shadow_exchange', 'Gölge Değişimi', 'Shadow Exchange', 30, 10, 60, 'En yakın gölgenle yer değiştirirsin.', 'Swap places with your nearest shadow.'),
    ('dragons_fear', 'Ejderhanın Korkusu', "Dragon's Fear", 60, 45, 70, 'Büyük alanda düşmanları felç edip hasar verir.', 'Paralyzes and damages enemies in a huge area.'),
    ('domain_of_monarch', 'Hükümdarın Alanı', "Monarch's Domain", 80, 90, 90, 'Gölgelerini ve seni güçlendiren bir alan yaratır.', 'Creates a domain empowering you and your shadows.'),
]

# gate themes: id, ranks, tr, en
THEMES = [
    ('goblin_cave', 'E-D', 'Goblin Mağarası', 'Goblin Cave'),
    ('temple', 'D-C', 'Çifte Zindan: Tanrı Tapınağı', 'Double Dungeon: Temple of the Gods'),
    ('venom_swamp', 'B', "Kasaka'nın Bataklığı", "Kasaka's Swamp"),
    ('ice_cave', 'A', 'Kırmızı Kapı: Buz Mağarası', 'Red Gate: Ice Cavern'),
    ('hell_den', 'A', 'Cehennem İni', "Cerberus' Den"),
    ('ant_nest', 'S', 'Cheju Adası Karınca Yuvası', 'Jeju Island Ant Nest'),
    ('demon_castle', 'S', 'Şeytan Şatosu', 'Demon Castle'),
    ('dragon_lair', 'N', 'Ejderha Kamish\'in İni', "Kamish's Lair"),
]

# regions of the Hunter World: id, tr, en, cx, cz, radius, type
REGIONS = [
    ('seoul', 'Seul', 'Seoul', 0, 0, 170, 'capital'),
    ('gangnam', 'Gangnam', 'Gangnam', 620, 340, 120, 'guild'),
    ('busan', 'Busan', 'Busan', -820, 700, 120, 'harbor'),
    ('incheon', 'Incheon', 'Incheon', -540, -560, 100, 'harbor'),
    ('jeju', 'Cheju Adası', 'Jeju Island', 960, -920, 100, 'ruins'),
]

SKILL_IDS = [s[0] for s in SKILLS]
