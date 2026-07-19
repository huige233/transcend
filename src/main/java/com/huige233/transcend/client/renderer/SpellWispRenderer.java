package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.SpellWispModel;
import com.huige233.transcend.entity.SpellWisp;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SpellWispRenderer extends MobRenderer<SpellWisp, SpellWispModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/spell_wisp.png");

    public SpellWispRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpellWispModel(ctx.bakeLayer(SpellWispModel.LAYER)), 0.3F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpellWisp entity) {
        return TEXTURE;
    }
}
