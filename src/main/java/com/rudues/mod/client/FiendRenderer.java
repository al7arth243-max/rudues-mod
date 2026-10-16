package com.rudues.mod.client;

import com.rudues.mod.RuduesMod;
import com.rudues.mod.entity.FiendEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Purple, glowing allay look-alike. */
public class FiendRenderer extends MobRenderer<FiendEntity, FiendModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RuduesMod.MODID, "textures/entity/fire_fiend_minion.png");

    public FiendRenderer(EntityRendererProvider.Context context) {
        super(context, new FiendModel(context.bakeLayer(FiendModel.LAYER)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(FiendEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(FiendEntity entity, BlockPos pos) {
        return 15;
    }
}
