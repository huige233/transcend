package huige233.transcend.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentSweepingEdge;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

public class EnchantmentTRANSCENDEnchantment extends Enchantment {
    private static final EntityEquipmentSlot[] EnchantmentSweepingEdge =  new EntityEquipmentSlot[]{EntityEquipmentSlot.MAINHAND};

    public EnchantmentTRANSCENDEnchantment() {
        super(Rarity.RARE,EnumEnchantmentType.WEAPON, EnchantmentSweepingEdge);
    }

    @Override
    public int getMinEnchantability(int enchantmentLevel) {
        return 10000;
    }
    @Override
    public int getMaxEnchantability(int enchantmentLevel) {
        return 100000;
    }
    @Override
    public int getMaxLevel() {
        return 1;
    }
    protected boolean canApplyTogether(Enchantment ench) {
        return super.canApplyTogether(ench);
    }
    public static boolean attackEntity(EntityEquipmentSlot slot, ItemStack stack) {
        if (slot == EntityEquipmentSlot.MAINHAND) {
            return true;
        }
        return false;
    }
}
