package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 法阵基座方块（地基组件）。 */
public class CircleFoundationBlock extends Block {

    private final int tier;

    public CircleFoundationBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.DEEPSLATE)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.DEEPSLATE_BRICKS)

            .lightLevel(state -> tier >= 3 ? 2 : 0));
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }
}
