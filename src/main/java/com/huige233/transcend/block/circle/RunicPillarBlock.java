package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 符文柱方块（纵轴旋转方块）。 */
public class RunicPillarBlock extends RotatedPillarBlock {

    private final int tier;

    public RunicPillarBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .requiresCorrectToolForDrops()
            .strength(4.0F, 8.0F)
            .sound(SoundType.DEEPSLATE_BRICKS));
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }
}
