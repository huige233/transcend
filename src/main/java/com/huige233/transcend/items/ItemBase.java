package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.init.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public class ItemBase extends Item {
    public ItemBase(String name) {
        super(new Item.Properties());
        ModItems.ITEMS.add(this);
    }

    public ItemBase(String name, Properties properties) {
        super(properties);
        ModItems.ITEMS.add(this);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return ModRarities.COSMIC;
    }
}
