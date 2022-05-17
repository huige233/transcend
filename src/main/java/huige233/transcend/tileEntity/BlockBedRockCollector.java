package huige233.transcend.tileEntity;

import codechicken.lib.util.RotationUtils;
import huige233.transcend.Main;
import huige233.transcend.blocks.BlockBase;
import huige233.transcend.gui.ModGuiElementLoader;
import huige233.transcend.init.Props;
import huige233.transcend.util.ItemUtils;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockBedRockCollector extends BlockBase {
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    private static boolean keepInventory;

    public BlockBedRockCollector(String name) {
        super(name, Material.IRON);
        setSoundType(SoundType.METAL);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, new IProperty[]{Props.HORIZONTAL_FACING, Props.ACTIVE});
    }

    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityCollerctor) {
            TileEntityCollerctor machineBase = (TileEntityCollerctor) tileEntity;
            state = state.withProperty(Props.HORIZONTAL_FACING, machineBase.getFacing());
            state = state.withProperty(Props.ACTIVE, machineBase.isActive());
        }

        return super.getActualState(state, worldIn, pos);
    }

    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return true;
        } else {
            player.openGui(Main.instance, ModGuiElementLoader.GUI_COLLECTOR, world, pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
    }

    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCollerctor();
    }

    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase player, ItemStack stack) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityCollerctor) {
            TileEntityCollerctor machine = (TileEntityCollerctor) tile;
            machine.setFacing(RotationUtils.getPlacedRotationHorizontal(player));
        }

    }

    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntityCollerctor collector = (TileEntityCollerctor) world.getTileEntity(pos);
        if (collector != null) {
            ItemUtils.dropInventory(world, pos, collector);
        }

        super.breakBlock(world, pos, state);
    }
}
