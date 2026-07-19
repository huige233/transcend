package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class PillarCapBlock extends Block {

    private final int tier;

    public PillarCapBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.GOLD)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.AMETHYST_CLUSTER)

            .lightLevel(state -> 7));
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }
}
