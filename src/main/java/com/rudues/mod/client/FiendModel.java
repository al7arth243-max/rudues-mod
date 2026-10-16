package com.rudues.mod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.rudues.mod.RuduesMod;
import com.rudues.mod.entity.FiendEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Allay-shaped model (own copy of the geometry) used by the summoned purple fiends. */
public class FiendModel extends EntityModel<FiendEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(RuduesMod.MODID, "fiend"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public FiendModel(ModelPart part) {
        super(RenderType::entityTranslucent);
        this.root = part.getChild("root");
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = body.getChild("right_arm");
        this.leftArm = body.getChild("left_arm");
        this.rightWing = body.getChild("right_wing");
        this.leftWing = body.getChild("left_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition def = mesh.getRoot();
        PartDefinition root = def.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 23.5F, 0.0F));
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-2.5F, -5.0F, -2.5F, 5.0F, 5.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.99F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10)
                .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 16)
                .addBox(-1.5F, 0.0F, -1.0F, 3.0F, 5.0F, 2.0F, new CubeDeformation(-0.2F)), PartPose.offset(0.0F, -4.0F, 0.0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(23, 0)
                .addBox(-0.75F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)), PartPose.offset(-1.75F, 0.5F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(23, 6)
                .addBox(-0.25F, -0.5F, -1.0F, 1.0F, 4.0F, 2.0F, new CubeDeformation(-0.01F)), PartPose.offset(1.75F, 0.5F, 0.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(16, 14)
                .addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.5F, 0.0F, 0.6F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(16, 14)
                .addBox(0.0F, 1.0F, 0.0F, 0.0F, 5.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.0F, 0.6F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(FiendEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        float flap = Mth.cos(ageInTicks * 1.1F) * 0.45F;
        rightWing.yRot = -(0.8F + flap);
        leftWing.yRot = 0.8F + flap;
        head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        head.xRot = headPitch * ((float) Math.PI / 180F);
        body.xRot = 0.35F;                       // lean forward like an allay
        rightArm.xRot = -0.6F + flap * 0.4F;
        leftArm.xRot = -0.6F + flap * 0.4F;
        root.y = 23.5F + Mth.sin(ageInTicks * 0.15F) * 0.8F; // hover bob
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float red, float green, float blue, float alpha) {
        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
