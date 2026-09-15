package com.mega.uom.event.eventhandler.common;

import com.mega.uom.event.item.ItemCraftingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RecipeEventHandler {
    @SubscribeEvent
    public static void onFeBladeCrafting(ItemCraftingEvent event) {
        /*
        if (SafeClass.isSlahbladeLoaded()) {
            ModSource.out(event.getCrafting().getItem());
            ItemStack stack = event.getCrafting();
            if (stack.getItem() instanceof FantasyEndingBlade) {
                FantasyEndingBlade.init0(stack);
            }
        }
         */
    }
}
