package com.huige233.transcend.lib.effect;

import com.huige233.transcend.lib.ScheduledTaskHandle;
import com.huige233.transcend.lib.TickTaskScheduler;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntConsumer;

public final class ShaderTaskOrchestrator {

    private static final Map<UUID, ScheduledTaskHandle> RUNNING_TASKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PULSE_COUNTERS = new ConcurrentHashMap<>();

    private ShaderTaskOrchestrator() {
    }

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

    public static boolean isRunning(UUID key) {
        return key != null && RUNNING_TASKS.containsKey(key);
    }
}
