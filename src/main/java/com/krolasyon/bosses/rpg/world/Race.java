package com.krolasyon.bosses.rpg.world;

import net.minecraft.util.RandomSource;

/** Playable world's peoples. Scale is applied by the NPC renderer, names come from per race pools. */
public enum Race {
    HUMAN("İnsan", 1.0F, 1.0F, 20,
            new String[]{"Aldric", "Bran", "Cedric", "Doran", "Edmund", "Gareth", "Hugo", "Ivan", "Jorah", "Kasım", "Leon", "Marek", "Osman", "Piran", "Rolf", "Selim", "Tomas", "Ulric", "Victor", "Yusuf", "Arda", "Emre", "Kerem", "Mert", "Baran", "Tarık"},
            new String[]{"Ada", "Beatris", "Ceren", "Elira", "Fiona", "Gül", "Helena", "Isolde", "Juna", "Lena", "Mira", "Nora", "Ophelia", "Rosa", "Selin", "Tara", "Vera", "Zeynep", "Elif", "Defne", "Asya", "Leyla", "Mina", "Sena"},
            new String[]{"Taşçı", "Demirci", "Değirmenci", "Kuzgun", "Akın", "Ormancı", "Kılıçoğlu", "Yılmaz", "Dumanlı", "Arslan", "Koç", "Ateşli"}),
    ELF("Elf", 1.08F, 0.92F, 22,
            new String[]{"Aelar", "Caelum", "Elandor", "Faelar", "Galinndan", "Ithil", "Lorien", "Mirthal", "Naeris", "Thalion", "Varis", "Erevan", "Saelthor"},
            new String[]{"Aelindra", "Arwen", "Caelynn", "Elanor", "Faenya", "Ilyana", "Lyra", "Miriel", "Naivara", "Sariel", "Thalia", "Yavanna", "Elowen"},
            new String[]{"Gümüşyaprak", "Ayışığı", "Rüzgarses", "Yıldızgözlü", "Şafakdal", "Çiğtanesi"}),
    DWARF("Cüce", 0.72F, 1.22F, 26,
            new String[]{"Balin", "Bruenor", "Durgrim", "Thorin", "Gimrak", "Harbek", "Kildrak", "Morgran", "Orsik", "Rurik", "Tordek", "Vondal"},
            new String[]{"Amber", "Bardryn", "Dagnal", "Eldeth", "Gunnloda", "Helja", "Kathra", "Mardred", "Riswynn", "Torbera", "Vistra"},
            new String[]{"Demirsakal", "Taşyumruk", "Altınçekiç", "Örsdöven", "Kayakalp", "Kömürgöz"}),
    DEMON("İblis", 1.1F, 1.05F, 28,
            new String[]{"Azgal", "Belzar", "Draxis", "Kaelthas", "Malakar", "Mordek", "Raziel", "Skarn", "Vorath", "Zarek", "Xerath"},
            new String[]{"Lilith", "Nyx", "Seraphine", "Vexa", "Morrigan", "Zethra", "Kalia", "Lamia", "Shira", "Velka"},
            new String[]{"Külkalp", "Ateşdoğan", "Kanboynuz", "Gölgekanat", "Lavgöz"}),
    GIANT("Dev", 1.9F, 1.6F, 60,
            new String[]{"Bjorn", "Hrothgar", "Ymirak", "Skadi", "Thrym", "Ulfgar", "Grumm", "Hagen", "Torvald"},
            new String[]{"Gerda", "Hilde", "Ragna", "Sigrun", "Thora", "Ylva", "Brynja", "Frida"},
            new String[]{"Kayayumruk", "Buzdağı", "Gökgürleyen", "Dağomuz"}),
    ORC("Ork", 1.12F, 1.18F, 30,
            new String[]{"Gorthak", "Grom", "Kargath", "Murzug", "Narok", "Throk", "Ugruk", "Zogrim", "Draka", "Brakk"},
            new String[]{"Agra", "Garona", "Kelga", "Murga", "Sharra", "Urzha", "Zarra", "Grisha"},
            new String[]{"Kemikkıran", "Kanlıdiş", "Kurtsöken", "Kafatası", "Demirdiş"}),
    BEASTKIN("Hayvan-insan", 1.0F, 1.0F, 22,
            new String[]{"Rakşa", "Kovu", "Simbar", "Tarku", "Fenrik", "Garu", "Mako", "Rinto", "Zuko"},
            new String[]{"Nala", "Kiara", "Mira", "Saya", "Tika", "Yuki", "Ranka", "Lio"},
            new String[]{"Altınyele", "Gecepençe", "Rüzgarkulak", "Sessizadım", "Kızılkuyruk"}),
    HALFLING("Buçukluk", 0.62F, 0.95F, 16,
            new String[]{"Pippin", "Merry", "Bilbo", "Cade", "Eldon", "Garret", "Lyle", "Milo", "Roscoe", "Wellby"},
            new String[]{"Andry", "Bree", "Callie", "Kithri", "Lavinia", "Merla", "Seraphina", "Verna", "Rosie"},
            new String[]{"Altınkese", "Çaydanlık", "Tatlıçayır", "Kıvırcık", "Yuvarlakkapı"}),
    DARK_ELF("Kara Elf", 1.06F, 0.92F, 22,
            new String[]{"Drizzt", "Jarlaxle", "Kelnozz", "Pharaun", "Ryld", "Valas", "Zaknafein", "Nalfein"},
            new String[]{"Shaelith", "Quenthel", "Viconia", "Triel", "Halisstra", "Liriel", "Ilvara", "Sabal"},
            new String[]{"Gecegölge", "Örümcekkızı", "Karadiken", "Sessizbıçak", "Yarımay"});

    public final String title;
    public final float height, width;
    public final int health;
    private final String[] male, female, family;

    Race(String title, float height, float width, int health, String[] male, String[] female, String[] family) {
        this.title = title; this.height = height; this.width = width; this.health = health;
        this.male = male; this.female = female; this.family = family;
    }

    public static Race of(int i) { return values()[Math.floorMod(i, values().length)]; }

    public String randomName(RandomSource r, boolean femaleGender) {
        String[] pool = femaleGender ? female : male;
        return pool[r.nextInt(pool.length)] + " " + family[r.nextInt(family.length)];
    }

    /** gift preferences: how much this race likes an item category (see GiftLogic) */
    public boolean longLived() { return this == ELF || this == DARK_ELF || this == DWARF || this == GIANT; }
}
