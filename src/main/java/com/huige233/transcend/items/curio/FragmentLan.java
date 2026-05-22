package com.huige233.transcend.items.curio;

import com.huige233.transcend.ModRarities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

public class FragmentLan extends Item implements ICurioItem {

    private static final int MAX_CHARGE = 1000;

    public FragmentLan() {
        super(new Properties().rarity(ModRarities.COSMIC).stacksTo(1));
    }

    @Override
    public void curioTick(SlotContext ctx, ItemStack stack) {
        LivingEntity entity = ctx.entity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));

        int range = 8;
        AABB aabb = player.getBoundingBox().inflate(range);
        List<Player> nearby = player.level().getEntitiesOfClass(Player.class, aabb, p -> p != player);
        for (Player target : nearby) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, false, false));
        }

        int charge = stack.getOrCreateTag().getInt("charge");
        if (charge < MAX_CHARGE) {
            stack.getOrCreateTag().putInt("charge", charge + 1);
            if (charge + 1 == MAX_CHARGE) {
                player.displayClientMessage(Component.translatable("FragmentLan.on")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    public static boolean isCharged(ItemStack stack) {
        return stack.getOrCreateTag().getInt("charge") >= MAX_CHARGE;
    }

    public static void discharge(ItemStack stack) {
        stack.getOrCreateTag().putInt("charge", 0);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fragment_lan.desc").withStyle(ChatFormatting.YELLOW));
    }
}
