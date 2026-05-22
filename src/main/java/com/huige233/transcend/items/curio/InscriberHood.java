package com.huige233.transcend.items.curio;

import com.huige233.transcend.items.LoreItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 执笔者头巾 — 头部饰品。
 *
 * <p>被动效果：佩戴时持续刷新「夜视」 — 因为执笔者的眼总要看清纸上的纹理。
 */
public class InscriberHood extends LoreItem implements ICurioItem {

    public InscriberHood() {
        super(Rarity.RARE,
                "tooltip.transcend.inscriber_hood.lore",
                "tooltip.transcend.inscriber_hood.lore2",
                "tooltip.transcend.inscriber_hood.curio");
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;
        // Refresh night vision; particles=false to avoid icon spam
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, true, false));
    }
}
