package com.huige233.transcend.util;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.huige233.transcend.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class TranscendUtil {
    public Player player;
    private boolean transcendplayer;

    public TranscendUtil(Player player){
        this.player = player;
    }

    public void tick(){
        Level level = player.level();
        if(!level.isClientSide) {
            this.transcendplayer = CuriosFinder.hasCurio(player, ModItems.transcend_curio.get());
            reviveCheck();
            CleanDebuffs();
            DecreaseCooldowns();
            if (!player.isSpectator()) baseTick(level);
            CleanDebuffs();
        }
    }


    public void reviveCheck() {
        Level level = player.level();
        if(!level.isClientSide){
            if((transcendplayer || ArmorUtils.fullEquipped(player)) && player.isDeadOrDying()){
                this.player.setHealth(this.player.getMaxHealth());
                if(this.player.getMaxHealth() > 0){
                    level.broadcastEntityEvent(player,(byte) 239);
                }
            }
        }
    }

    private void baseTick(Level level){
        if(!level.isClientSide){
            if (transcendplayer) {
                player.setAirSupply(300);
                player.getFoodData().setFoodLevel(20);
                player.getFoodData().setSaturation(20.0f);
                MobEffectInstance nv = player.getEffect(MobEffects.NIGHT_VISION);
                if(nv == null) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,600,0,false,false));
                if(!player.getAbilities().mayfly){
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                }
                player.getPersistentData().putBoolean("transcend_curio_flight", true);
            } else if (player.getPersistentData().getBoolean("transcend_curio_flight")) {
                player.getPersistentData().remove("transcend_curio_flight");
                if (!player.isCreative() && !player.isSpectator()
                        && !ArmorUtils.fullEquipped(player)) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    public void CleanDebuffs(){
        if(!this.player.level().isClientSide){
            List<MobEffectInstance> effects = Lists.newArrayList(player.getActiveEffects());
            for (MobEffectInstance potion : Collections2.filter(effects, potion -> !potion.getEffect().isBeneficial())) {
                player.removeEffect(potion.getEffect());
            }
        }
    }

    public void DecreaseCooldowns(){
        if(!this.player.level().isClientSide){
            Inventory inventory = player.getInventory();
            inventory.items.forEach(stack -> {if(!stack.isEmpty()){cleanCooldowns(player,stack.getItem());}});
            inventory.armor.forEach(stack -> {if (!stack.isEmpty()) {cleanCooldowns(player, stack.getItem());}});
            inventory.offhand.forEach(stack -> {if (!stack.isEmpty()) {cleanCooldowns(player, stack.getItem());}});
            if(player.tickCount % 5 == 0){
                CuriosApi.getCuriosInventory(player).ifPresent(iCuriosItemHandler -> iCuriosItemHandler.getCurios().values().forEach(iCurioStacksHandler -> {
                    for (int i = iCurioStacksHandler.getSlots() - 1; i >= 0; i--) {
                        ItemStack stack = iCurioStacksHandler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty()) cleanCooldowns(player,stack.getItem());
                    }
                }));
            }
        }
    }

    public static void cleanCooldowns(Player player, Item item){
        ItemCooldowns cooldowns = player.getCooldowns();
        if(cooldowns.isOnCooldown(item)){
            cooldowns.removeCooldown(item);
        }
    }

    public void heal(float amount, CallbackInfo ci) {
        if (transcendplayer) {
            float afterEvent = net.minecraftforge.event.ForgeEventFactory.onLivingHeal(player, amount);
            if (afterEvent > amount)
                amount = afterEvent;
            float f = player.getHealth();
            if (f > 0.0F)
                player.setHealth(f + amount);
            ci.cancel();
        }
    }

    public static ItemStack getPlayerTotemItem(Player player){
        return findItemInInv(player, stack -> stack.is(ModItems.thelasttotem.get()),stack -> stack);
    }

    public static ItemStack findItemInInv(Player player, Predicate<ItemStack> is, Function<ItemStack, ItemStack> map){
        if (ModList.get().isLoaded("curios")) {
            ItemStack resultStack = CuriosFinder.getFirstItemFromCuriosInv(player, is);
            if (!resultStack.isEmpty()) return map.apply(resultStack);
        }
        if (is.test(player.getMainHandItem())) return map.apply(player.getMainHandItem());
        if (is.test(player.getOffhandItem())) return map.apply(player.getOffhandItem());
        Inventory inv = player.getInventory();
        int size = inv.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack s = inv.getItem(i);
            if (is.test(s)) {return map.apply(s);}
        }
        return ItemStack.EMPTY;

    }

    public static ICapabilityProvider createCurioProvider(ItemStack stack, CompoundTag unused) {
        if (ModList.get().isLoaded("curios")) {
            return CuriosFinder.getSlowProvider(stack);
        }
        return null;
    }
}
