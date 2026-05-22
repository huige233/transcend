package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 支柱顶盖 — 装饰并强化符文支柱顶端的发光方块。
 * 与 RunicPillarBlock 配套使用。
 */
public class PillarCapBlock extends Block {
    // 阶级，需与所连接的支柱保持一致
    private final int tier;

    public PillarCapBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.GOLD)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.AMETHYST_CLUSTER)
            // 顶盖维持显著金光
            .lightLevel(state -> 7));
        this.tier = tier;
    }

    // 获取阶级
    public int getTier() {
        return tier;
    }
}
