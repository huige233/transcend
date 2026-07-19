package com.huige233.transcend.items.armor;

import com.huige233.transcend.Transcend;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class TranscendArmorMaterial implements ArmorMaterial {

    public static final TranscendArmorMaterial INSTANCE = new TranscendArmorMaterial();

    private static final int[] DURABILITY = {9999, 9999, 9999, 9999};
    private static final int[] DEFENSE = {1000, 1000, 1000, 1000};

    @Override
    public int getDurabilityForType(ArmorItem.@NotNull Type type) {
        return DURABILITY[type.ordinal()];
    }

    @Override
    public int getDefenseForType(ArmorItem.@NotNull Type type) {
        return DEFENSE[type.ordinal()];
    }

    @Override
    public int getEnchantmentValue() {
        return 100;
    }

    @Override
    public @NotNull SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public @NotNull String getName() {
        return Transcend.MODID + ":transcend";
    }

    @Override
    public float getToughness() {
        return 1000.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 1.0F;
    }
}
