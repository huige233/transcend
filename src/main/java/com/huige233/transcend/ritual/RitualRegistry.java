package com.huige233.transcend.ritual;

import com.huige233.transcend.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class RitualRegistry {

    private static final List<RitualRecipe> RECIPES = new ArrayList<>();

    public static void init() {
        RECIPES.clear();

        RECIPES.add(RitualRecipe.builder("altar_awakening", RitualType.ASCENSION)
                .name("ritual.transcend.awakening")
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ascension("AWAKENING")
                .build());

        RECIPES.add(RitualRecipe.builder("altar_tempering", RitualType.ASCENSION)
                .name("ritual.transcend.tempering")
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ascension("TEMPERING")
                .build());

        RECIPES.add(RitualRecipe.builder("altar_purification", RitualType.ASCENSION)
                .name("ritual.transcend.purification")
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ascension("PURIFICATION")
                .build());

        RECIPES.add(RitualRecipe.builder("altar_transcendence", RitualType.ASCENSION)
                .name("ritual.transcend.transcendence")
                .ingredient(Ingredient.of(ModItems.transcendence_core.get()))
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .ascension("TRANSCENDENCE")
                .build());

        RECIPES.add(RitualRecipe.builder("craft_respec_potion", RitualType.CRAFT)
                .name("ritual.transcend.craft_respec")
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .result(new ItemStack(ModItems.respec_potion.get()))
                .build());

        RECIPES.add(RitualRecipe.builder("battle_blessing", RitualType.BUFF)
                .name("ritual.transcend.battle_blessing")
                .ingredient(Ingredient.of(ModItems.magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.enhance_power.get()))
                .ingredient(Ingredient.of(ModItems.enhance_duration.get()))
                .ingredient(Ingredient.of(ModItems.enhance_efficiency.get()))
                .buff(new ResourceLocation("minecraft", "strength"), 6000, 1)
                .buff(new ResourceLocation("minecraft", "resistance"), 6000, 0)
                .buff(new ResourceLocation("minecraft", "speed"), 6000, 0)
                .buff(new ResourceLocation("minecraft", "regeneration"), 3000, 0)
                .build());

        RECIPES.add(RitualRecipe.builder("arena_invocation", RitualType.SUMMON)
                .name("ritual.transcend.arena_invocation")
                .ingredient(Ingredient.of(ModItems.transcendence_core.get()))
                .ingredient(Ingredient.of(ModItems.rift_fragment.get()))
                .ingredient(Ingredient.of(ModItems.magic_circle_void.get()))
                .ingredient(Ingredient.of(ModItems.enhance_special.get()))
                .build());

        RECIPES.add(RitualRecipe.builder("nexus_gateway", RitualType.SUMMON)
                .name("ritual.transcend.nexus_gateway")
                .ingredient(Ingredient.of(ModItems.transcendence_core.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.rift_fragment.get()))
                .build());

        // ─── Round 49: 8-pedestal apparatus 仪式（高阶 — Ars Nouveau 风）───

        // 1. Transcendence Proof 仪式合成（替代普通工作台合成）
        RECIPES.add(RitualRecipe.builder("apparatus_transcendence_proof", RitualType.CRAFT)
                .name("ritual.transcend.apparatus_transcendence_proof")
                .ingredient(Ingredient.of(ModItems.warden_essence.get()))
                .ingredient(Ingredient.of(ModItems.weaver_essence.get()))
                .ingredient(Ingredient.of(ModItems.avatar_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_ingot.get()))
                .ingredient(Ingredient.of(ModItems.transcendence_core.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .ingredient(Ingredient.of(ModItems.transcend_ingot.get()))
                .result(new ItemStack(ModItems.transcendence_proof.get()))
                .build());

        // 2. Aether Ingot 高效仪式（8 essence 一次得 2 锭，对比熔炉 9 essence 1 锭）
        RECIPES.add(RitualRecipe.builder("apparatus_aether_ingot", RitualType.CRAFT)
                .name("ritual.transcend.apparatus_aether_ingot")
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .ingredient(Ingredient.of(ModItems.aether_essence.get()))
                .result(new ItemStack(ModItems.aether_ingot.get(), 2))
                .build());

        // 3. 终焉祝福（8-pedestal BUFF — 强化 + 速度 + 抗性 II + 再生 II 共 10 分钟）
        RECIPES.add(RitualRecipe.builder("apparatus_endgame_blessing", RitualType.BUFF)
                .name("ritual.transcend.apparatus_endgame_blessing")
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.refined_magic_crystal.get()))
                .ingredient(Ingredient.of(ModItems.enhance_power.get()))
                .ingredient(Ingredient.of(ModItems.enhance_duration.get()))
                .ingredient(Ingredient.of(ModItems.enhance_efficiency.get()))
                .ingredient(Ingredient.of(ModItems.enhance_special.get()))
                .ingredient(Ingredient.of(ModItems.aether_ingot.get()))
                .ingredient(Ingredient.of(ModItems.transcendence_core.get()))
                .buff(new ResourceLocation("minecraft", "strength"), 12000, 1)
                .buff(new ResourceLocation("minecraft", "resistance"), 12000, 1)
                .buff(new ResourceLocation("minecraft", "speed"), 12000, 1)
                .buff(new ResourceLocation("minecraft", "regeneration"), 12000, 1)
                .buff(new ResourceLocation("minecraft", "fire_resistance"), 12000, 0)
                .build());

        // Round 49: 排序使 8-input recipes 优先匹配（防止 4-input 部分匹配 8 物品输入）
        RECIPES.sort((a, b) -> Integer.compare(b.getIngredients().size(), a.getIngredients().size()));
    }

    public static List<RitualRecipe> getRecipes() {
        return RECIPES;
    }

    public static RitualRecipe findMatch(List<ItemStack> pedestalItems) {
        for (RitualRecipe recipe : RECIPES) {
            if (recipe.matches(pedestalItems)) return recipe;
        }
        return null;
    }
}
