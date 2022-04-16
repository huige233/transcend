package huige233.transcend.init;

import huige233.transcend.Main;
import huige233.transcend.items.ItemBase;
import huige233.transcend.items.armor.ArmorBase;
import huige233.transcend.items.tools.*;
import huige233.transcend.util.Reference;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.item.Item.ToolMaterial;
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

    public static EnumRarity COSMIC_RARITY = EnumHelper.addRarity("COSMIC", TextFormatting.DARK_GRAY, "Cosmic");
    public static final List<Item> ITEMS = new ArrayList<Item>();
    public static final Item TRANSCEND = new ItemBase("transcend", Main.TranscendTab);
    public static final Item FLAWLESS = new ItemBase("flawless", Main.TranscendTab);
    public static final ItemArmor.ArmorMaterial flawless_armor = EnumHelper.addArmorMaterial("flawless_alloy","",0,new int[] {1000,1000,1000,1000},1000, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,1000.0f);
    public static final ArmorMaterial ARMOR_MATERIAL_FLAWLESS = EnumHelper.addArmorMaterial("FLAWLESS", Reference.MOD_ID+":flawless", 0, new int[]{1000 , 1000, 1000, 1000}, 100, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,1000.0f);
    public static final Item FLAWLESS_HELMET = new ArmorBase("flawless_helmet", ARMOR_MATERIAL_FLAWLESS, 1, EntityEquipmentSlot.HEAD, Main.TranscendTab);
    public static final Item FLAWLESS_CHESTPLATE = new ArmorBase("flawless_chestplate", ARMOR_MATERIAL_FLAWLESS, 1, EntityEquipmentSlot.CHEST, Main.TranscendTab);
    public static final Item FLAWLESS_LEGGINGS = new ArmorBase("flawless_leggings", ARMOR_MATERIAL_FLAWLESS, 2, EntityEquipmentSlot.LEGS, Main.TranscendTab);
    public static final Item FLAWLESS_BOOTS = new ArmorBase("flawless_boots", ARMOR_MATERIAL_FLAWLESS, 2, EntityEquipmentSlot.FEET, Main.TranscendTab);
    public static final ToolMaterial transcend_tool = EnumHelper.addToolMaterial("TRANSCEND",99,-1,9999,2147483647.0f,100000);
    public static final ItemSword TRANSCEND_SWORD = new ToolSword("transcend_sword",Main.TranscendTab,transcend_tool);
    public static final ItemAxe TRANSCEND_AXE = new ToolAxe("transcend_axe",Main.TranscendTab,transcend_tool);
    public static final ItemPickaxe TRANSCEND_PICKAXE = new ToolPickaxe("transcend_pickaxe",Main.TranscendTab,transcend_tool);
    public static final ItemSpade TRANSCEND_SHOVEL = new ToolShovel("transcend_shovel",Main.TranscendTab,transcend_tool);
    public static final ItemHoe TRANSCEND_HOE = new ToolHoe("transcend_hoe",Main.TranscendTab,transcend_tool);


}
