package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.handle.NetworkHandler;
import com.huige233.transcend.items.SpellBookItem;
import com.huige233.transcend.network.C2SSpellBookSlotChange;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SpellBookScrollHandler {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        Player player = mc.player;
        if (!player.isShiftKeyDown()) return;

        ItemStack stack;
        SpellBookItem book;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof SpellBookItem b) {
            stack = main;
            book = b;
        } else {
            ItemStack off = player.getOffhandItem();
            if (!(off.getItem() instanceof SpellBookItem b2)) return;
            stack = off;
            book = b2;
        }
        if (book.getUsedSlots(stack) <= 0) return;

        double scrollDelta = event.getScrollDelta();
        if (scrollDelta == 0) return;
        int delta = scrollDelta > 0 ? -1 : 1;

        event.setCanceled(true);
        NetworkHandler.CHANNEL.sendToServer(new C2SSpellBookSlotChange(delta));
    }
}
