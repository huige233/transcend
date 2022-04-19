package huige233.transcend.items.tools;

import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.lib.TranscendDamageSources;
import huige233.transcend.util.ArmorUtils;
import huige233.transcend.util.IHasModel;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;


public class ToolSword extends ItemSword implements IHasModel {
    public ToolSword(String name, CreativeTabs tab,ToolMaterial material) {
        super(material);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(tab);
        ModItems.ITEMS.add(this);
    }
    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(this,0,"inventory");
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase player) {
        if(player.world.isRemote) {return true;}
        if(target instanceof EntityPlayer) {
            EntityPlayer p = (EntityPlayer) target;
            if(ArmorUtils.fullEquipped(p)){
                target.setHealth(target.getHealth()-4);
                return true;
            }
            if(p.getHeldItem(EnumHand.MAIN_HAND) != null && p.getHeldItem(EnumHand.MAIN_HAND).getItem()==ModItems.TRANSCEND_SWORD&&p.isHandActive()) {
                return true;
            }
        }
        //target.recentlyHit = 60;
        target.setHealth(0);
        target.getCombatTracker().trackDamage(new TranscendDamageSources(player),target.getHealth(),target.getMaxHealth()*100);
        //target.onDeath(new EntityDamageSource("transcend",player));
        return true;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public Entity createEntity(World world,Entity location, ItemStack itemstack) {
        return new fireimmune(world,location,itemstack);
    }

    public EnumRarity getRarity(ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}


