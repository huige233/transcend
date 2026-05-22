package com.huige233.transcend.circle;

import net.minecraft.core.BlockPos;
import java.util.List;

/**
 * 多方块结构验证缓存。
 * 由 CircleStructureValidator 生成，存储在核心方块实体中。
 */
public class CircleStructureCache {
    private final CircleTier tier;
    private final boolean valid;
    private final List<BlockPos> missingPositions;
    private final List<MissingEntry> missingEntries;
    private final long validationTime;

    /** 缺失方块条目：位置 + 角色 + 最低方块等级 */
    public record MissingEntry(BlockPos pos, CircleStructurePattern.BlockRole role, int minBlockTier) {}

    public CircleStructureCache(CircleTier tier, boolean valid, List<BlockPos> missingPositions, long validationTime) {
        this(tier, valid, missingPositions, List.of(), validationTime);
    }

    public CircleStructureCache(CircleTier tier, boolean valid, List<BlockPos> missingPositions,
                                List<MissingEntry> missingEntries, long validationTime) {
        this.tier = tier;
        this.valid = valid;
        this.missingPositions = missingPositions;
        this.missingEntries = missingEntries;
        this.validationTime = validationTime;
    }

    public CircleTier getTier() { return tier; }
    public boolean isValid() { return valid; }
    public List<BlockPos> getMissingPositions() { return missingPositions; }
    public List<MissingEntry> getMissingEntries() { return missingEntries; }
    public long getValidationTime() { return validationTime; }

    /** 无效缓存（用于初始化或清空） */
    public static CircleStructureCache invalid(long time) {
        return new CircleStructureCache(null, false, List.of(), List.of(), time);
    }
}
