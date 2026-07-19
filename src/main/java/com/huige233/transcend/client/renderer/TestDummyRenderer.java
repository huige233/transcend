package com.huige233.transcend.client.renderer;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.client.model.TestDummyModel;
import com.huige233.transcend.entity.TestDummy;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class TestDummyRenderer extends MobRenderer<TestDummy, TestDummyModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Transcend.MODID, "textures/entity/test_dummy.png");

    public TestDummyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new TestDummyModel(ctx.bakeLayer(TestDummyModel.LAYER)), 0.4F);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull TestDummy entity) {
        return TEXTURE;
    }
}
