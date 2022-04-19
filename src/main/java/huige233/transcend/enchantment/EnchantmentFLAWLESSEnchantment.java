package huige233.transcend.enchantment;

import huige233.transcend.init.ModEnchantment;
import huige233.transcend.util.Reference;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

public class EnchantmentFLAWLESSEnchantment extends Enchantment {
    public EnchantmentFLAWLESSEnchantment() {
        super(Rarity.RARE, EnumEnchantmentType.ARMOR_CHEST, new EntityEquipmentSlot[]{EntityEquipmentSlot.CHEST});
        this.setName("flawless_enchantment");
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID +":flawless_enchantment"));
        ModEnchantment.ENCHANTMENTS.add(this);
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 100000;
    }

    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return 1000000;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }
    protected boolean canApplyTogether(Enchantment ench) {
        return super.canApplyTogether(ench) && ench != Enchantments.THORNS;
    }
    public static boolean shouldHit(int level, Random rnd){
        if(level == 1){
            return rnd.nextFloat() < 1.0F;
        }else{
            return false;
        }
    }


    public void onUserHurt(EntityLivingBase user, Entity attacker, int level){
        Random random = user.getRNG();
        if(shouldHit(level,random)){
            if(attacker != null) {
                //attacker.attackEntityFrom(DamageSource.causeMobDamage(user), hp*10.0f);
                attacker.attackEntityFrom(DamageSource.causeThornsDamage(user), user.getMaxHealth() * 1000.0F+ 2147483647f);
            }
        }
    }
}
