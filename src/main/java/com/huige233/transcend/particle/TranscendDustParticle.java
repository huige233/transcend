package com.huige233.transcend.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TranscendDustParticle extends TextureSheetParticle {

    private final boolean glow;
    private final float baseAlpha;

    public static final ParticleRenderType ADDITIVE_TRANSLUCENT = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "TRANSCEND_ADDITIVE_TRANSLUCENT";
        }
    };

    protected TranscendDustParticle(ClientLevel level, double x, double y, double z,
                                    double xd, double yd, double zd,
                                    TranscendDustParticleOptions options) {
        super(level, x, y, z, xd, yd, zd);
        this.rCol = options.getColor().x();
        this.gCol = options.getColor().y();
        this.bCol = options.getColor().z();
        this.quadSize = options.getScale() * 0.15F;
        this.lifetime = options.getLifetime();
        this.glow = options.isGlow();
        this.baseAlpha = 1.0F;
        this.alpha = 1.0F;
        this.hasPhysics = false;
        this.xd = xd * 0.01;
        this.yd = yd * 0.01;
        this.zd = zd * 0.01;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return glow ? ADDITIVE_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return glow ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = (float) this.age / this.lifetime;

        if (progress < 0.2F) {
            this.alpha = baseAlpha * (progress / 0.2F);
        } else if (progress > 0.7F) {
            this.alpha = baseAlpha * (1.0F - (progress - 0.7F) / 0.3F);
        } else {
            this.alpha = baseAlpha;
        }

        this.move(this.xd, this.yd, this.zd);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<TranscendDustParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(TranscendDustParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            TranscendDustParticle particle = new TranscendDustParticle(level, x, y, z, xd, yd, zd, options);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}
