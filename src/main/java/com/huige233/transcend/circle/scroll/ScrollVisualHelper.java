package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CShaderEffectPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/** 卷轴视觉辅助工具（粒子/音效）。 */
public final class ScrollVisualHelper {

    private static final double BROADCAST_RADIUS = 96.0;

    private ScrollVisualHelper() {}

    private static PacketDistributor.PacketTarget targetNear(ServerLevel level, BlockPos pos) {
        return PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                BROADCAST_RADIUS, level.dimension()));
    }

    public static void circle(ServerLevel level, BlockPos pos, float radius,
                               float r, float g, float b, int lifetime, String pattern) {
        Vec3 c = Vec3.atCenterOf(pos);
        NetworkHandler.CHANNEL.send(targetNear(level, pos),
                S2CShaderEffectPack.circle(c, radius, r, g, b, lifetime, 64, pattern));
    }

    public static void shockwave(ServerLevel level, BlockPos pos, float maxRadius,
                                  float r, float g, float b, int lifetime) {
        Vec3 c = Vec3.atCenterOf(pos);
        NetworkHandler.CHANNEL.send(targetNear(level, pos),
                S2CShaderEffectPack.shockwave(c, maxRadius, r, g, b, lifetime));
    }

    public static void shieldRipple(ServerLevel level, BlockPos pos, float radius,
                                     float r, float g, float b, int lifetime) {
        Vec3 c = Vec3.atCenterOf(pos);
        NetworkHandler.CHANNEL.send(targetNear(level, pos),
                S2CShaderEffectPack.shieldRipple(c, radius, r, g, b, lifetime));
    }

    public static void beam(ServerLevel level, BlockPos from, BlockPos to,
                             float r, float g, float b, int lifetime) {
        Vec3 fromV = Vec3.atCenterOf(from);
        Vec3 toV = Vec3.atCenterOf(to);
        NetworkHandler.CHANNEL.send(targetNear(level, from),
                S2CShaderEffectPack.beam(fromV, toV, r, g, b, lifetime, "beam"));
    }

    public static void pillar(ServerLevel level, Vec3 from, Vec3 to,
                               float r, float g, float b, int lifetime) {

        BlockPos centerPos = new BlockPos((int) from.x, (int) from.y, (int) from.z);
        NetworkHandler.CHANNEL.send(targetNear(level, centerPos),
                S2CShaderEffectPack.beam(from, to, r, g, b, lifetime, "pillar"));
    }

    public static void pillarFromSky(ServerLevel level, Vec3 targetPos,
                                      float skyHeight, float r, float g, float b, int lifetime) {
        Vec3 sky = targetPos.add(0, skyHeight, 0);
        pillar(level, sky, targetPos, r, g, b, lifetime);
    }

    public static void pillarFromGround(ServerLevel level, Vec3 groundPos,
                                         float height, float r, float g, float b, int lifetime) {
        Vec3 sky = groundPos.add(0, height, 0);
        pillar(level, groundPos, sky, r, g, b, lifetime);
    }
}
