package com.huige233.transcend.block.data;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** 魔力花转化配方注册表。 */
public class BlossomTransformRegistry {

    private static final BlossomTransformRegistry INSTANCE = new BlossomTransformRegistry();

    private final Map<Block, BlossomTransform> byInput = new HashMap<>();

    private BlossomTransformRegistry() {}

    public static BlossomTransformRegistry getInstance() {
        return INSTANCE;
    }

    public void clear() {
        byInput.clear();
    }

    public void register(BlossomTransform transform) {
        byInput.put(transform.input(), transform);
    }

    @Nullable
    public BlossomTransform get(Block input) {
        return byInput.get(input);
    }

    public int size() {
        return byInput.size();
    }
}
