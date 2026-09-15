package com.huige233.transcend.tech.research;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;


/** 声明用于访问玩家研究进度的 Forge 能力句柄。 */
public final class ResearchCapabilities {
    public static final Capability<ResearchProgress> PROGRESS =
            CapabilityManager.get(new CapabilityToken<>() {});
    private ResearchCapabilities() {}
}
