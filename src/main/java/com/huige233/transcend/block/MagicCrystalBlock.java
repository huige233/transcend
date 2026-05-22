package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 魔力水晶块 — 9个魔力水晶合成。
 * 可以作为信标基座方块（加入 #minecraft:beacon_base_blocks tag）。
 * 每个方块储存 300 点魔力。
 */
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
