package com.huige233.transcend.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class S2CVanillaParticleBatchPack {

    private final List<VanillaParticleEntry> entries;
    private final String particleId;

    public S2CVanillaParticleBatchPack(List<VanillaParticleEntry> entries, String particleId) {
        this.entries = entries;
        this.particleId = particleId;
    }

    public S2CVanillaParticleBatchPack(FriendlyByteBuf buf) {
        this.particleId = buf.readUtf();
        int count = buf.readVarInt();
        this.entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new VanillaParticleEntry(
                    buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble()
            ));
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(particleId);
        buf.writeVarInt(entries.size());
        for (VanillaParticleEntry e : entries) {
            buf.writeDouble(e.x);
            buf.writeDouble(e.y);
            buf.writeDouble(e.z);
            buf.writeDouble(e.xd);
            buf.writeDouble(e.yd);
            buf.writeDouble(e.zd);
        }
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level == null) return;

                SimpleParticleType type = resolveParticle(particleId);
                if (type == null) return;

                for (VanillaParticleEntry e : entries) {
                    level.addParticle(type, e.x, e.y, e.z, e.xd, e.yd, e.zd);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static SimpleParticleType resolveParticle(String id) {
        return switch (id) {
            case "enchant" -> ParticleTypes.ENCHANT;
            case "witch" -> ParticleTypes.WITCH;
            case "portal" -> ParticleTypes.PORTAL;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "soul_fire_flame" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "flame" -> ParticleTypes.FLAME;
            case "snowflake" -> ParticleTypes.SNOWFLAKE;
            case "smoke" -> ParticleTypes.SMOKE;
            case "campfire_cosy_smoke" -> ParticleTypes.CAMPFIRE_COSY_SMOKE;
            default -> null;
        };
    }

    public static class VanillaParticleEntry {
        public final double x, y, z;
        public final double xd, yd, zd;

        public VanillaParticleEntry(double x, double y, double z, double xd, double yd, double zd) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
        }

        public VanillaParticleEntry(double x, double y, double z) {
            this(x, y, z, 0, 0, 0);
        }
    }
}
