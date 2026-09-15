package com.huige233.transcend.tech.assembly;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.CraftingHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;


/** 定义双材料工艺配方的产物、耗能、等级、失败率和回收材料，并生成带配方等级的产物。 */
public record AssemblyRecipe(ResourceLocation id, String process, Ingredient first, int firstCount,
                             Ingredient second, int secondCount, ItemStack output, int energy,
                             int tier, double failure, List<ItemStack> baseMaterials) implements Recipe<Container> {
    public static final List<String> PROCESSES = List.of("press", "grind", "cast", "fabricate", "recycle");
    public static final String TAG_RECIPE_LEVEL = "AssemblyRecipeLevel";

    public AssemblyRecipe {
        if (!PROCESSES.contains(process) || firstCount < 1 || firstCount > 64 || secondCount < 0
                || secondCount > 64 || energy < 0 || energy > 50_000_000 || tier < 1 || tier > 7
                || !Double.isFinite(failure) || failure < 0 || failure > 1 || first.isEmpty()
                || (secondCount > 0 && second.isEmpty()) || output.isEmpty()
                || output.getCount() > output.getMaxStackSize() || baseMaterials.size() > 16) {
            throw new IllegalArgumentException("Invalid assembly recipe: " + id);
        }
        output = output.copy();
        baseMaterials = baseMaterials.stream().map(ItemStack::copy).toList();
    }

    public int recipeLevel(Level level) {
        if (level == null || level.getServer() == null) return 1;
        AssemblyRecipeData data = level.getServer().overworld().getDataStorage().computeIfAbsent(
                AssemblyRecipeData::load, AssemblyRecipeData::new, AssemblyRecipeData.dataName());
        return data.level(id);
    }

    public int recipeBonus(int recipeLevel) {
        return Math.max(0, Math.min(1000, Math.max(1, recipeLevel) - 1));
    }

    public ItemStack leveledOutput(int recipeLevel) {
        ItemStack result = output.copy();
        int boundedLevel = Math.max(1, Math.min(1000, recipeLevel));
        result.getOrCreateTag().putInt(TAG_RECIPE_LEVEL, boundedLevel);
        result.getOrCreateTag().putInt("AssemblyRecipeBonus", recipeBonus(boundedLevel));
        return result;
    }

    @Override
    public boolean matches(Container input, Level level) {
        return first.test(input.getItem(0)) && input.getItem(0).getCount() >= firstCount
                && (secondCount == 0 ? input.getItem(1).isEmpty()
                : second.test(input.getItem(1)) && input.getItem(1).getCount() >= secondCount);
    }
    @Override public ItemStack assemble(Container input, RegistryAccess access) { return output.copy(); }
    @Override public ItemStack getResultItem(RegistryAccess access) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return AssemblyRecipeRegistration.SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return AssemblyRecipeRegistration.TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    /** 负责装配配方的 JSON 解析和网络编解码，保留产物 NBT 与失败回收材料清单。 */
    public static final class Serializer implements RecipeSerializer<AssemblyRecipe> {
        @Override public AssemblyRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonObject a = GsonHelper.getAsJsonObject(json, "first");
            JsonObject b = GsonHelper.getAsJsonObject(json, "second", null);
            List<ItemStack> bases = new ArrayList<>();
            JsonArray array = GsonHelper.getAsJsonArray(json, "base_materials", new JsonArray());
            array.forEach(element -> bases.add(CraftingHelper.getItemStack(element.getAsJsonObject(), true)));
            return new AssemblyRecipe(id, GsonHelper.getAsString(json, "process"),
                    Ingredient.fromJson(a.get("ingredient")), GsonHelper.getAsInt(a, "count", 1),
                    b == null ? Ingredient.EMPTY : Ingredient.fromJson(b.get("ingredient")),
                    b == null ? 0 : GsonHelper.getAsInt(b, "count", 1),
                    CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true),
                    GsonHelper.getAsInt(json, "energy"), GsonHelper.getAsInt(json, "tier", 1),
                    GsonHelper.getAsDouble(json, "failure", 0), bases);
        }
        @Override public AssemblyRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String process = buf.readUtf();
            Ingredient first = Ingredient.fromNetwork(buf);
            int count = buf.readVarInt();
            Ingredient second = Ingredient.fromNetwork(buf);
            int secondCount = buf.readVarInt();
            ItemStack result = buf.readItem();
            int energy = buf.readVarInt();
            int tier = buf.readVarInt();
            double failure = buf.readDouble();
            int size = buf.readVarInt();
            if (size < 0 || size > 16) throw new IllegalArgumentException("Too many assembly salvage entries");
            List<ItemStack> bases = new ArrayList<>();
            for (int i = 0; i < size; i++) bases.add(buf.readItem());
            return new AssemblyRecipe(id, process, first, count, second, secondCount, result, energy, tier, failure, bases);
        }
        @Override public void toNetwork(FriendlyByteBuf buf, AssemblyRecipe recipe) {
            buf.writeUtf(recipe.process);
            recipe.first.toNetwork(buf);
            buf.writeVarInt(recipe.firstCount);
            recipe.second.toNetwork(buf);
            buf.writeVarInt(recipe.secondCount);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.energy);
            buf.writeVarInt(recipe.tier);
            buf.writeDouble(recipe.failure);
            buf.writeVarInt(recipe.baseMaterials.size());
            recipe.baseMaterials.forEach(buf::writeItem);
        }
    }
}
