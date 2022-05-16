package huige233.transcend.blocks;

import huige233.transcend.Main;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.util.Random;

public class voidblock extends BlockBase {
    public voidblock(String name, Material material) {
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
    public boolean isFullBlock(IBlockState state) {
        return false;
    }
    @Override
    public boolean isBlockNormalCube(IBlockState state) {
        return false;
    }
    @Override
    public boolean isNormalCube(IBlockState state) {
        return false;
    }
    @Override
    public boolean isCollidable(){
        return false;
    }
    @Override
    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }
    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.INVISIBLE;
    }
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
        return Block.NULL_AABB;
    }
}
