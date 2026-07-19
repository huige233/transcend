package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.nexus.NexusSentinel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class NexusSentinelModel extends EntityModel<NexusSentinel> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "nexus_sentinel"), "main");

    private final ModelPart core;
    private final ModelPart shardA;
    private final ModelPart shardB;
    private final ModelPart shardC;
    private final ModelPart shardD;
    private final ModelPart tailFlame;

    public NexusSentinelModel(ModelPart root) {
        this.core = root.getChild("core");
        this.shardA = root.getChild("shard_a");
        this.shardB = root.getChild("shard_b");
        this.shardC = root.getChild("shard_c");
        this.shardD = root.getChild("shard_d");
        this.tailFlame = root.getChild("tail_flame");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 14.0F, 0.0F,
                        0.7854F, 0.0F, 0.7854F));

        root.addOrReplaceChild("shard_a",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(5.0F, 14.0F, 0.0F));

        root.addOrReplaceChild("shard_b",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(-5.0F, 14.0F, 0.0F));

        root.addOrReplaceChild("shard_c",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 14.0F, 5.0F));

        root.addOrReplaceChild("shard_d",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.0F, -2.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 14.0F, -5.0F));

        root.addOrReplaceChild("tail_flame",
                CubeListBuilder.create()
                        .texOffs(8, 12)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(NexusSentinel entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        this.core.yRot = ageInTicks * 0.05F;

        float orbitAngle = ageInTicks * 0.1F;
        float radius = 4.5F;
        float bob = Mth.sin(ageInTicks * 0.08F) * 0.5F;

        this.shardA.x = Mth.cos(orbitAngle) * radius;
        this.shardA.z = Mth.sin(orbitAngle) * radius;
        this.shardA.y = 14.0F + bob;
        this.shardA.yRot = orbitAngle;

        this.shardB.x = Mth.cos(orbitAngle + Mth.PI) * radius;
        this.shardB.z = Mth.sin(orbitAngle + Mth.PI) * radius;
        this.shardB.y = 14.0F - bob;
        this.shardB.yRot = orbitAngle + Mth.PI;

        this.shardC.x = Mth.cos(orbitAngle + Mth.HALF_PI) * radius;
        this.shardC.z = Mth.sin(orbitAngle + Mth.HALF_PI) * radius;
        this.shardC.y = 14.0F + bob * 0.7F;
        this.shardC.yRot = orbitAngle + Mth.HALF_PI;

        this.shardD.x = Mth.cos(orbitAngle - Mth.HALF_PI) * radius;
        this.shardD.z = Mth.sin(orbitAngle - Mth.HALF_PI) * radius;
        this.shardD.y = 14.0F - bob * 0.7F;
        this.shardD.yRot = orbitAngle - Mth.HALF_PI;

        this.tailFlame.xRot = Mth.sin(ageInTicks * 0.15F) * 0.15F;
        this.tailFlame.zRot = Mth.cos(ageInTicks * 0.12F) * 0.1F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                int packedOverlay, float red, float green, float blue, float alpha) {
        core.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        shardA.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        shardB.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        shardC.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        shardD.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        tailFlame.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
