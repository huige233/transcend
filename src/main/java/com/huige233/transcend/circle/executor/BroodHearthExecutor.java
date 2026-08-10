package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

/** 育巢炉心法阵功能执行器。 */
public class BroodHearthExecutor implements CircleFunctionExecutor {

    private static final int SCAN_INTERVAL_TICKS = 100;

    private static final int MAX_ANIMALS_IN_RADIUS = 48;

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
