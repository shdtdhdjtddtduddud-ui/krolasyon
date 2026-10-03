"""Story, dialogue and journal texts (Turkish + English) -> lang files.  python3 gen/lore.py"""
import json, os
A = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/krolasyonbosses/lang')
en = json.load(open(f'{A}/en_us.json')); tr = json.load(open(f'{A}/tr_tr.json'))

def both(k, e, t):
    en[k] = e; tr[k] = t

# ---- options
for k, e, t in [
    ('accept', 'I accept.', 'Kabul ediyorum.'), ('report', 'The hunt is done.', 'Av tamamlandı.'),
    ('continue', 'Go on.', 'Devam et.'), ('back', 'Back.', 'Geri.'), ('leave', 'Leave.', 'Ayrıl.'),
    ('deliver', 'Hand over the offering.', 'Adağı teslim et.'), ('directions', 'Where is your citadel?', 'Kaleniz nerede?'),
    ('trade', 'Trade.', 'Takas yap.'), ('tribute', 'Claim my daily tribute.', 'Günlük haracımı al.'),
    ('lore', 'Tell me of your house.', 'Hanedanından bahset.'), ('more', 'And then?', 'Sonra?'),
    ('politics', 'What of the other houses?', 'Diğer hanedanlar ne olacak?'),
    ('swear', 'I swear the oath.', 'Yemin ediyorum.'), ('pay', 'Pay the tribute.', 'Haracı öde.'),
    ('challenge', 'I challenge you for the crown.', 'Seni taht için düelloya çağırıyorum.'),
    ('fight', 'Draw your weapon!', 'Silahını çek!'),
    ('trade1', 'Hellsteel ingot  (8 house materials)', 'Cehennem çeliği külçesi  (8 hanedan malzemesi)'),
    ('trade2', 'House tome  (10 materials + 4 ingots)', 'Hanedan büyü kitabı  (10 malzeme + 4 külçe)'),
    ('trade3', 'House blade  (16 materials + 6 ingots)', 'Hanedan kılıcı  (16 malzeme + 6 külçe)')]:
    both('dlg.opt.' + k, e, t)

# ---- envoy
both('dlg.env.stage0', 'Mortal. Few cross the gate and fewer are heard. I speak for the %s. We are at war with %s. Prove you are no spy: bring down %s of their warriors, and we will talk.',
     'Ölümlü. Kapıdan geçen az, dinlenen daha az. %s adına konuşuyorum. %s ile savaştayız. Casus olmadığını kanıtla: onların %s savaşçısını devir, sonra konuşuruz.')
both('dlg.env.accepted', 'Hunt among %2$s... no. Among %1$s. Return when %2$s have fallen.', 'Av başlasın: %s arasında %s tanesi. Düşünce bana dön.')
en['dlg.env.accepted'] = 'Hunt among %s. Return when %s have fallen.'
both('dlg.env.stage1.done', 'You reek of their blood. Good. The %s are counted.', 'Üzerinde onların kanı kokuyor. İyi. %s sayıldı.')
both('dlg.env.stage1.wait', 'Not yet. %s of %s. Keep culling %s.', 'Daha değil. %s / %s. %s avlamaya devam et.')
both('dlg.env.reward1', 'The %s remember a debt. Take this.', '%s bir borcu unutmaz. Bunu al.')
both('dlg.env.stage2', 'Now an offering: %s of %s. You carry %s.', 'Şimdi bir adak: %s adet %s. Sende %s var.')
both('dlg.env.reward2', 'The %s open the gates of the citadel to you. Our ruler will hear you.', '%s kale kapılarını sana açar. Hükümdarımız seni dinleyecek.')
both('dlg.env.missing', 'You do not have the %s required.', 'Gereken %s adet sende yok.')
both('dlg.env.stage3', 'You are known among the %s now. Seek the citadel; swear, or take the throne by force.', 'Artık %s arasında tanınıyorsun. Kaleyi ara; yemin et ya da tahtı zorla al.')
both('dlg.env.after', 'Hail, sworn one of the %s. Ask what you need.', 'Selam, %s yeminlisi. İste ne istersen.')
both('dlg.env.politics', 'The %s stand with %s, and bleed against %s. Azrakor has no peace, only pauses. Every oath you swear is a knife in somebody\'s back.',
     '%s, %s ile omuz omuza; %s ile kan davasında. Azrakor\'da barış yoktur, ara vardır. Verdiğin her yemin birinin sırtında bir bıçaktır.')
both('dlg.env.directions', 'The citadel lies at X %s, Z %s. About %s blocks from here.', 'Kale X %s, Z %s konumunda. Buradan yaklaşık %s blok.')
both('dlg.env.directions.none', 'The ash hides the way. Walk further, and ask again.', 'Kül yolu gizliyor. Biraz daha yürü, tekrar sor.')
both('dlg.trade', 'Bring %s and hellsteel; I deal in what the %s will part with.', '%s ve cehennem çeliği getir; %s\'nin elden çıkarabileceği şeylerle iş yaparım.')
both('dlg.trade.ok', 'Done. Do not ask where it came from.', 'Tamam. Nereden geldiğini sorma.')
both('dlg.trade.fail', 'Your purse is too light.', 'Kesen fazla hafif.')
both('dlg.tribute.ok', 'The %s pay what is owed. Take it.', '%s borcunu öder. Al.')
both('dlg.tribute.wait', 'You were paid today. The %s are not a bottomless well.', 'Bugün ödendin. %s dipsiz bir kuyu değil.')
# ---- ruler
both('dlg.ruler.early', 'A stray crawls into my hall. Earn the trust of my envoys, or leave.', 'Salonuma bir başıboş sürünüyor. Elçilerimin güvenini kazan ya da defol.')
both('dlg.ruler.offer', 'My envoys speak well of you. Kneel, and the %s are yours; defy, and you will find out why I wear the crown.', 'Elçilerim senden iyi söz ediyor. Diz çök, %s senin olsun; karşı koy, tacı neden taktığımı öğrenirsin.')
both('dlg.ruler.tribute', 'My court distrusts you. Pay tribute: %2$s of %3$s. Standing: %4$s.', '')
en['dlg.ruler.tribute'] = 'The %s distrust you. Pay a tribute: %s of %s. Your standing: %s.'
tr['dlg.ruler.tribute'] = '%s sana güvenmiyor. Haraç öde: %s adet %s. İtibarın: %s.'
both('dlg.ruler.paid', 'Accepted. Do not mistake coin for loyalty.', 'Kabul. Parayı sadakat sanma.')
both('dlg.ruler.ally', 'Sworn one of the %s. What does the court need of you today?', '%s yeminlisi. Divan bugün senden ne istiyor?')
both('dlg.ruler.conqueror', 'You hold the %s by the throat. Speak, conqueror.', '%s\'yi boğazından tutuyorsun. Konuş, fatih.')
both('dlg.ruler.challenge', 'Blood or oath, there is no third road. The %s will watch you die.', 'Kan ya da yemin, üçüncü yol yok. %s ölümünü izleyecek.')
both('dlg.ruler.oath_done', 'It is sworn. The %s are your allies; their sigil is yours.', 'Yemin edildi. %s artık müttefikin; mührü senin.')
both('msg.oath.sworn', '%s has sworn an oath to the %s!', '%s, %s\'ye yemin etti!')
both('msg.house.conquered', '%s has broken the %s!', '%s, %s\'yi yıktı!')

# ---- house lore (3 pages each)
LORE = {
 'ember': (['Born from the first lava tide, the Ember Dominion rules the Ember Wastes with banners of black iron and orange flame.',
            'Their Queen believes fire purifies: weakness is fuel, loyalty is the spark. They are bitter enemies of the Blood Conclave and the Shadow Covenant.',
            'Their envoy asks for ember shards. Their blades never cool.'],
           ['İlk lav gelgitinden doğan Kor Hanedanı, Kor Çölleri\'ni siyah demir ve turuncu alevden sancaklarla yönetir.',
            'Kraliçeleri ateşin arındırdığına inanır: zayıflık yakıttır, sadakat kıvılcım. Kan Konseyi ve Gölge Tarikatı\'nın amansız düşmanlarıdır.',
            'Elçileri kor parçası ister. Kılıçları asla soğumaz.']),
 'bone': (['The Bone Kingdom raised its halls from the ribs of the first dead. Its king never sleeps, because he never lived.',
           'They keep records of every debt in marrow-ink. They are allies of the Ember Dominion and the Rot Court, and loathe the Shadow Covenant and Blood Conclave.',
           'Their envoy asks for bone dust. Their law is old and simple: the dead outnumber the living.'],
          ['Kemik Krallığı salonlarını ilk ölülerin kaburgalarından yükseltti. Kralları hiç uyumaz, çünkü hiç yaşamadı.',
           'Her borcu ilik mürekkebiyle kaydederler. Kor Hanedanı ve Çürük Divanı ile müttefik, Gölge Tarikatı ve Kan Konseyi\'ne düşmandırlar.',
           'Elçileri kemik tozu ister. Yasaları eski ve basit: ölüler yaşayanlardan çoktur.']),
 'blood': (['The Blood Conclave is a council of nine that votes with knives. The Hierophant\'s chalice is never empty.',
            'Everything is a transaction; blood is the only currency that does not devalue. Allied with the Ember Dominion and Shadow Covenant, enemies of the Bone Kingdom and Rot Court.',
            'Their envoy asks for blood vials. Betray them and the debt is collected from your veins.'],
           ['Kan Konseyi, bıçakla oy veren dokuz kişilik bir meclistir. Başrahibin kadehi hiç boş kalmaz.',
            'Her şey takastır; kan, değer kaybetmeyen tek paradır. Kor Hanedanı ve Gölge Tarikatı ile müttefik, Kemik Krallığı ve Çürük Divanı\'na düşman.',
            'Elçileri kan şişesi ister. Onlara ihanet edersen borç damarlarından tahsil edilir.']),
 'shadow': (['The Shadow Covenant lives where the ash is thickest. Nyx, the Hierophant, is said to be everyone\'s shadow at once.',
             'They trade in secrets and silence. Allied with the Blood Conclave and Rot Court, enemies of the Ember Dominion and Bone Kingdom.',
             'Their envoy asks for void dust. Never trust a shadow that agrees with you.'],
            ['Gölge Tarikatı külün en yoğun olduğu yerde yaşar. Başrahip Nyx\'in herkesin gölgesi olduğu söylenir.',
             'Sır ve sessizlikle ticaret yaparlar. Kan Konseyi ve Çürük Divanı ile müttefik, Kor Hanedanı ve Kemik Krallığı\'na düşman.',
             'Elçileri boşluk tozu ister. Seninle hemfikir olan bir gölgeye asla güvenme.']),
 'rot': (['The Rot Court sits in the Weeping Marsh, where nothing ends, it only changes. The Rot Mother has outlived four kingdoms.',
          'They tend the spores that feed all of Azrakor, which gives them quiet leverage over everyone. Allied with the Bone Kingdom and Shadow Covenant, enemies of the Ember Dominion... and the Blood Conclave.',
          'Their envoy asks for spores. Patience is their weapon.'],
         ['Çürük Divanı, hiçbir şeyin bitmediği, sadece değiştiği Ağlayan Bataklık\'ta oturur. Çürük Ana dört krallıktan uzun yaşadı.',
          'Azrakor\'u besleyen sporları onlar yetiştirir; bu da herkes üzerinde sessiz bir güç verir. Kemik Krallığı ve Gölge Tarikatı ile müttefik, Kor Hanedanı ve Kan Konseyi\'ne düşman.',
          'Elçileri spor ister. Sabır onların silahıdır.']),
}
for f, (e, t) in LORE.items():
    for i in range(3):
        en[f'dlg.lore.{f}.{i}'] = e[i]; tr[f'dlg.lore.{f}.{i}'] = t[i]
    en[f'journal.house.{f}'] = e[0]; tr[f'journal.house.{f}'] = t[0]

# ---- journal
both('journal.title', 'Chronicle of Azrakor', 'Azrakor Günlüğü')
both('journal.rep', 'Standing: %s', 'İtibar: %s')
both('journal.oath.none', 'No oath sworn.', 'Yemin edilmedi.')
both('journal.oath.ally', 'Sworn ally.', 'Yeminli müttefik.')
both('journal.oath.conqueror', 'Conquered.', 'Fethedildi.')
for i, (e, t) in enumerate([('Not yet met their envoy.', 'Elçileriyle henüz tanışılmadı.'), ('Hunting their enemies for the envoy.', 'Elçi için düşmanları avlıyorsun.'),
                           ('Offering owed to the envoy.', 'Elçiye adak borçlusun.'), ('Admitted to the citadel; seek the ruler.', 'Kaleye kabul edildin; hükümdarı bul.'),
                           ('Their story is settled.', 'Onların hikâyesi sonlandı.')]):
    both(f'journal.quest.{i}', e, t)
both('journal.throne.title', 'The Ash Throne', 'Kül Taht')
both('journal.throne.todo', 'Win or break every house, gather their sigils, and carry them to the Throne Hall in the centre of Azrakor. The Ash Sovereign waits behind the sealed wall.',
     'Her hanedanı kazan ya da yık, mühürlerini topla ve Azrakor\'un ortasındaki Taht Salonu\'na götür. Kül Egemen mühürlü duvarın ardında bekliyor.')
both('journal.throne.done', 'You are Sovereign of Azrakor. The realm kneels, or burns.', 'Azrakor\'un Egemeni sensin. Diyar diz çöker ya da yanar.')
LP = [
 ('The Gate', 'Kapı', 'Beneath the old ruins in the overworld, a gate of black stone sleeps. Light it with a hell spark and it opens onto Azrakor, a dimension of ash, lava and old grudges.',
  'Üst dünyadaki eski kalıntıların altında kara taştan bir kapı uyur. Cehennem kıvılcımıyla yak; küllerin, lavın ve eski kinlerin diyarı Azrakor açılır.'),
 ('Five Houses', 'Beş Hanedan', 'Azrakor is split among five houses: Ember, Bone, Blood, Shadow and Rot. Each house befriends two neighbours and hates two others. No friend is free.',
  'Azrakor beş hanedana bölünmüş: Kor, Kemik, Kan, Gölge ve Çürük. Her hanedan iki komşuyla dosttur, ikisinden nefret eder. Hiçbir dostluk bedava değildir.'),
 ('The Path of Envoys', 'Elçilerin Yolu', 'Find the outposts. Each envoy sets a hunt, then an offering, then opens the way to the citadel. Kills you make for one house cost you with its enemies.',
  'Karakolları bul. Her elçi önce bir av, sonra bir adak ister, sonra kale yolunu açar. Bir hanedan için yaptığın öldürmeler, düşmanlarının gözünde sana mal olur.'),
 ('Oath or Blood', 'Yemin ya da Kan', 'In each citadel the ruler offers an oath, or a duel. Alliance grants tribute, tomes and blades. Conquest grants fear, and fear is also a currency.',
  'Her kalede hükümdar yemin ya da düello sunar. İttifak haraç, kitap ve kılıç getirir. Fetih korku getirir; korku da bir paradır.'),
 ('The Ash Sovereign', 'Kül Egemen', 'When the sigils are gathered, the Throne Hall in the heart of Azrakor unseals. Defeat the Ash Sovereign and the throne is yours.',
  'Mühürler toplandığında Azrakor\'un kalbindeki Taht Salonu açılır. Kül Egemen\'i yen ve taht senindir.'),
]
for i, (et, tt, e, t) in enumerate(LP):
    both(f'journal.lore.{i}.title', et, tt); both(f'journal.lore.{i}', e, t)
both('msg.sovereign','%s has become Sovereign of Azrakor!','%s, Azrakor\'un Egemeni oldu!')
both('item.krolasyonbosses.chronicle', 'Chronicle of Azrakor', 'Azrakor Günlüğü')

json.dump(en, open(f'{A}/en_us.json', 'w'), indent=1, ensure_ascii=False)
json.dump(tr, open(f'{A}/tr_tr.json', 'w'), indent=1, ensure_ascii=False)
print('lore keys ok')
