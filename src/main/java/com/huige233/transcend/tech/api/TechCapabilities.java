package com.huige233.transcend.tech.api;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;


/** 声明科技能量能力句柄并将长整型能量接口注册到 Forge 能力系统。 */
public final class TechCapabilities {
    public static final Capability<ITechEnergy> TECH_ENERGY = CapabilityManager.get(new CapabilityToken<>() {
    });

    private TechCapabilities() {
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(ITechEnergy.class);
    }
}
