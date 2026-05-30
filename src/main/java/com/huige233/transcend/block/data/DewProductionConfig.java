package com.huige233.transcend.block.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

/**
 * Mana Dew 产能配置（数据驱动）。
 *
 * <p>JSON 格式（{@code data/transcend/dew_production/<id>.json}）：
 * <pre>
 * {
 *   "produce_interval": 20,
 *   "produce_amount": 1,
 *   "inject_radius": 4,
 *   "required_fluid_tag": "minecraft:lava"
 * }
 * </pre>
 *
 * <p>当前实现仅使用 id = {@code transcend:default} 的配置，
 * 未来可扩展为按方块变体使用不同 id（如 water dew vs lava dew）。
 *
 * @param id                ResourceLocation 标识符
 * @param produceInterval   产能 tick 间隔（ticks）
 * @param produceAmount     单次产 mana 量（CM）
 * @param injectRadius      注入半径（球，方块）
 * @param requiredFluidTag  方块下方需要存在的流体 tag
 */
public record DewProductionConfig(ResourceLocation id, int produceInterval, int produceAmount,
                                   int injectRadius, TagKey<Fluid> requiredFluidTag) {

    /** 默认配置 id（ManaDewBlockEntity 查询此条目）。 */
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation("transcend", "default");

    /** 硬代码兜底默认值（用于 JSON 缺失时）。 */
    public static DewProductionConfig hardDefault() {
        return new DewProductionConfig(DEFAULT_ID, 20, 1, 4, FluidTags.LAVA);
    }

    public static DewProductionConfig fromJson(ResourceLocation id, JsonObject json) {
        int interval = json.has("produce_interval") ? json.get("produce_interval").getAsInt() : 20;
        int amount = json.has("produce_amount") ? json.get("produce_amount").getAsInt() : 1;
        int radius = json.has("inject_radius") ? json.get("inject_radius").getAsInt() : 4;

        if (interval <= 0) throw new IllegalArgumentException("produce_interval must be > 0");
        if (amount < 0)    throw new IllegalArgumentException("produce_amount must be >= 0");
        if (radius < 0 || radius > 16) {
            throw new IllegalArgumentException("inject_radius must be in [0, 16]");
        }

        TagKey<Fluid> tag = FluidTags.LAVA;
        if (json.has("required_fluid_tag")) {
            String tagStr = json.get("required_fluid_tag").getAsString();
            tag = TagKey.create(net.minecraft.core.registries.Registries.FLUID,
                    new ResourceLocation(tagStr));
        }
        return new DewProductionConfig(id, interval, amount, radius, tag);
    }
}
