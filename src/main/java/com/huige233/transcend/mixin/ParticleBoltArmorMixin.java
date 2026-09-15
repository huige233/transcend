package com.huige233.transcend.mixin;

import com.huige233.transcend.tech.combat.ArmorPenetration;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;


/** 按本次攻击的穿甲规则调整护甲减伤输入，并在护甲被削减时清零韧性输入而保留后续伤害流程。 */
@Mixin(LivingEntity.class)
public abstract class ParticleBoltArmorMixin {
    @WrapOperation(method = "getDamageAfterArmorAbsorb",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"))
    private float transcend$particleArmor(float damage, float armor, float toughness,
                                          Operation<Float> original, DamageSource source, float amount) {
        float effective = ArmorPenetration.effectiveArmor((LivingEntity) (Object) this, source, armor);
        return original.call(damage, effective, effective != armor ? 0.0F : toughness);
    }
}
