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
public class TranscendRuneParticle extends TextureSheetParticle {

    private final boolean glow;

    protected TranscendRuneParticle(ClientLevel level, double x, double y, double z,
                                    double vxOrTarget, double vyOrTarget, double vzEncoded,
                                    TranscendRuneParticleOptions options) {
        super(level, x, y, z);
        this.rCol = options.getColor().x();
        this.gCol = options.getColor().y();
        this.bCol = options.getColor().z();
        this.quadSize = options.getScale() * 0.12F;
        this.lifetime = options.getLifetime();
        this.glow = options.isGlow();
        this.alpha = 0F;
        this.hasPhysics = false;

        if (Math.abs(vzEncoded) >= 999.0) {
            int encoded = (int) (vzEncoded / 1000.0);
            double realVz = vzEncoded - encoded * 1000.0;
            this.xd = vxOrTarget;
            this.yd = vyOrTarget;
            this.zd = realVz;
        } else {
            this.xd = vxOrTarget;
            this.yd = vyOrTarget;
            this.zd = vzEncoded;
        }
    }

    protected void pickSpriteByIndex(SpriteSet spriteSet, int index) {
        if (index >= 0) {
            this.setSprite(spriteSet.get(index, 25));
        } else {
            this.pickSprite(spriteSet);
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return glow ? TranscendDustParticle.ADDITIVE_TRANSLUCENT : ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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

        if (progress < 0.15F) {
            this.alpha = progress / 0.15F;
        } else if (progress > 0.75F) {
            this.alpha = 1.0F - (progress - 0.75F) / 0.25F;
        } else {
            this.alpha = 1.0F;
        }

        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<TranscendRuneParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(TranscendRuneParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            int spriteIndex = -1;
            if (Math.abs(zd) >= 999.0) {
                spriteIndex = (int) (zd / 1000.0) - 1;
            }
            TranscendRuneParticle particle = new TranscendRuneParticle(level, x, y, z, xd, yd, zd, options);
            particle.pickSpriteByIndex(this.spriteSet, spriteIndex);
            return particle;
        }
    }
}
