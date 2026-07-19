package com.huige233.transcend.visual;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CGlitterBatchPack;
import com.huige233.transcend.network.S2CParticleBatchPack;
import com.huige233.transcend.network.S2CRuneBatchPack;
import com.huige233.transcend.network.S2CShaderEffectPack;
import com.huige233.transcend.network.S2CVanillaParticleBatchPack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;

public final class ServerVisualBroadcaster {
    public static final double SHADER_BROADCAST_RADIUS = 96.0;
    public static final double PARTICLE_BROADCAST_RADIUS = 64.0;

    private ServerVisualBroadcaster() {}

    public static void circle(ServerLevel level, Vec3 center, float radius,
                              float r, float g, float b, int lifetime, int segments, String pattern) {
        sendNear(level, center, SHADER_BROADCAST_RADIUS,
                circlePacket(center, radius, r, g, b, lifetime, segments, pattern));
    }

    public static void shockwave(ServerLevel level, Vec3 center, float maxRadius,
                                 float r, float g, float b, int lifetime) {
        sendNear(level, center, SHADER_BROADCAST_RADIUS,
                shockwavePacket(center, maxRadius, r, g, b, lifetime));
    }

    public static void shieldRipple(ServerLevel level, Vec3 center, float radius,
                                    float r, float g, float b, int lifetime) {
        sendNear(level, center, SHADER_BROADCAST_RADIUS,
                shieldRipplePacket(center, radius, r, g, b, lifetime));
    }

    public static void beam(ServerLevel level, Vec3 from, Vec3 to,
                            float r, float g, float b, int lifetime, String pattern) {
        sendNear(level, from, SHADER_BROADCAST_RADIUS,
                beamPacket(from, to, r, g, b, lifetime, pattern));
    }

    public static void particleBatch(ServerLevel level, Vec3 center,
                                     List<S2CParticleBatchPack.ParticleEntry> entries,
                                     float r, float g, float b, float scale, int lifetime, boolean glow) {
        Objects.requireNonNull(entries, "entries");
        validateVisual(center, scale, r, g, b, lifetime, 0);
        if (entries.isEmpty()) return;
        sendNear(level, center, PARTICLE_BROADCAST_RADIUS,
                new S2CParticleBatchPack(entries, new Vector3f(r, g, b), scale, lifetime, glow));
    }

    public static void vanillaParticleBatch(ServerLevel level, Vec3 center,
                                            List<S2CVanillaParticleBatchPack.VanillaParticleEntry> entries,
                                            String particleId) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(particleId, "particleId");
        requireVector(center, "center");
        if (entries.isEmpty()) return;
        sendNear(level, center, PARTICLE_BROADCAST_RADIUS,
                new S2CVanillaParticleBatchPack(entries, particleId));
    }

    public static void runeBatch(ServerLevel level, Vec3 center,
                                 List<S2CRuneBatchPack.RuneEntry> entries,
                                 float r, float g, float b, float scale, int lifetime, boolean glow) {
        Objects.requireNonNull(entries, "entries");
        validateVisual(center, scale, r, g, b, lifetime, 0);
        if (entries.isEmpty()) return;
        sendNear(level, center, PARTICLE_BROADCAST_RADIUS,
                new S2CRuneBatchPack(entries, new Vector3f(r, g, b), scale, lifetime, glow));
    }

    public static void glitterBatch(ServerLevel level, Vec3 center,
                                    List<S2CGlitterBatchPack.GlitterEntry> entries,
                                    float r, float g, float b, float scale, int lifetime, boolean glow) {
        Objects.requireNonNull(entries, "entries");
        validateVisual(center, scale, r, g, b, lifetime, 0);
        if (entries.isEmpty()) return;
        sendNear(level, center, PARTICLE_BROADCAST_RADIUS,
                new S2CGlitterBatchPack(entries, new Vector3f(r, g, b), scale, lifetime, glow));
    }

    static S2CShaderEffectPack circlePacket(Vec3 center, float radius,
                                            float r, float g, float b, int lifetime,
                                            int segments, String pattern) {
        validateVisual(center, radius, r, g, b, lifetime, segments);
        return S2CShaderEffectPack.circle(center, radius, r, g, b, lifetime, segments, pattern);
    }

    static S2CShaderEffectPack shockwavePacket(Vec3 center, float maxRadius,
                                               float r, float g, float b, int lifetime) {
        validateVisual(center, maxRadius, r, g, b, lifetime, 0);
        return S2CShaderEffectPack.shockwave(center, maxRadius, r, g, b, lifetime);
    }

    static S2CShaderEffectPack shieldRipplePacket(Vec3 center, float radius,
                                                  float r, float g, float b, int lifetime) {
        validateVisual(center, radius, r, g, b, lifetime, 0);
        return S2CShaderEffectPack.shieldRipple(center, radius, r, g, b, lifetime);
    }

    static S2CShaderEffectPack beamPacket(Vec3 from, Vec3 to,
                                          float r, float g, float b, int lifetime, String pattern) {
        requireVector(to, "to");
        validateVisual(from, 0.0F, r, g, b, lifetime, 0);
        return S2CShaderEffectPack.beam(from, to, r, g, b, lifetime, pattern);
    }

    static PacketDistributor.TargetPoint targetPoint(ResourceKey<Level> dimension, Vec3 center, double radius) {
        Objects.requireNonNull(dimension, "dimension");
        requireVector(center, "center");
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("broadcast radius must be finite and non-negative");
        }
        return new PacketDistributor.TargetPoint(center.x, center.y, center.z, radius, dimension);
    }

    static boolean isRecipient(Object effectDimension, Vec3 center,
                               Object playerDimension, Vec3 playerPosition, double radius) {
        Objects.requireNonNull(effectDimension, "effectDimension");
        Objects.requireNonNull(playerDimension, "playerDimension");
        requireVector(center, "center");
        requireVector(playerPosition, "playerPosition");
        if (!Double.isFinite(radius) || radius < 0.0) {
            throw new IllegalArgumentException("broadcast radius must be finite and non-negative");
        }
        return effectDimension.equals(playerDimension) && center.distanceToSqr(playerPosition) < radius * radius;
    }

    private static void sendNear(ServerLevel level, Vec3 center, double radius, Object packet) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(packet, "packet");
        PacketDistributor.TargetPoint point = targetPoint(level.dimension(), center, radius);
        NetworkHandler.CHANNEL.send(PacketDistributor.NEAR.with(() -> point), packet);
    }

    private static void validateVisual(Vec3 center, float size,
                                       float r, float g, float b, int lifetime, int segments) {
        requireVector(center, "center");
        requireFinite(size, "size");
        requireFinite(r, "red");
        requireFinite(g, "green");
        requireFinite(b, "blue");
        if (lifetime <= 0) throw new IllegalArgumentException("lifetime must be positive");
        if (segments < 0) throw new IllegalArgumentException("segments must be non-negative");
    }

    private static void requireVector(Vec3 vector, String name) {
        Objects.requireNonNull(vector, name);
        if (!Double.isFinite(vector.x) || !Double.isFinite(vector.y) || !Double.isFinite(vector.z)) {
            throw new IllegalArgumentException(name + " must contain finite coordinates");
        }
    }

    private static void requireFinite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite");
    }
}
