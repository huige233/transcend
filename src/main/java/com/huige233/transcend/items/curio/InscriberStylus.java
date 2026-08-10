package com.huige233.transcend.items.curio;

import com.huige233.transcend.items.LoreItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** 铭刻笔（饰品）。 */
public class InscriberStylus extends LoreItem implements ICurioItem {

    public InscriberStylus() {
        super(new Item.Properties().rarity(Rarity.RARE).stacksTo(1),
                "tooltip.transcend.inscriber_stylus.lore",
                "tooltip.transcend.inscriber_stylus.lore2",
                "tooltip.transcend.inscriber_stylus.curio",
                "tooltip.transcend.inscriber.set_bonus");
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, true, false));

        if (isWearingSetCompanion(player, InscriberHood.class)
                && isWearingSetCompanion(player, InscriberRobe.class)) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
        }
    }

    private static boolean isWearingSetCompanion(Player player, Class<?> clazz) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(handler -> handler.findFirstCurio(stack -> clazz.isInstance(stack.getItem())).isPresent())
                .orElse(false);
    }
}
