package com.huige233.transcend.block.aether;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * 以太矿石 — 蕴含古代飞升者残留以太能量的矿物。
 * 通过 loot_table 掉落 aether_shard，需铁镐+。
 * 三个变体共享此类：通过构造参数区分石头/深板岩/下界岩属性。
 */
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
