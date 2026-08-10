package com.huige233.transcend.block.aether;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 以太方块（装饰/资源方块）。 */
public class AetherBlock extends Block {

    public AetherBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(4.0F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 9)
                .requiresCorrectToolForDrops());
    }
}
