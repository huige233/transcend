package com.huige233.transcend.block.aether;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AetherOreBlock extends DropExperienceBlock {

    public enum Variant {
        OVERWORLD(MapColor.STONE, 3.0F, 3.0F, SoundType.STONE),
        DEEPSLATE(MapColor.DEEPSLATE, 4.5F, 3.0F, SoundType.DEEPSLATE),
        NETHER(MapColor.NETHER, 3.0F, 3.0F, SoundType.NETHERRACK);

        public final MapColor color;
        public final float hardness;
        public final float resistance;
        public final SoundType sound;

        Variant(MapColor color, float hardness, float resistance, SoundType sound) {
            this.color = color;
            this.hardness = hardness;
            this.resistance = resistance;
            this.sound = sound;
        }
    }

    public AetherOreBlock(Variant variant) {
        super(BlockBehaviour.Properties.of()
                .mapColor(variant.color)
                .strength(variant.hardness, variant.resistance)
                .sound(variant.sound)
                .requiresCorrectToolForDrops(),
                UniformInt.of(2, 5));
    }
}
