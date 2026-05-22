package com.huige233.transcend.block.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 以太玻璃 — 透光、不阻挡视线，自身微弱发光。
 * 仅丝绸之触可掉落。
 */
public class AetherGlassBlock extends HalfTransparentBlock {

    public AetherGlassBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE)
                .strength(0.5F, 0.5F)
                .sound(SoundType.GLASS)
                .lightLevel(state -> 4)
                .noOcclusion()
                .isViewBlocking((s, g, p) -> false)
                .isSuffocating((s, g, p) -> false));
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}

