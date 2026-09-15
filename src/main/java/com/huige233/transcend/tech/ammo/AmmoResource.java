package com.huige233.transcend.tech.ammo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

   
                                       
  
                                                        
                                      
   
/** 约定弹药装填资源的身份、可用余额、可用性和全额扣除接口。 */
public interface AmmoResource {

    
    String id();

    
    Component displayName();

    
    long available(Player holder, ItemStack gun);

       
            
      
                                                             
                                             
       
    long consume(Player holder, ItemStack gun, long cost);

    
    boolean isAvailable(Player holder);
}
