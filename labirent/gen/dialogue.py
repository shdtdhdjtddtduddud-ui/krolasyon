"""Personality-driven dialogue for the Gladers. Each topic holds scripts; a script is a list of lines said in a row."""

CHAT = {
    'newt': {
        'idle': [["Bazen duvarlara bakıp öbür tarafta ne olduğunu düşünüyorum.", "Sonra kendime kızıyorum. Burada yapacak çok iş var."],
                 ["Bahçeler iyi gidiyor. Domatesler bu ay kızardı bile.", "Kutu her şeyi getirmiyor, bilirsin. Gerisini toprak verecek."],
                 ["Herkes sana bakıyor, Kaptan. Fark ettin mi?", "Korkma. Ben arkandayım."],
                 ["Gally'ye aldırma. Korkunca bağırır, o kadar.", "Hepimiz korkuyoruz aslında."],
                 ["Chuck'ı gördün mü? Yine mutfaktan bir şeyler aşırıyordu.", "Bırak, mutlu olsun. Burada mutlu olmak zor."]],
        'stung': [["Seni sokmuşlar! Alby, serum! Çabuk!"]],
        'dusk': [["Kapılar kapanıyor! Hemen içeri gir, ne yapıyorsun?!"], ["Güneş batıyor! Koş, koş, koş!"]],
        'lowhp': [["Hey, iyi görünmüyorsun.", "Otur biraz, nefes al. Alby'ye bir göster kendini."]],
        'hungry': [["Karnın guruldadı, buradan duydum.", "Chuck'a söyle, mutfakta bir şeyler kalmıştır."]],
        'back': [["Döndün! Şükür.", "Haritaya ne ekledin? Anlat bakalım."], ["Labirent seni yutmadı, iyi.", "Bir dahakine bu kadar geç kalma, yüreğim ağzıma geldi."]],
        'kill': [["Bir Kederci'yi indirdin, öyle mi?", "Kimse bunu bu kadar çabuk yapmamıştı. Ama dikkatli ol."]],
        'night': [["İyi geceler, Kaptan. Yarın yine buradayız.", "Hep buradayız zaten."]],
        'help': [["Ona dokunma! Herkes buraya!"], ["Arkandayım, Kaptan!"]],
        'flee': [["Geri çekil, geri çekil!"]],
        'ack': [["Tamamdır, Kaptan."], ["Senin dediğin olsun."], ["Hemen hallediyorum."]],
        'grumble': [["Peki, peki."]],
    },
    'minho': {
        'idle': [["Bugün yedinci bölümde bir koridor gördüm, dün orada duvar vardı.", "Bu yer bizimle dalga geçiyor."],
                 ["Koşmak düşünmekten iyidir. Düşünürsen korkarsın.", "Korkarsan yavaşlarsın. Yavaşlarsan..."],
                 ["Bir gün beraber koşalım, Kaptan. Bakalım bacakların ne kadar dayanıyor.", "Şaka şaka. Yetişemezsin."],
                 ["Gally kapıyı korur, ben labirenti. Hangimiz daha delisiz sence?", "Cevap verme, biliyorum."],
                 ["Ayaklarım hâlâ zonkluyor ama yarın yine gideceğim.", "Başka ne yapacağız ki?"]],
        'stung': [["Sokulmuşsun! Yürü, Alby'ye, ŞİMDİ!"]],
        'dusk': [["Kapılar kapanıyor, ölmek mi istiyorsun? KOŞ!"], ["Bu taraftan! Çabuk ol, yetişemezsek bitti!"]],
        'lowhp': [["Berbat görünüyorsun, dostum.", "Bir Kederci'yle dans mı ettin?"]],
        'hungry': [["Koşucu aç karnına koşmaz. Git bir şey ye."]],
        'back': [["Hah, döndün! Ben de iddiaya girmiştim.", "Kaybettim tabii. Neyse."], ["Ne kadarını haritaladın? Benimkinden fazla değildir."]],
        'kill': [["Bir Kederci mi? Yalnız başına mı?", "Tamam, kabul. Etkilendim. Biraz."]],
        'night': [["Yarın şafakta kapıda ol. Geç kalırsan seni beklemem."]],
        'help': [["Çekil önünden! Bu benim!"], ["Hadi gel bakalım, metal yığını!"]],
        'flee': [["Geri çekil! Ben tutuyorum!"]],
        'ack': [["Anlaşıldı, patron."], ["Zaten yapacaktım."], ["Hızlı ve sessiz, her zamanki gibi."]],
        'grumble': [["Hıh. Tamam."]],
    },
    'alby': {
        'idle': [["Liderlik ağır bir yük. Bir zamanlar bendeydi.", "Şimdi sende. Taşıyabildiğini göster."],
                 ["Kurallar bizi hayatta tutuyor. Unutma bunu.", "Kimse kuralların üstünde değil. Sen bile."],
                 ["Yaraları sarıyorum artık. Hiç bu kadar çok kan görmemiştim.", "Ama kimseyi kaybetmeyeceğiz. Bu sefer değil."],
                 ["Gece kapıları dinliyorum. Bazen bir şey kapıya sürtünüyor sanki.", "Sonra susuyor."]],
        'stung': [["Kımıldama. Serumu yapıyorum.", "Biraz yanacak... Dayan."]],
        'dusk': [["Kapılar kapanıyor! İçeri, hemen!"]],
        'lowhp': [["Kanıyorsun. Otur.", "Şunu sarayım... Tamam. Bir dahakine bu kadar dikkatsiz olma."]],
        'hungry': [["Yemek yemeden çalışma. Bu bir emir. Benden sana."]],
        'back': [["Sağ salim döndün. İyi.", "Gördüklerini Harita Odası'na yaz. Hepsini."]],
        'kill': [["Bir Kederci öldürmüşsün.", "Gurur duy ama kendini kaybetme. Onlar binlerce."]],
        'night': [["Kapılar kapandı. Bu gece güvendeyiz. En azından öyle umuyorum."]],
        'help': [["Buraya! Hepiniz! Ona yardım edin!"], ["Ondan uzak dur, canavar!"]],
        'flee': [["Geri çekilin! Düzenli!"]],
        'ack': [["Emredersin."], ["Hallederim."]],
        'grumble': [["Pekâlâ."]],
    },
    'chuck': {
        'idle': [["Kaptan! Kaptan! Bugün mutfakta bir kaşık buldum, üzerinde yazı var!", "Okuyamadım ama. Paslıymış."],
                 ["Sence annem beni özlüyor mu? Yani... bir annem varsa.", "Neyse, boş ver! Yemek yemek ister misin?"],
                 ["Minho bana koşmayı öğretecekmiş. Dün de öyle dedi. Önceki gün de.", "Belki yarın!"],
                 ["Gally bana bağırdı yine. Ben sadece yanından geçiyordum!", "Senin yanında durayım, olur mu?"],
                 ["Seninle olmak güzel. Herkes ciddi burada.", "Sen de ciddisin ama... iyi bir ciddisin."],
                 ["Biliyor musun, gece duvarların sesini duyunca yorganın altına giriyorum.", "Şşş, kimseye söyleme!"]],
        'stung': [["Hayır hayır hayır! Seni soktular! Yardım edin!"]],
        'dusk': [["Kapılar kapanıyor! Lütfen içeri gel, lütfen!"]],
        'lowhp': [["Kanın akıyor! Alby! ALBY!"]],
        'hungry': [["Aç mısın? Al, sana sakladım bunu!", "Tavacı'nın tarifinden. Yani benim tarifim artık."]],
        'back': [["Geldin! Geldin! Çok korktum!", "Ne gördün? Kederci gördün mü? Büyük müydü?"]],
        'kill': [["Sen bir Kederci mi öldürdün?! Vay be!", "Sen bir kahramansın! Herkese anlatacağım!"]],
        'night': [["İyi geceler, Kaptan. Rüyanda annemi görürsen selam söyle.", "Şaka yaptım. Galiba."]],
        'help': [["Kaptan'a dokunma!"]],
        'flee': [["Kederci! Kederci var! Kaçın!"], ["İmdaaat! Kaptan, arkanda!"]],
        'ack': [["Hemen, Kaptan!"], ["Tamam tamam tamam!"], ["Yaparım! Bak nasıl yapıyorum!"]],
        'grumble': [["Peki..."]],
    },
    'gally': {
        'idle': [["Seni izliyorum. Her şey sen geldikten sonra değişti.", "Bunu herkes biliyor, sadece söylemiyorlar."],
                 ["Kapılar benim sorumluluğumda. Sen kendi işine bak."],
                 ["Kutu'daki o not... Kim yazdı onu? Neden sen?", "Cevabın yok, değil mi?"]],
        'grumble': [["Ne var? İşim var."], ["Sana bir şey söylemek zorunda değilim."], ["Kaptan... Hıh. Göreceğiz."]],
        'stung': [["Sokulmuşsun. Tabii ki."]],
        'dusk': [["Kapı kapanıyor. Dışarıda kalırsan seni kimse kurtarmaya gelmez."]],
        'lowhp': [["Kendine dikkat et. Ölü bir kaptan kimsenin işine yaramaz."]],
        'hungry': [["Git ye bir şey. Bayılırsan seni ben taşımam."]],
        'back': [["Döndün demek. Bir dahaki sefere bakalım."]],
        'kill': [["Bir Kederci öldürdün. Şans."], ["Fena değil... Ama bu bir şey değiştirmez."]],
        'night': [["Kapıdayım. Uyu. Sabah konuşuruz."]],
        'help': [["Çekil! Ben hallederim!"], ["Benim kapımda kimseyi alamazsın!"]],
        'flee': [["Geri çekilin!"]],
        'ack': [["Peki."]],
    },
    'teresa': {
        'idle': [["Bu yeri tanıyorum. Nereden bilmiyorum ama tanıyorum.", "Sen de tanıyorsun, değil mi?"],
                 ["Rüyamda sayılar görüyorum. Sekiz bölüm, sekiz kelime."]],
        'stung': [["Hayır! Serum, çabuk!"]],
        'dusk': [["İçeri gel, şimdi!"]],
        'lowhp': [["Yaralısın. Gel, bakayım."]],
        'hungry': [["Bir şeyler ye. Düşünmek için güce ihtiyacın var."]],
        'back': [["Şifre taşlarına baktın mı? Her şey onlarda."]],
        'kill': [["Onları durdurabiliyoruz demek ki."]],
        'night': [["İyi geceler. Hatırlamaya çalışacağım."]],
        'help': [["Yalnız değilsin!"]],
        'flee': [["Uzaklaş oradan!"]],
        'ack': [["Tamam."]],
        'grumble': [["Peki."]],
    },
}

# two-person exchanges between Gladers, overheard by players nearby
BANTER = [
    [('minho', "Gally, kapıya yine çivi mi çakıyordun? Kimin içeri gireceğini sanıyorsun?"), ('gally', "Sen koşmaya devam et, Minho. Konuşmak sana göre değil."),
     ('minho', "Ah, alındın mı? Ne hassas.")],
    [('newt', "Chuck, o ekmeği kimden aldın?"), ('chuck', "Ben... şey... yere düşmüştü! Gerçekten!"), ('newt', "Tabii. Yarısını Minho'ya ver.")],
    [('alby', "Newt, bu gece kapıda iki kişi daha istiyorum."), ('newt', "Gally zaten orada. Başka kimse istemez onun yanında olmayı."),
     ('alby', "O zaman ben dururum.")],
    [('chuck', "Minho, bugün bana koşmayı öğretecek misin?"), ('minho', "Bugün olmaz, ufaklık. Yarın."), ('chuck', "Hep yarın!")],
    [('gally', "Alby, o Kaptan'a gerçekten güveniyor musun?"), ('alby', "Güvenmek zorundayız, Gally. Başka seçeneğimiz yok."),
     ('gally', "Her zaman bir seçenek vardır.")],
    [('newt', "Minho, haritalarda bir desen var mı sence?"), ('minho', "Desen mi? Her gece değişen bir labirentte mi?"),
     ('newt', "Değişen şeyler bile bir kurala uyar.")],
    [('chuck', "Gally, korktuğun hiçbir şey yok mu?"), ('gally', "Var. Senin yemeklerin."), ('chuck', "Hey!")],
    [('teresa', "Hepiniz aynı rüyayı görüyor musunuz? Beyaz bir oda..."), ('newt', "Bazılarımız. Ama kimse konuşmak istemiyor."),
     ('teresa', "Belki de konuşmamız gerekiyor.")],
]

DEFAULT = {
    'idle': [["Bugün de duvarlar yerinde."]], 'stung': [["Seni soktular! Serum!"]], 'dusk': [["Kapılar kapanıyor!"]],
    'lowhp': [["İyi görünmüyorsun."]], 'hungry': [["Bir şeyler ye."]], 'back': [["Döndün!"]], 'kill': [["Bir Kederci mi öldürdün?"]],
    'night': [["İyi geceler."]], 'help': [["Yardıma geliyorum!"]], 'flee': [["Kaçın!"]], 'ack': [["Tamam."]], 'grumble': [["Peki."]],
}
TOPICS = list(DEFAULT.keys())


def build(L):
    """adds the lines to the language map and returns the Java source of DialogueTable"""
    table = {}
    for who in list(CHAT.keys()) + ['generic']:
        data = CHAT.get(who, {})
        for topic in TOPICS:
            scripts = data.get(topic) or DEFAULT[topic]
            counts = []
            for si, script in enumerate(scripts):
                for li, line in enumerate(script):
                    L[f'chat.labirent.{who}.{topic}.{si}.{li}'] = line
                counts.append(len(script))
            table[(who, topic)] = counts
    for bi, b in enumerate(BANTER):
        for li, (who, line) in enumerate(b):
            L[f'chat.labirent.banter.{bi}.{li}'] = line
    rows = ",\n".join(f'            Map.entry("{w}.{t}", new int[]{{{", ".join(map(str, c))}}})' for (w, t), c in table.items())
    banter = ",\n".join("            new String[]{" + ", ".join(f'"{w}"' for w, _ in b) + "}" for b in BANTER)
    return f"""// GENERATED by gen/lang.py - do not edit by hand
package com.krolasyon.labirent.entity.ai;

import java.util.Map;

/** How many dialogue scripts / lines exist per character and topic (the text itself is in the language file). */
public final class DialogueTable {{
    private DialogueTable() {{}}

    private static final Map<String, int[]> COUNTS = Map.ofEntries(
{rows});

    private static final String[][] BANTER = {{
{banter}}};

    private static int[] get(String who, String topic) {{
        int[] c = COUNTS.get(who + "." + topic);
        return c != null ? c : COUNTS.getOrDefault("generic." + topic, new int[0]);
    }}

    public static int scripts(String who, String topic) {{ return get(who, topic).length; }}

    public static int lines(String who, String topic, int script) {{
        int[] c = get(who, topic);
        return script >= 0 && script < c.length ? c[script] : 0;
    }}

    /** a banter script between these two characters (either order), or -1 */
    public static int banterWith(String a, String b, int rand) {{
        int found = 0;
        for (String[] s : BANTER) if (pair(s, a, b)) found++;
        if (found == 0) return -1;
        int pick = rand % found;
        for (int i = 0; i < BANTER.length; i++) if (pair(BANTER[i], a, b) && pick-- == 0) return i;
        return -1;
    }}

    private static boolean pair(String[] s, String a, String b) {{
        boolean ha = false, hb = false;
        for (String w : s) {{
            ha |= w.equals(a);
            hb |= w.equals(b);
        }}
        return ha && hb && !a.equals(b);
    }}

    public static String[] banterSpeakers(int i) {{ return BANTER[i]; }}
}}
"""
