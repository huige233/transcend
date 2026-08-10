package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 浓聚水晶方块（装饰/资源方块）。 */
public class ConcentratedCrystalBlock extends Block {

    public static final int MANA_STORAGE = 900;

    public ConcentratedCrystalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(5.0F, 12.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 11)
                .requiresCorrectToolForDrops());
    }
}
