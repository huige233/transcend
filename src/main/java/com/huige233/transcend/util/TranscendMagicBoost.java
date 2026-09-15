package com.huige233.transcend.util;

import com.huige233.transcend.Transcend;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

   
                                     
  
                                                
                                                               
                           
  
                                                           
                                                                   
                                         
                                                                   
                                        
   
/** 每五刻为全套超越护甲玩家调度已安装魔法模组的资源补充和冷却清除实现。 */
public final class TranscendMagicBoost {

    private TranscendMagicBoost() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("TranscendMagicBoost");
    
    private static final int INTERVAL = 5;
    private static boolean armed = false;

    
    /** 约定单个魔法模组的加载标识及服务端玩家资源与冷却维护入口。 */
    public interface IMagicBoost {
        
        String modId();

        
        void apply(Player player);
    }

    
    private static final Map<String, IMagicBoost> BOOSTS = new LinkedHashMap<>();
    private static boolean registered = false;

    
    private static synchronized void registerAll() {
        if (registered) return;
        registered = true;
        register(new IssMagicBoost());
        register(new GoetyMagicBoost());
    }

    private static void register(IMagicBoost boost) {
        BOOSTS.put(boost.modId(), boost);
    }

    
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.player == null) return;
        tick(event.player);
    }

    public static void tick(Player player) {
        if (player == null || player.level().isClientSide) return;
        if (!armed) {
            armed = true;
            registerAll();
            LOGGER.debug("[MagicBoost] armed; boost mods: {}", BOOSTS.keySet());
        }
        
        if (player.tickCount % INTERVAL != 0) return;
        
        if (!ArmorUtils.fullEquipped(player)) return;

        for (IMagicBoost boost : BOOSTS.values()) {
            try {
                if (!isLoaded(boost.modId())) continue;
                boost.apply(player);
            } catch (Throwable t) {
                LOGGER.error("[MagicBoost] {} apply error", boost.modId(), t);
            }
        }
    }

    private static boolean isLoaded(String modId) {
        try {
            return ModList.get() != null && ModList.get().isLoaded(modId);
        } catch (Throwable t) {
            return false;
        }
    }

    
    public static void init() {
        MinecraftForge.EVENT_BUS.register(TranscendMagicBoost.class);
    }
}
