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
    @SubscribeEvent public static void attach(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (!(event.getObject() instanceof Player)) return;
        ResearchProgressProvider provider = new ResearchProgressProvider();
        event.addCapability(ID, provider);
        event.addListener(provider::invalidate);
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) event.getOriginal().reviveCaps();
        try {
        event.getOriginal().getCapability(ResearchCapabilities.PROGRESS).ifPresent(old ->
                event.getEntity().getCapability(ResearchCapabilities.PROGRESS).ifPresent(current -> {
                    net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                    old.save(tag); current.load(tag);
                }));
        } finally {
            if (event.isWasDeath()) event.getOriginal().invalidateCaps();
        }
        applyRewards(event.getEntity());
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { applyRewards(event.getEntity()); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { applyRewards(event.getEntity()); }
    private static void applyRewards(Player player) {
        player.getCapability(ResearchCapabilities.PROGRESS).ifPresent(progress -> ResearchUnlocks.applyKnowledge(player, progress.completed()));
    }
}
