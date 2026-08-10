package com.huige233.transcend.mana;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

/** 魔力处理器 Capability 封装。 */
public class ManaHandlerCapability {
    public static final Capability<IManaHandler> MANA_HANDLER =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IManaHandler.class);
    }
}
