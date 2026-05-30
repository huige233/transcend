package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.SpellPillar;
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

/**
 * R93: SpellPillar 模型 — 静态柱状物，由 底座 + 主柱 + 顶冠 三段组成。
 *
 * <p>32×32 纹理。顶冠会缓慢旋转。
 */
public class SpellPillarModel extends EntityModel<SpellPillar> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "spell_pillar"), "main");

    private final ModelPart base;
    private final ModelPart shaft;
    private final ModelPart crown;

    public SpellPillarModel(ModelPart root) {
        this.base = root.getChild("base");
        this.shaft = root.getChild("shaft");
        this.crown = root.getChild("crown");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 底座 8×2×8
        root.addOrReplaceChild("base",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, 22.0F, -4.0F, 8.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 主柱 4×18×4
        root.addOrReplaceChild("shaft",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-2.0F, 4.0F, -2.0F, 4.0F, 18.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // 顶冠 6×4×6（带旋转）
        root.addOrReplaceChild("crown",
                CubeListBuilder.create()
                        .texOffs(16, 10)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(SpellPillar entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 顶冠缓慢旋转
        this.crown.yRot = ageInTicks * 0.03F;
        // 顶冠上下微动
        this.crown.y = 4.0F + Mth.sin(ageInTicks * 0.05F) * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                float r, float g, float b, float a) {
        base.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        shaft.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        crown.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
