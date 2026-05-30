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

/**
 * R78: 法术书 Shift+滚轮切换 active slot —— 模仿 Iron's Spells & Spellbooks。
 *
 * <p>触发条件（全部满足）：
 * <ul>
 *   <li>玩家未打开任何 GUI（in-world 视图）</li>
 *   <li>主手 OR 副手持 SpellBookItem</li>
 *   <li>书内已写入至少 1 个法术（active_slot 才有意义）</li>
 *   <li>玩家按住 Shift</li>
 * </ul>
 *
 * <p>滚轮 ↑（scrollDelta &gt; 0）→ delta = -1（前一个法术）<br>
 * 滚轮 ↓（scrollDelta &lt; 0）→ delta = +1（后一个法术）
 *
 * <p>触发时取消事件（{@link InputEvent.MouseScrollingEvent#setCanceled})，避免
 * 同时触发原版 hotbar 滚动。然后发 {@link C2SSpellBookSlotChange} 让服务端写回 NBT。
 */
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

        // 滚轮上 = 前一个；滚轮下 = 后一个
        double scrollDelta = event.getScrollDelta();
        if (scrollDelta == 0) return;
        int delta = scrollDelta > 0 ? -1 : 1;

        event.setCanceled(true);
        NetworkHandler.CHANNEL.sendToServer(new C2SSpellBookSlotChange(delta));
    }
}
