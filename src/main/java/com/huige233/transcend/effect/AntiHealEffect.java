package com.huige233.transcend.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 禁疗药水效果。 */
public class AntiHealEffect extends MobEffect {
    public AntiHealEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    public static float getHealMultiplier(int amplifier) {
        return Math.max(0, 1.0F - (amplifier + 1) * 0.25F);
    }
}
