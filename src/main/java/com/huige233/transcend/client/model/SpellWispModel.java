package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.SpellWisp;
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
 * R93: SpellWisp 模型 — 浮空小光球，由中心立方体 + 旋转的 4 个外环碎片组成。
 *
 * <p>16×16 纹理：上半 8×8 = 中心，下半 8×8 = 外环碎片（重复使用）。
 */
public class SpellWispModel extends EntityModel<SpellWisp> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "spell_wisp"), "main");

    private final ModelPart core;
    private final ModelPart shard1;
    private final ModelPart shard2;
    private final ModelPart shard3;
    private final ModelPart shard4;

    public SpellWispModel(ModelPart root) {
        this.core = root.getChild("core");
        this.shard1 = root.getChild("shard1");
        this.shard2 = root.getChild("shard2");
        this.shard3 = root.getChild("shard3");
        this.shard4 = root.getChild("shard4");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 中心 4×4×4 立方体
        root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        // 4 个外环碎片 2×2×2 各
        for (int i = 1; i <= 4; i++) {
            float ang = (float) (Math.PI * 2 / 4 * (i - 1));
            float dx = (float) (Math.cos(ang) * 5.0);
            float dz = (float) (Math.sin(ang) * 5.0);
            root.addOrReplaceChild("shard" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 8)
                            .addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                    PartPose.offset(dx, 16.0F, dz));
        }

        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(SpellWisp entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // 4 个 shard 绕中心旋转（围绕 y 轴）
        float rotSpeed = 0.05F;
        float rotateY = ageInTicks * rotSpeed;
        float radius = 5.0F;
        float bob = Mth.sin(ageInTicks * 0.10F) * 1.0F; // 上下浮动

        this.core.y = 16.0F + bob * 0.5F;
        this.core.yRot = ageInTicks * 0.04F;

        ModelPart[] shards = {shard1, shard2, shard3, shard4};
        for (int i = 0; i < 4; i++) {
            float baseAng = (float) (Math.PI * 2 / 4 * i);
            float ang = baseAng + rotateY;
            shards[i].x = Mth.cos(ang) * radius;
            shards[i].z = Mth.sin(ang) * radius;
            shards[i].y = 16.0F + bob;
            shards[i].yRot = ang;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer,
                                int packedLight, int packedOverlay,
                                float r, float g, float b, float a) {
        core.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        shard1.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        shard2.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        shard3.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
        shard4.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
