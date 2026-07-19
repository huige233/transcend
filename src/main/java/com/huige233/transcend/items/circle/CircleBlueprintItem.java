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

public class CircleBlueprintItem extends Item {

    public enum BlueprintType {

        FRAGMENT,

        PAGE,

        SCHEMATIC
    }

    private final BlueprintType type;

    public CircleBlueprintItem(BlueprintType type) {
        super(new Properties().stacksTo(16));
        this.type = type;
        ModItems.ITEMS.add(this);
    }

    public BlueprintType getType() {
        return type;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {

        return switch (type) {
            case FRAGMENT -> Rarity.COMMON;
            case PAGE -> Rarity.UNCOMMON;
            case SCHEMATIC -> Rarity.RARE;
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {

        String key = switch (type) {
            case FRAGMENT -> "tooltip.transcend.blueprint.fragment.desc";
            case PAGE -> "tooltip.transcend.blueprint.page.desc";
            case SCHEMATIC -> "tooltip.transcend.blueprint.schematic.desc";
        };
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

}
