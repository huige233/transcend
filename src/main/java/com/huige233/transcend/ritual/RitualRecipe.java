package com.huige233.transcend.ritual;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class RitualRecipe {

    private final String id;
    private final String nameKey;
    private final RitualType type;
    private final List<Ingredient> ingredients;
    private final ItemStack craftResult;
    private final String ascensionRitualName;
    private final List<BuffEntry> buffs;

    private RitualRecipe(String id, String nameKey, RitualType type,
                         List<Ingredient> ingredients, ItemStack craftResult,
                         String ascensionRitualName, List<BuffEntry> buffs) {
        this.id = id;
        this.nameKey = nameKey;
        this.type = type;
        this.ingredients = List.copyOf(ingredients);
        this.craftResult = craftResult;
        this.ascensionRitualName = ascensionRitualName;
        this.buffs = buffs != null ? List.copyOf(buffs) : List.of();
    }

    public String getId() { return id; }
    public String getNameKey() { return nameKey; }
    public RitualType getType() { return type; }
    public List<Ingredient> getIngredients() { return ingredients; }
    public ItemStack getCraftResult() { return craftResult; }
    public String getAscensionRitualName() { return ascensionRitualName; }
    public List<BuffEntry> getBuffs() { return buffs; }

    public boolean matches(List<ItemStack> pedestalItems) {
        if (pedestalItems.size() != ingredients.size()) return false;
        List<ItemStack> remaining = new ArrayList<>(pedestalItems);
        for (Ingredient ing : ingredients) {
            boolean found = false;
            for (int i = 0; i < remaining.size(); i++) {
                if (ing.test(remaining.get(i))) {
                    remaining.remove(i);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public record BuffEntry(net.minecraft.resources.ResourceLocation effect, int duration, int amplifier) {}

    public static Builder builder(String id, RitualType type) {
        return new Builder(id, type);
    }

    public static class Builder {
        private final String id;
        private final RitualType type;
        private String nameKey;
        private final List<Ingredient> ingredients = new ArrayList<>();
        private ItemStack craftResult = ItemStack.EMPTY;
        private String ascensionRitualName;
        private List<BuffEntry> buffs;

        Builder(String id, RitualType type) {
            this.id = id;
            this.type = type;
            this.nameKey = "ritual.transcend." + id;
        }

        public Builder name(String key) { this.nameKey = key; return this; }
        public Builder ingredient(Ingredient ing) { ingredients.add(ing); return this; }
        public Builder result(ItemStack stack) { this.craftResult = stack; return this; }
        public Builder ascension(String ritualName) { this.ascensionRitualName = ritualName; return this; }
        public Builder buff(net.minecraft.resources.ResourceLocation effect, int duration, int amplifier) {
            if (buffs == null) buffs = new ArrayList<>();
            buffs.add(new BuffEntry(effect, duration, amplifier));
            return this;
        }

        public RitualRecipe build() {
            return new RitualRecipe(id, nameKey, type, ingredients, craftResult, ascensionRitualName, buffs);
        }
    }
}
