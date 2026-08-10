package com.huige233.transcend.ascension;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

/** 飞升仪式种类枚举。 */
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

    public boolean isMet(PlayerAscensionData data) {
        if (data.getTotalKills() < requiredKills) return false;
        if (data.getTotalCasts() < requiredCasts) return false;
        if (data.getInsightLevel() < requiredLevel) return false;
        if (requiresBoss && data.getBossKills() < requiredBossKills()) return false;
        return true;
    }

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
