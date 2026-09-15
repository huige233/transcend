package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;


/** 为研究处理器、量子计算机和宇宙模拟器生成方块状态、立方体模型及物品模型。 */
public class ModBlockModelGen extends BlockStateProvider {

    public ModBlockModelGen(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, Transcend.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        registerBlockModelAndItem(ModBlocks.RESEARCH_PROCESSOR.get());
        registerBlockModelAndItem(ModBlocks.RESEARCH_QUANTUM_COMPUTER.get());
        registerBlockModelAndItem(ModBlocks.RESEARCH_COSMIC_SIMULATOR.get());
    }

    public void registerBlockModelAndItem(Block block) {
        this.simpleBlockWithItem(block, this.cubeAll(block));
    }
}
