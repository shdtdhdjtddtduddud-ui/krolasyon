package com.krolasyon.futbol.game;

/**
 * Every football move. {@code impact} is the animation tick at which the ball effect happens,
 * {@code lock} the time the actor is busy (= animation length), {@code cooldown} only used by super moves.
 */
public enum Move {
    // ---- passes
    SHORT_PASS(Cat.PASS, "Kısa Pas", "Yakındaki takım arkadaşına yerden hızlı pas.", true, 4, 10),
    THROUGH_PASS(Cat.PASS, "Ara Pas", "Koşu yolundaki arkadaşının önüne, savunma arkasına pas.", true, 5, 12),
    LOB_PASS(Cat.PASS, "Havadan Pas", "Topu yüksekten uzaktaki arkadaşına aşırtır.", true, 6, 14),
    BACKHEEL_PASS(Cat.PASS, "Topuk Pası", "Arkandaki arkadaşına bakmadan topukla pas.", true, 5, 12),
    RABONA_PASS(Cat.PASS, "Rabona Pas", "Bacaklarını çaprazlayıp şık bir rabona pası.", true, 7, 16),
    NO_LOOK_PASS(Cat.PASS, "Bakmadan Pas", "Bir yere bakıp başka yere pas — savunmayı kandırır.", true, 5, 14),
    CROSS(Cat.PASS, "Orta", "Kanattan ceza sahasına falsolu yüksek orta.", true, 7, 16),
    // ---- shots
    SHOT(Cat.SHOT, "Şut", "Tuşu basılı tutup güç topla, bırakınca vur.", true, 6, 14),
    FINESSE(Cat.SHOT, "Plase Şut", "Ayak içiyle falsolu, köşeye kıvrılan şut.", true, 7, 16),
    POWER_SHOT(Cat.SHOT, "Sert Şut", "Adım alıp füze gibi sert vuruş.", true, 9, 20),
    RABONA_SHOT(Cat.SHOT, "Rabona Şut", "Destek bacağının arkasından rabona vuruşu.", true, 8, 18),
    CHIP(Cat.SHOT, "Aşırtma", "Kalecinin üstünden yumuşak aşırtma.", true, 6, 14),
    BICYCLE(Cat.SHOT, "Röveşata", "Havaya sıçrayıp ters dönerek topu arkana vur.", true, 9, 26),
    VOLLEY(Cat.SHOT, "Vole", "Havadaki topa yere düşmeden vuruş.", true, 5, 14),
    HEADER(Cat.SHOT, "Kafa Vuruşu", "Sıçrayıp topu kafayla yönlendir.", true, 6, 16),
    KNUCKLE(Cat.SHOT, "Dipten Vuruş", "Dönmeyen, havada titreyerek giden knuckleball.", true, 7, 16),
    SCORPION(Cat.SHOT, "Akrep Vuruşu", "Öne atılıp topuğunla sırtının üstünden vur.", true, 9, 24),
    PANENKA(Cat.SHOT, "Panenka", "Kalecinin ortasından yavaş ve yumuşak dokunuş.", true, 6, 14),
    TRIVELA(Cat.SHOT, "Trivela", "Dış ayakla ters tarafa kıvrılan şut.", true, 7, 16),
    // ---- dribbling / skills
    RAINBOW(Cat.SKILL, "Gökkuşağı", "Topu topukla arkadan kafanın üstünden öne aşırt.", true, 7, 20),
    STEPOVER(Cat.SKILL, "Makas", "Topun üstünden bacak geçirip yön değiştir.", true, 10, 20),
    ELASTICO(Cat.SKILL, "Elastico", "Topu dışa itip anında içe çek — lastik gibi.", true, 8, 16),
    ROULETTE(Cat.SKILL, "Maradona Dönüşü", "Topun üstünde 360° dönerek rakipten sıyrıl.", true, 8, 18),
    CRUYFF(Cat.SKILL, "Cruyff Dönüşü", "Şut çeker gibi yapıp topu bacağının arkasından çevir.", true, 8, 16),
    NUTMEG(Cat.SKILL, "Bacak Arası", "Topu rakibin bacak arasından geçir.", true, 5, 12),
    DRAGBACK(Cat.SKILL, "Taban Çekme", "Topu tabanla geri çekip savunmacıyı boşa düşür.", true, 5, 12),
    SEAL(Cat.SKILL, "Fok Dribblingi", "Topu kafanda sektirerek koş!", true, 6, 18),
    JUGGLE(Cat.SKILL, "Top Sektirme", "Topu ayaklarında havada tutarak şov yap.", true, 4, 40),
    SOMBRERO(Cat.SKILL, "Şapka Çıkarma", "Topu rakibin kafasının üstünden aşırt.", true, 6, 14),
    SPEED_BURST(Cat.SKILL, "Hız Patlaması", "Topu ileri vurup depar at.", false, 4, 12),
    FIRST_TOUCH(Cat.SKILL, "İlk Dokunuş", "Gelen topu ayağının altında öldür.", true, 3, 10),
    // ---- defence
    TACKLE(Cat.DEFENSE, "Top Çalma", "Rakipten topu kapmak için ayakla müdahale.", false, 5, 14),
    SLIDE(Cat.DEFENSE, "Kayarak Müdahale", "Yerde kayarak topu uzaklaştır.", false, 4, 22),
    SHOULDER(Cat.DEFENSE, "Omuz Omuza", "Omuz vuruşuyla rakibi dengesizleştir.", false, 4, 12),
    BLOCK(Cat.DEFENSE, "Şut Bloğu", "Sıçrayıp gelen şutu gövdenle blokla.", false, 4, 16),
    // ---- goalkeeper
    DIVE(Cat.KEEPER, "Plonjon", "Kaleci gibi yana uçarak topu kurtar.", false, 5, 26),
    PUNCH(Cat.KEEPER, "Yumruklama", "Havadaki topu yumrukla uzaklaştır.", false, 5, 14),
    KEEPER_THROW(Cat.KEEPER, "Kaleci Atışı", "Topu elle hızlıca arkadaşına fırlat.", true, 8, 18),
    // ---- super abilities
    FIRE_SHOT(Cat.SUPER, "Ateş Şutu", "Alevler içinde yanan füze şut!", true, 12, 28, 400),
    LIGHTNING_SHOT(Cat.SUPER, "Yıldırım Şutu", "Gökten yıldırım düşer, top zikzak çizer!", true, 12, 28, 500),
    TORNADO_SHOT(Cat.SUPER, "Kasırga Şutu", "Kendi etrafında dönüp spiral çizen kasırga topu.", true, 10, 26, 400),
    EAGLE_SHOT(Cat.SUPER, "Kartal Pikesi", "Top göğe yükselir ve kaleye pike yapar.", true, 9, 24, 500),
    GHOST_DRIBBLE(Cat.SUPER, "Hayalet Dribbling", "4 saniye hayalet gibi hızlan, müdahaleler işlemez.", false, 4, 14, 600),
    ICE_PASS(Cat.SUPER, "Buz Pası", "Yolundaki rakipleri donduran buzlu pas.", true, 6, 16, 400),
    // ---- celebrations
    SIUU(Cat.CELEBRATION, "Siuuu!", "Zıpla, havada dön ve yere bas!", false, 0, 30),
    KNEE_SLIDE(Cat.CELEBRATION, "Diz Kayması", "Dizlerinin üstünde kayarak kutla.", false, 0, 34),
    AIRPLANE(Cat.CELEBRATION, "Uçak", "Kollarını açıp uçak gibi süzül.", false, 0, 40),
    BACKFLIP(Cat.CELEBRATION, "Ters Takla", "Havada ters takla at.", false, 0, 20),
    DANCE(Cat.CELEBRATION, "Zafer Dansı", "Kornerde zafer dansı.", false, 0, 40);

    public enum Cat {
        PASS("Paslar", 0x43A047), SHOT("Şutlar", 0xE53935), SKILL("Çalımlar", 0xFFB300), DEFENSE("Savunma", 0x5E35B1),
        KEEPER("Kaleci", 0x00897B), SUPER("Süper Yetenekler", 0xFF4081), CELEBRATION("Gol Sevinçleri", 0x1E88E5);

        public final String title;
        public final int color;

        Cat(String title, int color) {
            this.title = title;
            this.color = color;
        }
    }

    public final Cat cat;
    public final String title;
    public final String desc;
    public final boolean needsBall;
    public final int impact;
    public final int lock;
    public final int cooldown;

    Move(Cat cat, String title, String desc, boolean needsBall, int impact, int lock) {
        this(cat, title, desc, needsBall, impact, lock, 0);
    }

    Move(Cat cat, String title, String desc, boolean needsBall, int impact, int lock, int cooldown) {
        this.cat = cat;
        this.title = title;
        this.desc = desc;
        this.needsBall = needsBall;
        this.impact = impact;
        this.lock = lock;
        this.cooldown = cooldown;
    }

    public boolean isSuper() { return cat == Cat.SUPER; }

    public static Move byId(int id) {
        Move[] v = values();
        return id >= 0 && id < v.length ? v[id] : null;
    }

    public static Move parse(String s) {
        for (Move m : values()) {
            if (m.name().equalsIgnoreCase(s)) return m;
        }
        return null;
    }
}
