package com.huige233.transcend.tech.rank;

   
                                     
  
                                                        
                                       
  
                                                
   
/** 定义从 E 到 EX 的七档装备评级及各档的词缀随机数值上限和保底强度档位。 */
public enum TechRank {
    E(0.25F, 1),                          
    D(0.40F, 1),
    C(0.55F, 2),
    B(0.70F, 2),
    A(0.85F, 3),
    S(0.95F, 4),
    EX(1.00F, 5);                              

    public final float rollCeiling;
    public final int affixTierFloor;

    TechRank(float rollCeiling, int affixTierFloor) {
        this.rollCeiling = rollCeiling;
        this.affixTierFloor = affixTierFloor;
    }

    public String id() {
        return name().toLowerCase();
    }

    
    public static TechRank byId(String id) {
        if (id == null || id.isEmpty()) return C;
        try {
            return valueOf(id.toUpperCase());
        } catch (IllegalArgumentException e) {
            return C;
        }
    }
}
