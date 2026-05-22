package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.NexusGuardianModel;
import com.huige233.transcend.entity.nexus.NexusGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 法则守卫渲染器 — 使用铠甲人形模型。
 */
public class NexusGuardianRenderer extends MobRenderer<NexusGuardian, NexusGuardianModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/nexus_guardian.png");

    public NexusGuardianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NexusGuardianModel(ctx.bakeLayer(NexusGuardianModel.LAYER)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NexusGuardian entity) {
        return TEXTURE;
    }
}
