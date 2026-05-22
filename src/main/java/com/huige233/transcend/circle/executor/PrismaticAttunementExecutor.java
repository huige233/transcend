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

/**
 * 棱光协调（Prismatic Attunement）功能执行器。
 * <p>
 * 在催化剂栈中扫描首个 {@link SpellElementItem}，根据其元素施加针对性 buff 给半径内玩家。
 */
public class PrismaticAttunementExecutor implements CircleFunctionExecutor {

    private static final int DEFAULT_DURATION = 60;
    private static final int SHORT_DURATION = 40;

    @Override
    public boolean canActivate(CircleFunctionContext ctx) {
        return ctx.getTier().getLevel() >= CircleTier.MASTER.getLevel();
    }

    @Override
    public void onActivate(CircleFunctionContext ctx) {
        // 无状态
    }

    @Override
    public void tick(CircleFunctionContext ctx) {
        SpellElement element = findElement(ctx.getCatalystStacks());
        MobEffectInstance effect = effectForElement(element);

        double radius = ctx.getBaseRadius();
        List<Player> players = ctx.getMobsInRadius(Player.class, radius);
        if (players.isEmpty()) return;

        for (Player player : players) {
            // 每个玩家创建独立实例，避免共享状态导致的 duration 漂移
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
        // 无状态
    }

    /** 在催化剂栈中查找首个 SpellElementItem 的元素，找不到则返回 null。 */
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

    /** 根据元素返回对应的效果实例（默认护盾）。 */
    private MobEffectInstance effectForElement(SpellElement element) {
        if (element == null) {
            return makeEffect(MobEffects.ABSORPTION, DEFAULT_DURATION, 0);
        }
        return switch (element) {
            case FIRE -> makeEffect(MobEffects.FIRE_RESISTANCE, DEFAULT_DURATION, 0);
            case ICE -> makeEffect(MobEffects.MOVEMENT_SPEED, DEFAULT_DURATION, 0);
            case THUNDER -> makeEffect(MobEffects.LUCK, DEFAULT_DURATION, 0);
            case WIND -> makeEffect(MobEffects.MOVEMENT_SPEED, SHORT_DURATION, 1);
            case EARTH -> makeEffect(MobEffects.DAMAGE_RESISTANCE, DEFAULT_DURATION, 0);
            case HOLY, LIGHT -> makeEffect(MobEffects.REGENERATION, DEFAULT_DURATION, 0);
            case DARK, VOID -> makeEffect(MobEffects.DAMAGE_BOOST, DEFAULT_DURATION, 0);
            default -> makeEffect(MobEffects.ABSORPTION, DEFAULT_DURATION, 0);
        };
    }

    private static MobEffectInstance makeEffect(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, false, true);
    }
}
