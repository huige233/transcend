package huige233.transcend.items.tools;

import com.google.common.collect.Multimap;
import huige233.transcend.Main;
import huige233.transcend.compat.ThaumcraftSword;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.lib.TranscendDamageSources;
import huige233.transcend.util.*;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import vazkii.botania.api.mana.ICreativeManaProvider;
import vazkii.botania.api.mana.IManaItem;
import vazkii.botania.api.mana.IManaTooltipDisplay;

import java.util.List;
import java.util.UUID;

@Optional.Interface(iface="vazkii.botania.api.mana.IManaItem",modid="botania")
@Optional.Interface(iface="vazkii.botania.api.mana.IManaTooltipDisplay",modid="botania")
@Optional.Interface(iface="vazkii.botania.api.mana.ICreativeManaProvider",modid="botania")
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class ToolSword extends ItemSword implements IHasModel, ICreativeManaProvider, IManaItem, IManaTooltipDisplay {

    protected static final int MAX_MANA = Integer.MAX_VALUE;
    private static final String TAG_CREATIVE = "creative";
    private static final String TAG_ONE_USE = "oneUse";

    private static final String TAG_MANA = "mana";

    public ToolSword(String name, CreativeTabs tab, ToolMaterial material) {
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

    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack par1ItemStack) {
        return false;
    }

    public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase player) {
        if (!player.world.isRemote) {
            if (target instanceof EntityPlayer) {
                EntityPlayer t = (EntityPlayer) target;
                if (ArmorUtils.fullEquipped(t)) {
                    target.setHealth(4);
                    return true;
                }
                if (Loader.isModLoaded("thaumcraft")) {
                    ThaumcraftSword.damageEntity(target);
                }
                t.inventory.dropAllItems();
            }
            target.attackEntityFrom((new TranscendDamageSources(player)).setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(), Float.MAX_VALUE);
            target.getCombatTracker().trackDamage(new EntityDamageSource("transcend", player), Float.MAX_VALUE, Float.MAX_VALUE);
            target.setHealth(0);
            target.onDeath(new EntityDamageSource("transcend", player));
        }
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        if(!entity.world.isRemote && entity instanceof EntityPlayer) {
            EntityPlayer t = (EntityPlayer) entity;
            if(t.capabilities.isCreativeMode && !t.isDead && !ArmorUtils.fullEquipped(t)) {
                t.inventory.dropAllItems();
                t.attackEntityFrom((new TranscendDamageSources(player)).setDamageAllowedInCreativeMode().setDamageBypassesArmor().setDamageIsAbsolute(), Float.MAX_VALUE);
                t.setHealth(0);
                t.onDeath(new EntityDamageSource("transcend", player));
                return true;
            }
        }
        return false;
    }

    public boolean hasCustomEntity (ItemStack stack){
        return true;
    }

    public Entity createEntity(World world,Entity location, ItemStack itemstack) {
        return new fireimmune(world,location,itemstack);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event){
        if(event.getItemStack().getItem() instanceof ToolSword) {
            for (int x = 0; x < event.getToolTip().size(); ++x) {
                if (((String) event.getToolTip().get(x)).contains(I18n.translateToLocal("attribute.name.generic.attackDamage")) || ((String) event.getToolTip().get(x)).contains(I18n.translateToLocal("Attack Damage"))) {
                    if (event.getItemStack().getItem() == ModItems.TRANSCEND_SWORD) {
                        event.getToolTip().set(x, TextFormatting.BLUE + "+" + TextUtils.makeFabulous(I18n.translateToLocal("tip.transcend")) + " " + TextFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.attackDamage"));
                        event.getToolTip().set(x+1, TextFormatting.BLUE + "+" + TextUtils.makeFabulous(I18n.translateToLocal("tip.transcend")) + " " + TextFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.reachDistance"));
                    }
                    return;
                }
            }
        }
    }

    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> attrib = super.getAttributeModifiers(slot, stack);
        UUID uuid = new UUID((slot.toString()).hashCode(), 0);
        if(slot == EntityEquipmentSlot.MAINHAND) {
            attrib.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(uuid, "Weapon modifier", 0.99, 1));
            attrib.put(EntityPlayer.REACH_DISTANCE.getName(), new AttributeModifier(uuid, "Weapon modifier", 256, 0));
        }
        return attrib;
    }

    @Optional.Method(modid = "botania")
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stack) {
        ItemStack create = new ItemStack(this);
            setMana(create, MAX_MANA);
            isCreative(create);
            setStackCreative(create);

        stack.add(create);
    }

    @Optional.Method(modid = "botania")
    public static void setMana(ItemStack stack, int mana) {
        ItemNBTHelper.setInt(stack, TAG_MANA, MAX_MANA-1);
    }

    @Optional.Method(modid = "botania")
    public static void setStackCreative(ItemStack stack) {
        ItemNBTHelper.setBoolean(stack, TAG_CREATIVE, true);
    }

    @Override
    @Optional.Method(modid = "botania")
    public int getMana(ItemStack stack) {
        return ItemNBTHelper.getInt(stack, TAG_MANA, 0);
    }

    @Override
    @Optional.Method(modid = "botania")
    public int getMaxMana(ItemStack stack) {
        return MAX_MANA-1;
    }

    @Override
    @Optional.Method(modid = "botania")
    public void addMana(ItemStack stack, int mana) {
        setMana(stack, Math.min(getMana(stack) + mana, getMaxMana(stack)));
    }

    @Override
    @Optional.Method(modid = "botania")
    public boolean canReceiveManaFromPool(ItemStack stack, TileEntity pool) {
        return !ItemNBTHelper.getBoolean(stack, TAG_ONE_USE, false);
    }

    @Override
    @Optional.Method(modid = "botania")
    public boolean canReceiveManaFromItem(ItemStack stack, ItemStack otherStack) {
        return true;
    }

    @Override
    @Optional.Method(modid = "botania")
    public boolean canExportManaToPool(ItemStack stack, TileEntity pool) {
        return true;
    }

    @Override
    @Optional.Method(modid = "botania")
    public boolean canExportManaToItem(ItemStack stack, ItemStack otherStack) {
        return true;
    }

    @Override
    @Optional.Method(modid = "botania")
    public boolean isNoExport(ItemStack stack) {
        return true;
    }

    @Override
    @Optional.Method(modid = "botania")
    public float getManaFractionForDisplay(ItemStack stack) {
        return (float) getMana(stack) / (float)getMaxMana(stack);
    }

    @Override
    @Optional.Method(modid = "botania")
    public boolean isCreative(ItemStack stack) {
        return false;
    }


    public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag){
        tooltip.add(TextUtils.makeFabulous(I18n.translateToLocal("tooltip.transcend_sword1.desc")) + " " + TextUtils.makeFabulous(I18n.translateToLocal("tooltip.transcend_sword2.desc")));
    }

    public EnumRarity getRarity(ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}


