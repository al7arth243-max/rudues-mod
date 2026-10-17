package com.example.watchers.client;

import com.example.watchers.WatchersMod;
import com.example.watchers.entity.WatcherEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

/**
 * Draws a watcher, but only for the one player it belongs to. Everyone else on the server sees nothing.
 * It is lit as if in a bright place so the pale face stays visible in the dark; the black body stays black.
 */
public class WatcherRenderer extends MobRenderer<WatcherEntity, WatcherModel<WatcherEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(WatchersMod.MODID, "textures/entity/watcher.png");

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new WatcherModel<>(context.bakeLayer(WatcherModel.LAYER_LOCATION)), 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(WatcherEntity entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(WatcherEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        LocalPlayer me = Minecraft.getInstance().player;
        if (me == null) {
            return false;
        }
        Optional<UUID> owner = entity.getWatchedPlayer();
        return owner.isPresent() && owner.get().equals(me.getUUID())
                && super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    @Override
    protected int getBlockLightLevel(WatcherEntity entity, BlockPos pos) {
        return 12;
    }
}
