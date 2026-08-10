package com.huige233.transcend.items;

import java.util.Optional;
import java.util.function.Predicate;

/** 安全落点搜索工具类。 */
public final class SafeDestinationSearch {
    public record GridPos(int x, int y, int z) {}

    private SafeDestinationSearch() {}

    public static Optional<GridPos> find(int x, int y, int z, int radius,
                                         Predicate<GridPos> isSafe) {
        if (isSafe == null) return Optional.empty();
        int boundedRadius = Math.max(0, Math.min(4, radius));
        int[] verticalOffsets = {0, 1, -1, 2, -2};
        for (int dy : verticalOffsets) {
            for (int ring = 0; ring <= boundedRadius; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                        GridPos candidate = new GridPos(x + dx, y + dy, z + dz);
                        if (isSafe.test(candidate)) return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }
}
