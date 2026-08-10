package com.huige233.transcend.block.aether;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** 以太精华矿石方块：挖掘掉落经验。 */
public class AetherEssenceOreBlock extends DropExperienceBlock {

    public AetherEssenceOreBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN)
                .strength(5.0F, 6.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(s -> 6)
                .requiresCorrectToolForDrops(),
                UniformInt.of(3, 7));
    }
}
