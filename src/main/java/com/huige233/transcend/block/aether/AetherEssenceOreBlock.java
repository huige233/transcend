package com.huige233.transcend.block.aether;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * Round 25: Aether Essence 矿石 — Aether Realm 独占 (End biome 浮岛中生成)。
 *
 * <p>掉落 aether_essence 1-3 个（fortune 缩放）。
 * 比 aether_ore / magic_crystal_ore 都更难采（5.0 硬度）。
 * 仅在 the_end biome 中生成 — 玩家必须通过 aether_travel_stone 进入 aether_realm 才能采集。
 */
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
