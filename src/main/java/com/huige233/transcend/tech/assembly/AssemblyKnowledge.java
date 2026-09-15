package com.huige233.transcend.tech.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;


/** 管理玩家逐级学习的机械知识，并结合工艺熟练度判断制造等级资格。 */
public final class AssemblyKnowledge {
    public static final int MAX_TIER = 7;
    private static final String TIER = "KnowledgeTier";

    private AssemblyKnowledge() {}

    public static int tier(Player player) {
        return normalize(player.getPersistentData().getCompound(AssemblyProficiency.KEY).getInt(TIER));
    }

    public static int normalize(int tier) { return Math.max(0, Math.min(MAX_TIER, tier)); }

    public static boolean canLearn(int current, int requested) {
        return requested >= 1 && requested <= MAX_TIER && requested == normalize(current) + 1;
    }

    public static int requiredProficiency(int tier) {
        return Math.max(0, Math.min(MAX_TIER, tier) - 1) * 15;
    }

    public static boolean canManufacture(int knowledge, int proficiency, int tier) {
        return tier >= 1 && tier <= MAX_TIER && normalize(knowledge) >= tier
                && proficiency >= requiredProficiency(tier);
    }

    public static boolean learn(Player player, int tier) {
        if (player.level().isClientSide || !canLearn(tier(player), tier)) return false;
        CompoundTag data = player.getPersistentData().getCompound(AssemblyProficiency.KEY);
        data.putInt(TIER, tier);
        player.getPersistentData().put(AssemblyProficiency.KEY, data);
        return true;
    }
}
