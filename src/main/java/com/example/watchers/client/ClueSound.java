package com.example.watchers.client;

import com.example.watchers.ClientConfig;
import com.example.watchers.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/**
 * The haunting track. Plays once, straight into your ears (not positional), fades in over two seconds
 * and fades out over six when the clue ends.
 */
public class ClueSound extends AbstractTickableSoundInstance {
    private float fade;

    public ClueSound() {
        super(ModSounds.CLUE_TRACK.get(), SoundSource.MASTER, SoundInstance.createUnseededRandom());
        this.looping = false;
        this.delay = 0;
        this.volume = 0.001F;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        if (ClientState.active) {
            this.fade = Math.min(1.0F, this.fade + 1.0F / 40.0F);
        } else {
            this.fade = Math.max(0.0F, this.fade - 1.0F / 120.0F);
            if (this.fade <= 0.0F) {
                this.stop();
                return;
            }
        }
        this.volume = Math.max(0.001F, this.fade * ClientConfig.TRACK_VOLUME.get().floatValue());
    }
}
