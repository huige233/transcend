package com.huige233.transcend.lib;

import com.huige233.transcend.Transcend;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 通用服务端 Tick 任务调度器。
 * <p>
 * 参考 ending_library 的任务队列思路，提供：
 * 1) 延迟执行任务
 * 2) 重复执行任务
 * 3) 任务取消能力
 * <p>
 * 该类挂在 Forge 事件总线上，在服务端每 Tick 自动推进。
 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TickTaskScheduler {

    /**
     * 主任务表。Key 为任务 ID，Value 为任务实例。
     */
    private static final Map<UUID, TaskEntry> ACTIVE_TASKS = new ConcurrentHashMap<>();

    /**
     * 待加入队列。避免在遍历 ACTIVE_TASKS 时直接写入引发并发问题。
     */
    private static final ConcurrentLinkedQueue<TaskEntry> PENDING_ADD = new ConcurrentLinkedQueue<>();

    private TickTaskScheduler() {
    }

    /**
     * 添加一次性延迟任务。
     *
     * @param delayTicks 延迟 Tick，0 表示下一次推进时立即执行
     * @param task       任务逻辑
     * @return 可用于取消任务的句柄
     */
    public static ScheduledTaskHandle schedule(int delayTicks, Runnable task) {
        UUID id = UUID.randomUUID();
        PENDING_ADD.offer(new TaskEntry(id, Math.max(0, delayTicks), -1, task));
        return new ScheduledTaskHandle(id);
    }

    /**
     * 添加重复任务。
     *
     * @param delayTicks     首次延迟 Tick
     * @param intervalTicks  重复间隔 Tick，必须 >= 1
     * @param task           任务逻辑
     * @return 可用于取消任务的句柄
     */
    public static ScheduledTaskHandle scheduleRepeating(int delayTicks, int intervalTicks, Runnable task) {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("intervalTicks must be >= 1");
        }
        UUID id = UUID.randomUUID();
        PENDING_ADD.offer(new TaskEntry(id, Math.max(0, delayTicks), intervalTicks, task));
        return new ScheduledTaskHandle(id);
    }

    /**
     * 取消任务。
     *
     * @param handle 任务句柄
     * @return 是否成功取消（任务存在即返回 true）
     */
    public static boolean cancel(ScheduledTaskHandle handle) {
        if (handle == null) {
            return false;
        }
        return ACTIVE_TASKS.remove(handle.id()) != null;
    }

    /**
     * 取消任务。
     *
     * @param id 任务 UUID
     * @return 是否成功取消
     */
    public static boolean cancel(UUID id) {
        if (id == null) {
            return false;
        }
        return ACTIVE_TASKS.remove(id) != null;
    }

    /**
     * 当前活跃任务数量。
     */
    public static int size() {
        return ACTIVE_TASKS.size();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }

        drainPending();

        Iterator<TaskEntry> iterator = ACTIVE_TASKS.values().iterator();
        while (iterator.hasNext()) {
            TaskEntry entry = iterator.next();
            if (entry.removed) {
                iterator.remove();
                continue;
            }

            if (entry.remainingTicks > 0) {
                entry.remainingTicks--;
                continue;
            }

            try {
                entry.task.run();
            } catch (Exception ex) {
                Transcend.LOGGER.error("TickTaskScheduler task failed: {}", entry.id, ex);
            }

            if (entry.intervalTicks > 0) {
                entry.remainingTicks = entry.intervalTicks;
            } else {
                entry.removed = true;
                iterator.remove();
            }
        }
    }

    private static void drainPending() {
        TaskEntry entry;
        while ((entry = PENDING_ADD.poll()) != null) {
            ACTIVE_TASKS.put(entry.id, entry);
        }
    }

    /**
     * 任务内部结构。
     */
    private static final class TaskEntry {
        private final UUID id;
        private int remainingTicks;
        private final int intervalTicks;
        private final Runnable task;
        private boolean removed;

        private TaskEntry(UUID id, int remainingTicks, int intervalTicks, Runnable task) {
            this.id = id;
            this.remainingTicks = remainingTicks;
            this.intervalTicks = intervalTicks;
            this.task = task;
            this.removed = false;
        }
    }
}
