package com.huige233.transcend.items;

import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.AscensionHandler;
import com.huige233.transcend.ascension.PlayerAscensionData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RespecPotionItem extends Item {

    public RespecPotionItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            PlayerAscensionData data = AscensionCapability.get(sp);
            if (data.getUnlockedNodes().isEmpty() && !data.hasMastery()) {
                sp.sendSystemMessage(Component.translatable("msg.transcend.respec_nothing")
                        .withStyle(ChatFormatting.GRAY));
                return InteractionResultHolder.pass(stack);
            }
            int refund = data.respec();
            AscensionHandler.applyPersistentStats(sp, data);
            AscensionHandler.syncToClient(sp, data);
            if (!player.isCreative()) stack.shrink(1);
            sp.sendSystemMessage(Component.translatable("msg.transcend.respec_done", refund)
                    .withStyle(ChatFormatting.GOLD));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.respec_potion.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.transcend.respec_potion.hint")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
