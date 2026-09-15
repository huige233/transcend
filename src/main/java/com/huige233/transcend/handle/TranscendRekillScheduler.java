package com.huige233.transcend.handle;

import com.huige233.transcend.util.TranscendForceKillUtil;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 在有限存活期限内逐刻重试仍然存留实体的强制击杀，并对重复目标合并调度。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)

public final class TranscendRekillScheduler {

    private static final int REKILL_TICKS = 20;
    private static final int MAX_LIFETIME_TICKS = 20 * 10;

    private static final List<Entry> QUEUE = Collections.synchronizedList(new ArrayList<>());

    private TranscendRekillScheduler() {
    }

    /** 保存重杀目标、攻击者、剩余尝试刻数和任务存活期限。 */
    private static final class Entry {
        final Entity entity;
        @Nullable
        final Entity attacker;
        int ticks;
        int lifetime;

        Entry(Entity entity, @Nullable Entity attacker, int ticks) {
            this.entity = entity;
            this.attacker = attacker;
            this.ticks = ticks;
            this.lifetime = MAX_LIFETIME_TICKS;
        }
    }

    public static void schedule(Entity entity, @Nullable Entity attacker) {
        if (entity == null || entity.level().isClientSide) return;
        synchronized (QUEUE) {
            for (Entry e : QUEUE) {
                if (e.entity == entity) {
                    e.ticks = REKILL_TICKS;
                    return;
                }
            }
            QUEUE.add(new Entry(entity, attacker, REKILL_TICKS));
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) return;

        List<Entry> snapshot;
        synchronized (QUEUE) {
            snapshot = new ArrayList<>(QUEUE);
            QUEUE.clear();
        }

        List<Entry> survivors = new ArrayList<>();
        for (Entry e : snapshot) {
            e.ticks--;
            e.lifetime--;
            if (e.lifetime <= 0 || !TranscendForceKillUtil.isPersisting(e.entity)) {
                continue;
            }
            TranscendForceKillUtil.rekillTick(e.entity, e.attacker);
            if (e.ticks > 0 && TranscendForceKillUtil.isPersisting(e.entity)) {
                survivors.add(e);
            }
        }

        if (!survivors.isEmpty()) {
            synchronized (QUEUE) {
                QUEUE.addAll(survivors);
            }
        }
    }
}
