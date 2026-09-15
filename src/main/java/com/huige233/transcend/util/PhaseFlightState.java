package com.huige233.transcend.util;

import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.network.S2CPhaseFlightSync;
import net.minecraft.server.level.ServerPlayer;

   
                           
  
      
                                                                 
                                       
                                                                      
                                                            
                                            
   
/** 维护玩家持久化的服务端权威相位飞行开关，并通过数据包同步到客户端。 */
public final class PhaseFlightState {

    
    public static final String NBT_PHASE_FLIGHT = "transcend_phase_flight";
    
    public static final boolean DEFAULT = true;

    private PhaseFlightState() {
    }

    
    public static boolean isEnabled(ServerPlayer player) {
        if (player == null) return DEFAULT;
        return player.getPersistentData().contains(NBT_PHASE_FLIGHT)
                ? player.getPersistentData().getBoolean(NBT_PHASE_FLIGHT)
                : DEFAULT;
    }

    
    public static boolean isEnabled(net.minecraft.world.entity.player.Player player) {
        if (player == null) return DEFAULT;
        return player.getPersistentData().contains(NBT_PHASE_FLIGHT)
                ? player.getPersistentData().getBoolean(NBT_PHASE_FLIGHT)
                : DEFAULT;
    }

    
    public static void ensureInitialized(ServerPlayer player) {
        if (player == null) return;
        if (!player.getPersistentData().contains(NBT_PHASE_FLIGHT)) {
            player.getPersistentData().putBoolean(NBT_PHASE_FLIGHT, DEFAULT);
        }
    }

    
    public static boolean set(ServerPlayer player, boolean enabled) {
        if (player == null) return DEFAULT;
        player.getPersistentData().putBoolean(NBT_PHASE_FLIGHT, enabled);
        pushToClient(player);
        return enabled;
    }

    
    public static void pushToClient(ServerPlayer player) {
        if (player == null || player.connection == null) return;
        NetworkHandler.CHANNEL.sendTo(new S2CPhaseFlightSync(isEnabled(player)),
                player.connection.connection,
                net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT);
    }
}
