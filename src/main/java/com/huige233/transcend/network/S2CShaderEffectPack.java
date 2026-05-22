package com.huige233.transcend.network;

import com.huige233.transcend.client.renderer.ShaderSpellRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Round 39: 服务端 → 客户端：触发 ShaderSpellRenderer 视觉效果。
 *
 * <p>之前 mod 中各处（boss / scroll）直接调用 {@link ShaderSpellRenderer#addCircle}
 * 等静态方法，但 ShaderSpellRenderer 的 pendingCircles/pendingSpells 队列是 PER-JVM。
 * 服务端调用 → 入服务端队列 → 渲染器只在客户端运行 → 视觉永不触发。
 *
 * <p>本包让服务端把效果描述发给附近客户端，客户端解码后才入本地队列触发渲染。
 *
 * <p>支持四种效果（与 ShaderSpellRenderer 静态方法一一对应）：
 * <ul>
 *   <li>CIRCLE — 地面法环（{@code addCircle}）</li>
 *   <li>SHOCKWAVE — 扩张冲击波（{@code addShockwave}）</li>
 *   <li>SHIELD_RIPPLE — 球形护盾涟漪（{@code addShieldRipple}）</li>
 *   <li>BEAM — 直线法术光束（{@code addSpellEffect}）</li>
 * </ul>
 */
public class S2CShaderEffectPack {

    public enum EffectType {
        CIRCLE, SHOCKWAVE, SHIELD_RIPPLE, BEAM;

        public static EffectType byOrdinal(int i) {
            EffectType[] all = values();
            if (i < 0 || i >= all.length) return CIRCLE;
            return all[i];
        }
    }

    private final EffectType type;
    private final Vec3 center;
    private final Vec3 toOrSize;
    private final float r, g, b;
    private final int lifetime;
    private final int segments;
    private final String pattern;

    public S2CShaderEffectPack(EffectType type, Vec3 center, Vec3 toOrSize,
                                float r, float g, float b, int lifetime, int segments, String pattern) {
        this.type = type;
        this.center = center;
        this.toOrSize = toOrSize;
        this.r = r;
        this.g = g;
        this.b = b;
        this.lifetime = lifetime;
        this.segments = segments;
        this.pattern = pattern == null ? "" : pattern;
    }

    /** 解码构造器 */
    public S2CShaderEffectPack(FriendlyByteBuf buf) {
        this.type = EffectType.byOrdinal(buf.readVarInt());
        this.center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.toOrSize = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        this.r = buf.readFloat();
        this.g = buf.readFloat();
        this.b = buf.readFloat();
        this.lifetime = buf.readVarInt();
        this.segments = buf.readVarInt();
        this.pattern = buf.readUtf(32);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(type.ordinal());
        buf.writeDouble(center.x); buf.writeDouble(center.y); buf.writeDouble(center.z);
        buf.writeDouble(toOrSize.x); buf.writeDouble(toOrSize.y); buf.writeDouble(toOrSize.z);
        buf.writeFloat(r); buf.writeFloat(g); buf.writeFloat(b);
        buf.writeVarInt(lifetime);
        buf.writeVarInt(segments);
        buf.writeUtf(pattern, 32);
    }

    public void run(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    switch (type) {
                        case CIRCLE -> ShaderSpellRenderer.addCircle(
                                center, (float) toOrSize.x, r, g, b, lifetime,
                                segments == 0 ? 64 : segments, pattern);
                        case SHOCKWAVE -> ShaderSpellRenderer.addShockwave(
                                center, (float) toOrSize.x, r, g, b, lifetime);
                        case SHIELD_RIPPLE -> ShaderSpellRenderer.addShieldRipple(
                                center, (float) toOrSize.x, r, g, b, lifetime);
                        case BEAM -> ShaderSpellRenderer.addSpellEffect(
                                center, toOrSize, r, g, b, lifetime,
                                pattern.isEmpty() ? "beam" : pattern);
                    }
                }));
        ctx.get().setPacketHandled(true);
    }

    // ─── 便捷工厂（服务端使用）───

    public static S2CShaderEffectPack circle(Vec3 center, float radius, float r, float g, float b,
                                              int lifetime, int segments, String pattern) {
        return new S2CShaderEffectPack(EffectType.CIRCLE, center, new Vec3(radius, 0, 0),
                r, g, b, lifetime, segments, pattern);
    }

    public static S2CShaderEffectPack shockwave(Vec3 center, float maxRadius, float r, float g, float b, int lifetime) {
        return new S2CShaderEffectPack(EffectType.SHOCKWAVE, center, new Vec3(maxRadius, 0, 0),
                r, g, b, lifetime, 0, "");
    }

    public static S2CShaderEffectPack shieldRipple(Vec3 center, float radius, float r, float g, float b, int lifetime) {
        return new S2CShaderEffectPack(EffectType.SHIELD_RIPPLE, center, new Vec3(radius, 0, 0),
                r, g, b, lifetime, 0, "");
    }

    public static S2CShaderEffectPack beam(Vec3 from, Vec3 to, float r, float g, float b, int lifetime, String type) {
        return new S2CShaderEffectPack(EffectType.BEAM, from, to, r, g, b, lifetime, 0,
                type == null ? "beam" : type);
    }
}
