package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.TestDummy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class TestDummyModel extends HumanoidModel<TestDummy> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "test_dummy"), "main");

    public TestDummyModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partDef = mesh.getRoot();

        // Wooden post under the dummy
        partDef.addOrReplaceChild("post",
                CubeListBuilder.create()
                        .texOffs(0, 48)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        // Cross beam (arms rest)
        partDef.addOrReplaceChild("beam",
                CubeListBuilder.create()
                        .texOffs(8, 48)
                        .addBox(-8.0F, -1.0F, -1.0F, 16.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TestDummy entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Static pose — no animation
        this.head.xRot = 0;
        this.head.yRot = 0;
        this.body.xRot = 0;
        this.rightArm.xRot = 0;
        this.rightArm.zRot = 0.1F;
        this.leftArm.xRot = 0;
        this.leftArm.zRot = -0.1F;
        this.rightLeg.xRot = 0;
        this.leftLeg.xRot = 0;
    }
}
