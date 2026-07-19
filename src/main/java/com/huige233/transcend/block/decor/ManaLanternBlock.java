package com.huige233.transcend.block.decor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ManaLanternBlock extends Block {

    public ManaLanternBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(0.8F, 0.8F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 15));
    }
}
