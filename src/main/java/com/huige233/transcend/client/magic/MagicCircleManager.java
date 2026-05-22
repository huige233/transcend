package com.huige233.transcend.client.magic;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mod.EventBusSubscriber
public class MagicCircleManager {

    private static final List<AbstractMagicCircle> ACTIVE_EFFECTS = new ArrayList<>();
    private static final Queue<AbstractMagicCircle> PENDING_EFFECTS = new ConcurrentLinkedQueue<>();

    public static void addEffect(AbstractMagicCircle effect) {
        if (effect != null) {
            PENDING_EFFECTS.offer(effect);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        flushPending();

        Iterator<AbstractMagicCircle> it = ACTIVE_EFFECTS.iterator();
        while (it.hasNext()) {
            AbstractMagicCircle effect = it.next();
            effect.tick();
            if (effect.isRemoved()) {
                it.remove();
            }
        }
        flushPending();
    }

    private static void flushPending() {
        AbstractMagicCircle effect;
        while ((effect = PENDING_EFFECTS.poll()) != null) {
            ACTIVE_EFFECTS.add(effect);
        }
    }
}
