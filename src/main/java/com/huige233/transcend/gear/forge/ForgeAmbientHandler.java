package com.huige233.transcend.gear.forge;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.gear.GearCategory;
import com.huige233.transcend.gear.GearForgeData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 锻造周边环境事件处理。 */
public class ForgeAmbientHandler {

    public static final int AURA_INTERVAL = 20;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % AURA_INTERVAL != 0) return;

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (mainHand.isEmpty()) return;
        if (!GearForgeData.isInPipeline(mainHand)) return;
        if (GearCategory.classify(mainHand) != GearCategory.WEAPON) return;

        if (player.level() instanceof ServerLevel serverLevel) {
            ForgeVisualEffects.spawnIdleAura(serverLevel, player, mainHand);
        }
    }
}
