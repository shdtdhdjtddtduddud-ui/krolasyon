package com.krolasyon.realweather;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Kalıcı hava kaydı: şu anki ve sıradaki durum, kalan süre, yoğunluk, rüzgâr. */
public final class WeatherData extends SavedData {
    public WeatherState state = WeatherState.CLEAR, next = WeatherState.OVERCAST;
    public int ticksLeft = 20 * 60 * 8;
    public float intensity, windAngle, wind = 0.2F, targetIntensity, windTarget = 0.2F;
    /** Vanilya hava durumunu en son bizim ayarladığımız değer (komutla değiştirildiyse fark edilir). */
    public boolean appliedRain, appliedThunder;

    public static WeatherData load(CompoundTag t) {
        WeatherData d = new WeatherData();
        d.state = WeatherState.values()[Math.floorMod(t.getInt("State"), WeatherState.values().length)];
        d.next = WeatherState.values()[Math.floorMod(t.getInt("Next"), WeatherState.values().length)];
        d.ticksLeft = t.getInt("Left");
        d.intensity = t.getFloat("Intensity");
        d.targetIntensity = t.getFloat("Target");
        d.windAngle = t.getFloat("WindAngle");
        d.wind = t.getFloat("Wind");
        d.windTarget = t.getFloat("WindTarget");
        d.appliedRain = t.getBoolean("AppliedRain");
        d.appliedThunder = t.getBoolean("AppliedThunder");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putInt("State", state.ordinal());
        t.putInt("Next", next.ordinal());
        t.putInt("Left", ticksLeft);
        t.putFloat("Intensity", intensity);
        t.putFloat("Target", targetIntensity);
        t.putFloat("WindAngle", windAngle);
        t.putFloat("Wind", wind);
        t.putFloat("WindTarget", windTarget);
        t.putBoolean("AppliedRain", appliedRain);
        t.putBoolean("AppliedThunder", appliedThunder);
        return t;
    }
}
