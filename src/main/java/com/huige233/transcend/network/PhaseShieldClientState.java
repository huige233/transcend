package com.huige233.transcend.network;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.tech.shield.PhaseShieldStatus;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/** 按玩家标识缓存相位护盾显示状态与电量比例，并在退出或卸载世界时清空。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT)
public final class PhaseShieldClientState {
    private static final Map<UUID, Float> CHARGES = new HashMap<>();

    private static final Map<UUID, PhaseShieldStatus> STATES = new HashMap<>();


    public static PhaseShieldStatus status(UUID playerId) {
        return STATES.getOrDefault(playerId, PhaseShieldStatus.ABSENT);
    }

    public static float charge(UUID playerId) { return CHARGES.getOrDefault(playerId, 0.0F); }

    public static void set(UUID playerId, PhaseShieldStatus status, float charge) {
        if (status == null || status == PhaseShieldStatus.ABSENT) {
            STATES.remove(playerId);
            CHARGES.remove(playerId);
        } else {
            STATES.put(playerId, status);
            CHARGES.put(playerId, Math.max(0.0F, Math.min(1.0F, charge)));
        }
    }

    public static void set(UUID playerId, PhaseShieldStatus status) { set(playerId, status, 1.0F); }

    public static void clear() { STATES.clear(); CHARGES.clear(); }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }
}
