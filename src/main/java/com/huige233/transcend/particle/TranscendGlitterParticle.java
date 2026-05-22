package com.huige233.transcend.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TranscendGlitterParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;
    private final boolean glow;

    protected TranscendGlitterParticle(ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd,
                                       TranscendGlitterParticleOptions options,
                                       SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);
        this.spriteSet = spriteSet;
        this.rCol = options.getColor().x();
        this.gCol = options.getColor().y();
        this.bCol = options.getColor().z();
        this.quadSize = options.getScale() * 0.12F;
        this.lifetime = options.getLifetime();
        this.glow = options.isGlow();
        this.alpha = 1.0F;
        this.hasPhysics = false;
        this.xd = xd * 0.01;
        this.yd = yd * 0.01;
        this.zd = zd * 0.01;
        this.setSpriteFromAge(this.spriteSet);
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

        this.setSpriteFromAge(this.spriteSet);

        float progress = (float) this.age / this.lifetime;
        if (progress < 0.2F) {
            this.alpha = progress / 0.2F;
        } else if (progress > 0.7F) {
            this.alpha = 1.0F - (progress - 0.7F) / 0.3F;
        } else {
            this.alpha = 1.0F;
        }

        this.xd *= 0.96;
        this.yd *= 0.96;
        this.zd *= 0.96;
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<TranscendGlitterParticleOptions> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(TranscendGlitterParticleOptions options, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new TranscendGlitterParticle(level, x, y, z, xd, yd, zd, options, this.spriteSet);
        }
    }
}
