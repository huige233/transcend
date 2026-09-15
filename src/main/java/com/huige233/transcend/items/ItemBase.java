package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;


/** 为派生物品统一提供宇宙稀有度并声明自定义掉落实体需求。 */
public class ItemBase extends Item {
    public ItemBase(Properties properties) {
        super(properties);
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
