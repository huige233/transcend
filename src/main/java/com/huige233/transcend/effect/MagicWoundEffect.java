package com.huige233.transcend.effect;

import com.huige233.transcend.balance.BalanceConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * Round 37: 魔伤痕 (Magic Wound) — 禁卷代价标志性效果。
 *
 * <p>替代了早期 ForbiddenHollowQuarryEffect 中以 WITHER 顶替的占位符。
 * 受影响实体所有传入伤害放大 <code>(amplifier+1) × 25%</code>。
 *
 * <p>挂在受害实体身上而非伤害源 — 由 LivingHurtEvent 处理器读取 amplifier 后调整 damage。
 * 监听器在 {@link com.huige233.transcend.effect.MagicWoundHandler}。
 */
public class MagicWoundEffect extends MobEffect {

    public MagicWoundEffect() {
        super(MobEffectCategory.HARMFUL, 0x7A2EE8);
    }

    /** 受伤倍率： I 级 = 1.25x，II 级 = 1.50x，III 级 = 1.75x，IV 级 = 2.0x */
    public static float getDamageMultiplier(int amplifier) {
        return 1.0F + (amplifier + 1) * 0.25F;
    }

    /** 创建一个默认参数的 MobEffectInstance — 由 BalanceConfig 控制 duration/amp */
    public static net.minecraft.world.effect.MobEffectInstance defaultInstance() {
        BalanceConfig.ScrollBalance s = BalanceConfig.get().scroll;
        return new net.minecraft.world.effect.MobEffectInstance(
                com.huige233.transcend.init.ModEffects.MAGIC_WOUND.get(),
                s.magic_wound_duration,
                s.magic_wound_amplifier,
                false, true);
    }
}
