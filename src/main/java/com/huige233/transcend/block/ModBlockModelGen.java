package com.huige233.transcend.block;

import com.huige233.transcend.Transcend;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/** 方块与物品模型数据生成器。 */
public class ModBlockModelGen extends BlockStateProvider {

    public ModBlockModelGen(PackOutput packOutput, ExistingFileHelper existingFileHelper) {
        super(packOutput, Transcend.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

    }

    public void registerBlockModelAndItem(Block block) {
        this.simpleBlockWithItem(block, this.cubeAll(block));
    }
}
