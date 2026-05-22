package com.huige233.transcend.ascension;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/**
 * 飞升仪式体系 v2
 *
 * 每个仪式需要：
 *  - 击杀/施法/等级条件（降低后的阈值）
 *  - 特定物品消耗（放在主手触发仪式）
 *  - 完成后立即允许选择/更改专精（从觉醒开始）
 *
 * Stage 0 → 1 : 觉醒仪式 — 持有「魔力水晶」×8 + 选择职业
 * Stage 1 → 2 : 磨砺仪式 — 击杀100怪 + 施法50次 + 持有「精炼魔力水晶」×4 + Lv1
 * Stage 2 → 3 : 净化仪式 — 击杀1Boss + 施法200次 + Lv3 + 持有「超越合金」×2
 * Stage 3 → 4 : 超越仪式 — 击杀3Boss + 施法1000次 + Lv6 + 持有「超越核心」×1
 */
public enum AscensionRitual {

    AWAKENING(0,
            "ritual.transcend.awakening",
            "ritual.transcend.awakening.desc",
            ChatFormatting.WHITE,
            0, 0, 0, false,
            () -> com.huige233.transcend.init.ModItems.magic_crystal.get(), 8),

    TEMPERING(1,
            "ritual.transcend.tempering",
            "ritual.transcend.tempering.desc",
            ChatFormatting.YELLOW,
            100, 50, 1, false,
            () -> com.huige233.transcend.init.ModItems.refined_magic_crystal.get(), 4),

    PURIFICATION(2,
            "ritual.transcend.purification",
            "ritual.transcend.purification.desc",
            ChatFormatting.AQUA,
            300, 200, 3, true,
            () -> com.huige233.transcend.init.ModItems.transcend_ingot.get(), 2),

    TRANSCENDENCE(3,
            "ritual.transcend.transcendence",
            "ritual.transcend.transcendence.desc",
            ChatFormatting.GOLD,
            800, 1000, 6, true,
            () -> com.huige233.transcend.init.ModItems.transcendence_core.get(), 1);

    public final int stageIndex;
    public final String nameKey;
    public final String descKey;
    public final ChatFormatting color;
    public final int requiredKills;
    public final int requiredCasts;
    public final int requiredLevel;
    public final boolean requiresBoss;
    /** 触发仪式时从玩家背包消耗的物品（由服务端验证并消耗） */
    public final Supplier<Item> requiredItem;
    public final int requiredItemCount;

    public int requiredBossKills() {
        return switch (this) {
            case PURIFICATION  -> 1;
            case TRANSCENDENCE -> 3;
            default            -> 0;
        };
    }

    AscensionRitual(int stageIndex, String nameKey, String descKey, ChatFormatting color,
                    int requiredKills, int requiredCasts, int requiredLevel, boolean requiresBoss,
                    Supplier<Item> requiredItem, int requiredItemCount) {
        this.stageIndex        = stageIndex;
        this.nameKey           = nameKey;
        this.descKey           = descKey;
        this.color             = color;
        this.requiredKills     = requiredKills;
        this.requiredCasts     = requiredCasts;
        this.requiredLevel     = requiredLevel;
        this.requiresBoss      = requiresBoss;
        this.requiredItem      = requiredItem;
        this.requiredItemCount = requiredItemCount;
    }

    /**
     * 检查玩家数据是否满足该仪式的条件（不含物品检查，物品在 C2SAscensionAction 里处理）
     */
    public boolean isMet(PlayerAscensionData data) {
        if (data.getTotalKills() < requiredKills) return false;
        if (data.getTotalCasts() < requiredCasts) return false;
        if (data.getAscensionLevel() < requiredLevel) return false;
        if (requiresBoss && data.getBossKills() < requiredBossKills()) return false;
        return true;
    }

    /**
     * 从玩家背包中查找并消耗所需物品，返回是否成功
     */
    public boolean consumeItems(Player player) {
        if (player.isCreative()) return true;
        Item needed = requiredItem.get();
        int remaining = requiredItemCount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == needed) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        return remaining <= 0;
    }

    /**
     * 检查玩家背包中是否有足够物品
     */
    public boolean hasItems(Player player) {
        if (player.isCreative()) return true;
        Item needed = requiredItem.get();
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == needed) count += stack.getCount();
        }
        return count >= requiredItemCount;
    }

    public Component getDisplayName() {
        return Component.translatable(nameKey).withStyle(color);
    }

    public Component getDescription() {
        return Component.translatable(descKey).withStyle(ChatFormatting.GRAY);
    }

    public Component getRewardText() {
        return Component.translatable(nameKey + ".reward").withStyle(ChatFormatting.GREEN);
    }

    public static AscensionRitual getByStage(int stageIndex) {
        for (AscensionRitual r : values()) {
            if (r.stageIndex == stageIndex) return r;
        }
        return null;
    }
}
