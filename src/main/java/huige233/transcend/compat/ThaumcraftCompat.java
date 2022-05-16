package huige233.transcend.compat;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.IBauble;
import baubles.api.cap.IBaublesItemHandler;
import huige233.transcend.Main;
import huige233.transcend.items.ItemBase;
import huige233.transcend.items.fireimmune;
import huige233.transcend.util.ArmorUtils;
import huige233.transcend.util.ItemNBTHelper;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.RechargeHelper;
import thaumcraft.common.items.ItemTCBase;

@Optional.Interface(iface = "thaumcraft.common.items.ItemTCBase", modid = "thaumcraft")
@Optional.Interface(iface = "thaumcraft.api.items.IVisDiscountGear", modid = "thaumcraft")
@Optional.Interface(iface = "baubles.api.IBauble", modid = "baubles")
public class ThaumcraftCompat extends ItemBase implements IBauble, IVisDiscountGear, IRechargable {
    public ThaumcraftCompat(){
    super("transcend_vis", Main.TranscendTab);
    this.maxStackSize = 1;
    }

     public EnumRarity getRarity(ItemStack itemStack) {
        return EnumRarity.RARE;
    }

    public BaubleType getBaubleType(ItemStack itemstack){
        return BaubleType.RING;
    }

    @Optional.Method(modid = "thaumcraft")
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) {
        if (player instanceof EntityPlayer && !player.world.isRemote && player.ticksExisted % (itemstack.getItemDamage() == 0 ? 40 : 5) == 0) {
            NonNullList<ItemStack> inv = ((EntityPlayer)player).inventory.mainInventory;
            int a = 0;
            while(true) {
                InventoryPlayer var10001 = ((EntityPlayer)player).inventory;
                if (a >= InventoryPlayer.getHotbarSize()) {
                    IBaublesItemHandler baubles = BaublesApi.getBaublesHandler((EntityPlayer)player);
                    for(a = 0; a < baubles.getSlots(); ++a) {
                        if (RechargeHelper.rechargeItem(player.world, baubles.getStackInSlot(a), player.getPosition(), (EntityPlayer)player, 1) > 0.0F) {
                            return;
                        }
                    }
                    inv = ((EntityPlayer)player).inventory.armorInventory;
                    for(a = 0; a < inv.size(); ++a) {
                        if (RechargeHelper.rechargeItem(player.world, (ItemStack)inv.get(a), player.getPosition(), (EntityPlayer)player, 1) > 0.0F) {
                            return;
                        }
                    }
                    break;
                }
                if (RechargeHelper.rechargeItem(player.world, (ItemStack)inv.get(a), player.getPosition(), (EntityPlayer)player, 1) > 0.0F) {
                    return;
                }
                ++a;
            }
        }
    }


    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            ItemNBTHelper.setFloat(stack,"tc.charge",10000.0f);
        }
    }

    @Optional.Method(modid = "thaumcraft")
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stack) {
        if(tab == Main.TranscendTab) {
            ItemStack itemstack = new ItemStack(this);
            ItemNBTHelper.setByte(itemstack, "TC.RUNIC", (byte) 127);
            stack.add(itemstack);
        }
    }

    @Override
    @Optional.Method(modid = "thaumcraft")
    public int getVisDiscount(ItemStack itemStack, EntityPlayer entityPlayer) {
        return 99;
    }

    @Override
    @Optional.Method(modid = "thaumcraft")
    public int getMaxCharge(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        return 10000;
    }

    @Override
    public EnumChargeDisplay showInHud(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        return null;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public Entity createEntity(World world,Entity location, ItemStack itemstack) {
        return new fireimmune(world,location,itemstack);
    }
}
