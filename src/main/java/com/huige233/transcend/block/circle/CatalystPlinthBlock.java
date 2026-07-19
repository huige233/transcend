package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

public class CatalystPlinthBlock extends Block {

    private final boolean sealed;

    public CatalystPlinthBlock(boolean sealed) {
        super(Properties.of()
            .mapColor(MapColor.GOLD)
            .requiresCorrectToolForDrops()
            .strength(3.5F, 8.0F)
            .sound(SoundType.STONE)

            .noOcclusion());
        this.sealed = sealed;
    }

    public boolean isSealed() {
        return sealed;
    }
}
