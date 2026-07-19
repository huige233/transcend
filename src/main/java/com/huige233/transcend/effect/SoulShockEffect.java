package com.huige233.transcend.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class SoulShockEffect extends MobEffect {

    public SoulShockEffect() {
        super(MobEffectCategory.HARMFUL, 0x2C0E3A);
    }

    public static float getTriggerChance(int amplifier) {
        return Math.min(1.0F, 0.20F + amplifier * 0.10F);
    }

    public static float getVoidBonusDamage(int amplifier) {
        return 2.0F + amplifier;
    }
}
