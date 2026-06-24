package com.huige233.transcend.handle;

import com.huige233.transcend.mixinitf.ITranscendMarked;
import com.huige233.transcend.util.TranscendGuard;
import com.huige233.transcend.util.TranscendInvuln;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TranscendDefenseReassertHandler {

    private static final UUID KNOWN_CRUSH_UUID =
            UUID.fromString("c7a1e2b3-4d5f-6a7b-8c9d-0e1f2a3b4c5d");

    private TranscendDefenseReassertHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        boolean isProtected = TranscendGuard.isProtected(player);
        if (isProtected) {
            restoreCrushed(player, Attributes.MAX_HEALTH);
            restoreCrushed(player, Attributes.ARMOR);
            restoreCrushed(player, Attributes.ARMOR_TOUGHNESS);
        }

        // 通用无敌:读时锁血(换 entityData)+ 同步血量回满(治假死)+ 清死亡标记。
        // 每 tick 都调用:失去保护时把锁血开关关掉,玩家恢复正常可死。
        TranscendInvuln.apply(player, isProtected);

        if (isProtected && player instanceof ITranscendMarked marked && marked.transcend$isMarked()) {
            marked.transcend$unmark();
        }
    }

    private static void restoreCrushed(ServerPlayer player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;

        if (instance.getModifier(KNOWN_CRUSH_UUID) != null) {
            instance.removeModifier(KNOWN_CRUSH_UUID);
        }
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL
                    && modifier.getAmount() <= -1.0) {
                instance.removeModifier(modifier.getId());
            }
        }
    }
}
