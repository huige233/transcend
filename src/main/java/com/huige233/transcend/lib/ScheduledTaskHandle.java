package com.huige233.transcend.lib;

import java.util.UUID;

/** 定时任务句柄封装。 */
public final class ScheduledTaskHandle {

    private final UUID id;

    ScheduledTaskHandle(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
