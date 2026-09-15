package com.mega.uom.coremod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class EventUtil {
    public static void blitWrapper(GuiGraphics guiGraphics, ResourceLocation p_283377_, int p_281970_, int p_282111_, int p_283134_, int p_282778_, int p_281478_, int p_281821_) {
        guiGraphics.blit(p_283377_, p_281970_, p_282111_, 0, p_283134_, p_282778_, p_281478_, p_281821_, 288, 288);
    }
}
