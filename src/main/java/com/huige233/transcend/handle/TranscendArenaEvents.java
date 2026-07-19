package com.huige233.transcend.handle;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.world.TranscendDimensions;
import com.huige233.transcend.world.arena.TranscendArenaManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TranscendArenaEvents {

    private TranscendArenaEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.dimension() == TranscendDimensions.ARENA_LEVEL) {
            TranscendArenaManager.onChunkLoad(serverLevel, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer sp)) {
            return;
        }
        TranscendArenaManager.tickArenaPlayer(sp);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!TranscendArenaManager.shouldProtectBlock(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (event.getPlayer() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("msg.transcend.arena_block_protected")
                    .withStyle(ChatFormatting.RED));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!TranscendArenaManager.shouldProtectBlock(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("msg.transcend.arena_block_protected")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
