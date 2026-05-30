package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.TranscendFamiliarModel;
import com.huige233.transcend.entity.familiar.TranscendFamiliar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TranscendFamiliarRenderer extends MobRenderer<TranscendFamiliar, TranscendFamiliarModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/transcend_familiar.png");

    public TranscendFamiliarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TranscendFamiliarModel(ctx.bakeLayer(TranscendFamiliarModel.LAYER)), 0.4F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull TranscendFamiliar entity) {
        return TEXTURE;
    }
}
