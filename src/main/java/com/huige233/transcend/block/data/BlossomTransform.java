package com.huige233.transcend.block.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/** 魔力花转化配方记录。 */
public record BlossomTransform(ResourceLocation id, Block input, Block output, int manaCost) {

    public static BlossomTransform fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("input") || !json.has("output") || !json.has("mana_cost")) {
            throw new IllegalArgumentException("Missing required field (input/output/mana_cost)");
        }
        ResourceLocation inputId = new ResourceLocation(json.get("input").getAsString());
        ResourceLocation outputId = new ResourceLocation(json.get("output").getAsString());
        int cost = json.get("mana_cost").getAsInt();
        if (cost < 0) throw new IllegalArgumentException("mana_cost must be >= 0");

        Block input = BuiltInRegistries.BLOCK.get(inputId);
        Block output = BuiltInRegistries.BLOCK.get(outputId);
        if (input == null || input == net.minecraft.world.level.block.Blocks.AIR) {
            throw new IllegalArgumentException("Unknown input block: " + inputId);
        }
        if (output == null || output == net.minecraft.world.level.block.Blocks.AIR) {
            throw new IllegalArgumentException("Unknown output block: " + outputId);
        }
        return new BlossomTransform(id, input, output, cost);
    }
}
