package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/**
 * 育群之炉（Brood Hearth）功能执行器。
 * <p>
 * 每 100 tick（5 秒）扫描半径范围内的被动型动物：
 * <ul>
 *     <li>对幼年动物施加速度 I（100 tick），加速其成长。</li>
 *     <li>当范围内动物数量超过 {@link #MAX_ANIMALS_IN_RADIUS} 时，本次扫描跳过 buff，
 *         避免过度繁殖。</li>
 * </ul>
 */
public class BroodHearthExecutor implements CircleFunctionExecutor {

    /** 扫描周期（tick）。 */
    private static final int SCAN_INTERVAL_TICKS = 100;
    /** 范围内动物数量上限。 */
    private static final int MAX_ANIMALS_IN_RADIUS = 48;
    /** 幼崽 buff 持续时间（tick）。 */
    private static final int BUFF_DURATION_TICKS = 100;

    private int timer = 0;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.ADEPT.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        timer = 0;
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        if (ctx.getLevel() == null) {
            return;
        }

        timer += 20;
        if (timer < SCAN_INTERVAL_TICKS) {
            return;
        }
        timer = 0;

        double radius = ctx.getBaseRadius();
        List<Animal> animals = ctx.getMobsInRadius(Animal.class, radius);
        // 超出上限：跳过加速以抑制过度繁殖
        if (animals.size() > MAX_ANIMALS_IN_RADIUS) {
            return;
        }

        for (Animal animal : animals) {
            if (animal.isBaby()) {
                animal.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED,
                        BUFF_DURATION_TICKS,
                        0,
                        true,
                        false,
                        true
                ));
            }
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {
        timer = 0;
    }
}
