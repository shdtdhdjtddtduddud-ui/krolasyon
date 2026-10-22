package com.krolasyon.elvenrealm.entity;

/** Elfin boşta yaptığı iş; istemciye eşitlenir ve modelin pozunu belirler. */
public enum Activity {
    NONE, TALK, WAVE, CROSS_ARMS, READ, MEDITATE, KNEEL, PRACTICE, SIT, PRAY, RECOVER,
    /** Nöbette hazır ol duruşu. */
    STAND,
    /** Göğse yumruk: selam. */
    SALUTE,
    /** Saygıyla eğilme. */
    BOW;

    public static Activity of(int i) {
        return i >= 0 && i < values().length ? values()[i] : NONE;
    }

    /** Karaktere göre boşta seçilebilecek işler. */
    public static Activity[] idle(ElfKind k) {
        return switch (k) {
            case LIRAEL -> new Activity[]{PRACTICE, PRACTICE, CROSS_ARMS, SIT};
            case VAELOR -> new Activity[]{CROSS_ARMS, CROSS_ARMS, PRACTICE, KNEEL};
            case CAELAN -> new Activity[]{READ, READ, SIT, CROSS_ARMS};
            case NIMRAE -> new Activity[]{MEDITATE, MEDITATE, PRAY, KNEEL};
            case TAVI -> new Activity[]{KNEEL, PRACTICE, SIT, PRACTICE};
            case THALION -> new Activity[]{CROSS_ARMS, PRACTICE, KNEEL};
            case AERENDIL -> new Activity[]{SIT, READ, CROSS_ARMS};
            case GUARD -> new Activity[]{STAND, CROSS_ARMS};
            case TOWNSFOLK, TOWNSMAN -> new Activity[]{TALK, SIT, CROSS_ARMS, KNEEL};
            case MOON_SEER -> new Activity[]{MEDITATE, READ, PRAY};
            case BLOSSOM_LADY -> new Activity[]{KNEEL, MEDITATE, SIT};
        };
    }

    /** Kısa süreli jest (rutin bunları bölmez). */
    public boolean gesture() {
        return this == WAVE || this == SALUTE || this == BOW || this == TALK;
    }

    public boolean seated() {
        return this == SIT || this == MEDITATE || this == RECOVER;
    }
}
