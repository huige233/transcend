package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 浓缩魔力水晶块 — 9个精炼魔力水晶合成。
 * 可以作为信标基座方块（加入 #minecraft:beacon_base_blocks tag）。
 * 比普通魔力水晶块更高级。
 */
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
