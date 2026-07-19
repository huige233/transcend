package com.huige233.transcend.network;

import com.huige233.transcend.particle.TranscendRuneParticleOptions;
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

public class S2CRuneBatchPack {

    private final List<RuneEntry> entries;
    private final Vector3f color;
    private final float scale;
    private final int lifetime;
    private final boolean glow;

    public S2CRuneBatchPack(List<RuneEntry> entries, Vector3f color, float scale, int lifetime, boolean glow) {
        this.entries = entries;
        this.color = color;
        this.scale = scale;
        this.lifetime = lifetime;
        this.glow = glow;
    }

    public S2CRuneBatchPack(FriendlyByteBuf buf) {
        this.color = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
        this.scale = buf.readFloat();
        this.lifetime = buf.readInt();
        this.glow = buf.readBoolean();
        int count = buf.readVarInt();
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new RuneEntry(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readInt()
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
        for (RuneEntry e : entries) {
            buf.writeDouble(e.x);
            buf.writeDouble(e.y);
            buf.writeDouble(e.z);
            buf.writeDouble(e.targetX);
            buf.writeDouble(e.targetY);
            buf.writeDouble(e.targetZ);
            buf.writeInt(e.spriteIndex);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) return;
                TranscendRuneParticleOptions options = new TranscendRuneParticleOptions(color, scale, lifetime, glow);
                for (RuneEntry e : entries) {
                    double encodedZd;
                    if (e.spriteIndex >= 0) {
                        encodedZd = (e.spriteIndex + 1) * 1000.0 + e.targetZ;
                    } else {
                        encodedZd = e.targetZ;
                    }
                    level.addParticle(options, e.x, e.y, e.z,
                            e.targetX, e.targetY, encodedZd);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public static class RuneEntry {
        public final double x, y, z;
        public final double targetX, targetY, targetZ;
        public final int spriteIndex;

        public RuneEntry(double x, double y, double z, double targetX, double targetY, double targetZ, int spriteIndex) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.spriteIndex = spriteIndex;
        }

        public RuneEntry(double x, double y, double z, double targetX, double targetY, double targetZ) {
            this(x, y, z, targetX, targetY, targetZ, -1);
        }

        public RuneEntry(double x, double y, double z) {
            this(x, y, z, x, y, z, -1);
        }
    }
}
