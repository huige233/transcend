package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.nexus.NexusGuardian;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 法则守卫模型 — 身披厚重铠甲的人形守卫，带有披风和头盔角饰。
 * 基于 HumanoidModel，添加了肩甲、披风和头盔装饰部件。
 * 64×64 纹理。
 */
public class NexusGuardianModel extends HumanoidModel<NexusGuardian> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "nexus_guardian"), "main");

    private final ModelPart cape;
    private final ModelPart leftShoulderPad;
    private final ModelPart rightShoulderPad;
    private final ModelPart hornLeft;
    private final ModelPart hornRight;

    public NexusGuardianModel(ModelPart root) {
        super(root);
        this.cape = root.getChild("cape");
        this.leftShoulderPad = root.getChild("left_shoulder_pad");
        this.rightShoulderPad = root.getChild("right_shoulder_pad");
        this.hornLeft = root.getChild("horn_left");
        this.hornRight = root.getChild("horn_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // Head — slightly larger helmet
        root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F,
                                new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Hat layer (helmet visor detail)
        root.addOrReplaceChild("hat",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F,
                                new CubeDeformation(0.35F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Body — thicker (armor plate)
        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(16, 16)
                        .addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // Arms — wider for gauntlets
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-4.0F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16).mirror()
                        .addBox(-1.0F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        // Legs — armored greaves
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(0, 16).mirror()
                        .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        // ─── Custom parts ───

        // Cape (attached to back of body)
        root.addOrReplaceChild("cape",
                CubeListBuilder.create()
                        .texOffs(0, 33)
                        .addBox(-5.0F, 0.0F, 0.0F, 10.0F, 14.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, 3.0F));

        // Left shoulder pad
        root.addOrReplaceChild("left_shoulder_pad",
                CubeListBuilder.create()
                        .texOffs(22, 33)
                        .addBox(-2.0F, -3.0F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offset(6.0F, 1.0F, 0.0F));

        // Right shoulder pad
        root.addOrReplaceChild("right_shoulder_pad",
                CubeListBuilder.create()
                        .texOffs(22, 33).mirror()
                        .addBox(-2.0F, -3.0F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offset(-6.0F, 1.0F, 0.0F));

        // Helmet horns
        root.addOrReplaceChild("horn_left",
                CubeListBuilder.create()
                        .texOffs(42, 33)
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(3.5F, -8.5F, 0.0F,
                        0.0F, 0.0F, -0.3F));

        root.addOrReplaceChild("horn_right",
                CubeListBuilder.create()
                        .texOffs(42, 33).mirror()
                        .addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(-3.5F, -8.5F, 0.0F,
                        0.0F, 0.0F, 0.3F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(NexusGuardian entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Cape sways gently
        float capeSwing = Mth.cos(ageInTicks * 0.08F) * 0.06F;
        this.cape.xRot = 0.15F + capeSwing;

        // Shoulder pads follow arms slightly
        this.leftShoulderPad.xRot = this.leftArm.xRot * 0.5F;
        this.rightShoulderPad.xRot = this.rightArm.xRot * 0.5F;

        // Horns are static (part of helmet)
        this.hornLeft.visible = true;
        this.hornRight.visible = true;
    }
}
