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

/** 法阵幽灵方块客户端状态。 */
public final class CircleGhostClientState {

    private static final Map<BlockPos, GhostPreview> PREVIEWS = new HashMap<>();

    private CircleGhostClientState() {}

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

    public static Collection<GhostPreview> activePreviews(ClientLevel level) {
        long now = level.getGameTime();
        PREVIEWS.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        return PREVIEWS.values();
    }

    public static void clearAll() {
        PREVIEWS.clear();
    }

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
