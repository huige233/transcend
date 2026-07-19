package com.huige233.transcend.block.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public record DewProductionConfig(ResourceLocation id, int produceInterval, int produceAmount,
                                   int injectRadius, TagKey<Fluid> requiredFluidTag) {

    public static final ResourceLocation DEFAULT_ID = new ResourceLocation("transcend", "default");

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
