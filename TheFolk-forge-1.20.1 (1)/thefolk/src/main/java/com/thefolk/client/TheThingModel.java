package com.thefolk.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thefolk.TheFolkMod;
import com.thefolk.entity.TheThingEntity;
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

/** Criatura ancha y encorvada, con brazos larguísimos y movimientos bruscos. */
public class TheThingModel extends EntityModel<TheThingEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation(TheFolkMod.MOD_ID, "the_thing"), "main");

    private static final float RAD = (float) Math.PI / 180.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public TheThingModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();

        r.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -4.0F, 0.0F));

        r.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -14.0F, -2.5F, 8.0F, 14.0F, 5.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));

        r.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F),
                PartPose.offset(-5.5F, -2.0F, 0.0F));

        r.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 24.0F, 3.0F),
                PartPose.offset(5.5F, -2.0F, 0.0F));

        r.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(46, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F),
                PartPose.offset(-2.0F, 10.0F, 0.0F));

        r.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(46, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 14.0F, 3.0F),
                PartPose.offset(2.0F, 10.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TheThingEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * RAD;
        this.head.xRot = headPitch * RAD + 0.25F;

        float swing = Mth.cos(limbSwing * 0.8F) * 1.6F * limbSwingAmount;
        this.rightLeg.xRot = swing;
        this.leftLeg.xRot = -swing;

        float shake = Mth.cos(ageInTicks * 1.1F) * 0.25F;
        this.rightArm.xRot = -1.2F + shake;
        this.leftArm.xRot = -1.2F - shake;
        this.rightArm.zRot = 0.15F;
        this.leftArm.zRot = -0.15F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
