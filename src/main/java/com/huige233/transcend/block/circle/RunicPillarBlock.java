package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 符文支柱 — 高阶法阵所需的纵向支柱方块。
 * 继承自 RotatedPillarBlock，可沿三轴放置（同原木）。
 */
public class RunicPillarBlock extends RotatedPillarBlock {
    // 阶级（3-5），低阶法阵无需支柱
    private final int tier;

    public RunicPillarBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .requiresCorrectToolForDrops()
            .strength(4.0F, 8.0F)
            .sound(SoundType.DEEPSLATE_BRICKS));
        this.tier = tier;
    }

    // 获取阶级
    public int getTier() {
        return tier;
    }
}
