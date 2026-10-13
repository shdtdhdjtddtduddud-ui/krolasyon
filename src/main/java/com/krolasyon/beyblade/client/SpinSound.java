package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/** Looping whirr that follows a beyblade; pitch and volume track its spin. */
public class SpinSound extends AbstractTickableSoundInstance {
    private final BeybladeEntity bey;

    public SpinSound(BeybladeEntity bey) {
        super(ModSounds.SPIN_LOOP.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.bey = bey;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01F;
        this.pitch = 1F;
        this.x = bey.getX();
        this.y = bey.getY();
        this.z = bey.getZ();
    }

    @Override
    public void tick() {
        if (bey.isRemoved()) {
            stop();
            return;
        }
        this.x = bey.getX();
        this.y = bey.getY();
        this.z = bey.getZ();
        float ratio = bey.spinRatio();
        float targetVol;
        float targetPitch;
        switch (bey.getState()) {
            case BeybladeEntity.STATE_DEFEATED -> {
                targetVol = 0F;
                targetPitch = 0.5F;
            }
            case BeybladeEntity.STATE_RECALL -> {
                targetVol = 0.35F;
                targetPitch = 1.6F;
            }
            default -> {
                targetVol = 0.25F + 0.45F * ratio;
                targetPitch = 0.65F + 0.85F * ratio;
                if (bey.getAbility() != BeybladeEntity.AB_NONE) {
                    targetVol += 0.2F;
                    targetPitch += 0.25F;
                }
            }
        }
        this.volume = Mth.lerp(0.2F, this.volume, targetVol);
        this.pitch = Mth.clamp(Mth.lerp(0.2F, this.pitch, targetPitch), 0.5F, 2F);
    }
}
