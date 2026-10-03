"""Turkish + English texts: names, lore, story chapters, envoy dialogue."""
M = 'krolasyonbosses'

L = {}  # key -> (tr, en)


def t(key, tr, en):
    L[key] = (tr, en)


def blk(i, tr, en, desc=None):
    t(f'block.{M}.{i}', tr, en)
    if desc:
        t(f'block.{M}.{i}.desc', *desc)


def itm(i, tr, en, *descs):
    t(f'item.{M}.{i}', tr, en)
    for k, d in enumerate(descs):
        t(f'item.{M}.{i}.desc' + ('' if k == 0 else str(k + 1)), *d)


# ---------------------------------------------------------------- blocks
blk('infernal_stone', 'Cehennem Taşı', 'Infernal Stone')
blk('infernal_bricks', 'Cehennem Tuğlası', 'Infernal Bricks')
blk('chiseled_infernal_bricks', 'Oymalı Cehennem Tuğlası', 'Chiseled Infernal Bricks')
blk('ash_soil', 'Kül Toprağı', 'Ash Soil')
blk('blood_moss', 'Kan Yosunu', 'Blood Moss')
blk('shadow_turf', 'Gölge Çimi', 'Shadow Turf')
blk('scorched_earth', 'Kavrulmuş Toprak', 'Scorched Earth')
blk('crimson_sand', 'Kızıl Kum', 'Crimson Sand')
blk('coagulated_blood', 'Pıhtılaşmış Kan', 'Coagulated Blood')
blk('cursed_obsidian', 'Lanetli Obsidyen', 'Cursed Obsidian')
blk('realm_portal', 'Kızıl Geçit', 'Crimson Gate')
blk('infernal_steel_ore', 'Kızıl Çelik Cevheri', 'Infernal Steel Ore')
blk('arcane_crystal_ore', 'Büyü Kristali Cevheri', 'Arcane Crystal Ore')
blk('infernal_steel_block', 'Kızıl Çelik Bloğu', 'Block of Infernal Steel')
blk('blood_stem', 'Kan Ağacı Gövdesi', 'Blood Stem')
blk('blood_cap', 'Kan Mantarı Şapkası', 'Blood Cap')
blk('blood_planks', 'Kan Ağacı Kalası', 'Blood Planks')
blk('shadow_stem', 'Gölge Ağacı Gövdesi', 'Shadow Stem')
blk('shadow_crystal', 'Gölge Kristali', 'Shadow Crystal')
blk('shadow_planks', 'Gölge Ağacı Kalası', 'Shadow Planks')
blk('ember_flower', 'Kor Çiçeği', 'Ember Flower')
blk('soul_lily', 'Ruh Zambağı', 'Soul Lily')
blk('blood_thorn', 'Kan Dikeni', 'Blood Thorn')
blk('shadow_fern', 'Gölge Eğreltisi', 'Shadow Fern')
blk('infernal_lantern', 'Cehennem Feneri', 'Infernal Lantern')
blk('lord_altar', 'Lord Sunağı', 'Lord Altar', ('Bir krallık lordunu meydan okumaya çağırır.', 'Calls a kingdom lord to a duel.'))
blk('crimson_throne', 'Kızıl Taht', 'Crimson Throne', ('Taht Anahtarı ile kullan.', 'Use with the Throne Key.'))

# ---------------------------------------------------------------- items
itm('crimson_chronicle', 'Kızıl Kronik', 'Crimson Chronicle', ('Hikâyeni, krallıkları ve itibarını gösterir.', 'Shows your story, the kingdoms and your standing.'))
itm('ember_key', 'Kor Anahtarı', 'Ember Key', ('Lanetli Obsidyen çerçeveyi tutuşturur.', 'Ignites a Cursed Obsidian frame.'))
for f, tr, en in (('ash', 'Kül', 'Ash'), ('blood', 'Kan', 'Blood'), ('shadow', 'Gölge', 'Shadow'), ('legion', 'Lejyon', 'Legion'), ('soul', 'Ruh', 'Soul')):
    itm(f'{f}_sigil', f'{tr} Mührü', f'{en} Sigil', ('Beş Mühür birleşince Taht Anahtarı doğar.', 'Five sigils united forge the Throne Key.'))
    itm(f'{f}_war_horn', f'{tr} Savaş Borusu', f'{en} War Horn', ('Krallığın topraklarında öttür: lordunu düelloya çağırır.', "Blow it in the kingdom's lands to challenge its lord."))
itm('throne_key', 'Taht Anahtarı', 'Throne Key', ('Kızıl Taht\'ta ya da Taht Harabeleri\'nde kullan.', 'Use on the Crimson Throne or in the Throne Wastes.'),
    ('Tiran Azgaroth seni bekliyor...', 'The Tyrant Azgaroth awaits...'))
itm('tyrant_heart', 'Tiranın Kalbi', "Tyrant's Heart", ('Hâlâ atıyor.', 'It is still beating.'))
itm('raw_infernal_steel', 'Ham Kızıl Çelik', 'Raw Infernal Steel')
itm('infernal_steel_ingot', 'Kızıl Çelik Külçesi', 'Infernal Steel Ingot')
itm('arcane_crystal', 'Büyü Kristali', 'Arcane Crystal', ('Büyü kitaplarının ve mana iksirlerinin özü.', 'Core of spell tomes and mana potions.'))
itm('ash_essence', 'Kül Özü', 'Ash Essence')
itm('blood_vial', 'Kan Şişesi', 'Blood Vial')
itm('shadow_shard', 'Gölge Parçası', 'Shadow Shard')
itm('brimstone', 'Kükürt Taşı', 'Brimstone')
itm('soul_essence', 'Ruh Özü', 'Soul Essence')
itm('mana_potion', 'Mana İksiri', 'Mana Potion', ('60 mana yeniler.', 'Restores 60 mana.'))
itm('infernal_steel_sword', 'Kızıl Çelik Kılıç', 'Infernal Steel Sword', ('Vurduklarını tutuşturur.', 'Sets its victims ablaze.'))
itm('ashbringer', 'Kül Getiren', 'Ashbringer', ('Kül Kralı Varkhas\'ın kılıcı.', 'Blade of the Ash King Varkhas.'),
    ('Her 3. vuruşta kül patlaması.', 'Every 3rd hit: ash explosion.'), ('Sağ tık: Kül Novası (30 mana)', 'Right click: Ash Nova (30 mana)'))
itm('bloodthirster', 'Kan Emici', 'Bloodthirster', ('Kan Kraliçesi Serathis\'in kılıcı.', 'Blade of the Blood Queen Serathis.'),
    ('Vuruşların hasarın %25\'i kadar can çalar.', 'Steals 25% of damage dealt as health.'), ('Sağ tık: Kan Mızrağı (20 mana)', 'Right click: Blood Lance (20 mana)'))
itm('shadowfang', 'Gölge Dişi', 'Shadowfang', ('Gölge Veziri Nyx\'ar\'ın hançeri.', "Dagger of the Shadow Vizier Nyx'ar."),
    ('Arkadan vuruşlar 2 kat hasar verir.', 'Strikes from behind deal double damage.'), ('Sağ tık: Gölge Adımı (15 mana)', 'Right click: Shadow Step (15 mana)'))
itm('warlord_hammer', 'Grommak\'ın Gazabı', "Grommak's Wrath", ('Savaş Lordu Grommak\'ın çekici.', 'Hammer of Warlord Grommak.'),
    ('Sağ tık: Yer Sarsıntısı (25 mana)', 'Right click: Earthshatter (25 mana)'))
itm('soul_reaper', 'Ruh Biçen', 'Soul Reaper', ('Ruh Kâhini Ilvaine\'in tırpanı.', 'Scythe of the Soul Oracle Ilvaine.'),
    ('Öldürdüğün her canavar seni iyileştirir.', 'Every kill heals you.'), ('Sağ tık: Ruh Dalgası (25 mana)', 'Right click: Soul Wave (25 mana)'))
itm('sovereign_blade', 'Hükümdarın Kılıcı', "Sovereign's Blade", ('Kızıl Taht\'ın gerçek sahibinin kılıcı.', 'Blade of the true master of the Crimson Throne.'),
    ('Sağ tık: Meteor Darbesi (40 mana)', 'Right click: Meteor Strike (40 mana)'))
TOMES = {'fireball': ('Ateş Topu', 'Fireball', 'Patlayan bir ateş topu fırlatır.', 'Hurls an exploding fireball.'),
         'meteor': ('Meteor Yağmuru', 'Meteor Shower', 'Baktığın yere meteorlar yağdırır.', 'Rains meteors where you look.'),
         'blood_lance': ('Kan Mızrağı', 'Blood Lance', 'Delip geçen, can çalan bir mızrak.', 'A piercing, life-stealing lance.'),
         'shadow_step': ('Gölge Adımı', 'Shadow Step', 'Baktığın yöne ışınlanırsın.', 'Teleports you where you look.'),
         'soul_shield': ('Ruh Kalkanı', 'Soul Shield', 'Seni koruyan ruh kalkanı.', 'A shield of souls protects you.'),
         'chain_lightning': ('Zincir Şimşek', 'Chain Lightning', 'Düşmandan düşmana sıçrayan şimşek.', 'Lightning that leaps between foes.'),
         'lava_wave': ('Lav Dalgası', 'Lava Wave', 'Önündeki toprağı ateşle yarar.', 'Splits the ground ahead with fire.'),
         'summon_imps': ('İblis Çağırma', 'Summon Imps', 'Senin için savaşan iblisler çağırır.', 'Summons imps that fight for you.'),
         'life_drain': ('Hayat Emme', 'Life Drain', 'Hedefin canını emersin.', 'Drains the life of your target.'),
         'fear': ('Korku Çığlığı', 'Scream of Terror', 'Çevrendeki düşmanları dehşete düşürür.', 'Terrifies the enemies around you.')}
for k, (tr, en, dtr, den) in TOMES.items():
    itm(f'tome_{k}', f'Büyü Kitabı: {tr}', f'Spell Tome: {en}', (dtr, den))
    t(f'spell.{M}.{k}', tr, en)
for p, tr, en in (('helmet', 'Miğfer', 'Helmet'), ('chestplate', 'Göğüslük', 'Chestplate'), ('leggings', 'Dizlik', 'Leggings'), ('boots', 'Çizme', 'Boots')):
    itm(f'infernal_steel_{p}', f'Kızıl Çelik {tr}', f'Infernal Steel {en}')
itm('sovereign_crown', 'Hükümdar Tacı', 'Sovereign Crown', ('Kızıl Diyar\'ın hükümdarına aittir.', 'Belongs to the ruler of the Crimson Realm.'))
itm('ally_banner', 'Müttefik Sancağı', 'Ally Banner', ('Müttefik krallıklardan savaşçılar çağırır.', 'Calls warriors of your allied kingdoms.'))
t(f'itemGroup.{M}.realm', 'Kızıl Diyar', 'Crimson Realm')

# ---------------------------------------------------------------- kingdoms
KINGDOMS = {
    'ash': ('Kül Krallığı', 'Ash Kingdom', 'Kül Kralı Varkhas', 'Varkhas, the Ash King',
            'Yanık çöllerde demirci-şövalyeler hüküm sürer. Onura ve ateşe taparlar.',
            'Smith-knights rule the burnt deserts. They worship honour and fire.'),
    'blood': ('Kan Hanedanı', 'Blood Dynasty', 'Kan Kraliçesi Serathis', 'Serathis, the Blood Queen',
              'Bataklık saraylarında zarafet ve sonsuz bir açlık yaşar.', 'Grace and endless hunger dwell in the marsh palaces.'),
    'shadow': ('Gölge Konseyi', 'Shadow Council', "Gölge Veziri Nyx'ar", "Nyx'ar, the Shadow Vizier",
               'Obsidyen ormanında sırlar fısıldanır, ihanet bir sanattır.', 'Secrets are whispered in the obsidian forest; betrayal is an art.'),
    'legion': ('Demir Lejyonu', 'Iron Legion', 'Savaş Lordu Grommak', 'Grommak, the Warlord',
               'Bazalt savaş alanlarında güç ve disiplin her şeydir.', 'On the basalt warfields, strength and discipline are everything.'),
    'soul': ('Ruh Tarikatı', 'Soul Cult', 'Ruh Kâhini Ilvaine', 'Ilvaine, the Soul Oracle',
             'Sessiz vadide ölülerin ruhlarını korurlar ve kimseye güvenmezler.', 'In the silent valley they guard the souls of the dead and trust no one.'),
}
for f, (tr, en, ltr, len_, dtr, den) in KINGDOMS.items():
    t(f'faction.{M}.{f}', tr, en)
    t(f'faction.{M}.{f}.lord', ltr, len_)
    t(f'faction.{M}.{f}.desc', dtr, den)

for b, tr, en in (('ash_wastes', 'Kül Çölleri', 'Ash Wastes'), ('basalt_warfields', 'Bazalt Savaş Alanları', 'Basalt Warfields'),
                  ('throne_wastes', 'Taht Harabeleri', 'Throne Wastes'), ('blood_marsh', 'Kan Bataklığı', 'Blood Marsh'),
                  ('soul_valley', 'Ruh Vadisi', 'Soul Valley'), ('obsidian_forest', 'Obsidyen Ormanı', 'Obsidian Forest')):
    t(f'biome.{M}.{b}', tr, en)

t('standing.krolasyonbosses.hostile', 'Düşman', 'Hostile')
t('standing.krolasyonbosses.wary', 'Şüpheci', 'Wary')
t('standing.krolasyonbosses.neutral', 'Yabancı', 'Stranger')
t('standing.krolasyonbosses.friendly', 'Dost', 'Friendly')
t('standing.krolasyonbosses.allied', 'Müttefik', 'Allied')
t('standing.krolasyonbosses.conquered', 'Fethedildi', 'Conquered')
t('standing.krolasyonbosses.sworn', 'Biat Etti', 'Sworn')

# ---------------------------------------------------------------- story
CH = [
    ('Yarık', 'The Rift',
     'Gece yarısı toprak yarıldı ve kızıl bir ışık sızdı. Lanetli obsidyenden bir kapı... Öte tarafta biri adını fısıldıyor.',
     'At midnight the earth split and crimson light bled through. A gate of cursed obsidian... Beyond it, someone whispers your name.',
     'Bir Kızıl Geçit bul (ya da Lanetli Obsidyenden yap ve çakmakla yak) ve içinden geç.',
     'Find a Crimson Gate (or build one of Cursed Obsidian and light it) and step through.'),
    ('Ateşdoğan', 'The Fireborn',
     'Kızıl Diyar\'a hoş geldin. Yüzyıllar önce Tiran Azgaroth Kızıl Taht\'ı gasp etti, beş krallığı birbirine düşürdü ve gücünü beş Mühür\'e bölüp lordlarına dağıttı. Kehanet, dünyalar arasındaki yarıktan gelen bir Ateşdoğan\'ın Mühürleri birleştirip tahtı alacağını söyler. Krallıklar seni izliyor.',
     'Welcome to the Crimson Realm. Centuries ago the Tyrant Azgaroth seized the Crimson Throne, turned the five kingdoms against each other and split his power into five Sigils given to their lords. A prophecy speaks of a Fireborn from beyond the rift who will unite the Sigils and take the throne. The kingdoms are watching you.',
     'Bir krallığın elçisini bul ve onunla konuş (kalelerde ve krallık topraklarında dolaşırlar).',
     'Find an envoy of a kingdom and speak with them (they roam the keeps and kingdom lands).'),
    ('Taç İçin Pazarlık', 'Bargain for a Crown',
     'Elçiler sana kuşkuyla baktı ama dinledi. Her krallık bir Mühür saklıyor. Onu ya dostluğunla kazanacaksın ya da lordlarını dize getirerek.',
     'The envoys eyed you with suspicion, but they listened. Every kingdom guards a Sigil. You will earn it through friendship - or by bringing its lord to his knees.',
     'İlk Mühür\'ü kazan: bir krallıkla ittifak kur ya da lordunu yen.',
     'Win your first Sigil: forge an alliance with a kingdom or defeat its lord.'),
    ('Beş Mühür', 'Five Sigils',
     'Bir Mühür avucunda yanıyor. Diğer krallıklar artık seni ciddiye alıyor; bazıları korkuyla, bazıları kıskançlıkla. Unutma: birine verdiğin söz, düşmanlarını sana düşman eder.',
     'A Sigil burns in your palm. The other kingdoms now take you seriously - some with fear, some with envy. Remember: a promise to one makes its enemies yours.',
     'Beş Mühür\'ün tamamını topla.', 'Gather all five Sigils.'),
    ('Kızıl Taht', 'The Crimson Throne',
     'Beş Mühür birlikte titreşiyor. Taht Harabeleri\'nin derinliklerinden Azgaroth\'un kahkahası yankılanıyor: "Gel bakalım, Ateşdoğan. Tahtım seni bekliyor."',
     'The five Sigils resonate together. From deep within the Throne Wastes echoes the laughter of Azgaroth: "Come then, Fireborn. My throne awaits you."',
     'Mühürleri bir altın blokla birleştirip Taht Anahtarı\'nı yap ve Taht Harabeleri\'nde kullan.',
     'Combine the Sigils with a gold block into the Throne Key and use it in the Throne Wastes.'),
    ('Hükümdar', 'Sovereign',
     'Tiran düştü. Beş krallığın sancakları önünde eğiliyor. Kızıl Diyar\'ın yeni hükümdarı sensin - ama bir taç, onu taşıyabilene aittir.',
     'The Tyrant has fallen. The banners of five kingdoms bow before you. You are the new sovereign of the Crimson Realm - but a crown belongs to the one who can bear it.',
     'Diyar senin. Krallıkların sana biat etti; düşmanlarını onların ordularıyla ez.',
     'The realm is yours. Its kingdoms are sworn to you; crush your enemies with their armies.'),
]
for i, (tr, en, str_, sen, otr, oen) in enumerate(CH):
    t(f'chapter.{M}.{i}', tr, en)
    t(f'chapter.{M}.{i}.story', str_, sen)
    t(f'chapter.{M}.{i}.goal', otr, oen)

# ---------------------------------------------------------------- messages & gui
msg = {
    'rift': ('Yer sarsılıyor... Uzakta kızıl bir yarık açıldı!', 'The ground shakes... A crimson rift has torn open nearby!'),
    'chapter': ('Yeni Bölüm', 'New Chapter'),
    'rep_change': ('%s ile itibarın: %s', 'Standing with %s: %s'),
    'not_realm': ('Bu yalnızca Kızıl Diyar\'da işe yarar.', 'This only works in the Crimson Realm.'),
    'horn_wrong_land': ('Bu boru yalnızca %s topraklarında yankılanır.', 'This horn only echoes in the lands of %s.'),
    'lord_present': ('Lord zaten burada!', 'The lord is already here!'),
    'lord_conquered': ('%s zaten senin önünde diz çöktü.', '%s has already knelt before you.'),
    'lord_cooldown': ('Sunak henüz sessiz. Biraz bekle.', 'The altar is still silent. Wait a while.'),
    'lord_arrives': ('%s meydan okumanı kabul etti!', '%s accepts your challenge!'),
    'lord_defeated': ('%s yenildi! %s artık senin.', '%s is defeated! %s is now yours.'),
    'throne_need_key': ('Taht yalnızca Taht Anahtarı\'na yanıt verir.', 'The throne answers only to the Throne Key.'),
    'throne_wrong_place': ('Taht Anahtarı yalnızca Taht Harabeleri\'nde ya da Kızıl Taht\'ta kullanılabilir.', 'The Throne Key works only in the Throne Wastes or on the Crimson Throne.'),
    'tyrant_arrives': ('AZGAROTH: "Demek sonunda geldin, Ateşdoğan. Tahtım için kanını dökeceksin!"', 'AZGAROTH: "So you came at last, Fireborn. You will bleed for my throne!"'),
    'tyrant_defeated': ('%s, Kızıl Diyar\'ın yeni Hükümdarı oldu!', '%s has become the new Sovereign of the Crimson Realm!'),
    'tyrant_present': ('Tiran zaten burada!', 'The Tyrant is already here!'),
    'mana_low': ('Yetersiz mana!', 'Not enough mana!'),
    'cooldown': ('Henüz hazır değil.', 'Not ready yet.'),
    'allies_called': ('Müttefiklerin savaş çağrına yanıt verdi!', 'Your allies answer your call to war!'),
    'no_allies': ('Hiç müttefikin yok.', 'You have no allies.'),
    'dash_locked': ('Kızıl Atılım\'ı Kızıl Diyar\'a girince öğreneceksin.', 'You will learn the Crimson Dash after entering the realm.'),
    'blessing': ('Lütuf kazanıldı: %s', 'Blessing gained: %s'),
    'quest_done': ('Görev tamamlandı! Elçiye geri dön.', 'Quest complete! Return to the envoy.'),
    'quest_progress': ('Görev: %s / %s', 'Quest: %s / %s'),
    'betrayal': ('%s, düşmanlarıyla kurduğun dostluğu unutmayacak.', '%s will not forget your friendship with its enemies.'),
}
for k, (tr, en) in msg.items():
    t(f'message.{M}.{k}', tr, en)

blessings = {'ash': ('Kül Lütfu: ateşe bağışıklık', "Ash Blessing: immunity to fire"),
             'blood': ('Kan Lütfu: vuruşların can çalar', 'Blood Blessing: your strikes steal life'),
             'shadow': ('Gölge Lütfu: karanlıkta görürsün, körlüğe bağışıklık', 'Shadow Blessing: darkvision, immune to blindness'),
             'legion': ('Lejyon Lütfu: +4 zırh, +2 saldırı', 'Legion Blessing: +4 armour, +2 attack'),
             'soul': ('Ruh Lütfu: tehlikede yenilenme, hızlı mana', 'Soul Blessing: regeneration in peril, faster mana')}
for k, (tr, en) in blessings.items():
    t(f'blessing.{M}.{k}', tr, en)

gui = {
    'journal': ('Kızıl Kronik', 'Crimson Chronicle'),
    'kingdoms': ('Krallıklar', 'Kingdoms'),
    'story': ('Hikâye', 'Story'),
    'goal': ('Hedef', 'Goal'),
    'sigils': ('Mühürler: %s / 5', 'Sigils: %s / 5'),
    'ruler': ('Kızıl Diyar\'ın Hükümdarı', 'Sovereign of the Crimson Realm'),
    'allies_of': ('Dostları: %s', 'Friends: %s'),
    'enemies_of': ('Düşmanları: %s', 'Enemies: %s'),
    'none': ('yok', 'none'),
    'mana': ('Mana', 'Mana'),
    'reputation': ('İtibar: %s (%s)', 'Standing: %s (%s)'),
    'envoy_title': ('%s Elçisi', 'Envoy of %s'),
    'tribute': ('Haraç Sun', 'Offer Tribute'),
    'quest': ('Görev', 'Quest'),
    'alliance': ('İttifak Kur', 'Forge Alliance'),
    'leave': ('Ayrıl', 'Leave'),
    'page_prev': ('< Önceki', '< Previous'),
    'page_next': ('Sonraki >', 'Next >'),
    'quest_none': ('Görev al: rakip krallıktan 8 asker öldür.', 'Take a quest: slay 8 soldiers of a rival kingdom.'),
    'quest_active': ('Görev: %s askerlerinden %s / %s', 'Quest: %s soldiers %s / %s'),
    'tribute_hint': ('Elinde altın, elmas ya da düşman krallıkların özlerini tut.', 'Hold gold, diamonds or essences of enemy kingdoms in your hand.'),
    'alliance_hint': ('İttifak için 50 itibar gerekir.', 'An alliance requires 50 standing.'),
    'blessings': ('Lütuflar', 'Blessings'),
}
for k, (tr, en) in gui.items():
    t(f'gui.{M}.{k}', tr, en)

# envoy dialogue by standing
DIALOG = {
    'ash': [('Kül seni boğsun, yabancı. Kral Varkhas seni bekliyor - kılıcının ucunda.', 'May ash choke you, stranger. King Varkhas awaits you - at the tip of his sword.'),
            ('Ateşin çocuğu mu? Onurunu kanıtla, sonra konuşuruz.', 'Child of fire? Prove your honour, then we talk.'),
            ('Ocaklarımız sana açık, Ateşdoğan. Varkhas senden söz ediyor.', 'Our forges are open to you, Fireborn. Varkhas speaks of you.'),
            ('Kardeşim! Kül Krallığı\'nın kılıçları seninle.', "Brother in fire! The blades of the Ash Kingdom are yours.")],
    'blood': [('Kanın tatlı kokuyor... yaklaşma, yoksa kraliçe seni içer.', 'Your blood smells sweet... come no closer, or the queen will drink you.'),
              ('Kraliçe Serathis meraklı. Merak tehlikelidir, sevgilim.', 'Queen Serathis is curious. Curiosity is dangerous, darling.'),
              ('Saray kapıları sana aralık. Bir kadeh şarap... ya da kan?', 'The palace doors are ajar for you. A glass of wine... or blood?'),
              ('Hanedanın bir parçasısın artık. Düşmanların, ziyafetimiz olacak.', 'You are one of the Dynasty now. Your enemies will be our feast.')],
    'shadow': [('Gölgeler adını biliyor ve seni istemiyor.', 'The shadows know your name, and they do not want you.'),
               ("Konsey her şeyi görür. Nyx'ar seni izliyor... hâlâ.", "The Council sees all. Nyx'ar is watching you... still."),
               ('Fısıltılarımız artık senin de fısıltın. Dikkatli kullan.', 'Our whispers are now your whispers. Use them carefully.'),
               ('Konsey sana sırlarını açtı. İhanet edersen... biliriz.', 'The Council has opened its secrets to you. Betray us and... we will know.')],
    'legion': [('DUR! Lejyon topraklarında yabancılar ölür.', 'HALT! Strangers die on Legion soil.'),
               ('Güçlü görünüyorsun. Grommak güçlüleri sever - ya da ezer.', 'You look strong. Grommak loves the strong - or crushes them.'),
               ('Savaş davulları senin için çalıyor, asker.', 'The war drums beat for you, soldier.'),
               ('LEJYON! Ateşdoğan bizimle yürüyor!', 'LEGION! The Fireborn marches with us!')],
    'soul': [('Ölüler senin adını lanetliyor.', 'The dead curse your name.'),
             ('Ruhlar kararsız. Kâhin Ilvaine henüz konuşmadı.', 'The souls are undecided. The Oracle Ilvaine has not yet spoken.'),
             ('Ruhlar seni kabul etti. Yolun aydınlık olsun.', 'The souls accept you. May your path be lit.'),
             ('Ölülerin bekçisi olarak seni kutsuyoruz, Ateşdoğan.', 'As keepers of the dead, we bless you, Fireborn.')],
}
for f, lines in DIALOG.items():
    for i, (tr, en) in enumerate(lines):
        t(f'dialog.{M}.{f}.{i}', tr, en)
t(f'dialog.{M}.tribute_ok', 'Haracın kabul edildi. (+%s itibar)', 'Your tribute is accepted. (+%s standing)')
t(f'dialog.{M}.tribute_bad', 'Bu bize hakaret mi? Bize altın, elmas ya da düşmanlarımızın özünü getir.', 'Is this an insult? Bring us gold, diamonds or the essence of our enemies.')
t(f'dialog.{M}.quest_given', '%s bizim düşmanımız. Askerlerinden 8\'ini öldür.', '%s is our enemy. Slay 8 of their soldiers.')
t(f'dialog.{M}.quest_reward', 'Görevi tamamladın. Krallığımız bunu unutmayacak. (+15 itibar)', 'You have completed the task. Our kingdom will not forget. (+15 standing)')
t(f'dialog.{M}.alliance_ok', 'Ant içildi! Mührümüz artık senin. Bu yolda yalnız değilsin.', 'The oath is sworn! Our Sigil is yours now. You do not walk this path alone.')
t(f'dialog.{M}.alliance_no', 'Henüz sana güvenmiyoruz.', 'We do not trust you yet.')
t(f'dialog.{M}.alliance_soul', 'Tarikat kimseyle ant içmez... ama sana bir istisna yapacağız.', 'The Cult swears no oaths... but for you we will make an exception.')
t(f'dialog.{M}.already', 'Zaten kardeşiz.', 'We are already kin.')
t(f'dialog.{M}.conquered', 'Lordumuzu yendin. Krallığımız sana boyun eğiyor.', 'You defeated our lord. Our kingdom bows to you.')
t(f'dialog.{M}.ruler', 'Hükümdarım! Emrin bizim için kanundur.', 'My sovereign! Your word is our law.')
t(f'key.{M}.dash', 'Kızıl Atılım', 'Crimson Dash')
t(f'key.categories.{M}', 'Kızıl Diyar', 'Crimson Realm')

# advancements
ADV = {
    'root': ('Kızıl Diyar', 'Crimson Realm', 'Dünyalar arasındaki yarıktan geç.', 'Step through the rift between worlds.'),
    'first_sigil': ('Taç İçin Pazarlık', 'Bargain for a Crown', 'İlk Mühür\'ü kazan.', 'Win your first Sigil.'),
    'throne_key': ('Beş Mühür', 'Five Sigils', 'Taht Anahtarı\'nı yap.', 'Forge the Throne Key.'),
    'kill_lord': ('Kral Katili', 'Kingslayer', 'Bir krallık lordunu yen.', 'Defeat a kingdom lord.'),
    'sovereign': ('Hükümdar', 'Sovereign', 'Tiran Azgaroth\'u yen ve tahtı al.', 'Defeat the Tyrant Azgaroth and claim the throne.'),
    'spell': ('Büyücü', 'Spellcaster', 'Bir büyü kitabı edin.', 'Obtain a spell tome.'),
    'legendary': ('Efsanevi Silah', 'Legendary Weapon', 'Bir lordun silahını ele geçir.', "Claim a lord's weapon."),
}
for k, (tr, en, dtr, den) in ADV.items():
    t(f'advancements.{M}.{k}.title', tr, en)
    t(f'advancements.{M}.{k}.description', dtr, den)
t(f'entity.{M}.spell_projectile', 'Büyü Küresi', 'Spell Orb')
