package com.huige233.transcend.items.tech;

import com.huige233.transcend.tech.assembly.AssemblyKnowledge;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;


/** 在服务端校验前置等级后消耗知识物品，永久提升玩家的机械装配知识等级。 */
public class MechanicalKnowledgeItem extends Item {
    private final int tier;

    public MechanicalKnowledgeItem(int tier) {
        super(new Properties().stacksTo(1).rarity(tier >= 5 ? Rarity.EPIC : tier >= 3 ? Rarity.RARE : Rarity.UNCOMMON));
        if (tier < 1 || tier > AssemblyKnowledge.MAX_TIER) throw new IllegalArgumentException("Invalid knowledge tier");
        this.tier = tier;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        int current = AssemblyKnowledge.tier(player);
        if (current >= tier) {
            player.displayClientMessage(Component.translatable("knowledge.transcend.already", tier), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!AssemblyKnowledge.learn(player, tier)) {
            player.displayClientMessage(Component.translatable("knowledge.transcend.previous", tier - 1), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.displayClientMessage(Component.translatable("knowledge.transcend.learned", tier), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("knowledge.transcend.use", tier).withStyle(ChatFormatting.AQUA));
        if (tier > 1) lines.add(Component.translatable("knowledge.transcend.previous", tier - 1).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("knowledge.transcend.permanent").withStyle(ChatFormatting.GRAY));
    }
}
