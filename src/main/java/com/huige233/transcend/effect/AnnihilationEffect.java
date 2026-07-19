package com.huige233.transcend.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AnnihilationEffect extends MobEffect {
    public AnnihilationEffect() {
        super(MobEffectCategory.HARMFUL, 0x4B0082);
    }

    public static float getDamageMultiplier(int amplifier) {
        return 1.0F + (amplifier + 1) * 0.25F;
    }
}
