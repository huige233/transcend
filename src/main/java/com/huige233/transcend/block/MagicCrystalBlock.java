package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 魔法水晶方块（装饰/资源方块）。 */
public class MagicCrystalBlock extends Block {

    public static final int MANA_STORAGE = 300;

    public MagicCrystalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIAMOND)
                .strength(3.0F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 7)
                .requiresCorrectToolForDrops());
    }
}
