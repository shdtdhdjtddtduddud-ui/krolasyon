package com.krolasyon.elvenrealm.world;

/** Elf kentlerinin türleri: dünyada tek başkent (Sylvaren, özel karakterler) ve diğer kentler. */
public enum CityType {
    CAPITAL("Kadim ağaçların gölgesindeki elf kenti", new String[]{"Sylvaren"}),
    MOON("Ay ışığında yıkanan gizemli elf kenti", new String[]{"Lunareth", "Nythalin", "Selunar", "Ithilmar"}),
    BLOSSOM("Kiraz çiçekleri arasında saklı elf kenti", new String[]{"Elanthil", "Florael", "Saelia", "Rosmeren"}),
    COMMON("Ormanın derinliklerinde bir elf kasabası", new String[]{"Eldwyn", "Thalanor", "Myrrhen", "Calenor", "Faelis", "Arvendel"});

    public final String subtitle;
    private final String[] names;

    CityType(String subtitle, String[] names) {
        this.subtitle = subtitle;
        this.names = names;
    }

    public String name(long citySeed) {
        return names[(int) Math.floorMod(citySeed >>> 7, names.length)];
    }

    /** Muhafız giysi çeşidi (doku). */
    public int guardVariant() {
        return switch (this) {
            case MOON -> 1;
            case BLOSSOM -> 2;
            default -> 0;
        };
    }
}
