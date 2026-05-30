package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.SpellGuardianModel;
import com.huige233.transcend.entity.SpellGuardian;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SpellGuardianRenderer extends MobRenderer<SpellGuardian, SpellGuardianModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/spell_guardian.png");

    public SpellGuardianRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpellGuardianModel(ctx.bakeLayer(SpellGuardianModel.LAYER)), 0.5F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull SpellGuardian entity) {
        return TEXTURE;
    }
}
