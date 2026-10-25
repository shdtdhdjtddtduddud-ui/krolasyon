package com.krolasyon.labirent.entity.ai;

/**
 * Who a Glader is. The numbers drive behaviour: how often they come over to talk, whether they run towards
 * a fight or away from it, how they take the captain's orders and what they do when there is nothing to do.
 */
public enum Personality {
    //        courage social loyalty speed  idle style
    NEWT(0.70F, 0.80F, 0.90F, 1.00F, Idle.TEND),        // calm, wise, the one who keeps everybody together
    MINHO(0.95F, 0.60F, 0.70F, 1.15F, Idle.JOG),        // fearless, sarcastic, never stops moving
    ALBY(0.90F, 0.45F, 0.60F, 1.00F, Idle.PATROL),      // stern leader, protective, the Glade's medic now
    CHUCK(0.20F, 1.00F, 1.00F, 0.95F, Idle.TAG_ALONG),  // cheerful, chatty, scared of everything, devoted to you
    GALLY(0.90F, 0.25F, -0.40F, 1.00F, Idle.BROOD),     // suspicious of the captain, short-tempered, brave
    TERESA(0.75F, 0.40F, 0.80F, 1.05F, Idle.TEND),
    GENERIC(0.50F, 0.50F, 0.50F, 1.00F, Idle.TEND);

    public enum Idle { TEND, JOG, PATROL, TAG_ALONG, BROOD }

    public final float courage, social, loyalty, speed;
    public final Idle idle;

    Personality(float courage, float social, float loyalty, float speed, Idle idle) {
        this.courage = courage;
        this.social = social;
        this.loyalty = loyalty;
        this.speed = speed;
        this.idle = idle;
    }

    public String id() { return name().toLowerCase(); }

    public boolean brave() { return courage >= 0.5F; }

    public static Personality of(String storyId) {
        for (Personality p : values()) if (p.id().equals(storyId)) return p;
        return GENERIC;
    }
}
