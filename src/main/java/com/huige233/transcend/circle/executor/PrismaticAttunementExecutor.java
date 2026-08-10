package com.huige233.transcend.circle.executor;

import com.huige233.transcend.circle.CircleFunctionContext;
import com.huige233.transcend.circle.CircleFunctionExecutor;
import com.huige233.transcend.circle.CircleTier;
import com.huige233.transcend.items.SpellElementItem;
import com.huige233.transcend.spell.SpellElement;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 棱光调谐法阵功能执行器。 */
public class PrismaticAttunementExecutor implements CircleFunctionExecutor {

    private static final int DEFAULT_DURATION = 60;
    private static final int SHORT_DURATION = 40;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {

    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        SpellElement element = findElement(ctx.getCatalystStacks());
        MobEffectInstance effect = effectForElement(element);

        double radius = ctx.getBaseRadius();
        List<Player> players = ctx.getMobsInRadius(Player.class, radius);
        if (players.isEmpty()) return;

        for (Player player : players) {

            player.addEffect(new MobEffectInstance(
                    effect.getEffect(),
                    effect.getDuration(),
                    effect.getAmplifier(),
                    effect.isAmbient(),
                    effect.isVisible()
            ));
        }
    }

    @Override
    public void onDeactivate(CircleFunctionContext ctx) {

    }

    private SpellElement findElement(List<ItemStack> stacks) {
        if (stacks == null) return null;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) continue;
            if (stack.getItem() instanceof SpellElementItem sei) {
                return sei.getElement();
            }
        }
        return null;
    }

    private MobEffectInstance effectForElement(SpellElement element) {
        if (element == null) {
            return makeEffect(MobEffects.ABSORPTION, DEFAULT_DURATION, 0);
        }
        return switch (element) {
            case METAL -> makeEffect(MobEffects.LUCK, DEFAULT_DURATION, 0);
            case WOOD -> makeEffect(MobEffects.REGENERATION, DEFAULT_DURATION, 0);
            case WATER -> makeEffect(MobEffects.MOVEMENT_SPEED, DEFAULT_DURATION, 0);
            case FIRE -> makeEffect(MobEffects.FIRE_RESISTANCE, DEFAULT_DURATION, 0);
            case EARTH -> makeEffect(MobEffects.DAMAGE_RESISTANCE, DEFAULT_DURATION, 0);
            case CHAOS -> makeEffect(MobEffects.DAMAGE_BOOST, DEFAULT_DURATION, 0);
        };
    }

    private static MobEffectInstance makeEffect(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, false, true);
    }
}
