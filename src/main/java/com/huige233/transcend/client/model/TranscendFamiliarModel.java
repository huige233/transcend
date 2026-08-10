package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.familiar.TranscendFamiliar;
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

/** 超越使魔(宠物)实体模型。 */
public class TranscendFamiliarModel extends EntityModel<TranscendFamiliar> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "transcend_familiar"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart legFL;
    private final ModelPart legFR;
    private final ModelPart legBL;
    private final ModelPart legBR;
    private final ModelPart tail;

    public TranscendFamiliarModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.legFL = root.getChild("leg_fl");
        this.legFR = root.getChild("leg_fr");
        this.legBL = root.getChild("leg_bl");
        this.legBR = root.getChild("leg_br");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, -4.0F));

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-3.0F, -2.0F, 0.0F, 6.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 18.0F, -4.0F));

        root.addOrReplaceChild("leg_fl",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(2.0F, 18.0F, -3.0F));
        root.addOrReplaceChild("leg_fr",
                CubeListBuilder.create()
                        .texOffs(8, 8)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-2.0F, 18.0F, -3.0F));
        root.addOrReplaceChild("leg_bl",
                CubeListBuilder.create()
                        .texOffs(16, 12)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(2.0F, 18.0F, 3.0F));
        root.addOrReplaceChild("leg_br",
                CubeListBuilder.create()
                        .texOffs(24, 12)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offset(-2.0F, 18.0F, 3.0F));

        root.addOrReplaceChild("tail",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 4.0F, -0.3F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(TranscendFamiliar entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        this.head.yRot = netHeadYaw * 0.017453292F;
        this.head.xRot = headPitch * 0.017453292F;

        float leg = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbSwingAmount;
        this.legFL.xRot = leg;
        this.legBR.xRot = leg;
        this.legFR.xRot = -leg;
        this.legBL.xRot = -leg;

        this.tail.yRot = Mth.sin(ageInTicks * 0.15F) * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                float r, float g, float b, float a) {
        head.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        legFL.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        legFR.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        legBL.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        legBR.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        tail.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
