package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.tech.GunConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


/** 在第一人称持有带瞄具的枪并潜行时临时缩小视野，实现瞄准放大效果。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT)
public final class GunScopeHandler {
    private GunScopeHandler() {}

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!event.usedConfiguredFov() || minecraft.player == null || minecraft.screen != null
                || !minecraft.options.getCameraType().isFirstPerson()
                || event.getCamera().getEntity() != minecraft.player) return;
        if (minecraft.player.isShiftKeyDown() && GunConfig.hasScope(minecraft.player.getMainHandItem())) {
            event.setFOV(event.getFOV() * 0.5D);
        }
    }
}
