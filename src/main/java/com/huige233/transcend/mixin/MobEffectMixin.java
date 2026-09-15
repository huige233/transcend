package com.huige233.transcend.mixin;

import com.huige233.transcend.util.TranscendGuard;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

   
                                                              
   
/** 阻止服务端非增益状态效果向受保护生物写入属性修饰符。 */
@Mixin(MobEffect.class)
public abstract class MobEffectMixin {

    @Inject(method = "addAttributeModifiers", at = @At("HEAD"), cancellable = true)
    private void transcend$blockNegativeAttributeModifiers(LivingEntity entity, AttributeMap map, int amplifier, CallbackInfo ci) {
        if (entity == null || entity.level().isClientSide) return;
        MobEffect self = (MobEffect) (Object) this;
        if (self.isBeneficial()) return;
        if (TranscendGuard.isProtected(entity)) {
            ci.cancel();
        }
    }
}
