package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * 远古水晶方块 — 不可挖掘，用于法则之境中心平台。
 * 古城战利品箱子中可以获得远古魔力水晶物品，9个合成此方块。
 * 硬度 -1（等同基岩，不可被生存模式挖掘）。
 */
public class AncientCrystalBlock extends Block {

    public AncientCrystalBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(-1.0F, 3600000.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 12)
                .pushReaction(PushReaction.BLOCK)
                .noOcclusion());
    }
}
