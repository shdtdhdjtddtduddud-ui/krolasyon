package com.krolasyon.realweather.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Sürekli çalan ortam döngüsü; sesi hedefe yumuşakça yaklaşır, sessizleşince durur. */
public final class LoopSound extends AbstractTickableSoundInstance {
    public float target;
    private int quiet;

    public LoopSound(SoundEvent event, SoundSource src) {
        super(event, src, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = Attenuation.NONE;
        this.volume = 0.02F;
        this.x = this.y = this.z = 0;
    }

    @Override
    public void tick() {
        volume += (target - volume) * 0.04F;
        if (target < 0.015F && volume < 0.02F) {
            if (++quiet > 20) stop();
        } else quiet = 0;
    }

    /** Şu anki ses düzeyi (ses sistemi kapalıyken de güvenli). */
    public float level() {
        return volume;
    }

    public void kill() {
        stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
