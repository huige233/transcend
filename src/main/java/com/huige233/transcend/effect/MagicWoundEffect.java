package com.huige233.transcend.effect;

import com.huige233.transcend.balance.BalanceConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** 魔法创伤药水效果。 */
public class MagicWoundEffect extends MobEffect {

    public MagicWoundEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A2EE8);
    }

    public static float getDamageMultiplier(int amplifier) {
        return 1.0F + (amplifier + 1) * 0.25F;
    }

    public static net.minecraft.world.effect.MobEffectInstance defaultInstance() {
        BalanceConfig.ScrollBalance s = BalanceConfig.get().scroll;
        return new net.minecraft.world.effect.MobEffectInstance(
                com.huige233.transcend.init.ModEffects.MAGIC_WOUND.get(),
                s.magic_wound_duration,
                s.magic_wound_amplifier,
                false, true);
    }
}
