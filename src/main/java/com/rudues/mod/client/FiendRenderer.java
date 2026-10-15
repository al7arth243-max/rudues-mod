package com.rudues.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.rudues.mod.RuduesMod;
import com.rudues.mod.entity.FiendEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws the fiend as a full-bright camera-facing flame sprite. */
public class FiendRenderer extends EntityRenderer<FiendEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(RuduesMod.MODID, "textures/entity/fire_fiend_minion.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public FiendRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(FiendEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(FiendEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(FiendEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        float bob = Mth.sin((entity.tickCount + partialTick) * 0.3F) * 0.05F;
        poseStack.translate(0.0D, 0.4D + bob, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);

        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        vertex(vc, m, n, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(vc, m, n, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(vc, m, n, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(vc, m, n, -0.5F, 0.5F, 0.0F, 0.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float v) {
        vc.vertex(m, x, y, 0.0F).color(255, 255, 255, 255).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL_BRIGHT)
                .normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }
}
