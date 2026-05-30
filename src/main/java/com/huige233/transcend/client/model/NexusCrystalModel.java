package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.nexus.NexusCrystalEntity;
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
 * R93: NexusCrystalEntity 模型 — 飘浮旋转的双锥水晶（用 2 个 trapezoidal-ish 立方体堆叠表达 spinning crystal）。
 *
 * <p>16×16 纹理：上下两段。整体随 ageInTicks 旋转 + 上下飘浮。
 */
public class NexusCrystalModel extends EntityModel<NexusCrystalEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "nexus_crystal"), "main");

    private final ModelPart top;
    private final ModelPart bot;

    public NexusCrystalModel(ModelPart root) {
        this.top = root.getChild("top");
        this.bot = root.getChild("bot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 上锥：3×4×3 倒立（pose 旋转表达"锥尖向上"，纹理映射用顶部一半）
        root.addOrReplaceChild("top",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5F, -4.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // 下锥：3×4×3 正立
        root.addOrReplaceChild("bot",
                CubeListBuilder.create()
                        .texOffs(0, 8)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(NexusCrystalEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 整体旋转 + 上下飘浮
        float bob = Mth.sin(ageInTicks * 0.1F) * 1.0F;
        float yRot = ageInTicks * 0.06F;
        this.top.y = 16.0F + bob;
        this.bot.y = 16.0F + bob;
        this.top.yRot = yRot;
        this.bot.yRot = yRot;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                float r, float g, float b, float a) {
        top.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        bot.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
