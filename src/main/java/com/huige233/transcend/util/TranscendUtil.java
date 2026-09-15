package com.huige233.transcend.util;

import com.huige233.transcend.init.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Function;
import java.util.function.Predicate;

   
                           
                                  
   
/** 驱动受保护玩家的濒死救援和无敌计时，并统一查找背包或饰品中的目标物品。 */
@Mod.EventBusSubscriber(modid = "transcend", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TranscendUtil {

    
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) return;
        reviveCheck(player);
        TranscendDefense.tickInvulnerable(player);
    }

    
    public static void reviveCheck(Player player) {
        Level level = player.level();
        if (level.isClientSide) return;
        if (TranscendGuard.isProtected(player)
                && (player.isDeadOrDying() || player.getHealth() <= 1.0F)) {
            player.setHealth(player.getMaxHealth());
            if (player.getMaxHealth() > 0) {
                level.broadcastEntityEvent(player, (byte) 239);
                TranscendDefense.grantReviveInvulnerable(player);
            }
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
