package huige233.transcend.items.armor;

import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import huige233.transcend.Main;
import huige233.transcend.compat.PsiCompat;
import huige233.transcend.init.ModItems;
import huige233.transcend.items.fireimmune;
import huige233.transcend.util.ArmorUtils;
import huige233.transcend.util.IHasModel;
import huige233.transcend.util.ItemNBTHelper;
import huige233.transcend.util.Reference;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import thaumcraft.api.items.IRechargable;
import thaumcraft.api.items.IVisDiscountGear;


import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
@Optional.Interface(iface = "thaumcraft.api.items.IVisDiscountGear", modid = "thaumcraft")
@Optional.Interface(iface = "thaumcraft.api.items.IRechargable", modid = "thaumcraft")
public class ArmorBase extends ItemArmor implements IHasModel, IVisDiscountGear, IRechargable {
    public ArmorBase(String name, ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn, CreativeTabs tab) {
        super(materialIn, renderIndexIn, equipmentSlotIn);
        setTranslationKey(name);
        setRegistryName(name);
        setCreativeTab(tab);
        ModItems.ITEMS.add(this);
    }


    @Override
    public void registerModels() {
        Main.proxy.registerItemRenderer(this, 0, "inventory");
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.isCanceled())
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!player.isServerWorld())
            return;
        NonNullList<ItemStack> armor = player.inventory.armorInventory;
        if (armor.get(3).getItem() == ModItems.FLAWLESS_HELMET && armor.get(3).getItem() == ModItems.FLAWLESS_CHESTPLATE && armor.get(1).getItem() == ModItems.FLAWLESS_LEGGINGS && armor.get(0).getItem() == ModItems.FLAWLESS_BOOTS) {
            if (player.getHealth() <= 0) {
                event.setCanceled(true);
                event.getEntityLiving().setHealth(player.getMaxHealth());
            }
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.isCanceled())
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (!player.isServerWorld())
            return;
        if (ArmorUtils.fullEquipped(player)) {
            event.setCanceled(true);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void LivingAttackEvent(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.isCanceled())
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if (player.world.isRemote)
            return;
        NonNullList<ItemStack> armor = player.inventory.armorInventory;
        if (armor.get(3).getItem() == ModItems.FLAWLESS_HELMET && armor.get(2).getItem() == ModItems.FLAWLESS_CHESTPLATE && armor.get(1).getItem() == ModItems.FLAWLESS_LEGGINGS && armor.get(0).getItem() == ModItems.FLAWLESS_BOOTS) {

            Entity attacker = event.getSource().getTrueSource();
            if (attacker instanceof EntityPlayer) {
                PsiCompat.onPlayerAttack(player, (EntityPlayer) attacker);
            }

            event.setCanceled(true);
        }

    }


    @Override
    public void onArmorTick(@NotNull World world, @NotNull EntityPlayer player, @NotNull ItemStack itemStack) {
        if (this.armorType == EntityEquipmentSlot.HEAD) {
            player.setAir(300);
            player.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 300, 14, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 300, 0, false, false));
        } else if (this.armorType == EntityEquipmentSlot.CHEST) {
            player.capabilities.allowFlying = true;
            player.addPotionEffect(new PotionEffect(MobEffects.STRENGTH, 300, 14, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 300, 14, false, false));
            List<PotionEffect> effects = Lists.newArrayList(player.getActivePotionEffects());
            for(PotionEffect potion : Collections2.filter(effects,potion -> potion.getPotion().isBadEffect())) {
                player.removePotionEffect(potion.getPotion());
            }
        } else if (this.armorType == EntityEquipmentSlot.LEGS) {
            player.getFoodStats().addStats(20, 20.0f);
            player.addPotionEffect(new PotionEffect(MobEffects.SPEED, 300, 2, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.JUMP_BOOST, 300, 2, false, false));
        } else if (this.armorType == EntityEquipmentSlot.FEET) {
            player.setFire(0);
            player.addPotionEffect(new PotionEffect(MobEffects.LUCK, 300, 9, false, false));
            player.addPotionEffect(new PotionEffect(MobEffects.HASTE, 300, 44, false, false));
            if(player.isBurning()) {
                player.extinguish();
            }
        } else if (ArmorUtils.fullEquipped(player)) {
            if(!player.world.isRemote) {
                Multimap<String, AttributeModifier> attributes = HashMultimap.create();
                if(attributes.isEmpty()) return;

                player.setHealth(player.getMaxHealth());
            }
        }
    }

    @Override
    public Multimap<String,AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> attrib = super.getAttributeModifiers(slot, stack);
        Item item = stack.getItem();
        UUID uuid = new UUID(slot.toString().hashCode(), 0);
        if (slot == EntityEquipmentSlot.HEAD) {
            if(item == ModItems.FLAWLESS_HELMET) {
                attrib.put(SharedMonsterAttributes.LUCK.getName(), new AttributeModifier(uuid, "Flawless", 1000, 0));
            }
        } else if (slot == EntityEquipmentSlot.CHEST) {
            if(item == ModItems.FLAWLESS_CHESTPLATE) {
                attrib.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getName(), new AttributeModifier(uuid, "Flawless", 1000, 0));
            }
        } else if (slot == EntityEquipmentSlot.LEGS) {
            if(item == ModItems.FLAWLESS_LEGGINGS) {
                attrib.put(SharedMonsterAttributes.MAX_HEALTH.getName(), new AttributeModifier(uuid, "Flawless", 980, 0));
            }
        } else if (slot == EntityEquipmentSlot.FEET) {
            if(item == ModItems.FLAWLESS_BOOTS) {
                attrib.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(uuid, "Flawless", 0.2, 1));
            }
        }
        return attrib;
    }

    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    public Entity createEntity(World world,Entity location, ItemStack itemstack) {
        return new fireimmune(world,location,itemstack);
    }

    @SideOnly(Side.CLIENT)
    public boolean hasEffect(@NotNull ItemStack stack) {
        return (false);
    }

    public static boolean enabled = false;

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isRemote && entity instanceof EntityPlayer) {
            if(ArmorUtils.fullEquipped((EntityPlayer) entity)) {
                EntityPlayer player = (EntityPlayer) entity;
                ItemNBTHelper.setFloat(stack,"tc.charge",10000.0f);
                if(player.getHealth() < player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        }
    }

    public @NotNull EnumRarity getRarity(@NotNull ItemStack stack) {
        return (ModItems.COSMIC_RARITY);
    }

    @Optional.Method(modid = "thaumcraft")
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> stack) {
        ItemStack itemstack = new ItemStack(this);
        ItemNBTHelper.setByte(itemstack,"TC.RUNIC",(byte)127);
        stack.add(itemstack);
    }

    @Override
    @Optional.Method(modid = "thaumcraft")
    public int getVisDiscount(ItemStack itemStack, EntityPlayer entityPlayer) {
        return 100;
    }


    @Override
    @Optional.Method(modid = "thaumcraft")
    public int getMaxCharge(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        return 10000;
    }

    @Override
    @Optional.Method(modid = "thaumcraft")
    public EnumChargeDisplay showInHud(ItemStack itemStack, EntityLivingBase entityLivingBase) {
        return null;
    }
}
