package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** 地脉导管方块（地脉流通组件）。 */
public class LeylineConduitBlock extends Block {

    private final int tier;

    public LeylineConduitBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.ICE)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.GLASS)

            .lightLevel(state -> 3));
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }
}
