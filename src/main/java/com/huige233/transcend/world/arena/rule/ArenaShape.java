package com.huige233.transcend.world.arena.rule;

/**
 * 竞技场几何参数。
 * <p>
 * 将半径、高度等核心参数集中，便于不同规则模板复用。
 */
public record ArenaShape(
        int centerX,
        int centerZ,
        int arenaY,
        int platformRadius,
        int wallRadius,
        int hardRadius,
        int wallHeight
) {
}
