# -*- coding: utf-8 -*-
"""Turkish + English language files. Every entry: key -> (tr, en)."""
import json, os

P = 'sololeveling.'
L = {}


def add(key, tr, en=None):
    L[key] = (tr, en if en is not None else tr)


def s(key, tr, en=None):
    add(P + key, tr, en)


# ------------------------------------------------------------------ registry names
MOBS = {
    'goblin': ('Goblin', 'Goblin'), 'hobgoblin': ('Hobgoblin', 'Hobgoblin'), 'steel_fanged_lycan': ('Çelik Dişli Kurt Adam', 'Steel-Fanged Lycan'),
    'giant_centipede': ('Dev Kırkayak', 'Giant Centipede'), 'kasaka': ('Kasaka', 'Kasaka'), 'stone_statue': ('Taş Heykel', 'Stone Statue'),
    'statue_of_god': ('Tanrı Heykeli', 'Statue of God'), 'castle_knight': ('Kale Şövalyesi', 'Castle Knight'),
    'igris': ('Kanlı Kızıl Komutan Igris', 'Blood-Red Commander Igris'), 'ice_elf': ('Buz Elfi', 'Ice Elf'), 'ice_bear': ('Buz Ayısı', 'Ice Bear'),
    'baruka': ('Buz Elfi Şefi Baruka', 'Ice Elf Chief Baruka'), 'high_orc': ('Yüksek Ork', 'High Orc'), 'kargalgan': ('Ork Şamanı Kargalgan', 'Orc Shaman Kargalgan'),
    'demon': ('İblis', 'Demon'), 'cerberus': ('Kapı Bekçisi Cerberus', 'Gatekeeper Cerberus'), 'baran': ('İblis Kral Baran', 'Demon King Baran'),
    'ant_soldier': ('Karınca Askeri', 'Ant Soldier'), 'beru': ('Karınca Kral Beru', 'Ant King Beru'),
}
for k, (tr, en) in MOBS.items():
    add('entity.sololeveling.' + k, tr, en)
    add('item.sololeveling.' + k + '_spawn_egg', tr + ' Doğurma Yumurtası', en + ' Spawn Egg')
add('entity.sololeveling.shadow_soldier', 'Gölge Asker', 'Shadow Soldier')
add('entity.sololeveling.hunter_npc', 'Seullü', 'Seoul Citizen')
add('entity.sololeveling.gate', 'Kapı', 'Gate')
add('entity.sololeveling.magic_projectile', 'Büyü', 'Spell')

ITEMS = {
    'rusty_dagger': ('Paslı Hançer', 'Rusty Dagger', 'Sistemin verdiği ilk silah. Zayıf ama hızlı.', 'The first weapon the System gave you. Weak, but quick.'),
    'steel_dagger': ('Çelik Hançer', 'Steel Dagger', 'Avcı Derneği standart hançeri.', 'Standard issue Hunter Association dagger.'),
    'kasaka_venom_fang': ("Kasaka'nın Zehirli Dişi", "Kasaka's Venom Fang", 'Vuruşlar zehirler ve bazen felç eder.', 'Hits poison and sometimes paralyse.'),
    'knight_killer': ('Şövalye Katili', 'Knight Killer', 'Zırhlı düşmanlara ve şövalyelere ek hasar verir.', 'Deals extra damage to armored foes and knights.'),
    'baruka_dagger': ("Baruka'nın Hançeri", "Baruka's Dagger", 'Buz elfi şefinin hançeri. Vuruşlar yavaşlatır.', "The ice elf chief's dagger. Hits slow enemies."),
    'demon_king_dagger': ('İblis Kral Hançeri', "Demon King's Dagger", 'Vuruşlar bazen çevredeki düşmanlara yıldırım zinciri atar.', 'Hits sometimes chain lightning to nearby foes.'),
    'kamish_wrath': ("Kamish'in Gazabı", "Kamish's Wrath", 'Ejderha Kamish’in dişinden dövülmüş efsanevi hançer.', 'Legendary dagger forged from the fang of the dragon Kamish.'),
    'hunter_sword': ('Avcı Kılıcı', 'Hunter Sword', 'Sağlam, dengeli bir kılıç.', 'A sturdy, balanced sword.'),
    'crimson_greatsword': ('Kızıl Komutanın Büyük Kılıcı', "Crimson Commander's Greatsword", 'Igris’in devasa kılıcı. Vuruşlar geri iter.', "Igris' massive blade. Hits knock enemies back."),
    'demon_king_longsword': ('İblis Kral Uzun Kılıcı', "Demon King's Longsword", 'Baran’ın şimşek yüklü kılıcı.', "Baran's lightning-charged sword."),
    'haein_sword': ("Hae-In'in Kılıcı", "Hae-In's Sword", 'Cha Hae-In’in zarif ve ölümcül kılıcı.', "Cha Hae-In's elegant, deadly sword."),
    'orc_war_axe': ('Ork Savaş Baltası', 'Orc War Axe', 'Yüksek orkların ağır baltası.', 'The heavy axe of the high orcs.'),
    'flame_staff': ('Alev Asası', 'Flame Staff', 'Choi Jong-In tarzı alev büyüsü asası.', 'A flame magic staff in the style of Choi Jong-In.'),
    'orb_of_avarice': ('Açgözlülük Küresi', 'Orb of Avarice', 'Büyü hasarını artıran kadim küre.', 'An ancient orb that amplifies magic.'),
    'healing_potion': ('İyileştirme İksiri', 'Healing Potion', 'Canı yeniler.', 'Restores health.'),
    'greater_healing_potion': ('Büyük İyileştirme İksiri', 'Greater Healing Potion', 'Canı büyük oranda yeniler ve yenilenme verir.', 'Restores a lot of health and grants regeneration.'),
    'mana_potion': ('Mana İksiri', 'Mana Potion', 'Manayı yeniler.', 'Restores mana.'),
    'greater_mana_potion': ('Büyük Mana İksiri', 'Greater Mana Potion', 'Manayı büyük oranda yeniler.', 'Restores a lot of mana.'),
    'elixir_of_life': ('Yaşam İksiri', 'Elixir of Life', 'Her şeyi iyileştirir. Efsanevi bir iksir.', 'Heals everything. A legendary elixir.'),
    'holy_water_of_life': ('Yaşamın Kutsal Suyu', 'Holy Water of Life', 'Her hastalığı iyileştiren mucizevi su.', 'Miraculous water that cures any illness.'),
    'fatigue_recovery_potion': ('Yorgunluk Giderici İksir', 'Fatigue Recovery Potion', 'Açlığı ve yorgunluğu giderir.', 'Removes hunger and fatigue.'),
    'strength_elixir': ('Güç İksiri', 'Strength Elixir', '3 dakika güç verir.', 'Grants strength for 3 minutes.'),
    'agility_elixir': ('Çeviklik İksiri', 'Agility Elixir', '3 dakika hız ve zıplama verir.', 'Grants speed and jump for 3 minutes.'),
    'perception_draught': ('Algı İksiri', 'Perception Draught', 'Gece görüşü verir ve yakındaki düşmanları parlatır.', 'Night vision; nearby enemies glow.'),
    'stealth_tonic': ('Gizlilik Toniği', 'Stealth Tonic', '1 dakika görünmezlik.', 'Invisibility for 1 minute.'),
    'antidote': ('Panzehir', 'Antidote', 'Zehri ve kötü etkileri temizler.', 'Cures poison and bad effects.'),
    'magic_stone_e': ('E Rütbe Büyü Taşı', 'E-Rank Magic Stone', 'Derneğe satılabilir.', 'Can be sold to the Association.'),
    'magic_stone_d': ('D Rütbe Büyü Taşı', 'D-Rank Magic Stone', 'Derneğe satılabilir.', 'Can be sold to the Association.'),
    'magic_stone_c': ('C Rütbe Büyü Taşı', 'C-Rank Magic Stone', 'Derneğe satılabilir.', 'Can be sold to the Association.'),
    'magic_stone_b': ('B Rütbe Büyü Taşı', 'B-Rank Magic Stone', 'Derneğe satılabilir.', 'Can be sold to the Association.'),
    'magic_stone_a': ('A Rütbe Büyü Taşı', 'A-Rank Magic Stone', 'Derneğe satılabilir.', 'Can be sold to the Association.'),
    'magic_stone_s': ('S Rütbe Büyü Taşı', 'S-Rank Magic Stone', 'Son derece değerli.', 'Extremely valuable.'),
    'goblin_ear': ('Goblin Kulağı', 'Goblin Ear', 'Canavar malzemesi.', 'Monster material.'),
    'lycan_fang': ('Kurt Adam Dişi', 'Lycan Fang', 'Canavar malzemesi.', 'Monster material.'),
    'kasaka_scale': ('Kasaka Pulu', 'Kasaka Scale', 'Canavar malzemesi.', 'Monster material.'),
    'statue_fragment': ('Heykel Parçası', 'Statue Fragment', 'Çift Zindan’dan bir parça.', 'A piece of the Double Dungeon.'),
    'ice_bear_pelt': ('Buz Ayısı Postu', 'Ice Bear Pelt', 'Canavar malzemesi.', 'Monster material.'),
    'elf_frost_crystal': ('Elf Buz Kristali', 'Elf Frost Crystal', 'Canavar malzemesi.', 'Monster material.'),
    'orc_tusk': ('Ork Dişi', 'Orc Tusk', 'Canavar malzemesi.', 'Monster material.'),
    'demon_horn': ('İblis Boynuzu', 'Demon Horn', 'Canavar malzemesi.', 'Monster material.'),
    'cerberus_fang': ('Cerberus Dişi', 'Cerberus Fang', 'Kapı bekçisinin dişi.', "The gatekeeper's fang."),
    'ant_carapace': ('Karınca Kabuğu', 'Ant Carapace', 'Jeju karıncalarının sert kabuğu.', 'Hard shell of the Jeju ants.'),
    'red_knight_plume': ('Kızıl Şövalye Sorgucu', 'Red Knight Plume', 'Igris’in miğferinden.', "From Igris' helmet."),
    'baran_heart': ('Baran’ın Kalbi', "Baran's Heart", 'İblis Kral’ın hâlâ atan kalbi.', "The Demon King's still beating heart."),
    'return_stone': ('Dönüş Taşı', 'Return Stone', 'Seul Kapı Meydanı’na ışınlar (30 sn bekleme).', 'Teleports to the Seoul Gate Plaza (30s cooldown).'),
    'instant_dungeon_key': ('Anlık Zindan Anahtarı', 'Instant Dungeon Key', 'Sadece sana ait bir zindan açar.', 'Opens a dungeon just for you.'),
    'demon_castle_key': ('İblis Kalesi Anahtarı', "Demon Castle Key", 'İblis Kalesi’nin kapısını açar.', 'Opens the gates of the Demon Castle.'),
    'random_box': ('Rastgele Kutu', 'Random Box', 'Sistemden rastgele ödül.', 'A random reward from the System.'),
    'hunter_license': ('Avcı Lisansı', 'Hunter License', 'Avcı Derneği tarafından verilen resmi lisans.', 'Official license issued by the Hunter Association.'),
    'newspaper': ('Avcı Gazetesi', 'Hunter Newspaper', 'Son haberleri okumak için sağ tıkla.', 'Right click to read the latest news.'),
    'rune_stone_bloodlust': ('Rün Taşı: Kana Susamışlık', 'Rune Stone: Bloodlust', 'Kullanınca yeteneği öğretir.', 'Teaches the skill when used.'),
    'rune_stone_mutilation': ('Rün Taşı: Parçalama', 'Rune Stone: Mutilation', 'Kullanınca yeteneği öğretir.', 'Teaches the skill when used.'),
    'rune_stone_stealth': ('Rün Taşı: Gizlilik', 'Rune Stone: Stealth', 'Kullanınca yeteneği öğretir.', 'Teaches the skill when used.'),
    'rune_stone_dagger_storm': ('Rün Taşı: Hançer Fırtınası', 'Rune Stone: Dagger Storm', 'Kullanınca yeteneği öğretir.', 'Teaches the skill when used.'),
}
for k, (tr, en, dtr, den) in ITEMS.items():
    add('item.sololeveling.' + k, tr, en)
    add('item.sololeveling.' + k + '.desc', dtr, den)

ARMOR = {'hunter': ('Avcı', 'Hunter'), 'high_orc': ('Yüksek Ork', 'High Orc'), 'crimson_knight': ('Kızıl Şövalye', 'Crimson Knight'),
         'shadow_monarch': ('Gölge Hükümdarı', 'Shadow Monarch')}
PIECES = {'helmet': ('Miğferi', 'Helmet'), 'chestplate': ('Göğüslüğü', 'Chestplate'), 'leggings': ('Pantolonu', 'Leggings'), 'boots': ('Çizmeleri', 'Boots')}
for a, (tr, en) in ARMOR.items():
    for p, (ptr, pen) in PIECES.items():
        add(f'item.sololeveling.{a}_{p}', f'{tr} {ptr}', f'{en} {pen}')
s('armor.hunter.set', 'Set bonusu: Hız', 'Set bonus: Speed')
s('armor.high_orc.set', 'Set bonusu: Güç', 'Set bonus: Strength')
s('armor.crimson_knight.set', 'Set bonusu: Direnç + Güç', 'Set bonus: Resistance + Strength')
s('armor.shadow_monarch.set', 'Set bonusu: Direnç II, Hız II, Gece Görüşü', 'Set bonus: Resistance II, Speed II, Night Vision')

BLOCKS = {'asphalt': ('Asfalt', 'Asphalt'), 'road_line': ('Yol Çizgisi', 'Road Line'), 'road_crossing': ('Yaya Geçidi', 'Road Crossing'),
          'sidewalk': ('Kaldırım', 'Sidewalk'), 'concrete_panel': ('Beton Panel', 'Concrete Panel'), 'dark_panel': ('Koyu Panel', 'Dark Panel'),
          'glass_facade': ('Cam Cephe', 'Glass Facade'), 'office_window': ('Aydınlık Ofis Penceresi', 'Lit Office Window'), 'dark_window': ('Karanlık Pencere', 'Dark Window'),
          'neon_blue': ('Mavi Neon', 'Blue Neon'), 'neon_red': ('Kırmızı Neon', 'Red Neon'), 'neon_purple': ('Mor Neon', 'Purple Neon'), 'neon_gold': ('Altın Neon', 'Gold Neon'),
          'association_emblem': ('Avcı Derneği Amblemi', 'Hunter Association Emblem'), 'emblem_hunters': ('Avcılar Loncası Amblemi', 'Hunters Guild Emblem'),
          'emblem_white_tiger': ('Beyaz Kaplan Loncası Amblemi', 'White Tiger Guild Emblem'), 'emblem_fiend': ('Şeytan Loncası Amblemi', 'Fiend Guild Emblem'),
          'emblem_knights': ('Şövalyeler Loncası Amblemi', 'Knights Guild Emblem'), 'emblem_ahjin': ('Ahjin Loncası Amblemi', 'Ahjin Guild Emblem'),
          'dungeon_bricks': ('Zindan Tuğlası', 'Dungeon Bricks'), 'rune_bricks': ('Rünlü Tuğla', 'Rune Bricks'), 'dungeon_floor': ('Zindan Zemini', 'Dungeon Floor'),
          'temple_stone': ('Tapınak Taşı', 'Temple Stone'), 'temple_pillar': ('Tapınak Sütunu', 'Temple Pillar'), 'demon_bricks': ('İblis Tuğlası', 'Demon Bricks'),
          'hive_wall': ('Yuva Duvarı', 'Hive Wall'), 'mana_crystal_ore': ('Mana Kristali Cevheri', 'Mana Crystal Ore'), 'region_portal': ('Bölge Portalı', 'Region Portal'),
          'news_board': ('Haber Panosu', 'News Board')}
for k, (tr, en) in BLOCKS.items():
    add('block.sololeveling.' + k, tr, en)
add('itemGroup.sololeveling', 'Solo Leveling: Arise', 'Solo Leveling: Arise')

# ------------------------------------------------------------------ keys
add('key.categories.sololeveling', 'Solo Leveling — Sistem', 'Solo Leveling — System')
for k, tr, en in (('status', 'Durum Penceresi', 'Status Window'), ('map', 'Dünya Haritası', 'World Map'), ('arise', 'Gölge Çıkarma (ARISE)', 'Shadow Extraction (ARISE)'),
                  ('shadows', 'Gölgeleri Çağır / Geri Al', 'Summon / Recall Shadows'), ('news', 'Haberler', 'News'),
                  ('skill1', 'Yetenek 1', 'Skill 1'), ('skill2', 'Yetenek 2', 'Skill 2'), ('skill3', 'Yetenek 3', 'Skill 3'), ('skill4', 'Yetenek 4', 'Skill 4'),
                  ('skill5', 'Yetenek 5', 'Skill 5')):
    add('key.sololeveling.' + k, tr, en)

# ------------------------------------------------------------------ system enums
for r, tr, en in (('E', 'E Rütbe', 'E-Rank'), ('D', 'D Rütbe', 'D-Rank'), ('C', 'C Rütbe', 'C-Rank'), ('B', 'B Rütbe', 'B-Rank'), ('A', 'A Rütbe', 'A-Rank'),
                  ('S', 'S Rütbe', 'S-Rank'), ('N', 'Ulusal Seviye', 'National Level')):
    s('rank.' + r, tr, en)
s('job.none', 'Yok', 'None')
s('job.necromancer', 'Nekromansör', 'Necromancer')
s('job.shadow_monarch', 'Gölge Hükümdarı', 'Shadow Monarch')
for st, tr, en, dtr, den in (('str', 'Güç', 'Strength', 'Fiziksel hasarı artırır.', 'Increases physical damage.'),
                             ('agi', 'Çeviklik', 'Agility', 'Hareket ve saldırı hızını artırır, kaçınma şansı verir.', 'Increases movement and attack speed, adds dodge chance.'),
                             ('vit', 'Dayanıklılık', 'Vitality', 'Maksimum canı ve sertliği artırır, canı yeniler.', 'Increases max health and toughness, regenerates health.'),
                             ('int', 'Zekâ', 'Intelligence', 'Maksimum manayı, mana yenilenmesini ve büyü hasarını artırır. Gölge kapasitesini artırır.',
                              'Increases max mana, mana regeneration and magic damage. Raises shadow capacity.'),
                             ('per', 'Algı', 'Perception', 'Kritik vuruş ve kaçınma şansını artırır.', 'Increases critical hit and dodge chance.')):
    s('stat.' + st, tr, en)
    s('stat.' + st + '.desc', dtr, den)

SKILLS = {
    'sprint': ('Koşu', 'Sprint', '10 saniye boyunca büyük hız ve zıplama.', 'Great speed and jump for 10 seconds.'),
    'dagger_throw': ('Hançer Fırlatma', 'Dagger Throw', 'Gölge bir hançer fırlatır.', 'Throws a shadow dagger.'),
    'bloodlust': ('Kana Susamışlık', 'Bloodlust', 'Çevredeki düşmanları korkuyla yavaşlatır ve zayıflatır.', 'Terrifies nearby enemies: slow and weak.'),
    'mutilation': ('Parçalama', 'Mutilation', 'İleri atılır ve yoldaki herkesi keser.', 'Dashes forward and cuts everything in the way.'),
    'stealth': ('Gizlilik', 'Stealth', 'Görünmez olursun; ilk vuruş iki kat hasar verir.', 'Become invisible; the first hit deals double damage.'),
    'vital_strike': ('Hayati Darbe', 'Vital Strike', 'Hedefin zayıf noktasına yıkıcı bir darbe.', "A devastating blow to the target's weak point."),
    'shadow_exchange': ('Gölge Değişimi', 'Shadow Exchange', 'Baktığın gölge askerle yer değiştirirsin.', 'Swap places with the shadow you look at.'),
    'rulers_authority': ('Hükümdarın Otoritesi', "Ruler's Authority", 'Telekinezi: hedefi kaldırıp yere çarpar ya da önündekileri iter.',
                         'Telekinesis: lift and slam a target, or blast everything in front.'),
    'dagger_storm': ('Hançer Fırtınası', 'Dagger Storm', 'Hançer dalgaları yağdırır.', 'Unleashes waves of daggers.'),
    'dragons_fear': ('Ejderhanın Korkusu', "Dragon's Fear", 'Kükreme: yakındaki her şeyi dondurup sersemletir.', 'Roar: freezes and stuns everything nearby.'),
    'monarchs_domain': ('Hükümdarın Alanı', "Monarch's Domain", 'Gölgelerin hasarı ve iyileşmesi artar, sen güçlenirsin.', 'Shadows deal more damage and heal; you become stronger.'),
    'tenacity': ('Azim', 'Tenacity', 'Canın %30’un altındayken aldığın hasar azalır.', 'Take less damage while below 30% health.'),
    'advanced_dagger_arts': ('İleri Hançer Sanatı', 'Advanced Dagger Arts', 'Hançerlerle %20 daha fazla hasar.', '20% more damage with daggers.'),
    'detoxification': ('Zehir Arındırma', 'Detoxification', 'Zehre karşı bağışıklık.', 'Immunity to poison.'),
    'long_range_detection': ('Uzun Menzil Algısı', 'Long Range Detection', 'Algı ile daha fazla kaçınma.', 'More dodge with perception.'),
    'shadow_preservation': ('Gölge Koruma', 'Shadow Preservation', '+4 gölge kapasitesi.', '+4 shadow capacity.'),
}
for k, (tr, en, dtr, den) in SKILLS.items():
    s('skill.' + k, tr, en)
    s('skill.' + k + '.desc', dtr, den)

TITLES = {
    'wolf_assassin': ('Kurt Suikastçısı', 'Wolf Assassin', 'Canavarlara +%40 hasar.', '+40% damage to beasts.'),
    'one_who_overcame': ('Zorluğun Üstesinden Gelen', 'One Who Overcame Adversity', 'Canın %30 altındayken +%30 hasar.', '+30% damage while below 30% health.'),
    'demon_hunter': ('İblis Avcısı', 'Demon Hunter', 'İblislere +%40 hasar.', '+40% damage to demons.'),
    'king_slayer': ('Kral Katili', 'King Slayer', "Boss'lara +%15 hasar.", '+15% damage to bosses.'),
    'ant_exterminator': ('Karınca Avcısı', 'Ant Exterminator', 'Böceklere +%40 hasar.', '+40% damage to insects.'),
    'shadow_monarch': ('Gölge Hükümdarı', 'Shadow Monarch', 'Her şeye +%10, gölgelere +%20.', '+10% everything, shadows +20%.'),
    'none': ('Yok', 'None', '', ''),
}
for k, (tr, en, dtr, den) in TITLES.items():
    s('title.' + k, tr, en)
    s('title.' + k + '.desc', dtr, den)

QUESTS = {
    'awaken': ('Uyanış', 'Awakening', 'Sistemi kabul et.', 'Accept the System.'),
    'register': ('Avcı Kaydı', 'Hunter Registration', 'Seul’deki Avcı Derneği’ne git ve resepsiyonla konuşarak lisansını al.',
                 "Go to the Hunter Association in Seoul and get your license from the receptionist."),
    'first_gate': ('İlk Kapı', 'The First Gate', 'Herhangi bir kapıyı temizle (zindan boss’unu yen).', 'Clear any gate (defeat the dungeon boss).'),
    'double_dungeon': ('Çift Zindan', 'The Double Dungeon', 'Çift Zindan’a gir ve Tanrı Heykeli’ni yen. Emir 1: Tanrıya ibadet et (heykele bakarak eğil).',
                       'Enter the Double Dungeon and defeat the Statue of God. Commandment 1: Worship God (crouch facing the statue).'),
    'reach_20': ('Güçlenme', 'Getting Stronger', '20. seviyeye ulaş.', 'Reach level 20.'),
    'kasaka': ('Zehirli Diş', 'The Venom Fang', 'Kasaka’yı yen (C rütbe kapılarda veya anlık zindanlarda).', 'Defeat Kasaka (C-rank gates or instant dungeons).'),
    'job_change': ('Meslek Değişimi', 'Job Change', '40. seviyede Meslek Değişimi Salonu’na git ve Kızıl Komutan Igris’i yen.',
                   'At level 40 go to the Job Change Hall and defeat Blood-Red Commander Igris.'),
    'red_gate': ('Kırmızı Kapı', 'The Red Gate', 'Buz elfi şefi Baruka’yı yen (B rütbe / kırmızı kapılar).', 'Defeat ice elf chief Baruka (B-rank / red gates).'),
    'orc_chieftain': ('Ork Şefi', 'The Orc Chieftain', 'Ork şamanı Kargalgan’ı yen (A rütbe kapılar).', 'Defeat orc shaman Kargalgan (A-rank gates).'),
    'demon_castle': ('İblis Kalesi', "The Demon Castle", 'İblis Kalesi anahtarıyla kaleye gir ve İblis Kral Baran’ı yen.', 'Enter the Demon Castle with its key and defeat Demon King Baran.'),
    'jeju': ('Jeju Adası Baskını', 'Jeju Island Raid', 'S rütbeye ulaş, Başkan Go Gun-Hee ile konuş ve Karınca Kral Beru’yu yen.',
             'Reach S-rank, talk to Chairman Go Gun-Hee and defeat Ant King Beru.'),
    'monarch': ('Gölge Hükümdarı', 'The Shadow Monarch', '100. seviyeye ulaş ve Gölgelerin Hükümdarı ol.', 'Reach level 100 and become the Monarch of Shadows.'),
    'done': ('Efsane', 'Legend', 'Tüm ana görevleri tamamladın.', 'You completed every main quest.'),
}
for k, (tr, en, dtr, den) in QUESTS.items():
    s('quest.' + k, tr, en)
    s('quest.' + k + '.desc', dtr, den)

GUILDS = {'none': ('Loncasız', 'No Guild'), 'hunters': ('Avcılar Loncası', 'Hunters Guild'), 'white_tiger': ('Beyaz Kaplan Loncası', 'White Tiger Guild'),
          'fiend': ('Şeytan Loncası', 'Fiend Guild'), 'knights': ('Şövalyeler Loncası', 'Knights Guild'), 'ahjin': ('Ahjin Loncası', 'Ahjin Guild')}
for k, (tr, en) in GUILDS.items():
    s('guild.' + k, tr, en)
for i, (tr, en) in enumerate((('Çaylak', 'Rookie'), ('Üye', 'Member'), ('Kıdemli', 'Senior'), ('Elit', 'Elite'), ('Saldırı Lideri', 'Raid Leader'))):
    s('guild_rank.' + str(i), tr, en)

THEMES = {'goblin_cave': ('Goblin Mağarası', 'Goblin Cave'), 'lycan_forest': ('Kurt Adam Ormanı', 'Lycan Forest'), 'kasaka_lair': ('Kasaka’nın İni', "Kasaka's Lair"),
          'statue_hall': ('Heykeller Salonu', 'Hall of Statues'), 'knight_castle': ('Şövalye Kalesi', 'Knight Castle'), 'ice_forest': ('Buz Ormanı', 'Ice Forest'),
          'orc_fortress': ('Ork Kalesi', 'Orc Fortress'), 'demon_ruins': ('İblis Harabeleri', 'Demon Ruins'), 'ant_nest': ('Karınca Yuvası', 'Ant Nest')}
ELITES = {'goblin_cave': ('Hobgoblin Reisi', 'Hobgoblin Chieftain'), 'lycan_forest': ('Alfa Kurt Adam', 'Alpha Lycan'), 'kasaka_lair': ('Kasaka', 'Kasaka'),
          'statue_hall': ('Muhafız Heykel', 'Guardian Statue'), 'knight_castle': ('Şövalye Yüzbaşısı', 'Knight Captain'), 'ice_forest': ('Baruka', 'Baruka'),
          'orc_fortress': ('Kargalgan', 'Kargalgan'), 'demon_ruins': ('Cerberus', 'Cerberus'), 'ant_nest': ('Karınca Komutanı', 'Ant Commander')}
for k, (tr, en) in THEMES.items():
    s('theme.' + k, tr, en)
    s('elite.' + k, *ELITES[k])
PLACES = {'gangnam': 'Gangnam', 'hongdae': 'Hongdae', 'jongno': 'Jongno', 'itaewon': 'Itaewon', 'myeongdong': 'Myeongdong', 'yeouido': 'Yeouido', 'mapo': 'Mapo',
          'songpa': 'Songpa'}
for k, v in PLACES.items():
    s('place.' + k, v + ', Seul', v + ', Seoul')
s('place.wilderness', 'Vahşi doğa', 'the wilderness')
s('place.seoul', 'Seul', 'Seoul')

NPCS = {'receptionist': ('Dernek Resepsiyonisti', 'Association Receptionist'), 'woo_jinchul': ('Woo Jinchul', 'Woo Jinchul'), 'chairman': ('Başkan Go Gun-Hee', 'Chairman Go Gun-Hee'),
        'master_hunters': ('Choi Jong-In', 'Choi Jong-In'), 'master_white_tiger': ('Baek Yoonho', 'Baek Yoonho'), 'master_fiend': ('Şeytan Loncası Lideri', 'Fiend Guild Master'),
        'master_knights': ('Şövalyeler Loncası Lideri', 'Knights Guild Master'), 'yoo_jinho': ('Yoo Jinho', 'Yoo Jinho'), 'cha_haein': ('Cha Hae-In', 'Cha Hae-In'),
        'healer': ('Hastane Şifacısı', 'Hospital Healer'), 'merchant': ('Tüccar', 'Merchant'), 'blacksmith': ('Demirci', 'Blacksmith'), 'alchemist': ('Simyacı', 'Alchemist'),
        'guide': ('Kapı Meydanı Rehberi', 'Gate Plaza Guide'), 'reporter': ('Muhabir', 'Reporter'), 'jinah': ('Sung Jinah', 'Sung Jinah')}
for k, (tr, en) in NPCS.items():
    s('npc.' + k, tr, en)

REGIONS = {
    'seoul_plaza': ('Seul — Kapı Meydanı', 'Seoul — Gate Plaza', 'Şehrin merkezi. Bölge portalları burada.', 'Heart of the city. The region portals are here.'),
    'association': ('Avcı Derneği', 'Hunter Association', 'Lisans, rütbe değerlendirmesi ve büyü taşı satışı.', 'Licenses, rank assessment and magic stone sales.'),
    'guild_hunters': ('Avcılar Loncası', 'Hunters Guild', 'Kore’nin en büyük loncası.', "Korea's biggest guild."),
    'guild_white_tiger': ('Beyaz Kaplan Loncası', 'White Tiger Guild', 'Canavar tipi avcıların loncası.', 'The guild of beast-type hunters.'),
    'guild_fiend': ('Şeytan Loncası', 'Fiend Guild', 'Sert ve acımasız savaşçılar.', 'Tough and ruthless fighters.'),
    'guild_knights': ('Şövalyeler Loncası', 'Knights Guild', 'Disiplinli koruyucular.', 'Disciplined guardians.'),
    'guild_ahjin': ('Ahjin Loncası', 'Ahjin Guild', 'Kendi loncan.', 'Your own guild.'),
    'guild': ('Lonca Binası', 'Guild Hall', '', ''),
    'hospital': ('Hastane', 'Hospital', 'İyileşme ve tedavi.', 'Healing and treatment.'),
    'market': ('Pazar Sokağı', 'Market Street', 'İksirler, silahlar ve zırhlar.', 'Potions, weapons and armor.'),
    'home': ('Ev', 'Home', 'Kardeşin Jinah seni bekliyor.', 'Your sister Jinah is waiting.'),
    'double_dungeon': ('Çift Zindan', 'Double Dungeon', 'Her şeyin başladığı yer. Tanrı Heykeli bekliyor.', 'Where it all began. The Statue of God awaits.'),
    'job_change': ('Meslek Değişimi Salonu', 'Job Change Hall', 'Igris ve kızıl şövalyeler. (Seviye 40)', 'Igris and the crimson knights. (Level 40)'),
    'demon_castle': ('İblis Kalesi', 'Demon Castle', 'Cerberus kapıyı bekliyor, Baran tahtında. (Anahtar gerekir)', 'Cerberus guards the gate, Baran sits on the throne. (Key required)'),
    'jeju_island': ('Jeju Adası', 'Jeju Island', 'Karıncaların istila ettiği ada. (S rütbe)', 'The ant-infested island. (S-rank)'),
    'penalty_zone': ('Ceza Bölgesi', 'Penalty Zone', 'Günlük görevi tamamlamayanların cezası.', 'The punishment for skipping the daily quest.'),
    'overworld': ('Dış Dünya', 'Overworld', 'Doğduğun dünya.', 'The world you were born in.'),
}
for k, (tr, en, dtr, den) in REGIONS.items():
    s('region.' + k, tr, en)
    s('region.' + k + '.desc', dtr, den)

# ------------------------------------------------------------------ messages
M = [
    ('system.title', 'SİSTEM', 'SYSTEM'), ('system.warning', 'UYARI', 'WARNING'), ('system.notification', 'BİLDİRİM', 'NOTIFICATION'),
    ('welcome_back', 'Tekrar hoş geldin, Oyuncu. Seviye: %s', 'Welcome back, Player. Level: %s'),
    ('awaken.title', 'Uyanış', 'Awakening'),
    ('awaken.text', 'Bir "Oyuncu" olma niteliklerini kazandın. Kabul ediyor musun?', 'You have acquired the qualifications to be a Player. Will you accept?'),
    ('awaken.declined_text', 'Kabul etmemeyi seçersen kalbin 0.02 saniye içinde duracak. Kabul ediyor musun?',
     'If you choose not to accept, your heart will stop in 0.02 seconds. Will you accept?'),
    ('awaken.accept', 'Kabul et', 'Accept'), ('awaken.decline', 'Reddet', 'Decline'),
    ('awaken.accepted', 'Artık bir Oyuncusun. [K] ile Durum Penceresini aç. Sistem seni güçlendirecek.',
     'You are now a Player. Open the Status Window with [K]. The System will make you stronger.'),
    ('awaken.declined', 'Sistem cevabını bekliyor...', 'The System is waiting for your answer...'),
    ('level_up.title', 'SEVİYE ATLADIN!', 'LEVEL UP!'), ('level_up.body', 'Seviye %s! Dağıtılabilir stat puanı: %s', 'Level %s! Available stat points: %s'),
    ('skill.acquired', 'Yeni yetenek kazanıldı', 'New skill acquired'), ('skill.locked', 'Bu yeteneği henüz kullanamazsın.', 'You cannot use this skill yet.'),
    ('skill.no_mana', 'Yeterli mana yok.', 'Not enough mana.'), ('skill.cooldown', '%s bekleme süresinde: %s sn', '%s on cooldown: %ss'),
    ('skill.used', '» %s', '» %s'), ('skill.empty_slot', '%s. yetenek yuvası boş. Durum Penceresi > Yetenekler’den ata.', 'Skill slot %s is empty. Assign one in Status Window > Skills.'),
    ('skill.no_target', 'Hedef yok.', 'No target.'), ('skill.passive', 'Pasif', 'Passive'), ('skill.req', 'Gerekli seviye: %s', 'Requires level %s'),
    ('skill.stats', 'Mana: %s  •  Bekleme: %s sn', 'Mana: %s  •  Cooldown: %ss'), ('skill.job', 'Meslek gerekir: %s', 'Requires job: %s'),
    ('skill.click_assign', 'Tıkla, sonra aşağıdan bir yuvaya ata.', 'Click, then pick a slot below.'), ('skill.slots', 'Hızlı yuvalar:', 'Quick slots:'),
    ('dodge', 'Kaçındın!', 'Dodged!'),
    ('hint.arise', 'Öldürdüğün canavarların ruhlarını görebilirsin. Cesedin yanında [R] ile "ARISE!" diyerek gölge asker çıkar.',
     'You can see the souls of the monsters you kill. Press [R] near a corpse to call "ARISE!" and extract a shadow soldier.'),
    ('arise.title', 'GÖLGE ÇIKARMA', 'SHADOW EXTRACTION'), ('arise.shout', 'ARISE', 'ARISE'),
    ('arise.success', '%s gölge ordusuna katıldı!', '%s has joined your shadow army!'),
    ('arise.failed', 'Çıkarma başarısız. Kalan deneme: %s', 'Extraction failed. Attempts left: %s'),
    ('arise.failed_final', 'Ruh dağıldı. Çıkarma başarısız.', 'The soul has dispersed. Extraction failed.'),
    ('arise.locked', 'Gölge çıkarmak için meslek değiştirmelisin (Nekromansör).', 'You must change job (Necromancer) to extract shadows.'),
    ('arise.no_target', 'Yakında çıkarılabilecek bir ruh yok.', 'There is no soul to extract nearby.'),
    ('arise.full', 'Gölge kapasitesi dolu (%s).', 'Shadow capacity is full (%s).'),
    ('shadow_of', 'Gölge %s', 'Shadow %s'),
    ('shadows.none', 'Hiç gölge askerin yok.', 'You have no shadow soldiers.'), ('shadows.none_active', 'Çağrılmış gölge yok.', 'No shadows are summoned.'),
    ('shadows.recalled', 'Gölgeler gölgene geri döndü.', 'Your shadows returned to your shadow.'), ('shadows.summoned', '%s gölge çağrıldı.', '%s shadows summoned.'),
    ('shadows.level_up', '%s seviye atladı: %s', '%s leveled up: %s'), ('shadows.header', 'Gölge Ordusu: %s / %s', 'Shadow Army: %s / %s'),
    ('shadows.summon_all', 'Hepsini çağır', 'Summon all'), ('shadows.recall_all', 'Hepsini geri al', 'Recall all'), ('shadows.toggle', 'Çağır/Al', 'Toggle'),
    ('shadows.locked', 'Gölge ordusu, Meslek Değişimi görevini (seviye 40, Igris) tamamladıktan sonra açılır.',
     'The shadow army unlocks after the Job Change quest (level 40, Igris).'),
    ('shadows.release_hint', 'Shift + x: gölgeyi serbest bırak', 'Shift + x: release shadow'),
    ('daily.title', 'Günlük Görev: Güçlü Olmaya Hazırlık', 'Daily Quest: Preparation to Become Powerful'),
    ('daily.task0', 'Şınav (eğil)', 'Push-ups (crouch)'), ('daily.task1', 'Mekik (zıpla)', 'Sit-ups (jump)'), ('daily.task2', 'Squat (vur)', 'Squats (hit)'),
    ('daily.task3', 'Koşu (blok)', 'Running (blocks)'),
    ('daily.new', 'Yeni günlük görev geldi. Tamamlamazsan ceza alacaksın!', 'A new daily quest has arrived. Fail it and you will be punished!'),
    ('daily.part_done', '%s tamamlandı!', '%s complete!'), ('daily.complete', 'Günlük görev tamamlandı! Ödülünü Durum Penceresi > Görevler’den al.',
                                                           'Daily quest complete! Claim your reward in Status Window > Quests.'),
    ('daily.penalty', 'Günlük görevi tamamlamadın. Ceza görevi başlıyor!', 'You did not complete the daily quest. The penalty quest begins!'),
    ('daily.reward_title', 'Günlük Görev Ödülü', 'Daily Quest Reward'), ('daily.reward_body', 'Tam iyileşme, +3 stat puanı, %s altın ve Rastgele Kutu.',
                                                                         'Full recovery, +3 stat points, %s gold and a Random Box.'),
    ('daily.claim', 'Ödülü al', 'Claim reward'), ('daily.claimed', 'Ödül alındı', 'Reward claimed'),
    ('daily.warning', 'Uyarı: Görevi gün bitmeden tamamlamazsan Ceza Bölgesi’ne gönderilirsin.', 'Warning: fail before the day ends and you will be sent to the Penalty Zone.'),
    ('penalty.title', 'CEZA GÖREVİ', 'PENALTY QUEST'), ('penalty.body', 'Hayatta kal: 4 dakika boyunca dev kırkayaklardan kaç!', 'Survive: escape the giant centipedes for 4 minutes!'),
    ('penalty.survived', 'Ceza görevini atlattın.', 'You survived the penalty quest.'),
    ('quest.title', 'GÖREV', 'QUEST'), ('quest.complete', 'Görev tamamlandı', 'Quest complete'), ('quest.new', 'Yeni görev', 'New quest'),
    ('quest.reward', '(+%s altın, +3 stat)', '(+%s gold, +3 stats)'), ('quest.main', 'Ana Görev', 'Main Quest'),
    ('job_change.title', 'MESLEK DEĞİŞİMİ', 'JOB CHANGE'),
    ('job_change.available', 'Meslek Değişimi görevi açıldı! Haritadan Meslek Değişimi Salonu’na git.', 'The Job Change quest is available! Travel to the Job Change Hall from the map.'),
    ('job_change.done', 'Meslek değişimi tamamlandı: Nekromansör. "Gölge Çıkarma" yeteneği açıldı!', 'Job change complete: Necromancer. "Shadow Extraction" unlocked!'),
    ('job_change.arise_igris', 'Igris’in ruhu hâlâ orada... [R] tuşuna bas ve ARISE de!', "Igris' soul is still there... press [R] and say ARISE!"),
    ('title.acquired', 'Unvan kazanıldı', 'Title acquired'), ('title.locked', 'Henüz kazanılmadı', 'Not acquired yet'), ('title.equip', 'Kuşan', 'Equip'),
    ('association.title', 'AVCI DERNEĞİ', 'HUNTER ASSOCIATION'), ('association.assessed', 'Yeniden değerlendirme sonucu: %s', 'Re-assessment result: %s'),
    ('random_box.title', 'Rastgele Kutu', 'Random Box'), ('random_box.body', '%s ve %s altın kazandın!', 'You got %s and %s gold!'),
    ('rune.known', 'Bu yeteneği zaten biliyorsun.', 'You already know this skill.'),
    ('gate.entered_title', '[%s] Rütbe Kapı', '[%s]-Rank Gate'), ('gate.entered', 'Zindana girdin: %s. Boss’u yen!', 'You entered the dungeon: %s. Defeat the boss!'),
    ('gate.entered_red', 'Kırmızı Kapı! Boss ölene kadar çıkış yok: %s', 'Red Gate! No way out until the boss dies: %s'),
    ('gate.too_strong', 'Bu kapı senin için çok tehlikeli olabilir!', 'This gate may be far too dangerous for you!'),
    ('gate.cleared_title', 'ZİNDAN TEMİZLENDİ', 'DUNGEON CLEARED'), ('gate.cleared_body', '[%s] kapı temizlendi! +%s altın. Çıkış portalı açıldı.',
                                                                     '[%s] gate cleared! +%s gold. The exit portal has opened.'),
    ('gate.closing', 'Kapı kapanıyor...', 'The gate is closing...'), ('gate.gone', 'Bu kapı artık yok.', 'This gate no longer exists.'),
    ('gate.cleared', 'Bu kapı zaten temizlendi.', 'This gate is already cleared.'), ('gate.not_awakened', 'Sadece uyanmış avcılar kapılara girebilir.', 'Only awakened hunters can enter gates.'),
    ('instant.inside', 'Zaten bir zindanın içindesin.', 'You are already inside a dungeon.'),
    ('region.locked_level40', 'Seviye 40 gerekli.', 'Requires level 40.'), ('region.job_done', 'Meslek değişimini zaten tamamladın.', 'You already changed your job.'),
    ('region.locked_key', 'İblis Kalesi Anahtarı gerekli.', 'Requires the Demon Castle Key.'), ('region.locked_s', 'S rütbe ve Başkan’ın izni gerekli.', "Requires S-rank and the Chairman's permission."),
    ('region.locked_penalty', 'Buraya sadece Sistem gönderebilir.', 'Only the System can send you here.'), ('region.in_penalty', 'Ceza süresince ayrılamazsın!', 'You cannot leave during the penalty!'),
    ('region.locked_job', 'Bu anahtarı kullanmak için meslek değiştirmelisin.', 'You must change job to use this key.'), ('region.missing', 'Bölge yüklenemedi.', 'Region failed to load.'),
    ('region.cerberus_down', 'Kapı bekçisi Cerberus yenildi! Kalenin kapıları açık.', 'Gatekeeper Cerberus is defeated! The castle gates are open.'),
    ('region.double_cleared', 'Tanrı Heykeli susturuldu. Çift Zindan’dan sağ çıktın.', 'The Statue of God is silenced. You survived the Double Dungeon.'),
    ('region.baran_down', 'İblis Kral Baran yenildi!', 'Demon King Baran is defeated!'), ('region.jeju_cleared', 'Karınca Kral yenildi. Jeju Adası kurtarıldı!', 'The Ant King is defeated. Jeju Island is saved!'),
    ('region.commandment1.a', 'Birinci emir:', 'First commandment:'), ('region.commandment1.b', 'Tanrıya ibadet et.', 'Worship God.'),
    ('region.commandment2.a', 'İkinci emir:', 'Second commandment:'), ('region.commandment2.b', 'Tanrıyı öv.', 'Praise God.'),
    ('region.commandment3.a', 'Üçüncü emir:', 'Third commandment:'), ('region.commandment3.b', 'İmanını kanıtla.', 'Prove your faith.'),
    ('map.title', 'DÜNYA HARİTASI', 'WORLD MAP'), ('map.gate', 'Kapı', 'Gate'), ('map.click_travel', 'Işınlanmak için tıkla', 'Click to travel'),
    ('map.hint', 'Bölgeler, portallar ve açık kapılar. Bir yere ışınlanmak için tıkla (20 sn bekleme).', 'Regions, portals and open gates. Click to travel (20s cooldown).'),
    ('map.cooldown', 'Harita ışınlanması %s sn bekleme süresinde.', 'Map travel is on cooldown for %ss.'), ('map.discovered', 'Yeni bölge keşfedildi', 'New region discovered'),
    ('news.header', 'AVCI HABERLERİ', 'HUNTER NEWS'), ('news.breaking', 'SON DAKİKA', 'BREAKING NEWS'), ('news.day', 'Gün %s', 'Day %s'), ('news.empty', 'Henüz haber yok.', 'No news yet.'),
    ('news.gates', 'Açık Kapılar (%s)', 'Open Gates (%s)'), ('news.a_hunter', 'bir avcı', 'a hunter'),
    ('news.gate', '[%s] rütbe bir kapı açıldı: %s. Avcı Derneği bölgeyi kordon altına aldı.', 'A [%s]-rank gate has appeared in %s. The Hunter Association has cordoned off the area.'),
    ('news.red_gate', 'DİKKAT: %s bölgesinde [%s] rütbe KIRMIZI KAPI tespit edildi! Girenler boss ölene kadar çıkamaz.',
     'WARNING: a [%s]-rank RED GATE was detected in %s! Those who enter cannot leave until the boss dies.'),
    ('news.cleared', '[%s] rütbe kapı %s tarafından temizlendi.', 'A [%s]-rank gate was cleared by %s.'),
    ('news.break', 'ZİNDAN TAŞMASI! [%s] rütbe kapıdan canavarlar %s bölgesine yayıldı!', 'DUNGEON BREAK! Monsters poured out of a [%s]-rank gate in %s!'),
    ('news.milestone', 'Yükselen yıldız: avcı %s %s. seviyeye ulaştı!', 'Rising star: hunter %s has reached level %s!'),
    ('news.rank_up', 'Avcı %s yeniden değerlendirmede %s oldu!', 'Hunter %s was re-assessed as %s!'),
    ('news.new_s_rank', 'Kore’nin yeni S rütbe avcısı: %s! (%s)', "Korea's newest S-rank hunter: %s! (%s)"),
    ('news.guild_join', '%s, %s’na katıldı.', '%s has joined the %s.'), ('news.ahjin', 'Yeni bir lonca doğdu: %s tarafından kurulan %s!', 'A new guild is born: %s founded the %s!'),
    ('news.monarch', 'Dünya sarsıldı: %s artık Gölgelerin Hükümdarı!', 'The world trembles: %s is now the Monarch of Shadows!'),
    ('news.jeju_island_cleared', 'MUCİZE! Jeju Adası karıncalardan kurtarıldı!', 'MIRACLE! Jeju Island has been freed from the ants!'),
    ('news.demon_castle_cleared', 'İblis Kalesi’nin hükümdarı Baran yenildi!', 'Baran, ruler of the Demon Castle, has been defeated!'),
    ('hud.quest', 'GÖREV', 'QUEST'), ('hud.daily', 'Günlük Görev', 'Daily Quest'), ('hud.daily_claim', 'Günlük ödül hazır!', 'Daily reward ready!'),
    ('hud.daily_done', '✔ Günlük görev bitti', '✔ Daily quest done'), ('hud.penalty', 'CEZA: %s sn hayatta kal!', 'PENALTY: survive %ss!'),
    ('status.title', 'Durum Penceresi', 'Status Window'), ('status.window', 'DURUM', 'STATUS'), ('status.name', 'İsim', 'Name'), ('status.level', 'Seviye', 'Level'),
    ('status.job', 'Meslek', 'Job'), ('status.title_label', 'Unvan', 'Title'), ('status.rank', 'Rütbe', 'Rank'), ('status.guild', 'Lonca', 'Guild'),
    ('status.fatigue', 'Yorgunluk', 'Fatigue'), ('status.none', 'Yok', 'None'), ('status.points', 'Kalan puan: %s', 'Remaining points: %s'),
    ('status.shift_hint', 'Shift+tık: +5', 'Shift+click: +5'), ('status.gates_cleared', 'Temizlenen kapı: %s', 'Gates cleared: %s'),
    ('status.shadows_count', 'Gölgeler: %s/%s', 'Shadows: %s/%s'), ('status.shop', 'MAĞAZA', 'SHOP'), ('status.map', 'HARİTA', 'MAP'), ('status.news', 'HABERLER', 'NEWS'),
    ('status.gold', 'Altın: %s', 'Gold: %s'),
    ('status.tab.status', 'Durum', 'Status'), ('status.tab.skills', 'Yetenekler', 'Skills'), ('status.tab.shadows', 'Gölgeler', 'Shadows'),
    ('status.tab.quests', 'Görevler', 'Quests'), ('status.tab.titles', 'Unvanlar', 'Titles'),
    ('shop.title.0', 'SİSTEM MAĞAZASI', 'SYSTEM SHOP'), ('shop.title.1', 'Tüccar', 'Merchant'), ('shop.title.2', 'Demirci', 'Blacksmith'), ('shop.title.3', 'Simyacı', 'Alchemist'),
    ('shop.title.4', 'Lonca Deposu', 'Guild Store'), ('shop.buy', 'Satın al', 'Buy'), ('shop.need_rank', '%s rütbe', 'Rank %s'),
    ('shop.hint', 'Shift+tık: 5 adet', 'Shift+click: buy 5'), ('shop.no_gold', 'Yeterli altın yok.', 'Not enough gold.'),
    ('shop.rank_locked', 'Bu eşya için %s rütbe gerekli.', 'This item requires rank %s.'), ('shop.bought', '%s satın alındı (-%s altın)', 'Bought %s (-%s gold)'),
    ('shop.sold', 'Büyü taşları ve malzemeler satıldı: +%s altın', 'Magic stones and materials sold: +%s gold'),
    ('guild.title', 'LONCA', 'GUILD'), ('guild.already', 'Zaten bir loncadasın.', 'You are already in a guild.'),
    ('guild.need_license', 'Önce Avcı Derneği’nden lisans almalısın.', 'You need a license from the Hunter Association first.'),
    ('guild.need_rank', 'Katılmak için en az %s gerekli.', 'You need at least %s to join.'), ('guild.ahjin_gold', 'Lonca kurmak 1000 altın tutar.', 'Founding a guild costs 1000 gold.'),
    ('guild.joined', '%s’na katıldın!', 'You joined the %s!'), ('guild.welcome', '%s’na hoş geldin. Sana görevler vereceğim.', 'Welcome to the %s. I will give you requests.'),
    ('guild.ahjin_founded', '%s kuruldu! Ben, Yoo Jinho, başkan yardımcın olarak her zaman yanındayım!', 'The %s is founded! I, Yoo Jinho, will always be your vice-master!'),
    ('guild.left', '%s’ndan ayrıldın.', 'You left the %s.'), ('guild.task_current', 'Görev: %s (%s/%s)', 'Request: %s (%s/%s)'),
    ('guild.task_kill', '%s avla', 'Hunt %s'), ('guild.task_gate', '[%s]+ rütbe kapı temizle', 'Clear a [%s]+ rank gate'),
    ('guild.task_ready', 'Lonca görevi tamamlandı! Lonca liderine bildir.', 'Guild request complete! Report to the guild master.'),
    ('guild.task_done', 'Lonca görevi teslim edildi: +%s itibar, +%s altın', 'Guild request turned in: +%s reputation, +%s gold'),
    ('guild.task_thanks', 'Harika iş çıkardın. Loncamız seninle gurur duyuyor.', 'Great job. The guild is proud of you.'),
    ('guild.rep', 'İtibar: %s (%s)', 'Reputation: %s (%s)'),
    ('tooltip.rank', 'Rütbe: %s', 'Rank: %s'), ('tooltip.rune', 'Öğretir: %s', 'Teaches: %s'), ('tooltip.value', 'Değer: %s altın', 'Value: %s gold'),
    ('tooltip.active.thunder', 'Sağ tık: Yıldırım düşür', 'Right click: call down lightning'), ('tooltip.active.dragon_wave', 'Sağ tık: Ejderha alevi dalgası', 'Right click: dragon flame wave'),
    ('tooltip.active.dash_slash', 'Sağ tık: Atılarak kes', 'Right click: dash slash'), ('tooltip.active.fireball', 'Sağ tık: Ateş topu', 'Right click: fireball'),
    ('tooltip.active.mana_bolt', 'Sağ tık: Mana oku', 'Right click: mana bolt'), ('tooltip.active.ground_slam', 'Sağ tık: Yere vuruş', 'Right click: ground slam'),
    ('tooltip.active.shadow_slash', 'Sağ tık: Gölge kesiği', 'Right click: shadow slash'),
    ('license.line', '[Avcı Lisansı] %s • %s • Seviye %s • %s', '[Hunter License] %s • %s • Level %s • %s'),
    ('option.bye', 'Hoşça kal', 'Goodbye'), ('option.register', 'Avcı kaydı / rütbe testi', 'Register / rank test'), ('option.reassess', 'Yeniden değerlendirme', 'Re-assessment'),
    ('option.sell_stones', 'Büyü taşlarını sat', 'Sell magic stones'), ('option.gates', 'Açık kapılar?', 'Open gates?'), ('option.jeju', 'Jeju baskını', 'The Jeju raid'),
    ('option.go_jeju', 'Jeju’ya git', 'Go to Jeju'), ('option.tips', 'Tavsiye ver', 'Any advice?'), ('option.heal', 'Tedavi (%s altın)', 'Treatment (%s gold)'),
    ('option.portals', 'Portallar', 'Portals'), ('option.system_help', 'Sistem nedir?', 'What is the System?'), ('option.map', 'Haritayı aç', 'Open the map'),
    ('option.rest', 'Dinlen', 'Rest'), ('option.guild_task', 'Lonca görevi', 'Guild request'), ('option.guild_shop', 'Lonca deposu', 'Guild store'),
    ('option.leave_guild', 'Loncadan ayrıl', 'Leave guild'), ('option.join_guild', '%s’na katıl', 'Join the %s'), ('option.found_ahjin', '%s’nu kur (1000 altın)', 'Found the %s (1000 gold)'),
    ('dialog.no_gold', 'Yeterli altının yok.', "You don't have enough gold."),
    ('dialog.receptionist.main', 'Avcı Derneği’ne hoş geldiniz, %s. Henüz kayıtlı değilsiniz. Uyanmış biriyseniz rütbe testine girebilirsiniz.',
     'Welcome to the Hunter Association, %s. You are not registered yet. If you have awakened, you can take the rank test.'),
    ('dialog.receptionist.main_licensed', 'Tekrar hoş geldiniz, avcı %s. Kayıtlı rütbeniz: %s. Size nasıl yardımcı olabilirim?',
     'Welcome back, hunter %s. Your registered rank: %s. How can I help you?'),
    ('dialog.receptionist.not_awakened', 'Üzgünüm, ölçüm cihazı sizde mana algılamıyor. Önce uyanmalısınız.', "I'm sorry, the device detects no mana in you. You have to awaken first."),
    ('dialog.receptionist.registered', 'Test tamamlandı! Ölçülen rütbeniz: %s. İşte Avcı Lisansınız. Bol şans!', 'The test is complete! Your measured rank: %s. Here is your Hunter License. Good luck!'),
    ('dialog.receptionist.reassessed', 'Yeniden değerlendirme sonucu: %s. Gerçekten... büyüleyici.', 'Re-assessment result: %s. Truly... fascinating.'),
    ('dialog.receptionist.sold', 'Büyü taşlarınız için %s altın ödendi.', 'You were paid %s gold for your magic stones.'),
    ('dialog.receptionist.no_stones', 'Üzerinizde satılacak büyü taşı ya da malzeme yok.', 'You carry no magic stones or materials to sell.'),
    ('dialog.receptionist.gates', 'Şu anda %s açık kapı var. Konumlarını haritada ve haber panolarında görebilirsiniz.', 'There are %s open gates right now. See them on the map and news boards.'),
    ('dialog.chairman.main', '%s... Kapılar her gün daha fazla açılıyor. Güçlen, genç avcı. Kore’nin sana ihtiyacı olacak.',
     '%s... More gates open every day. Grow stronger, young hunter. Korea will need you.'),
    ('dialog.chairman.main_b', 'Yükselişini takip ediyorum, %s. Bu hız... daha önce hiç görmedim.', "I've been watching your rise, %s. That speed... I have never seen it before."),
    ('dialog.chairman.main_s', '%s, Jeju Adası’ndaki karıncalar için ulusal baskın planlıyoruz. Bize katılır mısın?',
     '%s, we are planning a national raid on the ants of Jeju Island. Will you join us?'),
    ('dialog.chairman.jeju_ok', 'Teşekkürler. Jeju Adası artık haritanda. Kraliçenin muhafızı... ondan sakın.', 'Thank you. Jeju Island is now on your map. The queen’s guard... beware of it.'),
    ('dialog.woo_jinchul.main', 'Avcı Derneği Gözetim Bölümü. Kapılarla ilgili bir sorun olursa bana gel.', 'Hunter Association, Surveillance Team. Come to me with any gate trouble.'),
    ('dialog.woo_jinchul.tips0', 'Kırmızı kapılardan uzak dur. İçeri giren boss ölmeden çıkamaz.', 'Stay away from red gates. Nobody leaves before the boss dies.'),
    ('dialog.woo_jinchul.tips1', 'Temizlenmeyen kapılar zamanla taşar. Bir zindan taşması felakettir.', 'Uncleared gates eventually break. A dungeon break is a disaster.'),
    ('dialog.woo_jinchul.tips2', 'Büyü taşlarını Derneğe sat. Fiyatlar rütbeye göre artar.', 'Sell magic stones to the Association. Prices rise with the rank.'),
    ('dialog.woo_jinchul.tips3', 'Loncalar sana görev ve indirim sağlar. Bir loncaya katılmayı düşün.', 'Guilds give you requests and discounts. Consider joining one.'),
    ('dialog.cha_haein.line0', '...Sende bir koku yok. Diğer avcıların aksine, rahatsız edici değil.', '...You have no smell. Unlike other hunters, it does not bother me.'),
    ('dialog.cha_haein.line1', 'Avcılar Loncası’nın alt başkanıyım. Bir baskında birlikte savaşabiliriz.', "I'm the vice-master of the Hunters Guild. We could fight together in a raid."),
    ('dialog.cha_haein.line2', 'Kılıç dansı mı? Pratik yapmaya her zaman hazırım.', 'Sword dance? I am always ready to practice.'),
    ('dialog.cha_haein.line3', 'Jeju... Orada çok arkadaşımızı kaybettik. Bir daha olmayacak.', 'Jeju... We lost many friends there. Never again.'),
    ('dialog.cha_haein.monarch', 'Hükümdar... Gölgelerin seni takip ediyor. Ama sen hâlâ sensin.', 'Monarch... Your shadows follow you. But you are still you.'),
    ('dialog.guide.main', 'Kapı Meydanı’na hoş geldin! Kemerlerdeki portallar seni bölgelere götürür. Doğu duvarında son haberler var.',
     'Welcome to the Gate Plaza! The portals in the arches lead to the regions. The latest news is on the east wall.'),
    ('dialog.guide.portals', 'Kuzey: Çift Zindan, Meslek Değişimi Salonu, İblis Kalesi. Güney: Jeju Adası, Dış Dünya, Ev. Bazıları özel şart ister.',
     'North: Double Dungeon, Job Change Hall, Demon Castle. South: Jeju Island, the Overworld, Home. Some need special conditions.'),
    ('dialog.guide.system', 'K: Durum Penceresi, M: Harita, N: Haberler, Z-X-C-V-B: yetenekler, R: Arise, G: gölgeleri çağır. Günlük görevini unutma!',
     'K: Status Window, M: Map, N: News, Z-X-C-V-B: skills, R: Arise, G: summon shadows. Don\'t forget your daily quest!'),
    ('dialog.healer.main', 'Yaralısın. Tam tedavi %s altın tutar.', 'You are hurt. A full treatment costs %s gold.'),
    ('dialog.healer.healed', 'İşte, yeni gibisin. Kendine dikkat et.', 'There, good as new. Take care of yourself.'),
    ('dialog.jinah.line0', 'Abi! %s! Yine mi zindana gidiyorsun? Dikkatli ol, tamam mı?', 'Brother! %s! Going to a dungeon again? Be careful, okay?'),
    ('dialog.jinah.line1', 'Annemi hastanede ziyaret ettim. Durumu iyiye gidiyor!', 'I visited mom at the hospital. She is getting better!'),
    ('dialog.jinah.line2', 'Sınavlarım yaklaşıyor... Sen de çok çalışıyorsun ama, değil mi?', 'My exams are coming... You are working hard too, right?'),
    ('dialog.jinah.line3', 'Yemek hazır! Biraz dinlenmelisin.', "Dinner's ready! You should rest a bit."),
    ('dialog.jinah.rested', 'Biraz dinlendin. Kendini çok daha iyi hissediyorsun.', 'You rested a while. You feel much better.'),
    ('dialog.guild.shop', 'Lonca deposu açıldı.', 'The guild store is open.'),
]
for k, tr, en in M:
    s(k, tr, en)

GUILD_LINES = {
    'hunters': ('Ben Choi Jong-In, Avcılar Loncası’nın lideri. Gücünü gördüm, %s. %s seni bekliyor. (Rütbe %s)',
                'I am Choi Jong-In, master of the Hunters Guild. I have seen your power, %s. The %s awaits you. (Rank %s)'),
    'white_tiger': ('Baek Yoonho. Beyaz Kaplan Loncası. Gözlerin vahşi, %s. %s içinde yerin olabilir. (Rütbe %s)',
                    'Baek Yoonho. White Tiger Guild. You have wild eyes, %s. There could be a place for you in the %s. (Rank %s)'),
    'fiend': ('Şeytan Loncası güçlüleri sever, %s. %s’na girmek istiyorsan kanıtla. (Rütbe %s)', 'The Fiend Guild loves the strong, %s. Want into the %s? Prove it. (Rank %s)'),
    'knights': ('Şövalyeler Loncası halkı korur. %s, %s’nda onur seni bekliyor. (Rütbe %s)', 'The Knights Guild protects the people. %s, honour awaits you in the %s. (Rank %s)'),
    'ahjin': ('Abi! Ben Yoo Jinho! Bir lonca kurmalıyız: %s! (%s, rütbe %s)', 'Hyung-nim! It\'s me, Yoo Jinho! We should found a guild: the %s! (%s, rank %s)'),
}
for g, (tr, en) in GUILD_LINES.items():
    s(f'dialog.guild.{g}.main', tr, en)
    s(f'dialog.guild.{g}.member', 'Hoş geldin, %s. %s içinde rütben: %s. Bugün ne yapalım?', 'Welcome, %s. Your standing in the %s: %s. What shall we do today?')

HUNTER = [('Dün bir D rütbe kapıyı temizledik. Büyü taşları iyi para etti!', 'We cleared a D-rank gate yesterday. The magic stones sold well!'),
          ('S rütbe avcılar... Onlar insan değil, felaket.', 'S-rank hunters... They are not people, they are disasters.'),
          ('Kırmızı kapı haberlerini duydun mu? Tüylerim diken diken.', 'Did you hear about the red gate? It gives me chills.'),
          ('Bir gün Avcılar Loncası’na katılacağım!', 'One day I will join the Hunters Guild!'),
          ('İksirsiz zindana girme. Bu bir tavsiye değil, kural.', 'Never enter a dungeon without potions. That is not advice, it is a rule.'),
          ('E rütbe bir avcının S rütbe olduğunu söylüyorlar... saçmalık değil mi?', 'They say an E-rank hunter became S-rank... nonsense, right?'),
          ('Kapı bekçisi Cerberus... Onu gören kimse anlatmak için dönmedi.', 'Gatekeeper Cerberus... No one who saw it came back to tell.'),
          ('Gölge askerleri olan bir avcı varmış. Gece sokakları onların.', 'They say there is a hunter with shadow soldiers. The night streets are theirs.')]
for i, (tr, en) in enumerate(HUNTER):
    s(f'dialog.hunter.line{i}', tr, en)
CITIZEN = [('Kapılar yüzünden metro yine kapandı...', 'The subway closed again because of the gates...'), ('Avcılar olmasa ne yapardık?', 'What would we do without hunters?'),
           ('Haber panosunu gördün mü? Yeni bir kapı açılmış!', 'Did you see the news board? A new gate opened!'), ('Gangnam’da harika bir kafe var, denemelisin.', "There's a great cafe in Gangnam, you should try it."),
           ('Oğlum avcı olmak istiyor. Korkuyorum.', 'My son wants to be a hunter. I am scared.'), ('Dernek binası çok etkileyici, değil mi?', 'The Association building is impressive, isn\'t it?'),
           ('Zindan taşmasında evimiz yıkılmıştı...', 'Our house was destroyed in a dungeon break...'), ('Cha Hae-In’i gördüm! Çok havalıydı!', 'I saw Cha Hae-In! She was so cool!'),
           ('Pazar sokağında iksirler indirimde.', 'Potions are on sale on Market Street.'), ('Bu şehirde gece bile ışıklar sönmez.', 'Even at night the lights never go out in this city.')]
for i, (tr, en) in enumerate(CITIZEN):
    s(f'dialog.citizen.line{i}', tr, en)
FLAVOR = [('Avcı Derneği: bu ay açılan kapı sayısı rekor kırdı.', 'Hunter Association: record number of gates this month.'),
          ('Amerikan S rütbe avcı Thomas Andre Kore’yi ziyaret ediyor.', 'American S-rank hunter Thomas Andre is visiting Korea.'),
          ('Büyü taşı fiyatları yükselişte; enerji şirketleri sevinçli.', 'Magic stone prices are rising; energy companies rejoice.'),
          ('Avcılar Loncası yeni bir baskın ekibi kurduğunu duyurdu.', 'The Hunters Guild announced a new raid team.'),
          ('Beyaz Kaplan Loncası çaylak avcılar için eğitim programı başlattı.', 'White Tiger Guild launches a training program for rookies.'),
          ('Uzmanlar: kapılar arasındaki sinyal artıyor.', 'Experts: the signal between gates is getting stronger.'),
          ('Bir E rütbe avcının yeniden uyandığı iddiası ortalığı karıştırdı.', 'Claims of an E-rank hunter re-awakening cause a stir.'),
          ('Jeju Adası hâlâ kapalı; karıncalar her geçen gün çoğalıyor.', 'Jeju Island remains closed; the ants multiply every day.'),
          ('Hastane: mana tükenmesi vakaları arttı, avcılar dinlenmeli.', 'Hospital: cases of mana exhaustion rise, hunters should rest.'),
          ('Gangnam’da trafik, kapı uyarısı nedeniyle durdu.', 'Traffic in Gangnam halted due to a gate alert.'),
          ('Şövalyeler Loncası: şehir devriyeleri iki katına çıktı.', 'Knights Guild: city patrols doubled.'),
          ('Şeytan Loncası bir A rütbe kapıyı rekor sürede temizledi.', 'Fiend Guild cleared an A-rank gate in record time.'),
          ('Avcı Derneği Başkanı Go Gun-Hee’nin sağlığı hakkında söylentiler.', 'Rumours about the health of Chairman Go Gun-Hee.'),
          ('Hükümet kapı sigortası için yeni yasa tasarısı hazırlıyor.', 'Government drafts a new gate insurance bill.'),
          ('Uluslararası Avcı Bürosu toplantısı yakında yapılacak.', 'The International Hunter Bureau will meet soon.'),
          ('Gece yarısı bir sokakta gölgelerin hareket ettiği görüldü.', 'Shadows were seen moving in a street at midnight.'),
          ('Yeni bir instant zindan anahtarı türü karaborsada satılıyor.', 'A new kind of instant dungeon key is sold on the black market.'),
          ('Hongdae’de avcı hayranları festival düzenledi.', 'Hunter fans held a festival in Hongdae.'),
          ('Han Nehri kıyısında mana kristali bulundu.', 'Mana crystals were found on the banks of the Han River.'),
          ('Reklam: Sistem onaylı iksirler Pazar Sokağı’nda!', 'Ad: System-approved potions on Market Street!'),
          ('Yorumcular: "S rütbeler bile kırmızı kapılardan korkar."', 'Commentators: "Even S-ranks fear red gates."'),
          ('Bir lonca savaşı yaklaşıyor mu? Söylentiler artıyor.', 'Is a guild war coming? Rumours grow.'),
          ('Hava durumu: Seul’de açık ve güneşli, kapı riski orta.', 'Weather: clear and sunny in Seoul, gate risk moderate.'),
          ('Kayıp avcı ekibi bir B rütbe kapıdan sağ çıktı!', 'A missing hunter team came out of a B-rank gate alive!')]
for i, (tr, en) in enumerate(FLAVOR):
    s(f'news.flavor.{i}', tr, en)

SUBS = {'system': 'Sistem bildirimi', 'system_warn': 'Sistem uyarısı', 'level_up': 'Seviye atlama', 'news': 'Son dakika haberi', 'arise': 'ARISE!', 'dash': 'Atılma',
        'slash': 'Kesik', 'bloodlust': 'Kana susamışlık', 'authority': 'Otorite', 'roar': 'Kükreme', 'domain': 'Hükümdarın alanı', 'howl': 'Uluma', 'laser': 'Işın',
        'fire': 'Alev', 'thunder': 'Gök gürültüsü', 'slam': 'Darbe', 'boss_death': 'Boss öldü', 'gate_hum': 'Kapı uğultusu', 'gate_enter': 'Kapıdan geçiş',
        'small_voice': 'Goblin cıyaklaması', 'beast_growl': 'Hırlama', 'insect_click': 'Böcek tıkırtısı', 'stone_grind': 'Taş gıcırtısı', 'armor_clank': 'Zırh şıngırtısı',
        'demon_growl': 'İblis hırlaması', 'serpent_hiss': 'Tıslama', 'hurt_flesh': 'Yaralanma', 'hurt_hard': 'Sert darbe'}
SUBS_EN = {'system': 'System notification', 'system_warn': 'System warning', 'level_up': 'Level up', 'news': 'Breaking news', 'arise': 'ARISE!', 'dash': 'Dash',
           'slash': 'Slash', 'bloodlust': 'Bloodlust', 'authority': 'Authority', 'roar': 'Roar', 'domain': "Monarch's domain", 'howl': 'Howl', 'laser': 'Beam',
           'fire': 'Flames', 'thunder': 'Thunder', 'slam': 'Slam', 'boss_death': 'Boss died', 'gate_hum': 'Gate hums', 'gate_enter': 'Gate travel',
           'small_voice': 'Goblin chatter', 'beast_growl': 'Growl', 'insect_click': 'Insect clicks', 'stone_grind': 'Stone grinds', 'armor_clank': 'Armor clanks',
           'demon_growl': 'Demon growls', 'serpent_hiss': 'Hiss', 'hurt_flesh': 'Hurt', 'hurt_hard': 'Hard hit'}
for k, v in SUBS.items():
    add('subtitles.sololeveling.' + k, v, SUBS_EN[k])


def run(res_root):
    d = os.path.join(res_root, 'lang')
    os.makedirs(d, exist_ok=True)
    tr = {k: v[0] for k, v in L.items()}
    en = {k: v[1] for k, v in L.items()}
    with open(os.path.join(d, 'tr_tr.json'), 'w', encoding='utf-8') as f:
        json.dump(tr, f, ensure_ascii=False, indent=1)
    with open(os.path.join(d, 'en_us.json'), 'w', encoding='utf-8') as f:
        json.dump(en, f, ensure_ascii=False, indent=1)
    return L
