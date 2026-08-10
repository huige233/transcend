package com.huige233.transcend.client.model;

import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import com.huige233.transcend.entity.boss.BossPhase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 超越首领通用模型（人形，泛型适配各首领）。 */
public class TranscendBossModel<T extends AbstractTranscendBoss> extends HumanoidModel<T> {

    public static final ModelLayerLocation WARDEN_LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "elemental_warden"), "main");
    public static final ModelLayerLocation WEAVER_LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "void_weaver"), "main");
    public static final ModelLayerLocation AVATAR_LAYER =
            new ModelLayerLocation(new ResourceLocation("transcend", "transcendence_avatar"), "main");

    public final ModelPart leftWing;
    public final ModelPart rightWing;
    private final boolean hasWings;

    public TranscendBossModel(ModelPart root, boolean hasWings) {
        super(root);
        this.hasWings = hasWings;
        if (hasWings) {
            this.leftWing = root.getChild("left_wing");
            this.rightWing = root.getChild("right_wing");
        } else {
            this.leftWing = null;
            this.rightWing = null;
        }
    }

    public static LayerDefinition createWardenMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.5F)), PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.3F)), PartPose.ZERO);

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F,
                        new CubeDeformation(0.3F)), PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F,
                        new CubeDeformation(0.3F)), PartPose.offset(5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.2F)), PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F,
                        new CubeDeformation(0.2F)), PartPose.offset(1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createWeaverMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                        new CubeDeformation(0.6F)), PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 16.0F, 6.0F,
                        new CubeDeformation(0.2F)), PartPose.ZERO);

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-2.0F, -2.0F, -1.5F, 3.0F, 16.0F, 3.0F,
                        new CubeDeformation(0.1F)), PartPose.offset(-6.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -1.5F, 3.0F, 16.0F, 3.0F,
                        new CubeDeformation(0.1F)), PartPose.offset(6.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F,
                        new CubeDeformation(0.3F)), PartPose.offset(-2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F,
                        new CubeDeformation(0.3F)), PartPose.offset(2.0F, 12.0F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createAvatarMesh() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F))

                .texOffs(32, 0).addBox(-5.0F, -14.0F, -1.0F, 2.0F, 7.0F, 2.0F)
                .texOffs(32, 0).addBox(3.0F,  -14.0F, -1.0F, 2.0F, 7.0F, 2.0F)

                .texOffs(40, 0).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 1.5F, 9.0F, new CubeDeformation(-0.3F)),
                PartPose.ZERO);

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.3F))

                .texOffs(0, 48).addBox(-6.5F, -1.0F, -2.5F, 2.5F, 4.0F, 5.0F)
                .texOffs(0, 48).mirror().addBox(4.0F, -1.0F, -2.5F, 2.5F, 4.0F, 5.0F)

                .texOffs(40, 48).addBox(-3.5F, 1.0F, -3.2F, 7.0F, 7.0F, 1.0F, new CubeDeformation(-0.1F))

                .texOffs(16, 48).addBox(-4.5F, 11.0F, -2.5F, 9.0F, 2.0F, 5.0F, new CubeDeformation(-0.2F)),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F,
                        new CubeDeformation(0.2F)), PartPose.offset(-6.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 14.0F, 4.0F,
                        new CubeDeformation(0.2F)), PartPose.offset(6.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F,
                        new CubeDeformation(0.15F)), PartPose.offset(-2.2F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F,
                        new CubeDeformation(0.15F)), PartPose.offset(2.2F, 12.0F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("left_wing", CubeListBuilder.create()
                .texOffs(0, 32).addBox(0.0F, -8.0F, 0.0F, 10.0F, 16.0F, 1.0F),
                PartPose.offsetAndRotation(2.0F, 2.0F, 2.0F, 0.0F, 0.25F, 0.05F));

        root.addOrReplaceChild("right_wing", CubeListBuilder.create()
                .texOffs(0, 32).mirror().addBox(-10.0F, -8.0F, 0.0F, 10.0F, 16.0F, 1.0F),
                PartPose.offsetAndRotation(-2.0F, 2.0F, 2.0F, 0.0F, -0.25F, -0.05F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        if (hasWings && leftWing != null && rightWing != null) {
            float flapSpeed = entity.getCurrentPhase() == BossPhase.PHASE_4 ? 0.15F : 0.08F;
            float flapAmplitude = entity.getCurrentPhase() == BossPhase.PHASE_4 ? 0.5F : 0.3F;
            float flap = Mth.sin(ageInTicks * flapSpeed) * flapAmplitude;

            leftWing.yRot = 0.4F + flap;
            leftWing.zRot = 0.1F - flap * 0.3F;
            rightWing.yRot = -(0.4F + flap);
            rightWing.zRot = -(0.1F - flap * 0.3F);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                                int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        if (hasWings && leftWing != null && rightWing != null) {
            leftWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
            rightWing.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}
