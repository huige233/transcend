package com.huige233.transcend.gear;

import com.huige233.transcend.items.TranscendWand;
import com.huige233.transcend.items.tools.AspectSwordItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public enum GearCategory {
    WEAPON,
    TOOL,
    ARMOR,
    OTHER;

    public static GearCategory classify(ItemStack stack) {
        if (stack.isEmpty()) return OTHER;
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof TranscendWand
                || item instanceof AspectSwordItem) {
            return WEAPON;
        }
        if (item instanceof DiggerItem) {

            return TOOL;
        }
        if (item instanceof ArmorItem) {
            return ARMOR;
        }
        return OTHER;
    }
}
