package com.huige233.transcend.tech.combat;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;


/** 在目标的短时命中窗口内统计不同来源的同级弹体，以提供协同护盾穿透加成。 */
public final class CoordinatedPenetration {
    private static final int WINDOW_TICKS = 3;
    private static final int MAX_TARGETS = 4096;
    private static final int MAX_CONTRIBUTORS = 32;
    private static final Map<TargetKey, Window> WINDOWS = new LinkedHashMap<>(16, 0.75F, true);

    private CoordinatedPenetration() {}

    public static int strength(LivingEntity target, Entity source, int individual, int tier, long tick) {
        if (target == null || source == null) return Math.max(0, individual);
        return strength(new TargetKey(target.level().dimension(), target.getUUID()), source.getUUID(), individual, tier, tick);
    }

    static int strength(TargetKey target, UUID source, int individual, int tier, long tick) {
        if (individual <= 0 || tier <= 0) return Math.max(0, individual);
        WINDOWS.entrySet().removeIf(entry -> expired(tick, entry.getValue().lastTick));
        Window window = WINDOWS.computeIfAbsent(target, ignored -> new Window());
        if (WINDOWS.size() > MAX_TARGETS) WINDOWS.remove(WINDOWS.keySet().iterator().next());
        window.lastTick = tick;
        window.contributors.entrySet().removeIf(entry -> expired(tick, entry.getValue().tick));
        window.contributors.put(source, new Shot(tier, tick));
        if (window.contributors.size() > MAX_CONTRIBUTORS) {
            window.contributors.remove(window.contributors.keySet().iterator().next());
        }
        int count = 0;
        for (Shot shot : window.contributors.values()) if (shot.tier == tier) count++;
        return count >= 2 ? Math.max(individual, Math.min(7, tier + 1)) : individual;
    }

    private static boolean expired(long now, long then) {
        return now < then || (double) now - then > WINDOW_TICKS;
    }

    public static void clear(LivingEntity target) {
        if (target != null) WINDOWS.remove(new TargetKey(target.level().dimension(), target.getUUID()));
    }

    static void clearAll() { WINDOWS.clear(); }
    static int trackedTargets() { return WINDOWS.size(); }
    /** 使用维度和目标 UUID 唯一标识协同穿透命中窗口所属的实体。 */
    record TargetKey(ResourceKey<Level> dimension, UUID target) {}
    /** 记录单个协同穿透贡献弹体的等级与命中时刻。 */
    private record Shot(int tier, long tick) {}
    /** 保存目标最近命中时刻及按来源区分的贡献弹体，用于协同穿透计数和过期淘汰。 */
    private static final class Window {
        private long lastTick;
        private final Map<UUID, Shot> contributors = new LinkedHashMap<>();
    }
}
