package com.huige233.transcend.tech.assembly;

import com.huige233.transcend.block.AssemblyBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/** 统一校验制造条件和配方签名，结算材料、成功耗能、失败回收及制造经验。 */
public final class AssemblyOperations {
    private AssemblyOperations() {}

    public static String signature(AssemblyRecipe recipe) {
        return recipe.process() + "|" + recipe.first().toJson() + "|" + recipe.firstCount()
                + "|" + recipe.second().toJson() + "|" + recipe.secondCount()
                + "|" + recipe.output().save(new CompoundTag()) + "|" + recipe.energy()
                + "|" + recipe.tier() + "|" + recipe.failure() + "|"
                + recipe.baseMaterials().stream().map(stack -> stack.save(new CompoundTag()).toString()).toList();
    }

    public static boolean canOutput(AssemblyBlockEntity bench, ItemStack result) {
        ItemStack existing = bench.items.getStackInSlot(4);
        return existing.isEmpty() ? result.getCount() <= result.getMaxStackSize()
                : ItemStack.isSameItemSameTags(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    public static int readiness(AssemblyBlockEntity bench, Player player, AssemblyRecipe recipe) {
        if (!AssemblyKnowledge.canManufacture(AssemblyKnowledge.tier(player),
                AssemblyRules.level(AssemblyProficiency.xp(player, recipe.process())), recipe.tier())) return 3;
        if (!recipe.matches(new SimpleContainer(bench.items.getStackInSlot(2), bench.items.getStackInSlot(3)), player.level())) return 4;
        if (bench.energy() < recipe.energy()) return 5;
        if (!canOutput(bench, recipe.leveledOutput(recipe.recipeLevel(player.level())))) return 6;
        return 1;
    }

    public static boolean complete(AssemblyBlockEntity bench, Player player, AssemblyRecipe recipe) {
        if (player.level().isClientSide || readiness(bench, player, recipe) != 1) return false;
        ItemStack result = recipe.leveledOutput(recipe.recipeLevel(player.level()));
        double failure = AssemblyRules.failureChance(recipe.failure(), AssemblyProficiency.xp(player, recipe.process()));
        boolean success = player.getRandom().nextDouble() >= failure;
        bench.items.extractItem(2, recipe.firstCount(), false);
        bench.items.extractItem(3, recipe.secondCount(), false);
        if (success) {
            bench.consumeEnergy(recipe.energy());
            result.grow(bench.items.getStackInSlot(4).getCount());
            bench.items.setStackInSlot(4, result);
            AssemblyRecipeData data = player.level().getServer().overworld().getDataStorage().computeIfAbsent(
                    AssemblyRecipeData::load, AssemblyRecipeData::new, AssemblyRecipeData.dataName());
            data.increase(recipe.getId(), 1);
            if (!recipe.process().equals("recycle")) AssemblyProficiency.award(player, recipe.process(), recipe.tier());
        } else {
            
            for (ItemStack base : recipe.baseMaterials()) {
                int count = AssemblyRules.salvage(base.getCount());
                if (count == 0) continue;
                ItemStack returned = base.copyWithCount(count);
                if (!player.getInventory().add(returned)) player.drop(returned, false);
            }
        }
        player.displayClientMessage(Component.translatable(success ? "assembly.transcend.success" : "assembly.transcend.failed"), true);
        bench.setChanged();
        return true;
    }
}
