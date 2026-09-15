package com.huige233.transcend.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


/** 保留旧着色器特效的数据包格式与构造入口，接收时仅标记处理完成而不执行渲染。 */
public class S2CShaderEffectPack {

    /** 定义旧特效协议中的圆环、冲击波、护盾涟漪和光束类型，并为无效序号提供回退。 */
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
        ctx.get().setPacketHandled(true);
    }

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
