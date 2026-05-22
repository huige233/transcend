package com.huige233.transcend.circle;

import java.util.ArrayList;
import java.util.List;

/**
 * 多方块结构图案定义。
 * 为每个法环等级定义所需方块的相对位置与角色。
 */
public class CircleStructurePattern {

    public enum BlockRole {
        CORE, FOUNDATION, RUNE, CATALYST_PLINTH, CONDUIT, PILLAR, PILLAR_CAP
    }

    public record PatternEntry(int dx, int dy, int dz, BlockRole role, int minBlockTier) {}

    /** 获取指定法环等级所需的全部方块位置（包含所有低阶条目 + 数据驱动追加） */
    public static List<PatternEntry> getPatternForTier(CircleTier tier) {
        List<PatternEntry> result = new ArrayList<>();
        // 累积所有 <= 该等级的条目 (Java 内置默认)
        int ordinal = tier.ordinal();
        result.addAll(T1_ENTRIES);
        if (ordinal >= CircleTier.ADEPT.ordinal()) result.addAll(T2_ADDITIONS);
        if (ordinal >= CircleTier.MASTER.ordinal()) result.addAll(T3_ADDITIONS);
        if (ordinal >= CircleTier.ARCHON.ordinal()) result.addAll(T4_ADDITIONS);
        if (ordinal >= CircleTier.PRIMORDIAL.ordinal()) result.addAll(T5_ADDITIONS);
        // Round 03: 追加数据驱动的额外条目（data/<ns>/circle_patterns/*.json）
        // 仅当 datapack 提供了该 tier 的追加文件时才有内容；默认空。
        for (CircleTier t : CircleTier.values()) {
            if (t.ordinal() <= ordinal) {
                result.addAll(
                        com.huige233.transcend.circle.data.CirclePatternAdditionRegistry
                                .getInstance().getAdditionsForTier(t));
            }
        }
        return List.copyOf(result);
    }

    // ============================================================
    // T1: 3x3 平面
    //   F R F
    //   R C R
    //   F R F
    // ============================================================
    private static final List<PatternEntry> T1_ENTRIES = List.of(
            // RUNE - 十字方位
            new PatternEntry(1, 0, 0, BlockRole.RUNE, 1),
            new PatternEntry(-1, 0, 0, BlockRole.RUNE, 1),
            new PatternEntry(0, 0, 1, BlockRole.RUNE, 1),
            new PatternEntry(0, 0, -1, BlockRole.RUNE, 1),
            // FOUNDATION - 四角
            new PatternEntry(1, 0, 1, BlockRole.FOUNDATION, 1),
            new PatternEntry(1, 0, -1, BlockRole.FOUNDATION, 1),
            new PatternEntry(-1, 0, 1, BlockRole.FOUNDATION, 1),
            new PatternEntry(-1, 0, -1, BlockRole.FOUNDATION, 1)
    );

    // ============================================================
    // T2: 5x5 平面外环
    //   P R K R P
    //   R . . . R
    //   K . C . K
    //   R . . . R
    //   P R K R P
    // ============================================================
    private static final List<PatternEntry> T2_ADDITIONS = List.of(
            // CONDUIT - 外环十字
            new PatternEntry(2, 0, 0, BlockRole.CONDUIT, 2),
            new PatternEntry(-2, 0, 0, BlockRole.CONDUIT, 2),
            new PatternEntry(0, 0, 2, BlockRole.CONDUIT, 2),
            new PatternEntry(0, 0, -2, BlockRole.CONDUIT, 2),
            // CATALYST_PLINTH - 外环四角
            new PatternEntry(2, 0, 2, BlockRole.CATALYST_PLINTH, 2),
            new PatternEntry(2, 0, -2, BlockRole.CATALYST_PLINTH, 2),
            new PatternEntry(-2, 0, 2, BlockRole.CATALYST_PLINTH, 2),
            new PatternEntry(-2, 0, -2, BlockRole.CATALYST_PLINTH, 2),
            // RUNE - 外环剩余
            new PatternEntry(2, 0, 1, BlockRole.RUNE, 2),
            new PatternEntry(2, 0, -1, BlockRole.RUNE, 2),
            new PatternEntry(-2, 0, 1, BlockRole.RUNE, 2),
            new PatternEntry(-2, 0, -1, BlockRole.RUNE, 2),
            new PatternEntry(1, 0, 2, BlockRole.RUNE, 2),
            new PatternEntry(-1, 0, 2, BlockRole.RUNE, 2),
            new PatternEntry(1, 0, -2, BlockRole.RUNE, 2),
            new PatternEntry(-1, 0, -2, BlockRole.RUNE, 2)
    );

    // ============================================================
    // T3: 9x9 平面 + 4 高石柱
    // 中心 5x5 已由 T1/T2 定义。
    // 外环 (半径 3-4) 添加 FOUNDATION/RUNE 填充。
    // 切角: (±4,0,±4) 必须为空 -> 不加入图案。
    // 4 根石柱位于 (±3,*,0) 与 (0,*,±3): y=1,2 = PILLAR, y=3 = PILLAR_CAP。
    // ============================================================
    private static final List<PatternEntry> T3_ADDITIONS = buildT3Additions();

    private static List<PatternEntry> buildT3Additions() {
        List<PatternEntry> list = new ArrayList<>();

        // --- 半径 3 环 (3x3 边框，距核心切比雪夫距离 = 3) ---
        // 主轴位置: PILLAR 基座 (FOUNDATION minTier=3)
        list.add(new PatternEntry(3, 0, 0, BlockRole.FOUNDATION, 3));
        list.add(new PatternEntry(-3, 0, 0, BlockRole.FOUNDATION, 3));
        list.add(new PatternEntry(0, 0, 3, BlockRole.FOUNDATION, 3));
        list.add(new PatternEntry(0, 0, -3, BlockRole.FOUNDATION, 3));
        // 对角: CATALYST_PLINTH
        list.add(new PatternEntry(3, 0, 3, BlockRole.CATALYST_PLINTH, 3));
        list.add(new PatternEntry(3, 0, -3, BlockRole.CATALYST_PLINTH, 3));
        list.add(new PatternEntry(-3, 0, 3, BlockRole.CATALYST_PLINTH, 3));
        list.add(new PatternEntry(-3, 0, -3, BlockRole.CATALYST_PLINTH, 3));
        // 半径 3 环剩余位置 - RUNE
        // (±3, 0, ±1), (±3, 0, ±2), (±1, 0, ±3), (±2, 0, ±3)
        int[][] r3Runes = {
                {3, 1}, {3, -1}, {3, 2}, {3, -2},
                {-3, 1}, {-3, -1}, {-3, 2}, {-3, -2},
                {1, 3}, {-1, 3}, {2, 3}, {-2, 3},
                {1, -3}, {-1, -3}, {2, -3}, {-2, -3}
        };
        for (int[] p : r3Runes) {
            list.add(new PatternEntry(p[0], 0, p[1], BlockRole.RUNE, 3));
        }

        // --- 半径 4 环 (切比雪夫距离 = 4，但切角不包括) ---
        // 主轴位置: CONDUIT
        list.add(new PatternEntry(4, 0, 0, BlockRole.CONDUIT, 3));
        list.add(new PatternEntry(-4, 0, 0, BlockRole.CONDUIT, 3));
        list.add(new PatternEntry(0, 0, 4, BlockRole.CONDUIT, 3));
        list.add(new PatternEntry(0, 0, -4, BlockRole.CONDUIT, 3));
        // 半径 4 环其余 (排除四角 ±4,±4) - FOUNDATION
        // (±4, 0, ±1), (±4, 0, ±2), (±4, 0, ±3), (±1..±3, 0, ±4)
        int[][] r4Foundations = {
                {4, 1}, {4, -1}, {4, 2}, {4, -2}, {4, 3}, {4, -3},
                {-4, 1}, {-4, -1}, {-4, 2}, {-4, -2}, {-4, 3}, {-4, -3},
                {1, 4}, {-1, 4}, {2, 4}, {-2, 4}, {3, 4}, {-3, 4},
                {1, -4}, {-1, -4}, {2, -4}, {-2, -4}, {3, -4}, {-3, -4}
        };
        for (int[] p : r4Foundations) {
            list.add(new PatternEntry(p[0], 0, p[1], BlockRole.FOUNDATION, 3));
        }

        // --- 竖直石柱: 4 根，位于 (±3,*,0) 与 (0,*,±3) ---
        int[][] pillarBases = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
        for (int[] b : pillarBases) {
            // y=1, y=2 是 PILLAR
            list.add(new PatternEntry(b[0], 1, b[1], BlockRole.PILLAR, 3));
            list.add(new PatternEntry(b[0], 2, b[1], BlockRole.PILLAR, 3));
            // y=3 是 PILLAR_CAP
            list.add(new PatternEntry(b[0], 3, b[1], BlockRole.PILLAR_CAP, 3));
        }

        return List.copyOf(list);
    }

    // ============================================================
    // ============================================================
    // T4: 执政法环 13x13 占地，6层高
    // 在T3基础上扩展 radius 5-6 环
    // ============================================================
    private static final List<PatternEntry> T4_ADDITIONS = buildT4();

    private static List<PatternEntry> buildT4() {
        List<PatternEntry> list = new ArrayList<>();
        // === y=0 地面层 ===
        // 导流门：4个正方向 radius 5 + 4个对角 radius (4,4)
        list.add(new PatternEntry(5, 0, 0, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(-5, 0, 0, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(0, 0, 5, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(0, 0, -5, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(4, 0, 4, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(4, 0, -4, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(-4, 0, 4, BlockRole.CONDUIT, 4));
        list.add(new PatternEntry(-4, 0, -4, BlockRole.CONDUIT, 4));
        // 封印触媒石座：4个对角 radius 5
        list.add(new PatternEntry(5, 0, 5, BlockRole.CATALYST_PLINTH, 4));
        list.add(new PatternEntry(5, 0, -5, BlockRole.CATALYST_PLINTH, 4));
        list.add(new PatternEntry(-5, 0, 5, BlockRole.CATALYST_PLINTH, 4));
        list.add(new PatternEntry(-5, 0, -5, BlockRole.CATALYST_PLINTH, 4));
        // 符文石填充 radius 5 环剩余位置
        for (int d = -5; d <= 5; d++) {
            for (int[] pos : new int[][]{{5, d}, {-5, d}, {d, 5}, {d, -5}}) {
                int dx = pos[0], dz = pos[1];
                if (Math.abs(dx) == 5 && Math.abs(dz) == 5) continue; // 催化石座
                if ((dx == 5 || dx == -5) && dz == 0) continue; // 导流门
                if (dx == 0 && (dz == 5 || dz == -5)) continue; // 导流门
                if (Math.abs(dx) == 4 && Math.abs(dz) == 4) continue; // 对角导流门
                list.add(new PatternEntry(dx, 0, dz, BlockRole.RUNE, 4));
            }
        }
        // 基石填充 radius 6 环（去掉4个 2x2 切角）
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (Math.abs(dx) <= 5 && Math.abs(dz) <= 5) continue; // 已在内圈
                if (Math.abs(dx) >= 5 && Math.abs(dz) >= 5) continue; // 切角
                list.add(new PatternEntry(dx, 0, dz, BlockRole.FOUNDATION, 4));
            }
        }
        // === 垂直层：4个方尖碑 ===
        // 位于 (±5,0,0) 和 (0,0,±5) 上方
        for (int[] base : new int[][]{{5,0},{-5,0},{0,5},{0,-5}}) {
            for (int y = 1; y <= 4; y++) {
                list.add(new PatternEntry(base[0], y, base[1], BlockRole.PILLAR, 4));
            }
            list.add(new PatternEntry(base[0], 5, base[1], BlockRole.PILLAR_CAP, 4));
        }
        return List.copyOf(list);
    }

    // ============================================================
    // T5: 太初法环 17x17 占地，9层高
    // 在T4基础上扩展到 radius 7-8 环
    // ============================================================
    private static final List<PatternEntry> T5_ADDITIONS = buildT5();

    private static List<PatternEntry> buildT5() {
        List<PatternEntry> list = new ArrayList<>();
        // === y=0 地面层 ===
        // 太初导流门：4正+4对角 radius 7
        list.add(new PatternEntry(7, 0, 0, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(-7, 0, 0, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(0, 0, 7, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(0, 0, -7, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(6, 0, 6, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(6, 0, -6, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(-6, 0, 6, BlockRole.CONDUIT, 5));
        list.add(new PatternEntry(-6, 0, -6, BlockRole.CONDUIT, 5));
        // 太初符文石填充 radius 7 环
        for (int d = -7; d <= 7; d++) {
            for (int[] pos : new int[][]{{7, d}, {-7, d}, {d, 7}, {d, -7}}) {
                int dx = pos[0], dz = pos[1];
                if ((Math.abs(dx) == 7 && dz == 0) || (dx == 0 && Math.abs(dz) == 7)) continue;
                if (Math.abs(dx) == 6 && Math.abs(dz) == 6) continue;
                list.add(new PatternEntry(dx, 0, dz, BlockRole.RUNE, 5));
            }
        }
        // 太初基石填充 radius 8 环（去掉 3x3 切角）
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (Math.abs(dx) <= 7 && Math.abs(dz) <= 7) continue;
                if (Math.abs(dx) >= 6 && Math.abs(dz) >= 6) continue; // 3x3切角
                list.add(new PatternEntry(dx, 0, dz, BlockRole.FOUNDATION, 5));
            }
        }
        // === 垂直层：4个太初高塔 ===
        // 位于 (±6,0,±6) 上方
        for (int[] base : new int[][]{{6,6},{6,-6},{-6,6},{-6,-6}}) {
            for (int y = 1; y <= 7; y++) {
                list.add(new PatternEntry(base[0], y, base[1], BlockRole.PILLAR, 5));
            }
            list.add(new PatternEntry(base[0], 8, base[1], BlockRole.PILLAR_CAP, 5));
        }
        return List.copyOf(list);
    }
}
