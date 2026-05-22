package com.huige233.transcend.block.decor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 魔力提灯 — 以以太碎片驱动的纯装饰性高亮度光源。
 * 比火把更亮 (15)，永久点亮，雨中不灭。
 */
public class ManaLanternBlock extends Block {

    public ManaLanternBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(0.8F, 0.8F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 15));
    }
}
