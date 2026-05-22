package com.huige233.transcend.block.aether;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Round 18: 魔力水晶矿石 — 与 Aether Ore 并行的基础魔力源。
 * <p>通过 loot_table 掉落 magic_crystal，铁镐+ 可挖。
 * 三个变体：地表 / 深板岩 / 下界。
 */
public class MagicCrystalOreBlock extends DropExperienceBlock {

    public enum Variant {
        STONE     (MapColor.STONE,     3.0F, 3.0F, SoundType.STONE),
        DEEPSLATE (MapColor.DEEPSLATE, 4.5F, 3.0F, SoundType.DEEPSLATE),
        NETHER    (MapColor.NETHER,    3.0F, 3.0F, SoundType.NETHERRACK);

        public final MapColor color;
        public final float hardness;
        public final float resistance;
        public final SoundType sound;

        Variant(MapColor c, float h, float r, SoundType s) {
            this.color = c;
            this.hardness = h;
            this.resistance = r;
            this.sound = s;
        }
    }

    public MagicCrystalOreBlock(Variant v) {
        super(BlockBehaviour.Properties.of()
                .mapColor(v.color)
                .strength(v.hardness, v.resistance)
                .sound(v.sound)
                .requiresCorrectToolForDrops(),
                UniformInt.of(1, 4));
    }
}
