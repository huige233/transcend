package com.huige233.transcend.items.forge;

import com.huige233.transcend.gear.forge.CelestialKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CelestialFragmentItem extends Item {

    private final CelestialKind kind;

    public CelestialFragmentItem(CelestialKind kind) {
        super(new Properties().rarity(Rarity.RARE).stacksTo(16));
        this.kind = kind;
    }

    public CelestialKind getKind() { return kind; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("celestial.transcend.tooltip.kind",
                Component.translatable(kind.langKey()).withStyle(kind.color))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("celestial.transcend.tooltip.usage")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
