package com.huige233.transcend.block.circle;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * 灵脉导管 — 法阵之间的法力连接节点。
 * 用于在多结构间引导法力流动。
 */
public class LeylineConduitBlock extends Block {
    // 阶级（1-5），决定可承载的法力上限
    private final int tier;

    public LeylineConduitBlock(int tier) {
        super(Properties.of()
            .mapColor(MapColor.ICE)
            .requiresCorrectToolForDrops()
            .strength(3.0F, 6.0F)
            .sound(SoundType.GLASS)
            // 导管常态自发蓝白冷光
            .lightLevel(state -> 3));
        this.tier = tier;
    }

    // 获取阶级
    public int getTier() {
        return tier;
    }
}
