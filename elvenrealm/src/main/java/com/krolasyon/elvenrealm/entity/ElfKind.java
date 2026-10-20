package com.krolasyon.elvenrealm.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Sylvaren'in elfleri: görünüş (model parçaları), istatistikler, rol ve kiralama bedeli.
 */
public enum ElfKind {
    //       id        ad          unvan                   slim   ölçek can  hasar zırh rol             bedel  ses   sevdiği hediye
    LIRAEL("lirael", "Lirael", "Kılıç Dansçısı", true, 1.00F, 40, 7, 4, Role.BLADE, 32, 1.25F, Items.SWEET_BERRIES,
            Parts.EARS | Parts.HAIR_LONG | Parts.LOCKS | Parts.SKIRT | Parts.SHEATH),
    VAELOR("vaelor", "Vaelor", "Gölge Kılıcı", false, 1.05F, 36, 9, 6, Role.ASSASSIN, 64, 0.75F, Items.AMETHYST_SHARD,
            Parts.EARS | Parts.HAIR_LONG | Parts.LOCKS | Parts.PAULDRONS | Parts.COAT_TAILS | Parts.POUCH),
    CAELAN("caelan", "Caelan", "Bilge Yazıcı", false, 1.00F, 30, 3, 3, Role.ARCANE, 40, 1.0F, Items.ENCHANTED_BOOK,
            Parts.EARS | Parts.HAIR_LONG | Parts.LOCKS | Parts.CLOAK | Parts.TUNIC | Parts.DAGGER),
    NIMRAE("nimrae", "Nimrae", "Ay Rahibesi", true, 1.00F, 34, 4, 2, Role.HEALER, 48, 1.15F, Items.LILY_OF_THE_VALLEY,
            Parts.EARS | Parts.HAIR_LONG | Parts.LOCKS | Parts.SKIRT),
    TAVI("tavi", "Tavi", "Genç Okçu", true, 0.88F, 28, 3, 2, Role.ARCHER, 24, 1.45F, Items.COOKIE,
            Parts.EARS | Parts.CLOAK_SHORT | Parts.QUIVER | Parts.BIRD),
    THALION("thalion", "Thalion", "Korucu Komutanı", false, 1.05F, 48, 6, 8, Role.RANGER, 56, 0.85F, Items.COOKED_BEEF,
            Parts.EARS | Parts.HOOD | Parts.CLOAK | Parts.TABARD | Parts.QUIVER | Parts.FEATHERS),
    GUARD("guard", "Sylvaren Muhafızı", "Kent Bekçisi", false, 1.02F, 30, 6, 6, Role.GUARD, 0, 0.9F, Items.BREAD,
            Parts.EARS | Parts.TABARD);

    public enum Role { BLADE, ASSASSIN, ARCANE, HEALER, ARCHER, RANGER, GUARD }

    /** Model ek parçalarının bit bayrakları. */
    public static final class Parts {
        public static final int EARS = 1, HAIR_LONG = 2, LOCKS = 4, SKIRT = 8, SHEATH = 16, PAULDRONS = 32, COAT_TAILS = 64,
                POUCH = 128, CLOAK = 256, TUNIC = 512, DAGGER = 1024, CLOAK_SHORT = 2048, QUIVER = 4096, BIRD = 8192,
                HOOD = 16384, TABARD = 32768, FEATHERS = 65536;
    }

    public final String id, name, title;
    public final boolean slim;
    public final float scale, voice;
    public final int health, damage, armor, hirePrice, parts;
    public final Role role;
    public final Item favorite;

    ElfKind(String id, String name, String title, boolean slim, float scale, int health, int damage, int armor, Role role,
            int hirePrice, float voice, Item favorite, int parts) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.slim = slim;
        this.scale = scale;
        this.health = health;
        this.damage = damage;
        this.armor = armor;
        this.role = role;
        this.hirePrice = hirePrice;
        this.voice = voice;
        this.favorite = favorite;
        this.parts = parts;
    }

    public boolean has(int part) {
        return (parts & part) != 0;
    }

    public boolean unique() {
        return this != GUARD;
    }

    public boolean ranged() {
        return role == Role.ARCHER || role == Role.RANGER;
    }

    public static ElfKind byId(String id) {
        for (ElfKind k : values()) if (k.id.equals(id)) return k;
        return GUARD;
    }
}
