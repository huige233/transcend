package huige233.transcend.items.tools;

import com.google.common.collect.Multimap;
import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.lib.TranscendDamageSources;
import huige233.transcend.util.ArmorUtils;
import huige233.transcend.util.IHasModel;
import huige233.transcend.util.Reference;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.UUID;


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

    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> attrib = super.getAttributeModifiers(slot, stack);
        UUID uuid = new UUID((slot.toString()).hashCode(), 0);
        if(slot == EntityEquipmentSlot.MAINHAND) {
            attrib.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),
                    new AttributeModifier(uuid,"Weapon modifier",0.99,1));
            attrib.put(EntityPlayer.REACH_DISTANCE.getName(),
                    new AttributeModifier(uuid,"Weapon modifier",256,0));
        }
        return attrib;
    }

    public EnumRarity getRarity(ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}


