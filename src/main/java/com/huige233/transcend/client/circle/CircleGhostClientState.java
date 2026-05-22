package com.huige233.transcend.client.circle;

import com.huige233.transcend.circle.CircleStructurePattern.BlockRole;
import com.huige233.transcend.network.S2CCircleGhostBlocks.GhostEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端法阵 Ghost 方块预览状态管理。
 *
 * <p>服务端通过 S2CCircleGhostBlocks 包推送缺失方块位置及角色信息，
 * 客户端在此缓存并在到期后自动移除。
 * 渲染器从 {@link #activePreviews(ClientLevel)} 读取当前需要显示的预览。
 */
public final class CircleGhostClientState {

    private static final Map<BlockPos, GhostPreview> PREVIEWS = new HashMap<>();

    private CircleGhostClientState() {}

    /**
     * 显示或清除某个核心坐标的 Ghost 预览。
     * 由 S2CCircleGhostBlocks 包客户端处理器调用。
     */
    public static void showOrClear(BlockPos corePos, int tier, int durationTicks, List<GhostEntry> entries) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            PREVIEWS.remove(corePos);
            return;
        }

        if (durationTicks <= 0 || entries == null || entries.isEmpty()) {
            PREVIEWS.remove(corePos);
            return;
        }

        long expiresAt = level.getGameTime() + durationTicks;
        PREVIEWS.put(corePos, new GhostPreview(corePos, tier, new ArrayList<>(entries), expiresAt));
    }

    /** 获取所有未过期的预览，同时清理已过期的。 */
    public static Collection<GhostPreview> activePreviews(ClientLevel level) {
        long now = level.getGameTime();
        PREVIEWS.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        return PREVIEWS.values();
    }

    /** 清除所有预览（维度切换 / 登出时调用）。 */
    public static void clearAll() {
        PREVIEWS.clear();
    }

    /**
     * Ghost 方块预览数据。
     */
    public static final class GhostPreview {
        public final BlockPos corePos;
        public final int tier;
        public final List<GhostEntry> entries;
        public final long expiresAt;

        public GhostPreview(BlockPos corePos, int tier, List<GhostEntry> entries, long expiresAt) {
            this.corePos = corePos;
            this.tier = tier;
            this.entries = entries;
            this.expiresAt = expiresAt;
        }
    }
}
