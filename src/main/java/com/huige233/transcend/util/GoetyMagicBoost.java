package com.huige233.transcend.util;

import com.Polarice3.Goety.common.capabilities.soulenergy.FocusCooldown;
import com.Polarice3.Goety.common.capabilities.soulenergy.ISoulEnergy;
import com.Polarice3.Goety.common.capabilities.soulenergy.SEProvider;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.LazyOptional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

   
                     
  
            
                                                              
                                                          
                                   
  
                       
                                                     
                                                                             
                                             
                                                                                
                                                    
                                                                     
   
/** 为符合条件的玩家清除 Goety 焦点冷却、补满灵魂能量，并将灵魂容量配置提升至三十万。 */
public class GoetyMagicBoost implements TranscendMagicBoost.IMagicBoost {

    private static final Logger LOGGER = LoggerFactory.getLogger("TranscendMagicBoost");
    public static final String MODID = "goety";

    
    public static final int SOUL_ENERGY_CAP = 300_000;
    
    private static boolean capApplied = false;

    
    @SuppressWarnings("unchecked")
    private static void applyCapOnce() {
        if (capApplied) return;
        capApplied = true;
        try {
            var cfg = com.Polarice3.Goety.config.MainConfig.MaxSouls;
            if (cfg != null) ((ForgeConfigSpec.ConfigValue<Integer>) cfg).set(SOUL_ENERGY_CAP);
            var arca = com.Polarice3.Goety.config.MainConfig.MaxArcaSouls;
            if (arca != null) ((ForgeConfigSpec.ConfigValue<Integer>) arca).set(SOUL_ENERGY_CAP);
            LOGGER.info("[MagicBoost/Goety] soul energy cap raised to {}", SOUL_ENERGY_CAP);
        } catch (Throwable t) {
            LOGGER.warn("[MagicBoost/Goety] failed to raise soul cap: {}", t.toString());
        }
    }

    @Override
    public String modId() {
        return MODID;
    }

    @Override
    public void apply(Player player) {
        try {
            applyCapOnce();
            LazyOptional<ISoulEnergy> opt = player.getCapability(SEProvider.CAPABILITY);
            if (!opt.isPresent()) return;
            opt.ifPresent(se -> {
                try {
                    
                    try {
                        FocusCooldown fc = se.cooldowns();
                        if (fc != null && fc.cooldowns != null) {
                            fc.cooldowns.clear();
                        }
                    } catch (Throwable t) {
                        LOGGER.debug("[MagicBoost/Goety] clear cooldowns failed: {}", t.toString());
                    }

                    
                    try {
                        if (se.getSoulEnergy() < SOUL_ENERGY_CAP) {
                            se.setSoulEnergy(SOUL_ENERGY_CAP);
                        }
                    } catch (Throwable t) {
                        LOGGER.debug("[MagicBoost/Goety] setSoulEnergy failed: {}", t.toString());
                    }
                } catch (Throwable t) {
                    LOGGER.error("[MagicBoost/Goety] inner error", t);
                }
            });
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            
        } catch (Throwable t) {
            LOGGER.error("[MagicBoost/Goety] apply error", t);
        }
    }
}
