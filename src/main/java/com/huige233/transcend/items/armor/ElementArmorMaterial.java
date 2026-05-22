package com.huige233.transcend.items.armor;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class ElementArmorMaterial implements ArmorMaterial {

    private final String name;
    private final int[] durability;
    private final int[] defense;
    private final int enchantValue;
    private final float toughness;
    private final float knockbackRes;

    public ElementArmorMaterial(String name, int dur, int def, int enchant, float tough, float kbRes) {
        this.name = name;
        this.durability = new int[]{dur, dur, dur, dur};
        this.defense = new int[]{def, def + 1, def + 2, def};
        this.enchantValue = enchant;
        this.toughness = tough;
        this.knockbackRes = kbRes;
    }

    public static final ElementArmorMaterial PYRO = new ElementArmorMaterial("transcend:pyro", 500, 7, 20, 3.0F, 0.1F);
    public static final ElementArmorMaterial CRYO = new ElementArmorMaterial("transcend:cryo", 500, 7, 20, 3.0F, 0.1F);
    public static final ElementArmorMaterial STORM = new ElementArmorMaterial("transcend:storm", 400, 6, 25, 2.5F, 0.0F);
    public static final ElementArmorMaterial TERRA = new ElementArmorMaterial("transcend:terra", 600, 9, 15, 4.0F, 0.2F);
    public static final ElementArmorMaterial ARCANE = new ElementArmorMaterial("transcend:arcane", 450, 6, 30, 2.0F, 0.0F);
    public static final ElementArmorMaterial ABYSS = new ElementArmorMaterial("transcend:abyss", 450, 7, 25, 3.0F, 0.1F);

    @Override public int getDurabilityForType(ArmorItem.Type type) { return durability[type.ordinal()]; }
    @Override public int getDefenseForType(ArmorItem.Type type) { return defense[type.ordinal()]; }
    @Override public int getEnchantmentValue() { return enchantValue; }
    @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_NETHERITE; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    @Override public String getName() { return name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockbackRes; }
}
