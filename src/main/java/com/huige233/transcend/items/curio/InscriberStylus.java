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

/**
 * 执笔者主笔 — 手部/挂件饰品。
 *
 * <p>被动效果：佩戴时持续刷新「急迫 I」 — 笔走如飞。
 *
 * <p>套装效果（佩戴齐三件时）：额外刷新「饱和 I」 — 静心而食。
 * 套装判定通过查找玩家其它已穿戴 Curio 栏位中的 InscriberHood 与 InscriberRobe 实例。
 */
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

        // Primary effect: Haste I
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, true, false));

        // Set bonus check: look for Hood + Robe in any curio slot.
        // Use a lightweight inventory scan via Curios API helper.
        if (isWearingSetCompanion(player, InscriberHood.class)
                && isWearingSetCompanion(player, InscriberRobe.class)) {
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 40, 0, true, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false));
        }
    }

    /** True if the player has any equipped Curio whose item is an instance of clazz. */
    private static boolean isWearingSetCompanion(Player player, Class<?> clazz) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player)
                .resolve()
                .map(handler -> handler.findFirstCurio(stack -> clazz.isInstance(stack.getItem())).isPresent())
                .orElse(false);
    }
}
