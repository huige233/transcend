package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 催化基座 — 用于摆放法阵催化物品的单槽位方块。
 * 普通基座可放入物品，封印基座则用于锁定结构。
 */
public class CatalystPlinthBlock extends Block {
    // 是否为封印态基座（true=封印基座，false=普通基座）
    private final boolean sealed;

    public CatalystPlinthBlock(boolean sealed) {
        super(Properties.of()
            .mapColor(MapColor.GOLD)
            .requiresCorrectToolForDrops()
            .strength(3.5F, 8.0F)
            .sound(SoundType.STONE)
            // 顶部物品展示需透光与异形渲染
            .noOcclusion());
        this.sealed = sealed;
    }

    // 是否封印
    public boolean isSealed() {
        return sealed;
    }
}
