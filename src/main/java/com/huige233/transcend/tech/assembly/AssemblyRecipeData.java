package com.huige233.transcend.tech.assembly;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;


/** 持久化服务器共享的装配配方专精等级，并在成功制造后提升对应等级。 */
public final class AssemblyRecipeData extends SavedData {
    private static final String DATA_NAME = "transcend_assembly_recipes";
    private static final String LEVELS = "Levels";
    private final CompoundTag levels;

    private AssemblyRecipeData(CompoundTag levels) {
        this.levels = levels;
    }

    public AssemblyRecipeData() {
        this(new CompoundTag());
    }

    public static AssemblyRecipeData load(CompoundTag tag) {
        return new AssemblyRecipeData(tag.getCompound(LEVELS).copy());
    }

    public int level(ResourceLocation recipeId) {
        return Math.max(1, levels.getInt(recipeId.toString()));
    }

    public int increase(ResourceLocation recipeId, int amount) {
        if (amount <= 0) return level(recipeId);
        int next = Math.max(1, Math.min(1000, level(recipeId) + amount));
        levels.putInt(recipeId.toString(), next);
        setDirty();
        return next;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put(LEVELS, levels.copy());
        return tag;
    }

    public static String dataName() {
        return DATA_NAME;
    }
}
