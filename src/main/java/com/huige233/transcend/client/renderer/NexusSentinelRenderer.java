package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.NexusSentinelModel;
import com.huige233.transcend.entity.nexus.NexusSentinel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * 法则哨兵渲染器 — 浮空晶体生物，带半透明发光效果。
 */
public class NexusSentinelRenderer extends MobRenderer<NexusSentinel, NexusSentinelModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/nexus_sentinel.png");

    public NexusSentinelRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NexusSentinelModel(ctx.bakeLayer(NexusSentinelModel.LAYER)), 0.3F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NexusSentinel entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(@NotNull NexusSentinel entity, @NotNull net.minecraft.core.BlockPos pos) {
        return 15; // Always fully lit (self-glowing)
    }
}
