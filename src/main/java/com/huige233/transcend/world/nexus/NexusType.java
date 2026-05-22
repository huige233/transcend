package com.huige233.transcend.world.nexus;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * 法则枢纽类型 — 法则之境中五座枢纽，每座对应一条被凝固的世界法则。
 * 摧毁枢纽核心将永久解除对应法则的束缚。
 */
public enum NexusType {

    BINDING   ("binding",    192, 100,    0, "transcendNexusBinding",   ChatFormatting.AQUA,
               "nexus.transcend.binding",   "nexus.transcend.binding.broken"),
    SCARCITY  ("scarcity",  -192, 100,    0, "transcendNexusScarcity",  ChatFormatting.GREEN,
               "nexus.transcend.scarcity",  "nexus.transcend.scarcity.broken"),
    MERCY     ("mercy",        0, 100,  192, "transcendNexusMercy",     ChatFormatting.RED,
               "nexus.transcend.mercy",     "nexus.transcend.mercy.broken"),
    FRAILTY   ("frailty",      0, 100, -192, "transcendNexusFrailty",   ChatFormatting.GOLD,
               "nexus.transcend.frailty",   "nexus.transcend.frailty.broken"),
    ISOLATION ("isolation",    0, 100,    0, "transcendNexusIsolation",  ChatFormatting.LIGHT_PURPLE,
               "nexus.transcend.isolation", "nexus.transcend.isolation.broken");

    public final String id;
    public final BlockPos corePosition;
    public final String gameRuleKey;
    public final ChatFormatting color;
    public final String nameKey;
    public final String brokenKey;

    NexusType(String id, int x, int y, int z, String gameRuleKey, ChatFormatting color,
              String nameKey, String brokenKey) {
        this.id = id;
        this.corePosition = new BlockPos(x, y + 3, z); // core sits above beacon (platform Y + 3)
        this.gameRuleKey = gameRuleKey;
        this.color = color;
        this.nameKey = nameKey;
        this.brokenKey = brokenKey;
    }

    /** The base Y of the platform (not the core block). */
    public BlockPos getPlatformCenter() {
        return new BlockPos(corePosition.getX(), corePosition.getY() - 3, corePosition.getZ());
    }

    /**
     * Returns the NexusType whose platform center contains the given chunk, or null.
     */
    public static NexusType getNexusForChunk(ChunkPos chunk) {
        for (NexusType type : values()) {
            BlockPos center = type.getPlatformCenter();
            int cx = center.getX() >> 4;
            int cz = center.getZ() >> 4;
            // Structure covers roughly ±12 blocks from center → ±1 chunk
            if (Math.abs(chunk.x - cx) <= 1 && Math.abs(chunk.z - cz) <= 1) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns the NexusType whose core position matches the given block position.
     */
    public static NexusType getByPosition(BlockPos pos) {
        for (NexusType type : values()) {
            if (type.corePosition.equals(pos)) {
                return type;
            }
        }
        return null;
    }

    /**
     * Returns the NexusType with the given string id.
     */
    public static NexusType getById(String id) {
        for (NexusType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    /**
     * How many nexuses have been destroyed out of the total 5.
     */
    public static int countDestroyed(java.util.Set<String> destroyedIds) {
        int count = 0;
        for (NexusType type : values()) {
            if (destroyedIds.contains(type.id)) count++;
        }
        return count;
    }
}
