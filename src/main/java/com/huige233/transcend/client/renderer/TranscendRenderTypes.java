package com.huige233.transcend.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** 模组自定义渲染类型(RenderType)。 */
public class TranscendRenderTypes extends RenderType {

    private TranscendRenderTypes(String n, VertexFormat f, VertexFormat.Mode m, int b,
                                  boolean a, boolean s, Runnable r1, Runnable r2) {
        super(n, f, m, b, a, s, r1, r2);
    }

    private static final Function<ResourceLocation, RenderType> MAGIC_GLOW = Util.memoize(
            tex -> create("transcend_magic_glow",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, 256, false, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> MAGIC_GLOW_XRAY = Util.memoize(
            tex -> create("transcend_magic_glow_xray",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, 256, false, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setDepthTestState(NO_DEPTH_TEST)
                            .setWriteMaskState(COLOR_WRITE)
                            .createCompositeState(false)));

    private static final Function<ResourceLocation, RenderType> MAGIC_SOLID = Util.memoize(
            tex -> create("transcend_magic_solid",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS, 256, false, true,
                    CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                            .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                            .setTransparencyState(ADDITIVE_TRANSPARENCY)
                            .setCullState(CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(false)));

    private static final RenderType SHIELD_BACK = create("transcend_shield_back",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 65536, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(new RenderStateShard.CullStateShard(true) {

                        @Override public void setupRenderState() {
                            com.mojang.blaze3d.systems.RenderSystem.enableCull();
                            org.lwjgl.opengl.GL11.glCullFace(org.lwjgl.opengl.GL11.GL_FRONT);
                        }
                        @Override public void clearRenderState() {
                            org.lwjgl.opengl.GL11.glCullFace(org.lwjgl.opengl.GL11.GL_BACK);
                        }
                    })
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    private static final RenderType SHIELD_FRONT = create("transcend_shield_front",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 65536, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(CULL)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .createCompositeState(false));

    public static RenderType shieldBack()  { return SHIELD_BACK;  }
    public static RenderType shieldFront() { return SHIELD_FRONT; }

    public static RenderType magicGlow(ResourceLocation texture) {
        return MAGIC_GLOW.apply(texture);
    }

    public static RenderType magicGlowXray(ResourceLocation texture) {
        return MAGIC_GLOW_XRAY.apply(texture);
    }

    public static RenderType magicSolid(ResourceLocation texture) {
        return MAGIC_SOLID.apply(texture);
    }

    private static final RenderType MANA_BEAM = create(
            "transcend_mana_beam",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256, false, true,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setWriteMaskState(COLOR_WRITE)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));

    public static RenderType manaBeam() { return MANA_BEAM; }
}

