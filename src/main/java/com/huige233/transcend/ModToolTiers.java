package com.huige233.transcend;
import com.huige233.transcend.init.ModItems;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public class ModToolTiers {
    public static final Tier NORMAL = TierSortingRegistry.registerTier(new ForgeTier(7777, 7777, 25f, 25F, 77, ModTags.NEEDS_NORMAL_TOOL,
            () -> Ingredient.of(ModItems.normal_ingot.get())), Transcend.rl("normal"), List.of(Tiers.DIAMOND), List.of());
    public static final Tier EPICC = TierSortingRegistry.registerTier(new ForgeTier(8888, 8888, 50f, 50F, 888, ModTags.NEEDS_EPICC_TOOL,
            () -> Ingredient.of(ModItems.epic_ingot.get())), Transcend.rl("epic"), List.of(Tiers.NETHERITE, NORMAL), List.of());
    public static final Tier TRANSCEND = TierSortingRegistry.registerTier(new ForgeTier(9999, 9999, 100f, 100F, 9999, ModTags.NEEDS_TRANSCEND_TOOL,
            () -> Ingredient.of(ModItems.transcend_ingot.get())), Transcend.rl("transcend"), List.of(ModToolTiers.EPICC), List.of());
}
