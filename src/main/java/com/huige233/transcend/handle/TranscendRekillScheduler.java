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

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TranscendRekillScheduler {

    private static final int REKILL_TICKS = 20;

    private static final List<Entry> QUEUE = Collections.synchronizedList(new ArrayList<>());

    private TranscendRekillScheduler() {
    }

    private static final class Entry {
        final Entity entity;
        @Nullable
        final Entity attacker;
        int ticks;

        Entry(Entity entity, @Nullable Entity attacker, int ticks) {
            this.entity = entity;
            this.attacker = attacker;
            this.ticks = ticks;
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
            if (!TranscendForceKillUtil.isPersisting(e.entity)) {
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
