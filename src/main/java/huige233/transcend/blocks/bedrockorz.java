package huige233.transcend.blocks;

import huige233.transcend.Main;
import huige233.transcend.init.ModBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;

import java.util.Random;

public class bedrockorz extends BlockBase{
    public bedrockorz(String name, Material material) {
        super(name, material);
        setSoundType(SoundType.METAL);
        setCreativeTab(Main.TranscendTab);
        setHardness(-1.0F);
        setResistance(6000000.0F);
        setHarvestLevel("pickaxe", 300000);
        setLightLevel(15.0f);
    }
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(ModBlock.BEDROCK_ORE);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }
}
