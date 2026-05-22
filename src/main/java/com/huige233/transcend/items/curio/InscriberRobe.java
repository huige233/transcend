package com.huige233.transcend.items.curio;

import com.huige233.transcend.items.LoreItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 执笔者长袍 — 身体饰品。
 *
 * <p>被动效果：佩戴时若玩家静止（水平速度 &lt; 0.02），每秒刷新「再生 I」3 秒滚动。
 * 奖励抄写时的「静坐」节奏 —— 移动则失效，专注则得益。
 */
public class InscriberRobe extends LoreItem implements ICurioItem {

    private static final double STILL_THRESHOLD = 0.02D;
    private static final int REGEN_DURATION_TICKS = 60; // 3s rolling

    public InscriberRobe() {
        super(Rarity.RARE,
                "tooltip.transcend.inscriber_robe.lore",
                "tooltip.transcend.inscriber_robe.lore2",
                "tooltip.transcend.inscriber_robe.curio");
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        Vec3 motion = player.getDeltaMovement();
        double horizontal = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontal < STILL_THRESHOLD) {
            // Apply Regen I; particles=false to avoid icon spam
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    REGEN_DURATION_TICKS, 0, true, false));
        }
    }
}
