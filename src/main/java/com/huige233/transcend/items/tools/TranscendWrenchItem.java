package com.huige233.transcend.items.tools;

import com.huige233.transcend.items.ItemBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


/** 提供供发电机识别的侧面配置扳手物品，并在使用时提示操作方式。 */
public class TranscendWrenchItem extends ItemBase {
    public TranscendWrenchItem() { super(new net.minecraft.world.item.Item.Properties().stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.transcend.wrench.use"), true);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
