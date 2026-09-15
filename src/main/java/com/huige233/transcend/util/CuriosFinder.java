package com.huige233.transcend.util;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.huige233.transcend.loaded.CuriosLoaded;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;


/** 按物品或条件查询玩家饰品栏，并创建提供急迫、速度和减速清除效果的饰品能力。 */
public class CuriosFinder {
    public static ItemStack findCurio(LivingEntity livingEntity, Predicate<ItemStack> filter){
        ItemStack foundStack = ItemStack.EMPTY;
        if (livingEntity instanceof Player) {
            if (CuriosLoaded.CURIOS.isLoaded()) {
                Optional<SlotResult> slotResult = CuriosApi.getCuriosInventory(livingEntity).map(inv -> inv.findFirstCurio(filter))
                        .orElse(Optional.empty());
                if (slotResult.isPresent()) {
                    foundStack = slotResult.get().stack();
                }
            }
        }

        return foundStack;
    }

    public static List<SlotResult> findCurios(LivingEntity livingEntity, Predicate<ItemStack> filter) {
        if (!(livingEntity instanceof Player) || !CuriosLoaded.CURIOS.isLoaded()) return List.of();
        return CuriosApi.getCuriosInventory(livingEntity)
                .map(handler -> List.copyOf(handler.findCurios(filter)))
                .orElse(List.of());
    }
    public static boolean hasCurio(LivingEntity livingEntity, Predicate<ItemStack> filter){
        return !findCurio(livingEntity, filter).isEmpty();
    }

    public static boolean hasCurio(LivingEntity livingEntity, Item item){
        return !findCurio(livingEntity, item).isEmpty();
    }

    public static ItemStack findCurio(LivingEntity livingEntity, Item item){
        ItemStack foundStack = ItemStack.EMPTY;
        if (livingEntity instanceof Player) {
            if (CuriosLoaded.CURIOS.isLoaded()) {
                Optional<SlotResult> slotResult = CuriosApi.getCuriosInventory(livingEntity).map(inv -> inv.findFirstCurio(item))
                        .orElse(Optional.empty());
                if (slotResult.isPresent()) {
                    foundStack = slotResult.get().stack();
                }
            }
        }

        return foundStack;
    }

    public static ItemStack getFirstItemFromCuriosInv(Player player, Predicate<ItemStack> filter) {
        return CuriosApi.getCuriosInventory(player)
                .map(handler -> handler.findFirstCurio(filter)
                        .map(SlotResult::stack)
                        .orElse(ItemStack.EMPTY))
                .orElse(ItemStack.EMPTY);
    }

    public static ICapabilityProvider getSlowProvider(ItemStack stack) {

        return CuriosApi.createCurioProvider(new ICurio() {

            @Override
            public ItemStack getStack() {
                return stack;
            }

            @Override
            public void curioTick(SlotContext slotContext) {
                LivingEntity entity = slotContext.entity();
                if (entity instanceof Player player && !player.level().isClientSide) {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, -1, 2, false, true));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 2, false, true));

                    List<MobEffectInstance> effects = Lists.newArrayList(player.getActiveEffects());
                    for (MobEffectInstance potion : Collections2.filter(effects, potion ->
                            (potion.getEffect().equals(MobEffects.MOVEMENT_SLOWDOWN) ||
                                    potion.getEffect().equals(MobEffects.DIG_SLOWDOWN)))) {
                        player.removeEffect(potion.getEffect());
                    }
                }
            }

            @Override
            public boolean canEquip(SlotContext slotContext) {
                return true;
            }

            @Override
            public boolean canUnequip(SlotContext slotContext) {
                return true;
            }
        });

    }
}
