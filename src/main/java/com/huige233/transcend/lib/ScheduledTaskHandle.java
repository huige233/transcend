package com.huige233.transcend.lib;

import java.util.UUID;

public final class ScheduledTaskHandle {

    private final UUID id;

    ScheduledTaskHandle(UUID id) {
        this.id = id;
    }

    public UUID id() {
        return id;
    }
}
