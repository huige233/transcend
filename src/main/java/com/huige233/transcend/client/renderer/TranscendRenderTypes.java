package com.huige233.transcend.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

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

    /**
     * Round 39: XRAY 版本的 magic glow — 关闭 depth test 让矿石/标记能穿墙显示。
     * 用于 OrebloodRevelation 矿石高亮（之前被墙体遮挡导致用户报告"显示没有正常工作"）。
     */
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

    // Shield sphere — two passes to avoid additive overdraw.
    // BACK pass: cull front faces → draws inner surface (back hemisphere visible).
    // FRONT pass: cull back faces (default) → draws outer surface (front hemisphere).
    // Each pixel is covered by exactly ONE quad per pass, so colours stay accurate.
    private static final RenderType SHIELD_BACK = create("transcend_shield_back",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 65536, false, false,
            CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(new RenderStateShard.CullStateShard(true) {
                        // "cull front" — enable GL cull but flip which face is culled
                        // Minecraft's CullStateShard just calls GL11.glEnable/Disable CULL_FACE.
                        // To cull FRONT faces we override with a custom shard.
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

    /** Round 39: 透墙版本 — 矿石透视用 */
    public static RenderType magicGlowXray(ResourceLocation texture) {
        return MAGIC_GLOW_XRAY.apply(texture);
    }

    public static RenderType magicSolid(ResourceLocation texture) {
        return MAGIC_SOLID.apply(texture);
    }

    /**
     * R62: 魔力传输水晶之间的激光束渲染类型。
     * <ul>
     *   <li>POSITION_COLOR 顶点格式 — 仅位置 + 颜色，不需贴图</li>
     *   <li>ADDITIVE_TRANSPARENCY — 线性叠加，多束重叠处自然变亮</li>
     *   <li>COLOR_WRITE only — 不写深度，让光束被半透明物体（粒子等）正确合成</li>
     *   <li>NO_CULL — 双面可见</li>
     * </ul>
     */
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

    /** R62: 魔力传输水晶之间的激光束渲染类型。 */
    public static RenderType manaBeam() { return MANA_BEAM; }
}

