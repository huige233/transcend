package huige233.transcend.items.tools;

import com.google.common.collect.Multimap;
import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.lib.TranscendDamageSources;
import huige233.transcend.util.ArmorUtils;
import huige233.transcend.util.IHasModel;
import huige233.transcend.util.TextUtils;
import net.minecraft.client.util.ITooltipFlag;
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
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.UUID;


public class ToolSword extends ItemSword implements IHasModel {
    private static final ToolMaterial TRANSCEND_SWORD = EnumHelper.addToolMaterial("TRANSCEND_SWORD", 32, -1, 9999.0f, 32763F, 10000);

    public ToolSword(String name, CreativeTabs tab,ToolMaterial material) {
        super(TRANSCEND_SWORD);
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
        target.attackEntityFrom((new TranscendDamageSources(player)).setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(),Float.MAX_VALUE);
        target.setHealth(0);
        target.getCombatTracker().trackDamage(new TranscendDamageSources(player),Float.MAX_VALUE,Float.MAX_VALUE);
        target.onDeath(new EntityDamageSource("transcend",player));
        target.isDead = true;
        target.setDead();
        return true;
    }

    @SideOnly(Side.CLIENT)
    public float getMaskOpacity(ItemStack stack) {
        return 1.0f;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public Entity createEntity(World world,Entity location, ItemStack itemstack) {
        return new fireimmune(world,location,itemstack);
    }

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event){
        if(event.getItemStack().getItem() instanceof ToolSword) {
          //  for(int x = 0; x < event.getToolTip().size(); ++x) {
               // if (((String)event.getToolTip().get(x)).contains(I18n.translateToLocal("attribute.name.generic.attackDamage")) || ((String)event.getToolTip().get(x)).contains(I18n.translateToLocal("Attack Damage"))) {
                    event.getToolTip().set(1, TextFormatting.BLUE + "+" + TextUtils.makeFabulous(I18n.translateToLocal("tip.transcend")) + " " + TextFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.attackDamage"));
                    return;
               // }
          //  }
        }
    }

    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> attrib = super.getAttributeModifiers(slot, stack);
        UUID uuid = new UUID((slot.toString()).hashCode(), 0);
        if(slot == EntityEquipmentSlot.MAINHAND) {
            attrib.put(SharedMonsterAttributes.ATTACK_SPEED.getName(),new AttributeModifier(uuid,"Weapon modifier",0.99,1));
            attrib.put(EntityPlayer.REACH_DISTANCE.getName(), new AttributeModifier(uuid,"Weapon modifier",256,0));
        }
        return attrib;
    }


    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag){
        tooltip.add(TextUtils.makeSANIC(I18n.translateToLocal("tooltip.transcend_sword.desc")));
    }

    public EnumRarity getRarity(ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}


