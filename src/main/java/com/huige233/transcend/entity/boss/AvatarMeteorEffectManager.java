package com.huige233.transcend.entity.boss;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
/** 分身陨石特效管理器。 */
public final class AvatarMeteorEffectManager {
    private static final List<AvatarMeteorEffect> ACTIVE_EFFECTS = new ArrayList<>();
    private static final Queue<AvatarMeteorEffect> PENDING_EFFECTS = new ConcurrentLinkedQueue<>();

    private AvatarMeteorEffectManager() {}

    static void add(AvatarMeteorEffect effect) {
        if (effect != null) PENDING_EFFECTS.offer(effect);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        flushPending();
        Iterator<AvatarMeteorEffect> iterator = ACTIVE_EFFECTS.iterator();
        while (iterator.hasNext()) {
            AvatarMeteorEffect effect = iterator.next();
            effect.tick();
            if (effect.isRemoved()) iterator.remove();
        }
        flushPending();
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_EFFECTS.clear();
        PENDING_EFFECTS.clear();
    }

    private static void flushPending() {
        AvatarMeteorEffect effect;
        while ((effect = PENDING_EFFECTS.poll()) != null) ACTIVE_EFFECTS.add(effect);
    }
}
