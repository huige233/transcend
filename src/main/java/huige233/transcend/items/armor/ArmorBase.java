package huige233.transcend.items.armor;

import huige233.transcend.Main;
import huige233.transcend.init.ModItems;
import huige233.transcend.util.IHasModel;
import huige233.transcend.util.Reference;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber( modid = Reference.MOD_ID )
public class ArmorBase extends ItemArmor implements IHasModel {
    public ArmorBase( String name, ArmorMaterial materialIn, int renderIndexIn, EntityEquipmentSlot equipmentSlotIn, CreativeTabs tab )
    {
        super(materialIn, renderIndexIn, equipmentSlotIn);
        setTranslationKey( name );
        setRegistryName( name );
        setCreativeTab( tab );
        ModItems.ITEMS.add( this );
    }


    @Override
    public void registerModels()
    {
        Main.proxy.registerItemRenderer( this, 0, "inventory" );
    }


    @SubscribeEvent( priority = EventPriority.HIGHEST )
    public static void onPlayerDeath( LivingDeathEvent event )
    {
        if ( !(event.getEntityLiving() instanceof EntityPlayer) || event.isCanceled() )
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if ( !player.isServerWorld() )
            return;
        NonNullList<ItemStack> armor = player.inventory.armorInventory;
        if ( armor.get( 3 ).getItem() == ModItems.FLAWLESS_HELMET && armor.get( 3 ).getItem() == ModItems.FLAWLESS_CHESTPLATE && armor.get( 1 ).getItem() == ModItems.FLAWLESS_LEGGINGS && armor.get( 0 ).getItem() == ModItems.FLAWLESS_BOOTS )
        {
            if ( player.getHealth() <= 0 )
            {
                event.setCanceled( true );
                float maxHP = player.getMaxHealth();
                event.getEntityLiving().setHealth( maxHP );
            }
        }
    }


    @SubscribeEvent( priority = EventPriority.HIGHEST )
    public static void onPlayerHurt( LivingHurtEvent event )
    {
        if ( !(event.getEntityLiving() instanceof EntityPlayer) || event.isCanceled() )
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if ( !player.isServerWorld() )
            return;
        NonNullList<ItemStack> armor = player.inventory.armorInventory;
        if ( armor.get( 3 ).getItem() == ModItems.FLAWLESS_HELMET && armor.get( 2 ).getItem() == ModItems.FLAWLESS_CHESTPLATE && armor.get( 1 ).getItem() == ModItems.FLAWLESS_LEGGINGS && armor.get( 0 ).getItem() == ModItems.FLAWLESS_BOOTS )
        {
            event.setCanceled( true );
        }
    }


    @SubscribeEvent( priority = EventPriority.HIGHEST )
    public static void LivingAttackEvent( LivingAttackEvent event )
    {
        if ( !(event.getEntityLiving() instanceof EntityPlayer) || event.isCanceled() )
            return;
        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        if ( player.world.isRemote )
            return;
        NonNullList<ItemStack> armor = player.inventory.armorInventory;
        if ( armor.get( 3 ).getItem() == ModItems.FLAWLESS_HELMET && armor.get( 2 ).getItem() == ModItems.FLAWLESS_CHESTPLATE && armor.get( 1 ).getItem() == ModItems.FLAWLESS_LEGGINGS && armor.get( 0 ).getItem() == ModItems.FLAWLESS_BOOTS )
        {
            event.setCanceled( true );
        }
    }


    @Override
    public void onArmorTick(@NotNull World world, @NotNull EntityPlayer player, @NotNull ItemStack itemStack )
    {
        if ( this.armorType == EntityEquipmentSlot.HEAD )
        {
            player.setAir( 300 );
            player.addPotionEffect( new PotionEffect( MobEffects.RESISTANCE, 300, 14, false, false ) );
            player.addPotionEffect( new PotionEffect( MobEffects.NIGHT_VISION, 300, 0, false, false ) );
        } else if ( this.armorType == EntityEquipmentSlot.CHEST )
        {
            player.capabilities.allowFlying = true;
            player.addPotionEffect( new PotionEffect( MobEffects.STRENGTH, 300, 14, false, false ) );
            player.addPotionEffect( new PotionEffect( MobEffects.REGENERATION, 300, 14, false, false ) );
        } else if ( this.armorType == EntityEquipmentSlot.LEGS )
        {
            player.getFoodStats().addStats( 20, 20.0f );
            player.addPotionEffect( new PotionEffect( MobEffects.SPEED, 300, 2, false, false ) );
            player.addPotionEffect( new PotionEffect( MobEffects.JUMP_BOOST, 300, 2, false, false ) );
        } else if ( this.armorType == EntityEquipmentSlot.FEET )
        {
            player.setFire(0);
            player.addPotionEffect( new PotionEffect( MobEffects.LUCK, 300, 9, false, false ) );
            player.addPotionEffect( new PotionEffect( MobEffects.HASTE, 300, 44, false, false ) );
        }
    }


    @SideOnly( Side.CLIENT )
    public boolean hasEffect(@NotNull ItemStack stack )
    {
        return(false);
    }


    public @NotNull EnumRarity getRarity(@NotNull ItemStack stack )
    {
        return(ModItems.COSMIC_RARITY);
    }
}
