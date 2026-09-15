package com.huige233.transcend.tech.attribute;

import java.util.UUID;

   
                                         
  
                                            
                                                 
                           
                            
                                                 
   
/** 记录属性修饰器的唯一标识、来源名、数值、运算方式和到期刻，并判断临时效果是否过期。 */
public record TechAttrModifier(UUID uuid, String name, double amount, ModifierOp op, long expiresAtTick) {

    
    public static TechAttrModifier permanent(UUID uuid, String name, double amount, ModifierOp op) {
        return new TechAttrModifier(uuid, name, amount, op, -1L);
    }

    
    public static TechAttrModifier temporary(UUID uuid, String name, double amount, ModifierOp op, long expireAt) {
        return new TechAttrModifier(uuid, name, amount, op, expireAt);
    }

    
    public boolean isExpired(long currentTick) {
        return expiresAtTick >= 0 && currentTick >= expiresAtTick;
    }
}
