package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * R76 灵魂烙印之笔（Soul Mark Quill）— 玩家在已激活的进阶图案锚上右键此物品，
 * 把锚标记为自己的"灵魂烙印点"。绑定后：
 * <ul>
 *   <li>死亡后自动复活在最近的同维度烙印点（vanilla 重生位置之上叠加 teleport）</li>
 *   <li>身处烙印点 100 格内时，每 3 秒获得再生 I + 抗性提升 I（持续 4 秒）</li>
 * </ul>
 *
 * <p>玩家可同时拥有的烙印数 = 当前飞升阶段（stage 1 → 1, stage 2 → 2, stage 3 → 3, stage 4 → 4）。
 * 阶段 0 玩家不可烙印。
 *
 * <p>实际交互在 {@link com.huige233.transcend.block.ascension.AscensionAnchorBlock#use} 内分支处理。
 */
public class SoulMarkQuillItem extends Item {

    public SoulMarkQuillItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
        ModItems.ITEMS.add(this);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                 @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.soul_mark_quill.usage")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.transcend.soul_mark_quill.cap_by_stage")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("tooltip.transcend.soul_mark_quill.respawn")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.soul_mark_quill.range_buff")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return ModRarities.COSMIC;
    }
}
