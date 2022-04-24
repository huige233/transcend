package huige233.transcend.blocks;

import huige233.transcend.Main;
import huige233.transcend.init.ModBlock;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.util.IHasModel;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Random;

public class BlockBase extends Block implements IHasModel {
    public BlockBase(String name, Material material) {
        super(material);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(Main.TranscendTab);
        ModBlock.BLOCKS.add(this);
        ModItems.ITEMS.add(new ItemBlock(this).setRegistryName(this.getRegistryName()));
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        if(state.getBlock() == ModBlock.NETHER_STAR_BLOCK) {
            return Items.NETHER_STAR;
        }
        return Item.getItemFromBlock(this);
    }

    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(Item.getItemFromBlock(this), 0, "inventory");
    }


}
