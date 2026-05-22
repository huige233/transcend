package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 法环基石 — 法阵地面铺设方块。
 * 5个阶级共用此类，通过构造参数区分。
 */
public class CircleFoundationBlock extends Block {
    // 阶级（1-5），决定外观与发光强度
    private final int tier;

    public CircleFoundationBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.DEEPSLATE_BRICKS)
            // 3阶及以上的基石会自发微光
            .lightLevel(state -> tier >= 3 ? 2 : 0));
        this.tier = tier;
    }

    // 获取阶级
    public int getTier() {
        return tier;
    }
}
