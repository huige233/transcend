package com.huige233.transcend.world.nexus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public enum NexusType {

    BINDING  ("binding",   192, 100,    0, "transcendNexusBinding",  ChatFormatting.AQUA,
              "nexus.transcend.binding",  "nexus.transcend.binding.broken"),
    SCARCITY ("scarcity", -192, 100,    0, "transcendNexusScarcity", ChatFormatting.GREEN,
              "nexus.transcend.scarcity", "nexus.transcend.scarcity.broken"),
    ENTROPY  ("entropy",    0,  100,  192, "transcendNexusEntropy",  ChatFormatting.RED,
              "nexus.transcend.entropy",  "nexus.transcend.entropy.broken"),
    FRAILTY  ("frailty",   0,  100, -192, "transcendNexusFrailty",  ChatFormatting.GOLD,
              "nexus.transcend.frailty",  "nexus.transcend.frailty.broken"),
    SILENCE  ("silence",   0,  100,    0, "transcendNexusSilence",  ChatFormatting.LIGHT_PURPLE,
              "nexus.transcend.silence",  "nexus.transcend.silence.broken");

    public final String id;
    public final BlockPos corePosition;
    public final String gameRuleKey;
    public final ChatFormatting color;
    public final String nameKey;
    public final String brokenKey;

    NexusType(String id, int x, int y, int z, String gameRuleKey, ChatFormatting color,
              String nameKey, String brokenKey) {
        this.id = id;
        this.corePosition = new BlockPos(x, y + 3, z);
        this.gameRuleKey = gameRuleKey;
        this.color = color;
        this.nameKey = nameKey;
        this.brokenKey = brokenKey;
    }

    public BlockPos getPlatformCenter() {
        return new BlockPos(corePosition.getX(), corePosition.getY() - 3, corePosition.getZ());
    }

    public static NexusType getNexusForChunk(ChunkPos chunk) {
        for (NexusType type : values()) {
            BlockPos center = type.getPlatformCenter();
            int cx = center.getX() >> 4;
            int cz = center.getZ() >> 4;

            if (Math.abs(chunk.x - cx) <= 1 && Math.abs(chunk.z - cz) <= 1) {
                return type;
            }
        }
        return null;
    }

    public static NexusType getByPosition(BlockPos pos) {
        for (NexusType type : values()) {
            if (type.corePosition.equals(pos)) {
                return type;
            }
        }
        return null;
    }

    public static NexusType getById(String id) {
        for (NexusType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public static int countDestroyed(java.util.Set<String> destroyedIds) {
        int count = 0;
        for (NexusType type : values()) {
            if (destroyedIds.contains(type.id)) count++;
        }
        return count;
    }
}
