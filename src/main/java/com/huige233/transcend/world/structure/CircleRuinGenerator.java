package com.huige233.transcend.world.structure;

import com.huige233.transcend.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Random;

/**
 * 法环遗迹程序化生成器。
 * 用于在世界中放置部分损坏的法环结构。
 */
public class CircleRuinGenerator {

    /**
     * 在指定位置生成 T1 初识法环遗迹。
     * 3x3 布局，随机移除 30-60% 的方块模拟破损。
     */
    public static void generateInitiateRuin(ServerLevel level, BlockPos center, Random random) {
        float decay = 0.3f + random.nextFloat() * 0.3f;
        BlockState foundation = ModBlocks.ANCIENT_CIRCLE_STONE.get().defaultBlockState();
        BlockState rune = ModBlocks.LESSER_RUNE_STONE.get().defaultBlockState();
        BlockState moss = Blocks.MOSS_BLOCK.defaultBlockState();

        // T1 pattern: F R F / R _ R / F R F (core position intentionally empty - player supplies)
        int[][] foundationPos = {{-1,-1},{-1,1},{1,-1},{1,1}};
        int[][] runePos = {{0,-1},{0,1},{-1,0},{1,0}};

        for (int[] p : foundationPos) {
            if (random.nextFloat() > decay) {
                level.setBlock(center.offset(p[0], 0, p[1]), foundation, 3);
            } else if (random.nextFloat() > 0.5f) {
                level.setBlock(center.offset(p[0], 0, p[1]), moss, 3);
            }
        }
        for (int[] p : runePos) {
            if (random.nextFloat() > decay) {
                level.setBlock(center.offset(p[0], 0, p[1]), rune, 3);
            } else if (random.nextFloat() > 0.6f) {
                level.setBlock(center.offset(p[0], 0, p[1]), Blocks.COBBLESTONE.defaultBlockState(), 3);
            }
        }
        // Scatter some decorative blocks around
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
                if (random.nextFloat() < 0.2f) {
                    level.setBlock(center.offset(dx, 0, dz),
                        random.nextBoolean() ? moss : Blocks.GRAVEL.defaultBlockState(), 3);
                }
            }
        }
    }

    /**
     * T2 精通法环遗迹 — 5x5 半埋结构。
     */
    public static void generateAdeptRuin(ServerLevel level, BlockPos center, Random random) {
        float decay = 0.35f + random.nextFloat() * 0.25f;
        BlockState foundation = ModBlocks.ANCIENT_CIRCLE_STONE.get().defaultBlockState();
        BlockState awakened = ModBlocks.AWAKENED_CIRCLE_STONE.get().defaultBlockState();
        BlockState rune1 = ModBlocks.LESSER_RUNE_STONE.get().defaultBlockState();
        BlockState rune2 = ModBlocks.AWAKENED_RUNE_STONE.get().defaultBlockState();
        BlockState conduit = ModBlocks.LEYLINE_CONDUIT_STONE.get().defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();

        // Inner 3x3 ring (T1)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (random.nextFloat() > decay) {
                    boolean isRune = (dx == 0 || dz == 0);
                    level.setBlock(center.offset(dx, 0, dz), isRune ? rune1 : foundation, 3);
                }
            }
        }
        // Outer ring (T2 additions)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) continue;
                if (random.nextFloat() > decay * 1.2f) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        // Corner: catalyst plinth position (just leave marker)
                        level.setBlock(center.offset(dx, 0, dz), awakened, 3);
                    } else if ((dx == 0 && Math.abs(dz) == 2) || (dz == 0 && Math.abs(dx) == 2)) {
                        level.setBlock(center.offset(dx, 0, dz), conduit, 3);
                    } else {
                        level.setBlock(center.offset(dx, 0, dz), rune2, 3);
                    }
                } else if (random.nextFloat() < 0.3f) {
                    level.setBlock(center.offset(dx, 0, dz), sand, 3);
                }
            }
        }
    }

    /**
     * T3 观测台遗迹 — 9x9 带残柱。
     */
    public static void generateObservatoryRuin(ServerLevel level, BlockPos center, Random random) {
        float decay = 0.4f + random.nextFloat() * 0.2f;
        BlockState astral = ModBlocks.ASTRAL_CIRCLE_STONE.get().defaultBlockState();
        BlockState rune3 = ModBlocks.GREATER_RUNE_STONE.get().defaultBlockState();
        BlockState pillar = ModBlocks.RUNIC_PILLAR.get().defaultBlockState();
        BlockState cap = ModBlocks.ASTRAL_CAPSTONE.get().defaultBlockState();
        BlockState stone = Blocks.STONE_BRICKS.defaultBlockState();
        BlockState cracked = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();

        // Floor: 9x9 with cut corners
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (Math.abs(dx) == 4 && Math.abs(dz) == 4) continue;
                if (random.nextFloat() > decay) {
                    BlockState block;
                    if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                        block = astral;
                    } else if ((dx == 0 || dz == 0) && (Math.abs(dx) + Math.abs(dz)) <= 3) {
                        block = rune3;
                    } else {
                        block = random.nextFloat() > 0.3f ? astral : cracked;
                    }
                    level.setBlock(center.offset(dx, 0, dz), block, 3);
                } else if (random.nextFloat() < 0.15f) {
                    level.setBlock(center.offset(dx, 0, dz), cracked, 3);
                }
            }
        }
        // Pillars at cardinal 3-positions
        int[][] pillarBases = {{3,0},{-3,0},{0,3},{0,-3}};
        for (int[] p : pillarBases) {
            int maxH = random.nextInt(3) + 1; // 1-3 blocks tall (damaged)
            for (int y = 1; y <= maxH; y++) {
                if (random.nextFloat() > decay * 0.5f) {
                    level.setBlock(center.offset(p[0], y, p[1]), pillar, 3);
                }
            }
            if (maxH >= 3 && random.nextFloat() > 0.5f) {
                level.setBlock(center.offset(p[0], maxH + 1, p[1]), cap, 3);
            }
        }
    }
}
