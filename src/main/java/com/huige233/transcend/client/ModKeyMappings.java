package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * v3 client integration: keyboard shortcuts for the ascension hub.
 *
 * <p>{@code K} → opens AscensionTreeScreen directly (no need to find the book in inventory).
 * Configurable via vanilla Controls UI; bind conflicts handled by user.
 */
public class ModKeyMappings {

    public static final String CATEGORY = "key.categories.transcend";

    public static final KeyMapping OPEN_ASCENSION = new KeyMapping(
            "key.transcend.open_ascension",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K, // 75 = K
            CATEGORY);

    public static final KeyMapping OPEN_VOWS = new KeyMapping(
            "key.transcend.open_vows",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_J, // 74 = J
            CATEGORY);

    @Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onRegister(RegisterKeyMappingsEvent event) {
            event.register(OPEN_ASCENSION);
            event.register(OPEN_VOWS);
        }
    }

    @Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            // No screen currently shown — only consume in world view
            if (net.minecraft.client.Minecraft.getInstance().screen != null) return;
            while (OPEN_ASCENSION.consumeClick()) {
                AscensionTreeScreen.open();
            }
            while (OPEN_VOWS.consumeClick()) {
                VowSelectionScreen.open();
            }
        }
    }
}
