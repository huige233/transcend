package com.huige233.transcend.client.circle;

import com.huige233.transcend.network.S2COreRevealPack.OreEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端矿石透视（Oreblood Revelation）状态。
 *
 * <p>由 {@link com.huige233.transcend.network.S2COreRevealPack} 推送，
 * 在 {@link com.huige233.transcend.client.renderer.ShaderSpellRenderer} 中读取并渲染。
 *
 * <p>每次新包到达会**替换**当前预览（卷轴一次施放只有一个有效预览，
 * 不需要按中心点缓存多份）。到期自动清空。
 */
public final class OreRevealClientState {

    private static volatile RevealSnapshot snapshot = null;

    private OreRevealClientState() {}

    /** 由 S2C 包客户端处理器调用。 */
    public static void show(BlockPos center, int durationTicks, List<OreEntry> entries) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || durationTicks <= 0 || entries == null || entries.isEmpty()) {
            snapshot = null;
            return;
        }
        long expiresAt = level.getGameTime() + durationTicks;
        snapshot = new RevealSnapshot(center, new ArrayList<>(entries), expiresAt);
    }

    /** 渲染调用：返回当前未过期的快照,或 null。 */
    public static RevealSnapshot active(ClientLevel level) {
        RevealSnapshot s = snapshot;
        if (s == null) return null;
        if (s.expiresAt <= level.getGameTime()) {
            snapshot = null;
            return null;
        }
        return s;
    }

    public static void clearAll() {
        snapshot = null;
    }

    /** 矿石高亮快照。 */
    public static final class RevealSnapshot {
        public final BlockPos center;
        public final List<OreEntry> entries;
        public final long expiresAt;

        public RevealSnapshot(BlockPos center, List<OreEntry> entries, long expiresAt) {
            this.center = center;
            this.entries = Collections.unmodifiableList(entries);
            this.expiresAt = expiresAt;
        }
    }
}
