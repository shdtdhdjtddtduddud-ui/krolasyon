"""Writes tr_tr / en_us language files."""
import json, os
OUT = os.path.join(os.path.dirname(__file__), '../src/main/resources/assets/krolben10/lang')
os.makedirs(OUT, exist_ok=True)

ALIENS = {
    'heatblast': ('Heatblast', ['Ateş Topu', 'Alev Püskürtücü', 'Süpernova', 'Ateş Sütunları'], 'Ateş Kalkanı',
                  'Ateşe ve lavlara bağışık, uçar. Yumrukları yakar, yürüdüğü yerdeki karı/buzu eritir. Su ona zarar verir.',
                  ['Fireball', 'Flamethrower', 'Supernova', 'Fire Geysers'], 'Fire Shield',
                  'Immune to fire and lava, can fly. Punches ignite, melts snow and ice. Water hurts him.'),
    'fourarms': ('Four Arms', ['Yer Sarsıntısı', 'Gök Gürültüsü Alkışı', 'Dev Sıçrayış', 'Kavra ve Fırlat'], 'Dörtlü Kol Savunması',
                 'Muazzam güç ve can, çok yüksek zıplar, düşme hasarı az, uzun erişim. Yumrukları savurur.',
                 ['Ground Slam', 'Thunder Clap', 'Mighty Leap', 'Grab & Throw'], 'Four-Arm Guard',
                 'Huge strength and health, jumps very high, reduced fall damage, long reach. Punches send enemies flying.'),
    'diamondhead': ('Diamondhead', ['Kristal Mermiler', 'Kristal Dikenler', 'Kristal Duvar', 'Kristal Yağmuru'], 'Kristal Kalkan',
                    'Çok yüksek zırh. Kalkan açıkken gelen okları kristal mermi olarak geri yansıtır.',
                    ['Crystal Shards', 'Crystal Spikes', 'Crystal Wall', 'Crystal Storm'], 'Crystal Shield',
                    'Very high armour. While shielding, incoming projectiles are reflected back as crystal shards.'),
    'xlr8': ('XLR8', ['Hız Atılımı', 'Kasırga', 'Şimşek Kombosu', 'Aşırı Hız'], 'Kaçınma',
             'Süper hız, suyun üstünde koşar, yüksek basamak çıkar. Az düşme hasarı.',
             ['Speed Dash', 'Cyclone', 'Blitz Combo', 'Overdrive'], 'Evasion',
             'Super speed, runs on water, steps up blocks. Reduced fall damage.'),
    'goop': ('Goop', ['Asit Tükürüğü', 'Sümük Kırbacı', 'Yutma', 'Sümük Dalgası'], 'Sıvı Beden',
             'Düşme hasarı almaz, zehre bağışık. Yumrukları zehirler. Antigravite UFO\'su ile süzülür.',
             ['Acid Spit', 'Slime Whip', 'Engulf', 'Goo Wave'], 'Liquid Body',
             'No fall damage, immune to poison. Punches poison. Floats with his anti-gravity UFO.'),
    'blowpoke': ('Blowpoke', ['Yuvarlanma', 'Gülle Atışı', 'Sekme Bombası', 'Bilardo Sekmesi'], 'Zırhlı Kabuk',
                   'Ağır zırh, geri itilmez, düşme hasarı almaz. Yuvarlanırken önüne çıkanı ezer.',
                   ['Rolling Charge', 'Cannonball Launch', 'Bounce Bomb', 'Ricochet'], 'Armoured Shell',
                   'Heavy armour, hard to knock back, no fall damage. Crushes everything while rolling.'),
    'bigchill': ('Big Chill', ['Buz Nefesi', 'Buz Hapsi', 'Buz Mızrağı', 'Kar Fırtınası'], 'Kanat Pelerini',
                 'Uçar, donmaya bağışık, düşme hasarı almaz. Suyun üstünden geçerken dondurur, yumrukları dondurur.',
                 ['Ice Breath', 'Ice Prison', 'Ice Spear', 'Blizzard'], 'Wing Cloak',
                 'Flies, immune to freezing and fall damage. Freezes water below, punches freeze.'),
    'echoecho': ('Ultimate Echo Echo', ['Ses Diskleri', 'Sonik Işın', 'Disk Yağmuru', 'Sonik Kıyamet'], 'Disk Duvarı',
                 'Süzülür ve uçar, düşme hasarı almaz. Yumrukları sersemletir ve camları kırar.',
                 ['Sonic Discs', 'Sonic Beam', 'Disc Barrage', 'Sonic Doom'], 'Disc Wall',
                 'Hovers and flies, no fall damage. Punches stun and shatter glass.'),
    'ghostfreak': ('Ghostfreak', ['Gölge Pençe', 'Korku Bakışı', 'Ele Geçirme', 'Hayalet Formu'], 'Hayalet Kaçış',
                   'Uçar, gece görür, düşme hasarı almaz. Yumrukları soldurur (wither).',
                   ['Shadow Claw', 'Fear Gaze', 'Possession', 'Phantom Form'], 'Ghostly Evasion',
                   'Flies, sees in the dark, no fall damage. Claws inflict wither.'),
}

def build(tr):
    L = {}
    L['item.krolben10.omnitrix'] = 'Omnitrix'
    L['itemGroup.krolben10'] = 'Ben 10 Omnitrix'
    L['block.krolben10.crystal_wall'] = 'Kristal Duvar' if tr else 'Crystal Wall'
    L['block.krolben10.frost_ice'] = 'Sihirli Buz' if tr else 'Frost Ice'
    tips = (['Koluna takmak için sağ tıkla', 'G: kolunu kaldır, tekerlek: uzaylı seç, sol tık: DÖNÜŞ!', 'Yetenekler: Z  X  C  V · Bloklama: R', 'Uzaylıyken G: insana dön · Eğilip G: saati çıkar', 'Süre dolunca Omnitrix şarj olur'] if tr else
            ['Right click to lock it onto your wrist', 'G: raise your arm, scroll: pick an alien, left click: SLAM!', 'Abilities: Z  X  C  V · Guard: R', 'As an alien G turns you back · Sneak + G takes it off', 'When time runs out the Omnitrix recharges'])
    for i, t in enumerate(tips):
        L[f'item.krolben10.omnitrix.tip{i + 1}'] = t
    L['key.categories.krolben10'] = 'Ben 10 Omnitrix'
    L['key.krolben10.dial'] = 'Omnitrix (kolu kaldır / dönüş)' if tr else 'Omnitrix (raise arm / transform)'
    L['key.krolben10.guard'] = 'Bloklama (basılı tut)' if tr else 'Guard (hold)'
    for i in range(4):
        L[f'key.krolben10.ability{i + 1}'] = (f'Yetenek {i + 1}' if tr else f'Ability {i + 1}')
    L['msg.krolben10.recharging'] = 'Omnitrix şarj oluyor... %ss' if tr else 'Omnitrix recharging... %ss'
    L['msg.krolben10.timeout'] = 'Omnitrix süresi doldu!' if tr else 'The Omnitrix timed out!'
    L['msg.krolben10.worn'] = 'Omnitrix bileğine kilitlendi! G ile kolunu kaldır.' if tr else 'The Omnitrix locked onto your wrist! Press G to raise your arm.'
    L['msg.krolben10.removed'] = 'Omnitrix çıkarıldı.' if tr else 'Omnitrix removed.'
    L['msg.krolben10.wear_first'] = 'Önce Omnitrix\'i koluna tak (eşyayla sağ tık).' if tr else 'Put the Omnitrix on first (right click the item).'
    L['watch.krolben10.hint'] = 'Tekerlek: seç  ·  Sol tık / G: DÖNÜŞ!  ·  Sağ tık: kapat' if tr else 'Scroll: choose  ·  Left click / G: TRANSFORM!  ·  Right click: close'
    L['key.krolben10.list'] = 'Uzaylı listesi (menü)' if tr else 'Alien list (menu)'
    L['msg.krolben10.notarget'] = 'Yakında hedef yok!' if tr else 'No target nearby!'
    L['hud.krolben10.recharging'] = 'Şarj oluyor' if tr else 'Recharging'
    L['gui.krolben10.dial'] = 'Omnitrix'
    L['gui.krolben10.hint'] = ('Kaydır / tıkla: seç  -  Tekrar tıkla veya Enter: DÖNÜŞ!' if tr else 'Scroll / click: select  -  click again or Enter: TRANSFORM!')
    L['gui.krolben10.human'] = 'İnsana Dön' if tr else 'Return to Human'
    for n in ('transform', 'detransform', 'timeout', 'beep', 'dial', 'dial_open', 'sonic', 'whoosh'):
        L[f'subtitle.krolben10.{n}'] = {'transform': ('Omnitrix dönüşümü', 'Omnitrix transformation'), 'detransform': ('Omnitrix geri dönüş', 'Omnitrix reverts'),
                                         'timeout': ('Omnitrix süresi doldu', 'Omnitrix times out'), 'beep': ('Omnitrix bip', 'Omnitrix beeps'),
                                         'dial': ('Kadran döner', 'Dial clicks'), 'dial_open': ('Omnitrix açılır', 'Omnitrix opens'),
                                         'sonic': ('Sonik dalga', 'Sonic pulse'), 'whoosh': ('Hız rüzgarı', 'Speed whoosh')}[n][0 if tr else 1]
    for k, (name, ab_tr, g_tr, p_tr, ab_en, g_en, p_en) in ALIENS.items():
        L[f'alien.krolben10.{k}'] = name
        for i in range(4):
            L[f'ability.krolben10.{k}.{i + 1}'] = (ab_tr if tr else ab_en)[i]
        L[f'guard.krolben10.{k}'] = g_tr if tr else g_en
        L[f'passive.krolben10.{k}'] = p_tr if tr else p_en
    L['entity.krolben10.alien_projectile'] = 'Uzaylı Saldırısı' if tr else 'Alien Attack'
    L['entity.krolben10.debris'] = 'Enkaz' if tr else 'Debris'
    L['entity.krolben10.fx'] = 'Efekt' if tr else 'Effect'
    return L

for code, tr in (('tr_tr', True), ('en_us', False)):
    with open(os.path.join(OUT, code + '.json'), 'w', encoding='utf-8') as f:
        json.dump(build(tr), f, ensure_ascii=False, indent=2)
print('lang ok')
