package com.example.watchers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * The Watcher: a 4.75 block tall, featureless black figure. Long hair falls around a small,
 * pale, mask-like face (hollow eyes, tear streaks, thin smile) and merges into sloped
 * shoulders and a robe that reaches the floor. It never walks or attacks, it only watches.
 *
 * AUTO-GENERATED geometry. Texture: assets/watchers/textures/entity/watcher.png.
 * The UV layout is 128x128 (that is what create(mesh, 128, 128) means), but the PNG itself is
 * 2048x2048. UVs are proportional, so the extra pixels give the face real detail.
 * Suggested entity size: 1.5 wide x 4.8 tall, eye height about 3.9.
 */
public class WatcherModel<T extends Entity> extends EntityModel<T> {

    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(new ResourceLocation("watchers", "watcher"), "main");

    private static final float DEG = (float) Math.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final float headBaseX, headBaseZ;

    public WatcherModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("body").getChild("head");
        this.headBaseX = head.xRot;
        this.headBaseZ = head.zRot;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(65, 60)
                        .addBox(-6.0F, -26.0F, -4.0F, 12.0F, 2.0F, 8.0F)
                        .texOffs(0, 73)
                        .addBox(-7.0F, -24.0F, -4.0F, 14.0F, 2.0F, 8.0F)
                        .texOffs(45, 73)
                        .addBox(-8.0F, -22.0F, -4.0F, 16.0F, 2.0F, 8.0F)
                        .texOffs(0, 84)
                        .addBox(-9.0F, -20.0F, -4.0F, 18.0F, 2.0F, 8.0F)
                        .texOffs(53, 84)
                        .addBox(-10.0F, -18.0F, -4.0F, 20.0F, 2.0F, 8.0F)
                        .texOffs(0, 60)
                        .addBox(-12.0F, -16.0F, -4.0F, 24.0F, 4.0F, 8.0F)
                        .texOffs(0, 41)
                        .addBox(-11.0F, -12.0F, -4.0F, 22.0F, 10.0F, 8.0F)
                        .texOffs(37, 0)
                        .addBox(-10.0F, -2.0F, -4.0F, 20.0F, 13.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition legL = body.addOrReplaceChild("leg_l",
                CubeListBuilder.create()
                        .texOffs(94, 0)
                        .addBox(1.0F, 11.0F, -4.0F, 8.0F, 13.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition legR = body.addOrReplaceChild("leg_r",
                CubeListBuilder.create()
                        .texOffs(94, 0)
                        .addBox(-9.0F, 11.0F, -4.0F, 8.0F, 13.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -28.0F, -4.0F, 10.0F, 32.0F, 8.0F)
                        .texOffs(61, 41)
                        .addBox(-4.0F, -26.0F, -5.0F, 8.0F, 14.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -24.0F, 0.0F, 0.0F, 0.0F, 0.03F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // The head follows its target; a very slow sway keeps the tilt from looking frozen.
        this.head.yRot = Mth.clamp(netHeadYaw, -55.0F, 55.0F) * DEG;
        this.head.xRot = this.headBaseX + Mth.clamp(headPitch, -30.0F, 30.0F) * DEG;
        this.head.zRot = this.headBaseZ + Mth.sin(ageInTicks * 0.02F) * 0.025F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                               int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
