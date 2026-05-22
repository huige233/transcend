package com.huige233.transcend.lib;

import java.util.UUID;

/**
 * 任务句柄。
 * <p>
 * 用于持有调度任务的唯一标识，外部可通过该句柄取消任务。
 */
public final class ScheduledTaskHandle {

    private final UUID id;

    ScheduledTaskHandle(UUID id) {
        this.id = id;
    }

    /**
     * 获取任务唯一 ID。
     */
    public UUID id() {
        return id;
    }
}
