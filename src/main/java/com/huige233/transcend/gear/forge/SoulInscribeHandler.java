package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.forge.SoulInscriberItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 灵魂刻印事件处理。 */
public class SoulInscribeHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim == null) return;
        if (victim instanceof Player) return;

        if (!(event.getSource().getEntity() instanceof Player killer)) return;
        if (killer.level().isClientSide) return;

        ItemStack mainHand = killer.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof SoulInscriberItem)) return;

        boolean captured = SoulInscriberItem.tryCapture(mainHand, victim);
        if (captured) {
            killer.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            "msg.transcend.soul_inscriber.captured",
                            victim.getType().getDescription())
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
        }
    }
}
