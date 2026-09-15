package com.huige233.transcend.tech.research;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.huige233.transcend.Transcend;


/** 为玩家附加研究进度能力，并在玩家克隆事件中复制研究状态。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID)
public final class ResearchEvents {
    private static final ResourceLocation ID = Transcend.rl("research_progress");
    private ResearchEvents() {}
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<Player> event) {
        event.addCapability(ID, new ResearchProgressProvider());
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        event.getOriginal().getCapability(ResearchCapabilities.PROGRESS).ifPresent(old ->
                event.getEntity().getCapability(ResearchCapabilities.PROGRESS).ifPresent(current -> {
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    old.save(tag); current.load(tag);
                }));
    }
}
