package com.huige233.transcend.client.circle;

import com.huige233.transcend.network.S2COreRevealPack.OreEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OreRevealClientState {

    private static volatile RevealSnapshot snapshot = null;

    private OreRevealClientState() {}

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
