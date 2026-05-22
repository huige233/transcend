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

/**
 * 简单"lore物品"基类：仅承载稀有度 + 多行 tooltip lore 文本。
 *
 * <p>用法：{@code new LoreItem(Rarity.RARE, "tooltip.lore1", "tooltip.lore2")}。
 *
 * <p>不污染 {@link ItemBase} —— 专用于希望以叙事为主、不涉及具体战斗逻辑的物品。
 */
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
