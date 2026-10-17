package com.example.watchers.registry;

import com.example.watchers.WatchersMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, WatchersMod.MODID);

    /** The haunting track. File: assets/watchers/sounds/clue_track.ogg */
    public static final RegistryObject<SoundEvent> CLUE_TRACK = SOUNDS.register("clue_track",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(WatchersMod.MODID, "clue_track")));

    private ModSounds() {
    }
}
