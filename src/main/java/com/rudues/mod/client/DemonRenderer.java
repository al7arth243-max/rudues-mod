package com.rudues.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rudues.mod.RuduesMod;
import com.rudues.mod.entity.DemonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

import java.util.UUID;

/**
 * Renders the Hero of Hell demon.
 * Assets: geo/entity/rage_demon.geo.json, textures/entity/rage_demon(.png/_glowing.png),
 *         animations/entity/rage_demon.animation.json
 */
public class DemonRenderer extends GeoEntityRenderer<DemonEntity> {
    public DemonRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(new ResourceLocation(RuduesMod.MODID, "rage_demon")));
        // the model is ~4.25 blocks tall; scale it up so it stands 5 blocks tall
        withScale(1.18F);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(DemonEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // in first person the camera would be inside the demon's head, so hide your own demon there
        Minecraft mc = Minecraft.getInstance();
        UUID owner = entity.getOwnerUUID();
        if (owner != null && mc.player != null && owner.equals(mc.player.getUUID())
                && mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        // the texture is dark obsidian: never let world lighting drop it below a readable floor
        int block = Math.max(LightTexture.block(packedLight), 13);
        int sky = Math.max(LightTexture.sky(packedLight), 13);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, LightTexture.pack(block, sky));
    }
}
