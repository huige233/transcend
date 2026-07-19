package com.huige233.transcend.items.curio;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public class AnvilCompat extends Item implements ICurioItem {

    private static final int CHARGE_TIME = 120;

    public AnvilCompat() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1));
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false));

        int charge = stack.getOrCreateTag().getInt("Anvil");
        if (charge < CHARGE_TIME) {
            stack.getOrCreateTag().putInt("Anvil", charge + 1);
            if (charge + 1 == CHARGE_TIME) {
                player.displayClientMessage(Component.translatable("Anvil.Attack")
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    public boolean isCharged(ItemStack stack) {
        return stack.getOrCreateTag().getInt("Anvil") >= CHARGE_TIME;
    }

    public void discharge(ItemStack stack) {
        stack.getOrCreateTag().putInt("Anvil", 0);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.anvil_compat.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.anvil_compat.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.anvil_compat.desc3").withStyle(ChatFormatting.GRAY));
    }
}
