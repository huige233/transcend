package com.huige233.transcend.items.circle;

import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 法阵蓝图 — 用于解锁与构筑法阵的图纸物品。
 * 分为残片、书页与完整蓝图三种类型。
 */
public class CircleBlueprintItem extends Item {

    /**
     * 蓝图类型枚举。
     */
    public enum BlueprintType {
        /** 蓝图残片：拼合后可形成书页。 */
        FRAGMENT,
        /** 蓝图书页：包含部分法阵信息。 */
        PAGE,
        /** 完整蓝图：可直接用于构筑法阵。 */
        SCHEMATIC
    }

    private final BlueprintType type;

    public CircleBlueprintItem(BlueprintType type) {
        super(new Properties().stacksTo(16));
        this.type = type;
        ModItems.ITEMS.add(this);
    }

    /**
     * 获取该蓝图的类型。
     */
    public BlueprintType getType() {
        return type;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        // 完整蓝图更稀有
        return switch (type) {
            case FRAGMENT -> Rarity.COMMON;
            case PAGE -> Rarity.UNCOMMON;
            case SCHEMATIC -> Rarity.RARE;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        // 根据类型展示不同描述
        String key = switch (type) {
            case FRAGMENT -> "tooltip.transcend.blueprint.fragment.desc";
            case PAGE -> "tooltip.transcend.blueprint.page.desc";
            case SCHEMATIC -> "tooltip.transcend.blueprint.schematic.desc";
        };
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    // TODO: Phase 4+ - use() opens blueprint UI / unlocks circle recipe
}
