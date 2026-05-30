package com.huige233.transcend.items.forge;

import com.huige233.transcend.gear.forge.ResonanceKind;
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
 * R83: 共鸣水晶物品（6 种 ResonanceKind 各 1 个）。
 *
 * <p>玩家把共鸣水晶投入 {@code resonance_inlay_table}：每投 1 颗即在装备上写入
 * 一个 socket（{@link com.huige233.transcend.gear.GearForgeData#addResonanceSocket}）。
 * 上限 4 socket（R80 锁定）。
 */
public class ResonanceCrystalItem extends Item {

    private final ResonanceKind kind;

    public ResonanceCrystalItem(ResonanceKind kind) {
        super(new Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
        this.kind = kind;
    }

    public ResonanceKind getKind() { return kind; }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 主题名（带颜色）
        tooltip.add(Component.translatable("resonance.transcend.tooltip.theme",
                Component.translatable(kind.langKey()).withStyle(kind.color))
                .withStyle(ChatFormatting.GRAY));
        // 效果简介
        tooltip.add(Component.translatable(kind.descKey())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        // 用法
        tooltip.add(Component.translatable("resonance.transcend.tooltip.usage")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
