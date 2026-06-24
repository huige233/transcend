package com.huige233.transcend.agent;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在 OmniMobs attach 完 agent 之后,把 {@link TranscendOmniPatch} 注册上去(必须比 omni 晚 → 最外层)。
 * 服务器开始时装一次;再用前几个 tick 兜底重试,确保 omni 的 instrumentation 已就绪。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TranscendAgentBootstrap {

    private static int retries = 0;

    private TranscendAgentBootstrap() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        TranscendOmniPatch.tryInstall();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || retries >= 40) return;
        retries++;
        TranscendOmniPatch.tryInstall();
    }
}
