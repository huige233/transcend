package com.huige233.transcend.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 接入原版击杀归属窗口及包含 Forge 掉落、经验事件的死亡结算。 */
@Mixin(LivingEntity.class)
public interface LivingEntityDeathAccess {
    @Accessor("lastHurtByPlayerTime")
    void transcend$setLastHurtByPlayerTime(int ticks);

    @Invoker("dropAllDeathLoot")
    void transcend$dropAllDeathLoot(DamageSource source);
}
