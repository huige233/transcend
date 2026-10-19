package com.huige233.transcend.handle;

import com.huige233.transcend.util.TranscendGuard;
import com.huige233.transcend.combat.protection.TranscendAttributeGuard;
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

/** 在玩家刻结束时清除受保护玩家的属性压制修饰符并重新施加无敌防护。 */
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
            TranscendAttributeGuard.enforce(player);
            restoreCrushed(player, Attributes.MAX_HEALTH);
            restoreCrushed(player, Attributes.ARMOR);
            restoreCrushed(player, Attributes.ARMOR_TOUGHNESS);
        }

        TranscendInvuln.apply(player, isProtected);

    }

    /** 保护状态必须覆盖任意实体 tick，不只覆盖玩家事件。 */
    @SubscribeEvent
    public static void onLivingTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (!(event.level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        for (var entity : serverLevel.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living
                    && TranscendGuard.isProtected(living)) {
                TranscendAttributeGuard.enforce(living);
                TranscendInvuln.apply(living, true);
            }
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
