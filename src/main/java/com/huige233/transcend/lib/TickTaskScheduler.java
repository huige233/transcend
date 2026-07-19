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

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TickTaskScheduler {

    private static final Map<UUID, TaskEntry> ACTIVE_TASKS = new ConcurrentHashMap<>();

    private static final ConcurrentLinkedQueue<TaskEntry> PENDING_ADD = new ConcurrentLinkedQueue<>();

    private TickTaskScheduler() {
    }

    public static ScheduledTaskHandle schedule(int delayTicks, Runnable task) {
        UUID id = UUID.randomUUID();
        PENDING_ADD.offer(new TaskEntry(id, Math.max(0, delayTicks), -1, task));
        return new ScheduledTaskHandle(id);
    }

    public static ScheduledTaskHandle scheduleRepeating(int delayTicks, int intervalTicks, Runnable task) {
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("intervalTicks must be >= 1");
        }
        UUID id = UUID.randomUUID();
        PENDING_ADD.offer(new TaskEntry(id, Math.max(0, delayTicks), intervalTicks, task));
        return new ScheduledTaskHandle(id);
    }

    public static boolean cancel(ScheduledTaskHandle handle) {
        if (handle == null) {
            return false;
        }
        return ACTIVE_TASKS.remove(handle.id()) != null;
    }

    public static boolean cancel(UUID id) {
        if (id == null) {
            return false;
        }
        return ACTIVE_TASKS.remove(id) != null;
    }

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
