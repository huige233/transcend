package com.huige233.transcend.util;

import com.huige233.transcend.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ArmorUtils {
    public static boolean fullEquipped(Player player) {
        if (player == null || player.getInventory() == null) return false;
        ItemStack head = player.getInventory().armor.get(3);
        ItemStack chest = player.getInventory().armor.get(2);
        ItemStack legs = player.getInventory().armor.get(1);
        ItemStack feet = player.getInventory().armor.get(0);
        return head.getItem() == ModItems.transcend_helmet.get()
                && chest.getItem() == ModItems.transcend_chestplate.get()
                && legs.getItem() == ModItems.transcend_leggings.get()
                && feet.getItem() == ModItems.transcend_boots.get();
    }
}
