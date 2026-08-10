package com.huige233.transcend.items;

import com.huige233.transcend.init.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** 带背景设定文本的物品基类。 */
public class LoreItem extends Item {

    private final String[] loreKeys;

    public LoreItem(Rarity rarity, String... loreKeys) {
        super(new Properties().rarity(rarity));
        this.loreKeys = loreKeys != null ? loreKeys : new String[0];
        ModItems.ITEMS.add(this);
    }

    public LoreItem(Properties properties, String... loreKeys) {
        super(properties);
        this.loreKeys = loreKeys != null ? loreKeys : new String[0];
        ModItems.ITEMS.add(this);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (String key : loreKeys) {
            tooltip.add(Component.translatable(key));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
