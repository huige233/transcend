package com.huige233.transcend.entity.shield;

import com.huige233.transcend.entity.TestDummy.DamageCategory;
import net.minecraft.nbt.CompoundTag;

   
                         
                                     
                                                 
                                         
                                            
                                  
                                                  
                                                  
  
                                                         
                                        
   
/** 通过宿主接口结算护盾抗性、分类韧性吸收、溢出伤害及受击和延迟回充。 */
public final class ShieldSystem {

    
    /** 向护盾系统提供盾值、回复配置、分类抗性和韧性的存取及吸收播报回调。 */
    public interface Host {
        
        float getShieldValue();
        
        void setShieldValueRaw(float value);
        
        float getShieldMax();
        
        void setShieldMaxRaw(float max);
        
        int getRegenStep();
        
        void setRegenStepRaw(int step);
        
        boolean isRegenOnHit();
        
        void setRegenOnHitRaw(boolean value);
        
        boolean isAffectedByResist();
        
        void setAffectedByResistRaw(boolean value);
        
        int getToughness(int categoryIndex);
        
        void setToughnessRaw(int categoryIndex, int toughness);
        
        int getRegenDelay();
        
        int getRegenOnHitPercent();
        
        int getCategoryResistance(int categoryIndex);
        
        int getResistanceLevel();
        
        boolean isAnnounceReduce();
           
                              
                                                        
           
        void announceShieldAbsorb(float absorbed, float overflow, float shieldValue, float shieldMax, float regenOnHit);
        
        default void onShieldHit() {
        }
    }

    
    public static final int SHIELD_REGEN_DELAY = 60;
    
    public static final float REGEN_BASE_PERCENT = 0.0025F;
    
    public static final int MAX_TOUGHNESS = 100;
    
    public static final int MIN_REGEN_STEP = 1;
    public static final int MAX_REGEN_STEP = 20;
    
    public static final int MIN_REGEN_ON_HIT_PERCENT = 0;
    public static final int MAX_REGEN_ON_HIT_PERCENT = 100;
    
    public static final int DEFAULT_REGEN_ON_HIT_PERCENT = 5;
       
                     
                                        
                                     
       
    public static float sanitizeDamage(float amount) {
        return Float.isFinite(amount) ? Math.max(0.0F, amount) : 0.0F;
    }
    
    public static final int MIN_REGEN_DELAY = 0;
    public static final int MAX_REGEN_DELAY = 600;

    
    private int regenTimer = 0;

    

       
                   
      
                            
                              
                                                  
                                                         
                                   
       
    public float absorb(Host host, float amount, DamageCategory category, float preResisted) {
        amount = sanitizeDamage(amount);
        preResisted = sanitizeDamage(preResisted);
        
        preResisted = Math.min(amount, preResisted);
        
        float toughness = category != null ? Math.max(1, host.getToughness(category.index)) : 1;
        float shieldCost = preResisted / toughness;

        float shieldValue = host.getShieldValue();
        
        float cost = Math.min(shieldValue, shieldCost);
        host.setShieldValueRaw(shieldValue - cost);
        regenTimer = Math.max(0, host.getRegenDelay());
        host.onShieldHit();

        
        
        float regenOnHit = 0;
        if (host.isRegenOnHit()) {
            regenOnHit = host.getShieldMax() * host.getRegenOnHitPercent() / 100.0F;
            if (regenOnHit > 0) {
                host.setShieldValueRaw(host.getShieldValue() + regenOnHit);
            }
        }

        
        float overflowCost = shieldCost - cost;
        float overflow = overflowCost > 0 ? overflowCost * toughness : 0;

        if (host.isAnnounceReduce()) {
            host.announceShieldAbsorb(preResisted - overflow, overflow,
                    host.getShieldValue(), host.getShieldMax(), regenOnHit);
        }
        return overflow;
    }

       
                                                            
                                    
       
    public float applyResistChain(Host host, float amount, DamageCategory category) {
        float input = sanitizeDamage(amount);
        if (host.isAffectedByResist()) {
            if (category != null) {
                input *= 1.0F - host.getCategoryResistance(category.index) / 100.0F;
            }
            int resistPct = host.getResistanceLevel();
            if (resistPct > 0) {
                input *= 1.0F - Math.min(100, resistPct) / 100.0F;
            }
        }
        return input;
    }

    

       
                                                          
                                  
       
    public boolean tickRegen(Host host) {
        if (host.getShieldMax() <= 0 || host.getShieldValue() >= host.getShieldMax()) {
            return false;
        }
        if (regenTimer > 0) {
            regenTimer--;
            return false;
        }
        host.setShieldValueRaw(host.getShieldValue()
                + host.getShieldMax() * REGEN_BASE_PERCENT * host.getRegenStep());
        return true;
    }

    
    public void resetRegenDelay() {
        regenTimer = 0;
    }

    
    private static final String NBT_TOUGHNESS = "ShieldToughness";

    
    public static int[] readToughness(CompoundTag tag) {
        int[] arr = new int[DamageCategory.values().length];
        java.util.Arrays.fill(arr, 1);
        if (tag.contains(NBT_TOUGHNESS)) {
            int[] saved = tag.getIntArray(NBT_TOUGHNESS);
            for (int i = 0; i < Math.min(saved.length, arr.length); i++) {
                arr[i] = Math.max(1, Math.min(MAX_TOUGHNESS, saved[i]));
            }
        }
        return arr;
    }

    
    public static void writeToughness(CompoundTag tag, int[] toughness) {
        tag.putIntArray(NBT_TOUGHNESS, toughness);
    }

    
    public static int defaultRegenStep(CompoundTag tag) {
        return tag.contains("ShieldRegenStep") ? tag.getInt("ShieldRegenStep") : 2;
    }
}
