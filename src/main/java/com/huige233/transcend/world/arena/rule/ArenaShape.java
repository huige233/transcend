package com.huige233.transcend.world.arena.rule;

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
