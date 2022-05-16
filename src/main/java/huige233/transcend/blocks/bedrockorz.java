package huige233.transcend.blocks;

import huige233.transcend.Main;
import huige233.transcend.init.ModBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

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
        return Items.AIR;
    }

    @Override
    public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
        return 15;
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public boolean canEntityDestroy(IBlockState state, IBlockAccess world, BlockPos pos, Entity entity) {
        return false;
    }
}
