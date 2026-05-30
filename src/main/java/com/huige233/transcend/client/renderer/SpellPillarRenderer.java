package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.SpellPillarModel;
import com.huige233.transcend.entity.SpellPillar;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SpellPillarRenderer extends MobRenderer<SpellPillar, SpellPillarModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/spell_pillar.png");

    public SpellPillarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpellPillarModel(ctx.bakeLayer(SpellPillarModel.LAYER)), 0.4F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpellPillar entity) {
        return TEXTURE;
    }
}
