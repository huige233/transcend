package com.huige233.transcend.items;

import com.huige233.transcend.ModRarities;
import com.huige233.transcend.ascension.AscensionCapability;
import com.huige233.transcend.ascension.PlayerAscensionData;
import com.huige233.transcend.init.ModItems;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 法师飞升之书 — 右键打开飞升天赋树界面
 */
public class AscensionBookItem extends Item {

    public AscensionBookItem() {
        super(new Properties().stacksTo(1).fireResistant());
        ModItems.ITEMS.add(this);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    com.huige233.transcend.client.AscensionTreeScreen.open());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.transcend.ascension_book.desc")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.transcend.ascension_book.hint")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return ModRarities.COSMIC;
    }
}
