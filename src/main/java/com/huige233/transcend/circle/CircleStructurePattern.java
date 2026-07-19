package com.huige233.transcend.circle;

import java.util.ArrayList;
import java.util.List;

public class CircleStructurePattern {

    public enum BlockRole {
        CORE, FOUNDATION, RUNE, CATALYST_PLINTH, CONDUIT, PILLAR, PILLAR_CAP
    }

    public record PatternEntry(int dx, int dy, int dz, BlockRole role, int minBlockTier) {}

    public static List<PatternEntry> getPatternForTier(CircleTier tier) {
        List<PatternEntry> result = new ArrayList<>();

        int ordinal = tier.ordinal();
        result.addAll(T1_ENTRIES);
        if (ordinal >= CircleTier.ADEPT.ordinal()) result.addAll(T2_ADDITIONS);
        if (ordinal >= CircleTier.MASTER.ordinal()) result.addAll(T3_ADDITIONS);
        if (ordinal >= CircleTier.ARCHON.ordinal()) result.addAll(T4_ADDITIONS);
        if (ordinal >= CircleTier.PRIMORDIAL.ordinal()) result.addAll(T5_ADDITIONS);

        for (CircleTier t : CircleTier.values()) {
            if (t.ordinal() <= ordinal) {
                result.addAll(
                        com.huige233.transcend.circle.data.CirclePatternAdditionRegistry
                                .getInstance().getAdditionsForTier(t));
            }
        }
        return List.copyOf(result);
    }

    private static final List<PatternEntry> T1_ENTRIES = List.of(

            new PatternEntry(1, 0, 0, BlockRole.RUNE, 1),
            new PatternEntry(-1, 0, 0, BlockRole.RUNE, 1),
            new PatternEntry(0, 0, 1, BlockRole.RUNE, 1),
            new PatternEntry(0, 0, -1, BlockRole.RUNE, 1),

            new PatternEntry(1, 0, 1, BlockRole.FOUNDATION, 1),
            new PatternEntry(1, 0, -1, BlockRole.FOUNDATION, 1),
            new PatternEntry(-1, 0, 1, BlockRole.FOUNDATION, 1),
            new PatternEntry(-1, 0, -1, BlockRole.FOUNDATION, 1)
    );

    private static final List<PatternEntry> T2_ADDITIONS = List.of(

            new PatternEntry(2, 0, 0, BlockRole.CONDUIT, 2),
            new PatternEntry(-2, 0, 0, BlockRole.CONDUIT, 2),
            new PatternEntry(0, 0, 2, BlockRole.CONDUIT, 2),
            new PatternEntry(0, 0, -2, BlockRole.CONDUIT, 2),

            new PatternEntry(2, 0, 2, BlockRole.CATALYST_PLINTH, 2),
            new PatternEntry(2, 0, -2, BlockRole.CATALYST_PLINTH, 2),
            new PatternEntry(-2, 0, 2, BlockRole.CATALYST_PLINTH, 2),
            new PatternEntry(-2, 0, -2, BlockRole.CATALYST_PLINTH, 2),

            new PatternEntry(2, 0, 1, BlockRole.RUNE, 2),
            new PatternEntry(2, 0, -1, BlockRole.RUNE, 2),
            new PatternEntry(-2, 0, 1, BlockRole.RUNE, 2),
            new PatternEntry(-2, 0, -1, BlockRole.RUNE, 2),
            new PatternEntry(1, 0, 2, BlockRole.RUNE, 2),
            new PatternEntry(-1, 0, 2, BlockRole.RUNE, 2),
            new PatternEntry(1, 0, -2, BlockRole.RUNE, 2),
            new PatternEntry(-1, 0, -2, BlockRole.RUNE, 2)
    );

    private static final List<PatternEntry> T3_ADDITIONS = buildT3Additions();

    private static List<PatternEntry> buildT3Additions() {
        List<PatternEntry> list = new ArrayList<>();

        list.add(new PatternEntry(3, 0, 0, BlockRole.FOUNDATION, 3));
        list.add(new PatternEntry(-3, 0, 0, BlockRole.FOUNDATION, 3));
        list.add(new PatternEntry(0, 0, 3, BlockRole.FOUNDATION, 3));
        list.add(new PatternEntry(0, 0, -3, BlockRole.FOUNDATION, 3));

        list.add(new PatternEntry(3, 0, 3, BlockRole.CATALYST_PLINTH, 3));
        list.add(new PatternEntry(3, 0, -3, BlockRole.CATALYST_PLINTH, 3));
        list.add(new PatternEntry(-3, 0, 3, BlockRole.CATALYST_PLINTH, 3));
        list.add(new PatternEntry(-3, 0, -3, BlockRole.CATALYST_PLINTH, 3));

        int[][] r3Runes = {
                {3, 1}, {3, -1}, {3, 2}, {3, -2},
                {-3, 1}, {-3, -1}, {-3, 2}, {-3, -2},
                {1, 3}, {-1, 3}, {2, 3}, {-2, 3},
                {1, -3}, {-1, -3}, {2, -3}, {-2, -3}
        };
        for (int[] p : r3Runes) {
            list.add(new PatternEntry(p[0], 0, p[1], BlockRole.RUNE, 3));
        }

        list.add(new PatternEntry(4, 0, 0, BlockRole.CONDUIT, 3));
        list.add(new PatternEntry(-4, 0, 0, BlockRole.CONDUIT, 3));
        list.add(new PatternEntry(0, 0, 4, BlockRole.CONDUIT, 3));
        list.add(new PatternEntry(0, 0, -4, BlockRole.CONDUIT, 3));

        int[][] r4Foundations = {
                {4, 1}, {4, -1}, {4, 2}, {4, -2}, {4, 3}, {4, -3},
                {-4, 1}, {-4, -1}, {-4, 2}, {-4, -2}, {-4, 3}, {-4, -3},
                {1, 4}, {-1, 4}, {2, 4}, {-2, 4}, {3, 4}, {-3, 4},
                {1, -4}, {-1, -4}, {2, -4}, {-2, -4}, {3, -4}, {-3, -4}
        };
        for (int[] p : r4Foundations) {
            list.add(new PatternEntry(p[0], 0, p[1], BlockRole.FOUNDATION, 3));
        }

        int[][] pillarBases = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
        for (int[] b : pillarBases) {

            list.add(new PatternEntry(b[0], 1, b[1], BlockRole.PILLAR, 3));
            list.add(new PatternEntry(b[0], 2, b[1], BlockRole.PILLAR, 3));

            list.add(new PatternEntry(b[0], 3, b[1], BlockRole.PILLAR_CAP, 3));
        }

        return List.copyOf(list);
    }

    private static final List<PatternEntry> T4_ADDITIONS = buildT4();

    private static List<PatternEntry> buildT4() {
        List<PatternEntry> list = new ArrayList<>();

        list.add(new PatternEntry(5, 0, 0, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(-5, 0, 0, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(0, 0, 5, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(0, 0, -5, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(4, 0, 4, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(4, 0, -4, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(-4, 0, 4, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(-4, 0, -4, BlockRole.CONDUIT, 4));

        list.add(new PatternEntry(5, 0, 5, BlockRole.CATALYST_PLINTH, 4));
        list.add(new PatternEntry(5, 0, -5, BlockRole.CATALYST_PLINTH, 4));
        list.add(new PatternEntry(-5, 0, 5, BlockRole.CATALYST_PLINTH, 4));
        list.add(new PatternEntry(-5, 0, -5, BlockRole.CATALYST_PLINTH, 4));

        for (int d = -5; d <= 5; d++) {
            for (int[] pos : new int[][]{{5, d}, {-5, d}, {d, 5}, {d, -5}}) {
                int dx = pos[0], dz = pos[1];
                if (Math.abs(dx) == 5 && Math.abs(dz) == 5) continue;
                if ((dx == 5 || dx == -5) && dz == 0) continue;
                if (dx == 0 && (dz == 5 || dz == -5)) continue;
                if (Math.abs(dx) == 4 && Math.abs(dz) == 4) continue;
                list.add(new PatternEntry(dx, 0, dz, BlockRole.RUNE, 4));
            }
        }

        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (Math.abs(dx) <= 5 && Math.abs(dz) <= 5) continue;
                if (Math.abs(dx) >= 5 && Math.abs(dz) >= 5) continue;
                list.add(new PatternEntry(dx, 0, dz, BlockRole.FOUNDATION, 4));
            }
        }

        for (int[] base : new int[][]{{5,0},{-5,0},{0,5},{0,-5}}) {
            for (int y = 1; y <= 4; y++) {
                list.add(new PatternEntry(base[0], y, base[1], BlockRole.PILLAR, 4));
            }
            list.add(new PatternEntry(base[0], 5, base[1], BlockRole.PILLAR_CAP, 4));
        }
        return List.copyOf(list);
    }

    private static final List<PatternEntry> T5_ADDITIONS = buildT5();

    private static List<PatternEntry> buildT5() {
        List<PatternEntry> list = new ArrayList<>();

        list.add(new PatternEntry(7, 0, 0, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(-7, 0, 0, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(0, 0, 7, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(0, 0, -7, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(6, 0, 6, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(6, 0, -6, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(-6, 0, 6, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(-6, 0, -6, BlockRole.CONDUIT, 5));

        for (int d = -7; d <= 7; d++) {
            for (int[] pos : new int[][]{{7, d}, {-7, d}, {d, 7}, {d, -7}}) {
                int dx = pos[0], dz = pos[1];
                if ((Math.abs(dx) == 7 && dz == 0) || (dx == 0 && Math.abs(dz) == 7)) continue;
                if (Math.abs(dx) == 6 && Math.abs(dz) == 6) continue;
                list.add(new PatternEntry(dx, 0, dz, BlockRole.RUNE, 5));
            }
        }

        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (Math.abs(dx) <= 7 && Math.abs(dz) <= 7) continue;
                if (Math.abs(dx) >= 6 && Math.abs(dz) >= 6) continue;
                list.add(new PatternEntry(dx, 0, dz, BlockRole.FOUNDATION, 5));
            }
        }

        for (int[] base : new int[][]{{6,6},{6,-6},{-6,6},{-6,-6}}) {
            for (int y = 1; y <= 7; y++) {
                list.add(new PatternEntry(base[0], y, base[1], BlockRole.PILLAR, 5));
            }
            list.add(new PatternEntry(base[0], 8, base[1], BlockRole.PILLAR_CAP, 5));
        }
        return List.copyOf(list);
    }
}
