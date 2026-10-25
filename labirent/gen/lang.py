"""Turkish language file (also used as en_us so the game shows Turkish in every locale)."""
import json, os

L = {}


def add(prefix, d):
    for k, v in d.items():
        L[f'{prefix}{k}'] = v


add('', {
    'itemGroup.labirent': 'Labirent: Ölümcül Kaçış',
    'key.categories.labirent': 'Labirent',
    'key.labirent.map': 'Labirent haritası',
    'key.labirent.captain': 'Kaptan / Ayrıklılar ekranı',
    'key.labirent.journal': 'Ayrık Günlüğü',
    'key.labirent.minimap': 'Mini haritayı aç/kapat',
    'label.labirent.greenie': 'Çaylak',
    'cinematic.labirent.wicked_good': 'WICKED iyidir.',
})

add('block.labirent.', {
    'maze_wall': 'Labirent Duvarı', 'maze_wall_mossy': 'Yosunlu Labirent Duvarı', 'maze_wall_cracked': 'Çatlak Labirent Duvarı',
    'maze_wall_ivy': 'Sarmaşıklı Labirent Duvarı', 'maze_door': 'Labirent Kapısı', 'maze_floor': 'Labirent Zemini',
    'maze_floor_mossy': 'Yosunlu Labirent Zemini', 'box_floor': 'Kutu Izgarası', 'wicked_plate': 'WICKED Plakası',
    'lab_wall': 'Laboratuvar Duvarı', 'lab_light': 'Laboratuvar Lambası', 'code_stone': 'Şifre Taşı', 'map_table': 'Harita Masası',
    'code_terminal': 'Şifre Terminali', 'griever_hole': 'Kederci Deliği',
})
add('item.labirent.', {
    'glade_journal': 'Ayrık Günlüğü', 'glade_journal.tip': 'Yaşadıklarımız. Her bölümde yeni sayfalar eklenir. (J)',
    'captain_baton': 'Kaptan Asası', 'captain_baton.tip1': 'Sağ tık: Kaptan emir ekranı',
    'captain_baton.tip2': 'Ayrıklıya sağ tık: Ona özel emir ver', 'captain_baton.tip3': 'Eğilip sağ tık: Herkesi yanına çağır',
    'runner_map': 'Koşucu Haritası', 'runner_map.tip': 'Herkesin gezdiği yerler otomatik işlenir. (M)',
    'grief_serum': 'Keder Serumu', 'grief_serum.tip': 'Kederci zehrinin tek panzehiri. Kendine ya da birine sağ tıkla.',
    'bandage': 'Sargı Bezi', 'griever_stinger': 'Kederci İğnesi', 'griever_plate': 'Kederci Zırh Plakası',
    'griever_core': 'Kederci Çekirdeği', 'griever_core.tip': 'İçinde bir şey nabız gibi atıyor... Kederciler bununla bir yerden geçiyor olmalı.',
    'beetle_eye': 'Böcek-Bıçak Gözü', 'code_word': 'Şifre Kelimesi', 'code_word.named': 'Şifre: %s. bölüm – %s',
    'code_word.tip': 'Şifre Taşı\'ndan kopyalandı.', 'machete': 'Pala', 'glade_spear': 'Ayrık Mızrağı',
    'frying_pan': 'Tavacı\'nın Tavası', 'frying_pan.tip': 'Yanan bir ocağa/kamp ateşine sağ tıkla: üzerindeki çiğ yemekleri pişirir. Aşçılar çok daha hızlı pişirir.',
    'glade_stew': 'Ayrık Yahnisi', 'glade_bread': 'Ayrık Ekmeği', 'frypan_breakfast': 'Tavacı\'nın Kahvaltısı',
    'griever_spawn_egg': 'Kederci Çağırma Yumurtası', 'glader_spawn_egg': 'Ayrıklı Çağırma Yumurtası',
    'beetle_blade_spawn_egg': 'Böcek-Bıçak Çağırma Yumurtası',
})
add('entity.labirent.', {'griever_corpse': 'Kederci Cesedi', 'griever': 'Kederci', 'glader': 'Ayrıklı', 'beetle_blade': 'Böcek-Bıçak', 'box_elevator': 'Kutu'})
add('effect.labirent.', {'griever_venom': 'Kederci Zehri', 'changing': 'Değişim'})
add('subtitles.labirent.', {
    'wall_grind': 'Labirent duvarları yer değiştiriyor', 'door_close': 'Dev kapılar kapanıyor', 'door_slam': 'Kapılar kapandı',
    'door_open': 'Dev kapılar açılıyor', 'box_alarm': 'Kutu alarmı', 'box_rise': 'Kutu yükseliyor', 'box_arrive': 'Kutu geldi',
    'griever_ambient': 'Kederci tıkırdıyor', 'griever_hurt': 'Kederci çığlık atıyor', 'griever_death': 'Kederci ölüyor',
    'griever_step': 'Metal ayak sesleri', 'griever_sting': 'Kederci sokuyor', 'griever_roll': 'Kederci yuvarlanıyor',
    'beetle_scuttle': 'Böcek-bıçak koşuşturuyor', 'story_sting': 'Bir şeyler değişti', 'maze_ambient': 'Labirentin uğultusu',
    'serum_inject': 'Serum enjekte edildi', 'terminal_beep': 'Terminal bipliyor', 'escape': 'Özgürlük', 'wind_gust': 'Koridorlarda rüzgâr', 'heartbeat': 'Kalp atışı', 'distant_cry': 'Uzaktan bir çığlık',
    'birds': 'Kuş cıvıltıları', 'crickets': 'Cırcır böcekleri', 'tension': 'Gerilim', 'griever_scream': 'Kederci çığlık atıyor', 'griever_chase': 'Kederci avlanıyor',
})

add('role.labirent.', {
    'serbest': 'Serbest', 'serbest.desc': 'Henüz bir işin yok. Kaptan sana bir görev verecek.',
    'kosucu': 'Koşucu', 'kosucu.desc': 'Gün boyu labirenti koşar ve haritalarsın. Labirentte hızlı ve dayanıklısın; daha uzağı görüp haritaya işlersin.',
    'asci': 'Aşçı', 'asci.desc': 'Ayrık\'ı doyurursun. Tavayla yemekleri anında pişirir, ocak başında herkese yemek dağıtırsın.',
    'bahcivan': 'Bahçıvan', 'bahcivan.desc': 'Bahçelere bakarsın. Çapayla ekinlere dokununca hızla büyürler.',
    'kasap': 'Kasap', 'kasap.desc': 'Kan Evi\'nin sorumlususun. Hayvanlardan iki kat et alırsın.',
    'sifaci': 'Şifacı', 'sifaci.desc': 'Yaralıları iyileştirirsin. Sargıların iki kat işe yarar, yanındakiler yavaşça iyileşir.',
    'bekci': 'Bekçi', 'bekci.desc': 'Kapıları korursun. Daha güçlüsün, Kedercilere fazla hasar verirsin, geceleri dayanıklısın.',
})

add('chapter.labirent.', {
    'num': 'Bölüm %s', '0': 'Kutu', '1': 'Ayrık\'ın Kuralları', '2': 'Koşucular', '3': 'Kederciler', '4': 'Duvarlar Değişiyor',
    '5': 'Sonuncusu', '6': 'Şifre', '7': 'Kederci Deliği', '8': 'Kaçış',
})
add('objective.labirent.', {
    'talk_newt': 'Newt ile konuş', 'read_journal': 'Ayrık Günlüğü\'nü oku (J)', 'watch_doors': 'Kapılar kapanırken Ayrık\'ta ol',
    'assign_runner': 'Birine Koşucu görevi ver (K)', 'assign_cook': 'Birine Aşçı görevi ver', 'eat_meal': 'Ayrık yemeği ye',
    'map_10': 'Labirentin %%10\'unu haritala', 'maze_time': 'Gündüz labirentte 3 dakika geçir (saniye)',
    'kill_griever': 'Bir Kederci öldür', 'ben_fate': 'Sokulan Alby\'yi serumla iyileştir',
    'map_30': 'Labirentin %%30\'unu haritala', 'find_stones': '4 farklı bölümde Şifre Taşı bul',
    'talk_teresa': 'Teresa ile konuş', 'survive_nights': 'Kapısız 3 gece hayatta kal',
    'code_words': '8 şifre kelimesini çöz (Şifre Taşları)', 'remember': 'Bilerek sokul ve serumla Değişim\'i yaşa',
    'get_core': 'Bir Kederci Çekirdeği ele geçir', 'enter_hole': 'Uçurum\'daki Kederci Deliği\'ne atla', 'enter_code': 'Terminale şifreyi gir',
})

add('hud.labirent.', {
    'day': 'Gün %s', 'no_doors': '§8Kapılar artık kapanmıyor', 'doors_closed': '§cKapılar kapalı §7(açılmasına %s)',
    'doors_moving': '§6Kapılar hareket ediyor!', 'doors_open': '§aKapılar açık §7(kapanmasına %s)',
    'doors_soon': '§eKapılar kapanıyor: %s', 'box_in': 'Kutu: %s gün sonra', 'box_today': '§eKutu bugün geliyor',
    'box_never': '§8Kutu bir daha gelmeyecek', 'mapped': 'Haritalanan: %%%s', 'captain': '§6★ Kaptan',
})
add('legend.labirent.', {'path': 'Koridor', 'wall': 'Duvar', 'old': 'Eski bilgi', 'door': 'Kapı'})
add('screen.labirent.', {
    'map': 'Labirent Haritası', 'map.info': 'Haritalanan: %s  •  Sürükle: kaydır, Tekerlek: yakınlaştır',
    'map.sections': 'Bölümler & Şifre', 'map.hint': 'Bir bölümün en az %%35\'i haritalanınca o bölümün Şifre Taşı kelimesini verir. ↻ = dün gece değişti.',
    'captain': 'Kaptan Masası', 'captain.say': 'Emir yaz', 'captain.send': 'Duyur', 'captain.transfer': 'Kaptanlığı devret',
    'captain.not': 'Sadece kaptan emir verebilir.', 'captain.hint': 'Rol düğmesine tıkla: sıradaki göreve geçer. Tekerlek: listeyi kaydır.',
    'terminal': 'Şifre Terminali', 'terminal.hint': 'Kelimeleri labirent bölümlerinin sırasıyla gir (1→8).',
    'terminal.clear': 'Temizle', 'terminal.send': 'Gönder',
})
add('npc.labirent.', {'follow': 'Beni takip et', 'stay': 'Burada bekle', 'work': 'İşine dön', 'return': 'Ayrık\'a dön',
                      'home': 'Evin burası olsun', 'health': 'Can: %s / %s'})
add('order.labirent.', {
    'title': 'KAPTANIN EMRİ', '1': 'Herkes Ayrık\'a dönsün!', '2': 'Koşucular, labirente!', '3': 'Kapıları savunun!',
    '4': 'Herkes yanıma toplansın!', '5': 'İşinizin başına dönün.',
    'btn.1': 'Ayrık\'a dönün', 'btn.2': 'Koşucular, yola!', 'btn.3': 'Kapıları savunun', 'btn.4': 'Toplanın', 'btn.5': 'İşinize dönün',
})

add('title.labirent.', {
    'wake': '...', 'wake.sub': 'Karanlık. Metalin gıcırtısı. Sadece adını hatırlıyorsun.',
    'box': 'KUTU GELİYOR', 'box.sub': 'Alarm çalıyor! Herkes Kutu\'nun başına!',
    'captain': '★ KAPTAN', 'captain.sub': 'Kutu\'daki not: "Bu kişi sizin yeni lideriniz."',
    'greenie': 'ÇAYLAK', 'greenie.sub': 'Ayrık\'a hoş geldin, %s.',
    'trapped': 'KAPILAR KAPANDI', 'trapped.sub': 'Labirentte kaldın. Kimse bir geceyi orada geçirip yaşamadı...',
    'sun_gone': 'GÜNEŞ SÖNDÜ', 'sun_gone.sub': 'Gökyüzü gri bir boşluk. Kapılar bu gece kapanmayacak.',
    'last_one': 'SONUNCUSU', 'last_one.sub': 'Kutu\'dan bir kız çıktı... elinde bir not.',
    'remember': 'HATIRLIYORSUN', 'word': '%s. bölümün kelimesi', 'hole': 'KEDERCİ DELİĞİ', 'hole.sub': 'Gökyüzünden düştün... karanlık bir yuvaya.',
    'code_ok': 'ŞİFRE KABUL EDİLDİ', 'code_ok.sub': 'Bütün Kederciler aynı anda çöktü. Kapı açılıyor!',
    'escaped': 'KAÇTINIZ', 'escaped.sub': 'Labirent: Ölümcül Kaçış — Son', 'new_role': 'YENİ GÖREV',
})

add('msg.labirent.', {
    'building': 'Labirent inşa ediliyor...', 'building_pct': 'Labirent inşa ediliyor... %%%s',
    'built': '§aLabirent tamamlandı. Duvarlar yükseldi.', 'waiting_box': 'Kutu seni almaya geliyor...',
    'pulled_out': '§7Ayrıklılar çaylak §f%s§7\'i Kutu\'dan çekip çıkardı.', 'box_arrived': '§eKutu geldi! İçinden §f%s§e çıktı. Erzakları Kutu\'dan almayı unutmayın.',
    'no_build': '§cLabirentte inşa edemezsin. Duvarlar her şeyi yutar.', 'no_break': '§cBu taşa ne yaparsan yap iz bile bırakamıyorsun.',
    'no_climb': '§7Sarmaşıklar seni tutmuyor. Duvarların tepesi yok gibi... aşağı kaydın.',
    'not_captain': 'Bunu sadece kaptan yapabilir.', 'role_set': '%s artık: %s', 'new_captain': '★ Yeni kaptan: %s',
    'shift': '§7Uzaktan taş gıcırtısı... Labirentin %s. bölümleri değişiyor.',
    'sleeping': '%s uyuyor.', 'serum_waste': 'Zehirlenmedin; serumu israf etme.', 'cured_other': '%s serumla kurtarıldı.',
    'pan_nothing': 'Tavada pişecek bir şey yok.', 'pan_cooked': '%s yemek pişti!',
    'hole_blocked': '§5Görünmez bir yüzey seni itiyor... Kederciler buradan nasıl geçiyor?',
    'venom_death': '§2Zehir kanına karıştı. Serum olmadan Değişim\'i atlatamadın...',
    'glader_died': '☠ %s öldü. Ölüler Ormanı bir isim daha kazandı.',
    'journal_updated': 'Ayrık Günlüğü güncellendi. (J)',
    'survived': 'Kapısız bir gece daha atlatıldı! (%s/3)',
    'teresa_note': 'Not: "O sonuncusu. Ebediyen."',
    'event.ben': '☠ Alby labirentten sendeleyerek döndü. Kederci onu sokmuş! Serum, çabuk!',
    'corpse_empty': '§7Bu cesette sökülecek bir şey kalmadı.', 'corpse_stripped': '§aCesetten parçaları söktün.',
    'tripped': '§7Bir taşa takılıp yüzüstü düştün!', 'slipped': '§7Islak yosunda kaydın!',
    'box_supplies': '§eKutu geldi! Bu ay sadece erzak var. Kutu\'dan almayı unutmayın.',
    'event.walls': 'Koşucular haritaları üst üste koydu: her gece değişen duvarlar bir desen oluşturuyor...',
    'event.code': 'Teresa: "Labirent bir şifre. Her bölümün taşında bir kelime var. Önce o bölümü iyi haritalayın."',
    'event.hole': 'Hatırladın: Kederciler Uçurum\'daki görünmez bir delikten gelip gidiyor. Bir Çekirdek taşıyan geçebilir!',
    'stone_strange': '§7Garip bir taş (%s. bölüm). Üzerinde silik harfler var ama anlamı yok... şimdilik.',
    'stone_unmapped': '§eBu taşın kelimesini çözmek için %s. bölümü daha iyi haritalamalısın (şu an %%%s, en az %%35).',
    'stone_known': '%s. bölümün kelimesi zaten biliniyor: %s',
    'code_missing': 'TERMİNAL: EKSİK ŞİFRE. Sekiz kelimenin hepsi gerekli.', 'code_wrong': 'TERMİNAL: HATALI SIRA. Kelimeleri bölüm sırasıyla gir.',
    'maproom': 'Harita Odası: labirentin %%%s\'i haritalandı. Bulunan Şifre Taşı: %s/8, çözülen kelime: %s/8.',
    'howto': 'İpucu: M = harita, J = günlük, K = Ayrıklılar/kaptan ekranı, N = mini harita. Kapılar akşam kapanır, gece duvarlar değişir!',
    'status': 'Kaptan: %s • Haritalanan: %%%s • Şifre kelimesi: %s/8 • Kutu gelişleri: %s',
    'bad_role': 'Bilinmeyen rol.', 'reset': 'Labirent sıfırlandı; bir sonraki tikte yeniden inşa edilecek.',
    'after_ending': 'Hikaye bitti ama Ayrık hâlâ orada. Dönmek için öl ya da /labirent git. Yeni bir oyun için: /labirent sifirla',
})

add('ending.labirent.', {
    '1': 'Kapı aralandı ve beyaz bir ışık koridoru doldurdu. Arkanızda labirent ilk kez sessizdi.',
    '2': 'Koridorun sonunda camların ardından sizi izleyen insanlar vardı. Notlar alıyorlardı.',
    '3': 'Biri alkışladı. "Tebrikler," dedi. "Deneklerin çoğu bu kadar ileri gidemez."',
    '4': 'Kimse adını söylemedi. Duvardaki plakada yalnızca tek bir kelime vardı: WICKED.',
    '5': 'Labirentten kaçtınız. Ama oyunun sadece ilk aşaması bittiğini, içinizde bir yer biliyordu.',
})

add('memory.labirent.', {
    '1.short': 'Beyaz bir oda, bir ekran, bir isim...', '1': 'Bir anı: beyaz önlüklü insanlar, ekranlarda labirentin planı. Sen de oradaydın. Sen de izliyordun.',
    '2.short': 'Bir kız sesi: "Bunu yapmak zorundayız."', '2': 'Bir anı: Teresa yanındaydı. "Bunu yapmak zorundayız," diyordu. "Dünya için."',
    '3.short': 'Kederciler... bir delikten.', '3': 'Bir anı: Kederciler Uçurum\'un yanındaki görünmez bir delikten geliyor. Sadece bir Çekirdek taşıyan geçebiliyor.',
    '4.short': 'Şifre: bölüm sırası.', '4': 'Bir anı: terminal. Kelimeler bölümlerin sırasıyla girilmeli. Bir\'den Sekiz\'e.',
})

add('talk.labirent.', {
    'ack.0': 'Anlaşıldı, kaptan.', 'ack.1': 'Tamamdır, hemen.', 'ack.2': 'Peki peki, gidiyorum.', 'ack.3': 'Emredersin.',
    'runner_back': 'Döndüm! Labirentin %%%s\'i artık haritada. Ayaklarım koptu.',
    'serve': 'Al bakalım, sıcak sıcak: %s. Afiyet olsun.', 'serve_last': 'Kiler boş ama sana bir ekmek buldum. Bahçıvanlara söyle, biraz acele etsinler.',
    'stung.0': 'Sıcak... her yer çok sıcak... seruuum...', 'stung.1': 'Duvarlar... duvarlar konuşuyor...', 'stung.2': 'Uzak dur benden! Ne yaptığımı bilmiyorum!',
    'newcomer.0': 'Neredeyim? Siz kimsiniz?', 'newcomer.1': 'Hiçbir şey hatırlamıyorum... sadece adımı.', 'newcomer.2': 'Burası neresi? Bırakın gideyim!',
    'event.runners': 'Koşucular, toplanın! Kapılar açıldığı an koşuyoruz, kapanmadan önce dönüyoruz. Kural bu.',
    'event.doors': 'Bak... kapılar kapanıyor. Her akşam böyle. İçeride kalırsan sabahı göremezsin.',
    'event.welcome_captain': 'Kutu\'da bir not vardı: "Bu kişi sizin yeni lideriniz." Alby bundan hiç hoşlanmadı, Gally hiç... Ama şimdilik sana uyacağız, Kaptan. Bizi hayal kırıklığına uğratma.',
    'event.welcome': 'Bir çaylak daha! Hoş geldin. Kaptan sana bir iş verecek; o zamana kadar ayak altında dolaşma.',
})

ROLE_LINES = {
    'serbest': ['Bana da bir iş verseler...', 'Duvarların ardında ne var dersin?', 'Bugün de kimse kaçamadı.', 'Gece duvarların sesini duyuyor musun?'],
    'kosucu': ['Labirent koşmak için yapılmış. Ya koşarsın ya ölürsün.', 'Kapanmadan dönmek zorundayız, unutma.',
               'Bugün yedinci bölümde yeni bir koridor buldum.', 'Haritaları karşılaştırmamız lazım, bir desen var.'],
    'asci': ['Karnın aç mı? Söyle, bir şeyler çıkarırım.', 'Bu tava bir Kederci\'yi bile bayıltır.', 'Erzak yine azaldı. Kutu\'yu bekliyoruz.',
             'Yemeğimi beğenmeyen Hapishane\'ye!'],
    'bahcivan': ['Toprak iyi, ama güneş... güneş tuhaf.', 'Domates yetiştirmeye çalışıyorum, bekle gör.', 'Sulama kanalını kimse bozmasın!',
                 'Bahçe Ayrık\'ın kalbi.'],
    'kasap': ['Kan Evi\'ne pek kimse gelmez. Ben severim.', 'İnekler bugün huzursuz. Bir şey seziyorlar.', 'Et lazımsa söyle.',
              'Hayvanlar bizden önce duyuyor duvarları.'],
    'sifaci': ['Yaran mı var? Gel bakayım.', 'Serumu boşa harcama. Sadece sokulana.', 'Değişim\'den geçenler bir daha aynı olmuyor.',
               'Sargı bezi bitmek üzere.'],
    'bekci': ['Kapılar benim sorumluluğumda.', 'Gece bir şey yaklaşırsa önce benimle karşılaşır.', 'Mızrağını hep yanında taşı.',
              'Bu duvarlar bizi içeride tutmuyor; onları dışarıda tutuyor.'],
}
for role, lines in ROLE_LINES.items():
    for i, l in enumerate(lines):
        L[f'talk.labirent.{role}.{i}'] = l

STORY = {
    'newt': ['Ayrık\'a hoş geldin. Ben Newt. Burada üç kural var: herkes çalışır, kimse kimseye zarar vermez ve Koşucu değilsen Labirent\'e girmezsin.',
             'Görev dağıtmak senin işin artık. Bir Koşucu, bir de Aşçı seç; gerisi gelir.',
             'Haritaları Harita Odası\'nda saklıyoruz. Her bölüm için bir sandık. Koşucular her akşam yeni çizimleri bırakır.',
             'Ben\'in hâli... Daha önce de gördük. Serum varsa hemen kullanın. Yoksa... yoksa onu kaybederiz.',
             'Duvarlar sadece rastgele değişmiyor. Bunu hissediyorum. Bir şey anlatmaya çalışıyorlar.',
             'Gökyüzü gri. Kapılar açık. Bu gece hepimiz uyanık kalıyoruz, Kaptan. Bekçileri kapılara diz.',
             'Bütün kelimeleri toplarsak... belki bu cehennemden çıkış yolunu açarız.',
             'Uçurum\'a mı gideceğiz? Delirdin mi? ...Peki. Seninle geliyorum.',
             'Başardık. Yine de... içimde bir şey bitmediğini söylüyor.'],
    'minho': ['Yeni misin? Ben Minho, Koşucuların sorumlusu. Labirent\'i benden iyi bilen yok.',
              'Koşucu seçeceksen hızlı birini seç. Yavaş Koşucu, ölü Koşucu demek.',
              'Kapılar açılınca beni takip et. Gün batmadan geri dönmezsek kapılar bizi beklemez.',
              'Bir Kederci gördüm. Gündüz! Bu daha önce hiç olmamıştı.',
              'Haritaları üst üste koyunca harfler çıkıyor. Ciddiyim. Bölümler bir şey yazıyor.',
              'Artık Labirent\'te değil, Ayrık\'ta da güvende değiliz. İyi. En azından sıkılmayacağız.',
              'Şifre Taşları her bölümün en ücra köşesinde. Haritalanmamış bölümde taşın yazısı okunmuyor.',
              'Delikten atlamak mı? Hep yapmak istemiştim zaten.',
              'Koşmaya devam. Her zaman koşmaya devam.'],
    'frypan': ['Ben Tavacı, Ayrık\'ın aşçısı. Burada kimse aç yatmaz, söz.',
               'Mutfak sandığına çiğ et ve sebze bırakın, gerisini ben hallederim.',
               'Koşuculara fazladan porsiyon. Onlar bütün gün koşuyor.',
               'Ben\'e çorba götürdüm. Yemedi. Gözleri... gözleri başkaydı.',
               'Erzaklar her ay geliyor ama bu sefer bir şey eksikti. Kimse fark etmedi mi?',
               'Kutu artık gelmeyecekse yemeği idareli kullanmamız lazım. Bahçıvanları çalıştır, Kaptan.',
               'Ben aşçıyım, şifre çözücü değil. Ama size güveniyorum.',
               'Gidiyorsak tavamı da alıyorum. Kederci kafası bu tavaya tam oturur.',
               'Bir dahaki yemeği dışarıda pişireceğim. Gerçek bir gökyüzünün altında.'],
    'chuck': ['Selam! Ben Chuck. Senden önceki çaylak bendim, artık sen çaylaksın! Hehe.',
              'Bir iş verdiler bana: tuvalet temizliği. Kaptan, lütfen bana başka bir iş ver!',
              'Koşucular çok havalı. Bir gün ben de koşacağım. Belki.',
              'Kederciler... onlar gerçekten var mı? Hiç görmedim. Görmek de istemiyorum.',
              'Annemi hatırlamaya çalışıyorum. Yüzü yok. Sadece sesi var.',
              'Korkuyorum. Ama yanında olacağım. Söz.',
              'Sence kurtulunca ailemi bulabilir miyim?',
              'Deliğe ben de geliyorum! Beni bırakma.',
              'Başardık mı? Gerçekten başardık mı?'],
    'gally': ['Sen de kimsin? Kutu\'dan bir not çıktı diye herkes önünde eğilecek değil.',
              'Bekçiler kapıları korur. Ben de seni izliyorum, "Kaptan".',
              'Labirent\'e giren her Koşucu bir gün dönmeyecek. Bunu unutma.',
              'Sen geldiğinden beri her şey bozuldu. Kederciler gündüz çıkıyor!',
              'Duvarlar değişiyorsa bunu yapan biri var. Ve sen o tarafı hatırlıyor gibisin.',
              'Bu gece kapıda ben duracağım. Ama bu sana güvendiğim anlamına gelmiyor.',
              'Şifre ha? Ya bu da onların oyunuysa?',
              'Delikten geçen geri dönmez. Ben burada kalıyorum.',
              'Belki de haklıydın. Belki.'],
    'alby': ['Ben Alby. Eskiden lider bendim. Not seni seçmiş... Ben de seni izleyeceğim.',
             'Herkese bir iş ver. Boş duran düşünür, düşünen delirir.',
             'Koşucuları akşamdan önce say. Biri eksikse kapıda ben beklerim.',
             '...ateş... Labirentte bir şey gördüm... seni... Değişim\'de senin yüzünü gördüm...',
             'Değişim\'den geçtim. Hatırladıklarımı anlatamıyorum; boğazım düğümleniyor.',
             'Kapılar açık kaldı. Bu gece kimse uyumayacak. Yaralıları bana getirin.',
             'Kelimeleri çözün. Ben gördüklerimi anlatamam ama şifre gerçek.', 'Git. Ben burada sizi kollarım.',
             'Ayrık seninle gurur duyar.'],
    'winston': ['Ben Winston, Kan Evi\'nden sorumluyum. Hayvanlara iyi bak, onlar da sana bakar.',
                'Kasap lazım mı? Benden iyisini bulamazsın.', 'Koşucular et yiyor, bahçıvanlar sebze. Denge.', 'Hayvanlar geceleri susuyor artık.',
                'Kutu her ay iki inek daha getirse ne güzel olurdu.', 'Kapılar açık... Kan Evi\'nin çitini güçlendirdim ama ne kadar dayanır?',
                'Şifre işi bana göre değil.', 'Ben hayvanlarla kalıyorum.', 'Dışarıda da inek var mıdır?'],
    'clint': ['Ben Clint, şifacıyım. Kanaman varsa sargı, sokulduysan serum.', 'Serumlar Ev\'in üst katındaki sandıkta. Sadece acil durumda.',
              'Koşucular her akşam yara bere dönüyor. İşim bitmiyor.', 'Ben\'i kurtarabiliriz. Serum getirin, hemen!',
              'Değişim\'den geçen biri bana annesini anlattı. Sonra unuttu.', 'Bu gece çok yaralı olacak. Sargı bezi stoklayın.',
              'Bilerek sokulmak mı? Bu çılgınlık. Ama hatırlamanın tek yolu bu olabilir.', 'Yanınıza serum alın. Fazlasını.',
              'Kimseyi kaybetmedik mi? Bir mucize.'],
    'ben': ['Ben Ben, Koşucuyum. Minho\'dan sonra en hızlısı benim.', 'Labirent\'te bir şey var. Bizi izliyor.',
            'Bugün çok uzağa gideceğim. Haritayı genişletmem lazım.', 'Sen... SEN! Seni gördüm! Değişim\'de seni gördüm, sen onlardansın!',
            'Teşekkürler... serum... hayatımı kurtardın.', 'Artık koşamıyorum. Ama kapıda durabilirim.', 'Gördüklerimi anlatamıyorum. Ama şifre gerçek.',
            'Deliğe gidin. Ben arkanızı kollarım.', 'Hep söylemiştim: koşmaya devam.'],
    'zart': ['Zart. Bahçıvanların başı. Ellerin toprakla kirlenmeyecekse buraya gelme.', 'Bahçede iş çok. Bir bahçıvan daha lazım.',
             'Mısır güzel bitti bu ay.', 'Toprak titriyor geceleri. Duvarlardan.', 'Bitkiler doğru yöne büyümüyor. Işık değişti.',
             'Güneş yok ama bitkiler hâlâ büyüyor. Nasıl? Bilmiyorum.', 'Ben ekinlere bakarım, siz şifreye.', 'Tohumları da alın yanınıza.',
             'Dışarıda tarla var mıdır acaba?'],
    'teresa': ['...', '...', '...', '...', '...', 'Seni... seni tanıyorum. Nereden bilmiyorum ama tanıyorum. Labirent bir şifre. Hatırlıyorum.',
               'Her bölümün taşında bir kelime var. Sırayla birleştirirsek... bir kapı açılacak.',
               'Delik Uçurum\'da. Kederciler oradan geçiyor. Bir Çekirdek lazım.', 'Başardık. Ama bu son değil, biliyorsun değil mi?'],
}
for who, lines in STORY.items():
    for i, l in enumerate(lines):
        L[f'talk.labirent.{who}.{i}'] = l

import dialogue
JAVA_TABLE = dialogue.build(L)
with open(os.path.join(os.path.dirname(__file__), '..', 'src/main/java/com/krolasyon/labirent/entity/ai/DialogueTable.java'), 'w', encoding='utf-8') as f:
    f.write(JAVA_TABLE)

for i, l in enumerate(['Ah! Kahretsin!', 'Ayağım takıldı!', 'Of... iyiyim, iyiyim!', 'Kim koydu bu taşı buraya?!']):
    L[f'chat.labirent.trip.{i}'] = l

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src/main/resources/assets/labirent/lang')
os.makedirs(ROOT, exist_ok=True)
for lang in ('tr_tr', 'en_us'):
    with open(os.path.join(ROOT, f'{lang}.json'), 'w', encoding='utf-8') as f:
        json.dump(L, f, ensure_ascii=False, indent=1, sort_keys=True)
print(len(L), 'keys')
