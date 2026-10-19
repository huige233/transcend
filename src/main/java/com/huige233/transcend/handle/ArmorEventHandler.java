package com.huige233.transcend.handle;

import com.huige233.transcend.items.TranscendShield;
import com.huige233.transcend.items.tools.TranscendSword;
import com.huige233.transcend.init.ModItems;
import com.huige233.transcend.combat.protection.TranscendProtectionEngine;
import com.huige233.transcend.util.ArmorUtils;
import com.huige233.transcend.util.SwordUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;

/** 处理超越套装的伤害与死亡防护、受击反制及玩家死亡后的背包和装备保留。 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)

public class ArmorEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onArmorHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (TranscendProtectionEngine.isProtected(player) && ArmorUtils.fullEquipped(player)) {
                Entity source = event.getSource().getEntity();
                if (source instanceof LivingEntity living && !(source instanceof Player)) {
                    SwordUtil.annihilate(living, player);
                }
            }
        }
    }

    private static boolean isProtectedItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.transcend_helmet.get()
                || item == ModItems.transcend_chestplate.get()
                || item == ModItems.transcend_leggings.get()
                || item == ModItems.transcend_boots.get()
                || item instanceof TranscendSword
                || item instanceof TranscendShield;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TranscendProtectionEngine.isTerminal(player)) return;

        if (com.huige233.transcend.util.TranscendGuard.isProtected(player)) {
            event.setCanceled(true);
            return;
        }

        Iterator<ItemEntity> iter = event.getDrops().iterator();
        while (iter.hasNext()) {
            ItemEntity drop = iter.next();
            ItemStack stack = drop.getItem();
            if (isProtectedItem(stack)) {
                iter.remove();
                player.getInventory().add(stack.copy());
            }
        }
    }

    @SubscribeEvent
    public static void onCloneRestoreItems(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        if (TranscendProtectionEngine.isTerminal(oldPlayer)) return;

        if (com.huige233.transcend.util.TranscendGuard.isProtected(oldPlayer)) {
            for (int i = 0; i < oldPlayer.getInventory().items.size(); i++) {
                newPlayer.getInventory().items.set(i, oldPlayer.getInventory().items.get(i).copy());
            }
            for (int i = 0; i < oldPlayer.getInventory().armor.size(); i++) {
                newPlayer.getInventory().armor.set(i, oldPlayer.getInventory().armor.get(i).copy());
            }
            for (int i = 0; i < oldPlayer.getInventory().offhand.size(); i++) {
                newPlayer.getInventory().offhand.set(i, oldPlayer.getInventory().offhand.get(i).copy());
            }
            return;
        }

        for (int i = 0; i < oldPlayer.getInventory().items.size(); i++) {
            ItemStack stack = oldPlayer.getInventory().items.get(i);
            if (isProtectedItem(stack)) newPlayer.getInventory().items.set(i, stack.copy());
        }
        for (int i = 0; i < oldPlayer.getInventory().armor.size(); i++) {
            ItemStack stack = oldPlayer.getInventory().armor.get(i);
            if (isProtectedItem(stack)) newPlayer.getInventory().armor.set(i, stack.copy());
        }
        if (!oldPlayer.getInventory().offhand.isEmpty()
                && isProtectedItem(oldPlayer.getInventory().offhand.get(0))) {
            newPlayer.getInventory().offhand.set(0, oldPlayer.getInventory().offhand.get(0).copy());
        }
    }
}
