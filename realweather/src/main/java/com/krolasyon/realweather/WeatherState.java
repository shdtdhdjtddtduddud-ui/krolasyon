package com.krolasyon.realweather;

/**
 * Dünyanın genel hava durumu (bir hava cephesi). Her bölgede biyoma göre farklı görünür: aynı fırtına soğukta tipi,
 * çölde kum fırtınası, ılıman yerlerde gök gürültülü sağanaktır.
 */
public enum WeatherState {
    //      komut adı     görünen ad          yağış  gök gürültüsü  hedef yoğunluk  rüzgâr   süre (dk)
    CLEAR("acik", "Açık", false, false, 0.0F, 0.22F, 10, 24),
    OVERCAST("bulutlu", "Bulutlu", false, false, 0.0F, 0.35F, 4, 10),
    FOG("sis", "Sisli", false, false, 0.0F, 0.06F, 3, 7),
    DRIZZLE("ciseleme", "Çiseleme", true, false, 0.32F, 0.3F, 3, 7),
    RAIN("yagmur", "Yağmurlu", true, false, 0.72F, 0.48F, 4, 9),
    STORM("firtina", "Fırtına", true, true, 1.0F, 0.92F, 3, 6);

    public final String id, title;
    public final boolean precipitation, thunder;
    public final float intensity, wind;
    public final int minMinutes, maxMinutes;

    WeatherState(String id, String title, boolean precipitation, boolean thunder, float intensity, float wind, int minMinutes, int maxMinutes) {
        this.id = id;
        this.title = title;
        this.precipitation = precipitation;
        this.thunder = thunder;
        this.intensity = intensity;
        this.wind = wind;
        this.minMinutes = minMinutes;
        this.maxMinutes = maxMinutes;
    }

    /** Bir sonraki durumun olasılıkları (Markov zinciri): hava cepheleri kademeli gelir ve gider. */
    public WeatherState next(net.minecraft.util.RandomSource r) {
        float f = r.nextFloat();
        return switch (this) {
            case CLEAR -> f < 0.65F ? OVERCAST : f < 0.85F ? FOG : DRIZZLE;
            case OVERCAST -> f < 0.35F ? CLEAR : f < 0.65F ? DRIZZLE : f < 0.9F ? RAIN : FOG;
            case FOG -> f < 0.6F ? CLEAR : OVERCAST;
            case DRIZZLE -> f < 0.45F ? RAIN : f < 0.8F ? OVERCAST : CLEAR;
            case RAIN -> f < 0.35F ? STORM : f < 0.7F ? DRIZZLE : OVERCAST;
            case STORM -> f < 0.75F ? RAIN : DRIZZLE;
        };
    }

    public static WeatherState byId(String id) {
        for (WeatherState s : values()) if (s.id.equals(id)) return s;
        return null;
    }
}
