package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.SpellGuardian;
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
 * R93: SpellGuardian 模型 — 浮空大块头守护者，类似缩小版 Iron Golem。
 *
 * <p>32×32 纹理：head, body, arms（无腿，悬浮）。
 */
public class SpellGuardianModel extends EntityModel<SpellGuardian> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "spell_guardian"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart aura;

    public SpellGuardianModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.leftArm = root.getChild("left_arm");
        this.rightArm = root.getChild("right_arm");
        this.aura = root.getChild("aura");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 头部 6×6×6
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        // 身体 10×8×4
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-5.0F, 0.0F, -2.0F, 10.0F, 8.0F, 4.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        // 双臂 2×8×2
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(-6.0F, 9.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(8, 24).mirror()
                        .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                PartPose.offset(6.0F, 9.0F, 0.0F));

        // 底部光环 — 1 个扁立方体替代 "灵魂尾巴"
        root.addOrReplaceChild("aura",
                CubeListBuilder.create()
                        .texOffs(16, 24)
                        .addBox(-4.0F, 16.0F, -4.0F, 8.0F, 1.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(SpellGuardian entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 头部跟随
        this.head.yRot = netHeadYaw * 0.017453292F;
        this.head.xRot = headPitch * 0.017453292F;

        // 双臂随移动晃动
        this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        // 身体上下飘浮
        float bob = Mth.sin(ageInTicks * 0.08F) * 0.5F;
        this.body.y = 8.0F + bob;
        this.head.y = 8.0F + bob;
        this.leftArm.y = 9.0F + bob;
        this.rightArm.y = 9.0F + bob;

        // 光环旋转
        this.aura.yRot = ageInTicks * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                float r, float g, float b, float a) {
        head.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        body.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        leftArm.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        rightArm.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        aura.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
