package com.huige233.transcend.network;

import com.huige233.transcend.init.ModParticles;
import com.huige233.transcend.particle.TranscendDustParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CParticleBatchPack {

    private final List<ParticleEntry> entries;
    private final Vector3f color;
    private final float scale;
    private final int lifetime;
    private final boolean glow;

    public S2CParticleBatchPack(List<ParticleEntry> entries, Vector3f color, float scale, int lifetime, boolean glow) {
        this.entries = entries;
        this.color = color;
        this.scale = scale;
        this.lifetime = lifetime;
        this.glow = glow;
    }

    public S2CParticleBatchPack(FriendlyByteBuf buf) {
        this.color = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.scale = buf.readFloat();
        this.lifetime = buf.readInt();
        this.glow = buf.readBoolean();
        int count = buf.readVarInt();
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new ParticleEntry(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat()
            ));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(color.x());
        buf.writeFloat(color.y());
        buf.writeFloat(color.z());
        buf.writeFloat(scale);
        buf.writeInt(lifetime);
        buf.writeBoolean(glow);
        buf.writeVarInt(entries.size());
        for (ParticleEntry e : entries) {
            buf.writeDouble(e.x);
            buf.writeDouble(e.y);
            buf.writeDouble(e.z);
            buf.writeFloat(e.xd);
            buf.writeFloat(e.yd);
            buf.writeFloat(e.zd);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) return;
                TranscendDustParticleOptions options = new TranscendDustParticleOptions(color, scale, lifetime, glow);
                for (ParticleEntry e : entries) {
                    level.addParticle(options, e.x, e.y, e.z, e.xd, e.yd, e.zd);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public static class ParticleEntry {
        public final double x, y, z;
        public final float xd, yd, zd;

        public ParticleEntry(double x, double y, double z, float xd, float yd, float zd) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
        }

        public ParticleEntry(double x, double y, double z) {
            this(x, y, z, 0, 0, 0);
        }
    }

    public static class Builder {
        private final List<ParticleEntry> entries = new ArrayList<>();
        private Vector3f color = new Vector3f(0, 1, 1);
        private float scale = 1.0F;
        private int lifetime = 20;
        private boolean glow = true;

        public Builder color(float r, float g, float b) {
            this.color = new Vector3f(r, g, b);
            return this;
        }

        public Builder color(int r, int g, int b) {
            this.color = new Vector3f(r / 255F, g / 255F, b / 255F);
            return this;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder lifetime(int ticks) {
            this.lifetime = ticks;
            return this;
        }

        public Builder glow(boolean glow) {
            this.glow = glow;
            return this;
        }

        public Builder addParticle(double x, double y, double z) {
            entries.add(new ParticleEntry(x, y, z));
            return this;
        }

        public Builder addParticle(double x, double y, double z, float xd, float yd, float zd) {
            entries.add(new ParticleEntry(x, y, z, xd, yd, zd));
            return this;
        }

        public S2CParticleBatchPack build() {
            return new S2CParticleBatchPack(entries, color, scale, lifetime, glow);
        }
    }
}
