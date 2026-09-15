package com.mega.uom.event.eventhandler.client;

import com.mega.uom.client.key.KeyRegistry;
import com.mega.uom.common.items.combat.ShockwaveData;
import com.mega.uom.common.items.combat.ShockwaveWeapon;
import com.mega.uom.common.network.PacketHandler;
import com.mega.uom.common.network.c2s.item.CMergeHandItemTagPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ShockwaveChangeHandler {
    static Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void onKeyboard(InputEvent.Key event) {
        if (mc.screen == null && event.getAction() == GLFW.GLFW_PRESS && event.getKey() == KeyRegistry.CHANGE_SHOCKWAVE.getKey().getValue()) {
            if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof ShockwaveWeapon weapon) {
                int nextIndex = weapon.getSWType(mc.player.getMainHandItem()).getIndex() + 1;
                if (nextIndex > ShockwaveData.size() - 1)
                    nextIndex = 0;
                PacketHandler.sendToServer(new CMergeHandItemTagPacket.IntegerPacket(
                        true,
                        mc.player.getId(),
                        weapon.getSWDataName(),
                        nextIndex));
                weapon.changeType(mc.player.getMainHandItem(), mc.player, ShockwaveData.MAPPING.get(nextIndex));
            }
        }
    }
}
