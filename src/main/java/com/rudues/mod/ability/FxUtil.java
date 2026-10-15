package com.rudues.mod.ability;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class FxUtil {
    private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.65F, 0.1F, 0.95F), 1.6F);

    public static void purpleBurst(ServerLevel level, Vec3 pos, int count, double spread) {
        level.sendParticles(PURPLE, pos.x, pos.y, pos.z, count, spread, spread, spread, 0.02);
        level.sendParticles(ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z, Math.max(1, count / 2), spread, spread, spread, 0.03);
        level.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, Math.max(1, count / 2), spread, spread, spread, 0.05);
    }
}
