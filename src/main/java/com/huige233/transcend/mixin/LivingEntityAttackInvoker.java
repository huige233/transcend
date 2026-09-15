package com.huige233.transcend.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

                                                                   
                                                                        
/** 通过 Mixin 调用器暴露生物受保护的实际伤害方法，保留其内部 Forge 伤害事件流程。 */
@Mixin(LivingEntity.class)
public interface LivingEntityAttackInvoker {
    @Invoker("actuallyHurt")
    void transcend$actuallyHurt(DamageSource source, float amount);
}
