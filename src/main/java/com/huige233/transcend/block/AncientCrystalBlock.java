package com.huige233.transcend.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

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
