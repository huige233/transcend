package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.tech.research.ResearchUnlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 物品和 JEI 的通用提示只展示制作要求，不推测客户端未同步的玩家进度。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT)
public final class ResearchCraftingHints {
    private ResearchCraftingHints() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        String research = ResearchUnlocks.required(event.getItemStack());
        if (research != null) event.getToolTip().add(Component.translatable("tooltip.transcend.research_manufacture",
                Component.translatable("research.transcend." + research)).withStyle(ChatFormatting.GOLD));
    }
}
