package com.huige233.transcend.circle.scroll;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CShaderEffectPack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Round 39: 卷轴视觉效果广播工具类。
 *
 * <p>scroll execute() 在服务端运行，但 ShaderSpellRenderer 队列在客户端 JVM。
 * 直接调静态方法不工作。本工具发 S2CShaderEffectPack 给附近玩家 → 客户端入队渲染。
 *
 * <p>每张卷轴的视觉特征：
 * <table>
 *   <tr><th>效果</th><th>用法</th></tr>
 *   <tr><td>circle</td><td>地面法环 — 多用于持续 buff/debuff（chronal/aegis/eclipse）</td></tr>
 *   <tr><td>shockwave</td><td>扩张冲击波 — 多用于 AOE 伤害（solar/leyline/avatar）</td></tr>
 *   <tr><td>shieldRipple</td><td>球形涟漪 — 多用于护盾/包裹（aegis/worldmender/fog）</td></tr>
 *   <tr><td>beam</td><td>直线光束 — 多用于点对点（return/exile）</td></tr>
 * </table>
 */
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

    /**
     * Round 41: 垂直光柱 — 在指定坐标处生成 from→to 的发光柱。
     * 推荐用法：
     * <ul>
     *   <li>下落型（日裁天罚 / 黑日 / 化身坠落）：from = pos+(0,30,0), to = pos</li>
     *   <li>升起型（地脉喷发 / 灵脉归一）：from = pos, to = pos+(0,8,0)</li>
     * </ul>
     */
    public static void pillar(ServerLevel level, Vec3 from, Vec3 to,
                               float r, float g, float b, int lifetime) {
        // 用 from 位置作为 broadcast center（pos 转 BlockPos 用于 PacketDistributor.NEAR）
        BlockPos centerPos = new BlockPos((int) from.x, (int) from.y, (int) from.z);
        NetworkHandler.CHANNEL.send(targetNear(level, centerPos),
                S2CShaderEffectPack.beam(from, to, r, g, b, lifetime, "pillar"));
    }

    /** 便捷: 从天空（pos.y + height）下落到 pos 的光柱（用于审判/坠落型攻击） */
    public static void pillarFromSky(ServerLevel level, Vec3 targetPos,
                                      float skyHeight, float r, float g, float b, int lifetime) {
        Vec3 sky = targetPos.add(0, skyHeight, 0);
        pillar(level, sky, targetPos, r, g, b, lifetime);
    }

    /** 便捷: 从地面 pos 向上升起 height 的光柱（用于喷发/灵脉型） */
    public static void pillarFromGround(ServerLevel level, Vec3 groundPos,
                                         float height, float r, float g, float b, int lifetime) {
        Vec3 sky = groundPos.add(0, height, 0);
        pillar(level, groundPos, sky, r, g, b, lifetime);
    }
}
