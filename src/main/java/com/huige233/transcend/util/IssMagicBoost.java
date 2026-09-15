package com.huige233.transcend.util;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

   
                                           
  
            
                                                        
                                                
  
                       
                                                            
                                                      
                                                      
                                          
                                                                                                             
   
/** 为符合条件的玩家清除铁魔法法术冷却，并按最大法力属性补满法力。 */
public class IssMagicBoost implements TranscendMagicBoost.IMagicBoost {

    private static final Logger LOGGER = LoggerFactory.getLogger("TranscendMagicBoost");
    public static final String MODID = "irons_spellbooks";

    private static volatile boolean warnedNoAttr = false;

    @Override
    public String modId() {
        return MODID;
    }

    @Override
    public void apply(Player player) {
        try {
            MagicData md = MagicData.getPlayerMagicData(player);
            if (md == null) return;

            
            try {
                md.getPlayerCooldowns().clearCooldowns();
            } catch (Throwable t) {
                LOGGER.debug("[MagicBoost/ISS] clearCooldowns failed: {}", t.toString());
            }

            
            try {
                float max = maxMana(player);
                if (max > 0 && md.getMana() < max) {
                    md.setMana(max);
                }
            } catch (Throwable t) {
                LOGGER.debug("[MagicBoost/ISS] setMana failed: {}", t.toString());
            }
        } catch (NoClassDefFoundError | NoSuchMethodError ignored) {
            
        } catch (Throwable t) {
            LOGGER.error("[MagicBoost/ISS] apply error", t);
        }
    }

    
    private static float maxMana(Player player) {
        Object attr = readStaticAttr(AttributeRegistry.class, "MAX_MANA");
        if (!(attr instanceof net.minecraft.world.entity.ai.attributes.Attribute a)) return 0f;
        AttributeInstance inst = player.getAttribute(a);
        if (inst == null) {
            if (!warnedNoAttr) {
                warnedNoAttr = true;
                LOGGER.warn("[MagicBoost/ISS] MAX_MANA attribute not attached to player; using default 100");
            }
            
            return 100f;
        }
        return (float) inst.getValue();
    }

    
    @SuppressWarnings("unchecked")
    private static Object readStaticAttr(Class<?> holder, String field) {
        try {
            Field f = holder.getField(field);
            Object v = f.get(null);
            if (v instanceof RegistryObject<?> ro) return ro.get();
            return v;
        } catch (Throwable t) {
            return null;
        }
    }
}
