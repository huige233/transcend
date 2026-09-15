package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;


/** 定义携带正整数功率倍率的研究站辅助插件方块。 */
public final class ResearchPluginBlock extends Block {
    private final long powerMultiplier;

    public ResearchPluginBlock(long multiplier) {
        super(Properties.of().mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL));
        if (multiplier < 1) throw new IllegalArgumentException("Research plugin multiplier must be positive");
        this.powerMultiplier = multiplier;
    }

    public long powerMultiplier() {
        return powerMultiplier;
    }
}
