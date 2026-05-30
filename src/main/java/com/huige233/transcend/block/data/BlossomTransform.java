package com.huige233.transcend.block.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Mana Blossom 转化条目（数据驱动）。
 *
 * <p>JSON 格式（{@code data/transcend/blossom_transforms/*.json}）：
 * <pre>
 * {
 *   "input": "minecraft:cobblestone",
 *   "output": "transcend:runed_stone_bricks",
 *   "mana_cost": 50
 * }
 * </pre>
 *
 * @param id        ResourceLocation 标识符（来自 JSON 文件路径）
 * @param input     输入方块（被替换的相邻方块）
 * @param output    输出方块（替换后的魔化版本）
 * @param manaCost  从相邻 mana 容器扣除的 CM 数量
 */
public record BlossomTransform(ResourceLocation id, Block input, Block output, int manaCost) {

    /**
     * 从 JSON 解析；未知方块名 / 缺失字段 / 非法 mana_cost 都抛 IllegalArgumentException。
     */
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
