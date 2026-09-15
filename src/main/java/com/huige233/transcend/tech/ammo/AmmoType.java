package com.huige233.transcend.tech.ammo;

import com.huige233.transcend.tech.attribute.AttributeContainer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

   
                         
  
                                        
                                                       
                              
   
/** 约定弹种的伤害来源、装填资源与容量、弹体外观、行为及伤害和速度倍率。 */
public interface AmmoType {

    
    String id();

    
    String displayName();

    
    DamageSource createDamageSource(Level level, @Nullable Entity owner);

    
    AmmoResource resource();

    
    BoltVisual visual();

    
    BoltBehavior behavior(AttributeContainer attrs);

    
    long reloadCost();

    
    int reloadCapacity();

    
    default float damageMultiplier() {
        return 1.0F;
    }

    
    default float speedMultiplier() {
        return 1.0F;
    }

}
