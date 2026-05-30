package com.huige233.transcend.block.data;

import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Mana Blossom 转化表注册中心（单例）。由 {@link BlossomTransformLoader} 在 reload 时填充。
 *
 * <p>注册表按 {@code input block → BlossomTransform} 索引，
 * BlockEntity 查询时仅需 O(1) lookup。
 */
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

    /** 查询：未注册时返回 null。 */
    @Nullable
    public BlossomTransform get(Block input) {
        return byInput.get(input);
    }

    public int size() {
        return byInput.size();
    }
}
