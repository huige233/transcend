package com.mega.uom.client.bossevent;

import com.mega.uom.common.entity.boss.uom.UomWither;
import com.mega.uom.mixin.BossEventAccessor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;

import java.util.UUID;

public class UUIDServerBossEvent extends ServerBossEvent {
    public UomWither boss;
    public UUID bossId;

    public UUIDServerBossEvent(Component p_8300_, BossBarColor p_8301_, BossBarOverlay p_8302_, UUID uuid) {
        super(p_8300_, p_8301_, p_8302_);
        this.setBossId(uuid);
    }

    public UUIDServerBossEvent setBoss(UomWither boss) {
        this.boss = boss;
        return this;
    }

    public UUIDServerBossEvent setBossId(UUID bossId) {
        this.bossId = bossId;
        ((BossEventAccessor) this).setUUID(bossId);
        return this;
    }
}
