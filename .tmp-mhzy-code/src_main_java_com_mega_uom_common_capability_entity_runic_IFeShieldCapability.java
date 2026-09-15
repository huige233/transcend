package com.mega.uom.common.capability.entity.runic;

import com.mega.uom.common.items.armor.FantasyEndingArmorItem;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public interface IFeShieldCapability extends IRunicShieldCapability {
    @Override
    default void dealDamage(LivingEntity direct, DamageSource damageSource, float amount, LivingHurtEvent context) {
        if (FantasyEndingArmorItem.is4Armor(direct)) {
            if (amount > 128F) amount = Math.max(0, amount - 128);
        }
        if (getOutResistance() >= 0F) context.setAmount(amount * (1F - getOutResistance()));
    }
}
