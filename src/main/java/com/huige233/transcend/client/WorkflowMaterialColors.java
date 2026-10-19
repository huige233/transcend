package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.init.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WorkflowMaterialColors {
    @SubscribeEvent public static void register(RegisterColorHandlersEvent.Item event) {
        for (var item : ModItems.WORKFLOW_MATERIALS)
            // Textures now contain their finished palettes, including neutral metal edges.
            // Applying the old full-sprite tint would multiply and muddy those colors.
            event.register((stack, tint) -> 0xffffff, item.get());
    }
}
