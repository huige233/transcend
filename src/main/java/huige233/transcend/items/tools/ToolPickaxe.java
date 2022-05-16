package huige233.transcend.items.tools;

import com.google.common.collect.Multimap;
import huige233.transcend.Main;
import huige233.transcend.init.ModBlock;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.util.IHasModel;
import huige233.transcend.util.Reference;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ToolPickaxe extends ItemPickaxe implements IHasModel {
    public ToolPickaxe(String name, CreativeTabs tab, ToolMaterial material) {
        super(material);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(tab);
        ModItems.ITEMS.add(this);
    }

    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(this, 0, "inventory");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        //  if (player.isSneaking()) {
        if (player.getHeldItem(hand).getItem() == ModItems.TRANSCEND_PICKAXE) {
            if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, stack) < 10) {
                stack.addEnchantment(Enchantments.FORTUNE, 10);
            }
            //  }
            return new ActionResult(EnumActionResult.SUCCESS, stack);
        }
        return new ActionResult(EnumActionResult.PASS, stack);
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        World world = player.world;
        IBlockState state = world.getBlockState(pos);
        BlockPos blockpos1 = pos.add(1,-1,0);
        BlockPos blockpos2 = pos.add(0,-1,1);
        BlockPos blockpos3 = pos.add(-1,-1,0);
        BlockPos blockpos4 = pos.add(0,-1,-1);
        BlockPos blockPos = pos.add(0, -1, 0);
        int yi = 0;
        if (!world.isRemote) {
            if (state.getBlock() == ModBlock.NETHER_STAR_BLOCK && player.getHeldItem(EnumHand.MAIN_HAND).getItem() == ModItems.BREAK_BEDROCK_TOOL) {
                if (world.getBlockState(blockpos1).getBlock() == Blocks.DRAGON_EGG) {
                    world.setBlockToAir(blockpos1);
                    yi++;
                }
                if (world.getBlockState(blockpos2).getBlock() == Blocks.PURPUR_PILLAR) {
                    world.setBlockToAir(blockpos2);
                    yi++;
                }
                if (world.getBlockState(blockpos3).getBlock() == Blocks.REDSTONE_BLOCK) {
                    world.setBlockToAir(blockpos3);
                    yi++;
                }
                if (world.getBlockState(blockpos4).getBlock() == Blocks.EMERALD_BLOCK) {
                    world.setBlockToAir(blockpos4);
                    yi++;
                }
                stack.setCount(stack.getCount() - 1);
            }

            if(yi>=2) {
                Random ran = world.rand;
                double wa = yi*0.25f;
                if(wa >= ran.nextDouble()) {
                    world.spawnEntity(new EntityItem(world, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(Item.getItemFromBlock(ModBlock.BEDROCK_ORE))));
                    world.setBlockToAir(blockPos);
                } else {
                    player.sendMessage(new TextComponentString(I18n.translateToLocal("message.bedrock_ore_failed")));
                }
            }
        }
        return false;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public void setDamage(ItemStack stack, int damage) {
        if(stack.getItem() == ModItems.TRANSCEND_PICKAXE) {
            super.setDamage(stack, 0);
        }
    }

    public Entity createEntity(World world,Entity location, ItemStack itemstack) {
        return new fireimmune(world,location,itemstack);
    }

    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> attrib = super.getAttributeModifiers(slot, stack);
        UUID uuid = new UUID((slot.toString()).hashCode(), 0);
        if(slot == EntityEquipmentSlot.MAINHAND && stack.getItem() == ModItems.TRANSCEND_PICKAXE) {
            attrib.put(EntityPlayer.REACH_DISTANCE.getName(),new AttributeModifier(uuid,"Pickaxe modifier",256,0));
        }
        return attrib;
    }

    public EnumRarity getRarity(ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}
