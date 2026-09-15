package com.huige233.transcend.items.curio;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;


/** 为雷肤佩戴者持续提供抗性、跳跃、移速与挖掘增益并清除坠落距离。 */
public class ThunderSkin extends Item implements ICurioItem {

    public ThunderSkin() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1));
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, false, false));
        player.fallDistance = 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.thunder_skin.desc1").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.thunder_skin.desc2").withStyle(ChatFormatting.RED));
    }
}
