package com.huige233.transcend.lib.effect;

import com.huige233.transcend.lib.ScheduledTaskHandle;
import com.huige233.transcend.lib.TickTaskScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

/**
 * Shader 特效任务编排器。
 * <p>
 * 用于将“按 Tick 重复触发的特效逻辑”集中管理，避免每个实体都手写计时器字段。
 * 其底层依赖 {@link TickTaskScheduler}，通过任务句柄统一创建/取消。
 */
public final class ShaderTaskOrchestrator {

    private static final Map<UUID, ScheduledTaskHandle> RUNNING_TASKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PULSE_COUNTERS = new ConcurrentHashMap<>();

    private ShaderTaskOrchestrator() {
    }

    /**
     * 启动（或保持）一个重复任务。
     * <p>
     * 如果 key 已经存在任务，则不会重复创建。
     *
     * @param key          任务唯一键（推荐使用实体 UUID）
     * @param firstDelay   首次延迟 Tick
     * @param intervalTick 重复间隔 Tick（>= 1）
     * @param action       执行动作，参数为当前第几次触发（从 0 开始）
     */
    public static void ensureRepeating(UUID key, int firstDelay, int intervalTick, IntConsumer action) {
        if (key == null || action == null || intervalTick < 1) {
            return;
        }
        if (RUNNING_TASKS.containsKey(key)) {
            return;
        }
        ScheduledTaskHandle handle = TickTaskScheduler.scheduleRepeating(firstDelay, intervalTick, () -> {
            int pulse = PULSE_COUNTERS.getOrDefault(key, 0);
            action.accept(pulse);
            PULSE_COUNTERS.put(key, pulse + 1);
        });
        RUNNING_TASKS.put(key, handle);
        PULSE_COUNTERS.putIfAbsent(key, 0);
    }

    /**
     * 停止并清理指定任务。
     */
    public static void stop(UUID key) {
        if (key == null) {
            return;
        }
        ScheduledTaskHandle handle = RUNNING_TASKS.remove(key);
        if (handle != null) {
            TickTaskScheduler.cancel(handle);
        }
        PULSE_COUNTERS.remove(key);
    }

    /**
     * 判断指定 key 是否存在活跃任务。
     */
    public static boolean isRunning(UUID key) {
        return key != null && RUNNING_TASKS.containsKey(key);
    }
}
