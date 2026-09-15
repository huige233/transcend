package com.huige233.transcend.util;

import com.huige233.transcend.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/** 检查玩家是否穿齐超越护甲或穿戴其中任意一件，以判定装备能力的启用条件。 */
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

    
    public static boolean hasAnyTranscendArmor(Player player) {
        if (player == null || player.getInventory() == null) return false;
        return player.getInventory().armor.stream().anyMatch(s -> {
            net.minecraft.world.item.Item i = s.getItem();
            return i == ModItems.transcend_helmet.get()
                    || i == ModItems.transcend_chestplate.get()
                    || i == ModItems.transcend_leggings.get()
                    || i == ModItems.transcend_boots.get();
        });
    }
}
