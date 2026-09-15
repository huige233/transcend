package com.huige233.transcend.items.tools;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;


/** 按注册伤害类型标识构造测试剑伤害源，无效时回退为玩家攻击或通用伤害。 */
public final class TestSwordDamage {

    private TestSwordDamage() {
    }

       
                                                
      
                                                                          
       
    public static DamageSource source(Level level, @Nullable Player attacker, String typeId) {
        ResourceLocation rl = ResourceLocation.tryParse(typeId);
        if (rl == null) return fallback(level, attacker);
        try {
            ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, rl);
            var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key);
            return attacker != null ? new DamageSource(holder, attacker)
                    : new DamageSource(holder);
        } catch (Exception e) {
            return fallback(level, attacker);
        }
    }

    private static DamageSource fallback(Level level, @Nullable Player attacker) {
        return attacker != null ? level.damageSources().playerAttack(attacker)
                : level.damageSources().generic();
    }
}
