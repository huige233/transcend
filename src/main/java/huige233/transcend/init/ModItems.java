package huige233.transcend.init;

import huige233.transcend.Main;
import huige233.transcend.items.ItemBase;
import huige233.transcend.items.armor.ArmorBase;
import huige233.transcend.util.Reference;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.EnumHelper;

import java.util.ArrayList;
import java.util.List;

public class ModItems {

    //public static FLAWLESSARMOR flawless_helmet;
   //public static FLAWLESSARMOR flawless_chestplate;
    //public static FLAWLESSARMOR flawless_leggings;
    //public static FLAWLESSARMOR flawless_boots;

    public static EnumRarity COSMIC_RARITY = EnumHelper.addRarity("COSMIC", TextFormatting.RED, "Cosmic");
    public static final List<Item> ITEMS = new ArrayList<Item>();
    public static final Item TRANSCEND = new ItemBase("transcend", Main.TranscendTab);
    public static final Item FLAWLESS = new ItemBase("flawless", Main.TranscendTab);
    public static final ItemArmor.ArmorMaterial flawless_armor = EnumHelper.addArmorMaterial("flawless_alloy","",0,new int[] {1000,1000,1000,1000},1000, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,1000.0f);
    public static final ArmorMaterial ARMOR_MATERIAL_FLAWLESS = EnumHelper.addArmorMaterial("FLAWLESS", Reference.MOD_ID+":flawless", 0, new int[]{1000 , 1000, 1000, 1000}, 100, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,1000.0f);
    public static final Item FLAWLESS_HELMET = new ArmorBase("flawless_helmet", ARMOR_MATERIAL_FLAWLESS, 1, EntityEquipmentSlot.HEAD, Main.TranscendTab);
    public static final Item FLAWLESS_CHESTPLATE = new ArmorBase("flawless_chestplate", ARMOR_MATERIAL_FLAWLESS, 1, EntityEquipmentSlot.CHEST, Main.TranscendTab);
    public static final Item FLAWLESS_LEGGINGS = new ArmorBase("flawless_leggings", ARMOR_MATERIAL_FLAWLESS, 2, EntityEquipmentSlot.LEGS, Main.TranscendTab);
    public static final Item FLAWLESS_BOOTS = new ArmorBase("flawless_boots", ARMOR_MATERIAL_FLAWLESS, 2, EntityEquipmentSlot.FEET, Main.TranscendTab);

    /*
    public static void init() {
        flawless_helmet = new FLAWLESSARMOR(EntityEquipmentSlot.HEAD);
        flawless_helmet.setTranslationKey("transcend:flawless_helmet");

        flawless_chestplate = new FLAWLESSARMOR(EntityEquipmentSlot.CHEST);
        flawless_chestplate.setTranslationKey("transcend:flawless_chestplate");
        flawless_leggings = new FLAWLESSARMOR(EntityEquipmentSlot.LEGS);
        flawless_leggings.setTranslationKey("transcend:flawless_leggings");
        flawless_boots = new FLAWLESSARMOR(EntityEquipmentSlot.FEET);
        flawless_boots.setTranslationKey("transcend:flawless_boots");

    }
    public static <V extends Item>  V registerItem(V item) {
        ITEMS.add(item);
        return item;
    }
    */


}
