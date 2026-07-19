package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.NexusCrystalModel;
import com.huige233.transcend.entity.nexus.NexusCrystalEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NexusCrystalRenderer extends MobRenderer<NexusCrystalEntity, NexusCrystalModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/nexus_crystal.png");

    public NexusCrystalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new NexusCrystalModel(ctx.bakeLayer(NexusCrystalModel.LAYER)), 0.3F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NexusCrystalEntity entity) {
        return TEXTURE;
    }
}
