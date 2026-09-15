package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.tech.ParticleGun;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** 定义相位飞行切换与粒子枪装填界面的按键绑定，并组织注册和输入事件处理。 */
public class ModKeyMappings {
    public static final String CATEGORY = "key.categories.transcend";

    public static final KeyMapping TOGGLE_PHASE_FLIGHT = new KeyMapping(
            "key.transcend.toggle_phase_flight", InputConstants.Type.KEYSYM, InputConstants.KEY_G, CATEGORY);
    public static final KeyMapping OPEN_GUN_AMMO = new KeyMapping(
            "key.transcend.open_gun_ammo", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORY);

    /** 在客户端模组事件总线上注册相位飞行切换和粒子枪装填按键。 */
    @Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_PHASE_FLIGHT);
            event.register(OPEN_GUN_AMMO);
        }
    }

    /** 在客户端刻结束时消费按键输入，发送相位切换请求或打开主手粒子枪装填界面。 */
    @Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen != null) return;

            while (TOGGLE_PHASE_FLIGHT.consumeClick()) {
                com.huige233.transcend.network.C2STogglePhaseFlight.send();
            }
            while (OPEN_GUN_AMMO.consumeClick()) {
                if (minecraft.player != null && !minecraft.player.isUsingItem()
                        && minecraft.player.getMainHandItem().getItem() instanceof ParticleGun) {
                    GunAmmoScreen.open();
                }
            }
        }
    }
}
