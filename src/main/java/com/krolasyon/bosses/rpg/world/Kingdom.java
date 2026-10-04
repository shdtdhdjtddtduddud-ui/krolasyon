package com.krolasyon.bosses.rpg.world;

/** The ten realms of the world. Capitals are placed relative to the world spawn (Aldoria's capital sits on it). */
public enum Kingdom {
    ALDORIA("Aldoria Krallığı", "Solmere", Race.HUMAN, "Kral", "III. Aldric", 0, 0, 1300, 0xE0B040, false,
            "Işık ve kalkan! Aldoria, insan krallıklarının en eskisidir. Kölelik yasaktır, fakat sokaklarda açlık hüküm sürer."),
    SYLVARIEN("Sylvarien Elf Krallığı", "Lunareth", Race.ELF, "Kraliçe", "Aelindra Gümüşyaprak", 1600, -900, 1050, 0x60D080, false,
            "Bin yıllık ağaçların arasında yaşayan elfler insanlara pek güvenmez, ama yardım edeni asla unutmazlar."),
    KHAZDUR("Khazdur Cüce Krallığı", "Karak Dûm", Race.DWARF, "Dağ Kralı", "Durgrim Demirsakal", -1700, -1100, 1050, 0xC08040, false,
            "Dağların altındaki demir şehirler. Cüceler sözüne sadıktır; borcunu ödemeyeni de affetmez."),
    INFERNAX("Infernax İblis İmparatorluğu", "Kül Tahtı", Race.DEMON, "İblis Lordu", "Malakar", 300, 3300, 1100, 0xC02010, true,
            "Kül ve ateş toprakları. İblisler gücü yüceltir; zayıflar köle olur, güçlüler hükmeder."),
    YMIRHEIM("Ymirheim Devler Diyarı", "Buzhisar", Race.GIANT, "Jarl", "Hrothgar Kayayumruk", -200, -2900, 1100, 0x80B0E0, false,
            "Donuk zirvelerin devleri yavaş düşünür ama bir kez dost olursan dağ gibi arkanda dururlar."),
    GORMASH("Gormash Ork Hanlığı", "Kanlı Kaya", Race.ORC, "Han", "Gorthak Kemikkıran", -2700, 900, 1050, 0x8A3020, true,
            "Kızıl bozkırın orkları savaşla yaşar. Saygı kazanmanın tek yolu güç göstermektir."),
    FELARIS("Felaris Pençe Kabileleri", "Pençevadi", Race.BEASTKIN, "Sürü Annesi", "Rakşa Altınyele", 2800, 900, 1050, 0xE09030, false,
            "Hayvan-insanların özgür kabileleri. Doğaya saygı duyanı kardeş bilirler."),
    VALDREN("Valdren İmparatorluğu", "Karadoruk", Race.HUMAN, "İmparator", "Varek Kanmühür", -1300, 2100, 1050, 0x6A1A2A, true,
            "Aldoria'nın kadim rakibi. Disiplinli lejyonları ve köle pazarlarıyla ünlüdür."),
    MERIDIA("Meridia Tüccar Cumhuriyeti", "Altınliman", Race.HALFLING, "Büyük Konsül", "Pippin Altınkese", 1500, 2000, 1000, 0x40A0C0, true,
            "Altın konuşur, her şey satılıktır. Meridia'da tüccarlar loncaları, loncalar şehri yönetir."),
    NOCTHERA("Nocthera Kara Elf Hanedanı", "Gecegölge", Race.DARK_ELF, "Matriark", "Shaelith Gecegölge", 3000, -2500, 1050, 0x7040A0, true,
            "Ay ışığı görmeyen vadilerin kara elfleri. Entrika ve gölge büyüsünün ustalarıdır.");

    public static final int COUNT = values().length;

    public final String title, capital, rulerTitle, ruler, lore;
    public final Race race;
    public final int dx, dz, radius, color;
    public final boolean slavery;

    Kingdom(String title, String capital, Race race, String rulerTitle, String ruler, int dx, int dz, int radius, int color, boolean slavery, String lore) {
        this.title = title; this.capital = capital; this.race = race; this.rulerTitle = rulerTitle; this.ruler = ruler;
        this.dx = dx; this.dz = dz; this.radius = radius; this.color = color; this.slavery = slavery; this.lore = lore;
    }

    public static Kingdom of(int i) { return values()[Math.floorMod(i, COUNT)]; }

    /** the races that commonly live in this kingdom besides the main one (weights) */
    public Race[] citizens() {
        return switch (this) {
            case ALDORIA -> new Race[]{Race.HUMAN, Race.HUMAN, Race.HUMAN, Race.HUMAN, Race.HALFLING, Race.DWARF, Race.ELF, Race.BEASTKIN};
            case SYLVARIEN -> new Race[]{Race.ELF, Race.ELF, Race.ELF, Race.ELF, Race.HUMAN, Race.BEASTKIN};
            case KHAZDUR -> new Race[]{Race.DWARF, Race.DWARF, Race.DWARF, Race.DWARF, Race.HUMAN, Race.GIANT};
            case INFERNAX -> new Race[]{Race.DEMON, Race.DEMON, Race.DEMON, Race.DEMON, Race.ORC, Race.DARK_ELF, Race.HUMAN};
            case YMIRHEIM -> new Race[]{Race.GIANT, Race.GIANT, Race.GIANT, Race.HUMAN, Race.DWARF};
            case GORMASH -> new Race[]{Race.ORC, Race.ORC, Race.ORC, Race.ORC, Race.BEASTKIN, Race.HUMAN};
            case FELARIS -> new Race[]{Race.BEASTKIN, Race.BEASTKIN, Race.BEASTKIN, Race.BEASTKIN, Race.ELF, Race.HUMAN};
            case VALDREN -> new Race[]{Race.HUMAN, Race.HUMAN, Race.HUMAN, Race.HUMAN, Race.ORC, Race.DARK_ELF};
            case MERIDIA -> new Race[]{Race.HALFLING, Race.HALFLING, Race.HUMAN, Race.HUMAN, Race.DWARF, Race.ELF, Race.BEASTKIN, Race.ORC};
            case NOCTHERA -> new Race[]{Race.DARK_ELF, Race.DARK_ELF, Race.DARK_ELF, Race.DEMON, Race.HUMAN};
        };
    }
}
