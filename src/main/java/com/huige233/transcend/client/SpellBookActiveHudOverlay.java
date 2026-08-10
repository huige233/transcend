package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.SpellBookItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
/** 法术书激活状态 HUD 覆盖层。 */
public class SpellBookActiveHudOverlay {

    private static final int RIGHT_PADDING = 4;
    private static final int TOP_PADDING = 4;
    private static final int LINE_GAP = 2;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.isSpectator()) return;

        Player player = mc.player;
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

        int used = book.getUsedSlots(stack);
        if (used <= 0) return;

        int active = book.getActiveSlot(stack);
        CompoundTag slotData = book.getSlotData(stack, active);
        if (slotData == null) return;

        SpellBookItem.BookTier tier = book.getTier();

        String header = "📖 " + tier.name() + " (" + (active + 1) + "/" + used + ")";
        Component headerComp = Component.literal(header).withStyle(ChatFormatting.LIGHT_PURPLE);

        String carrier = slotData.getString("carrier");
        String element = slotData.getString("element");
        String effect = slotData.getString("effect");
        StringBuilder sb = new StringBuilder("✦ ");
        sb.append(carrier).append(" / ").append(element);
        if (!effect.isEmpty()) sb.append(" +").append(effect);
        Component spellComp = Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA);

        Component hintComp = Component.translatable("hud.transcend.spellbook.scroll_hint")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int line = mc.font.lineHeight;

        int y = TOP_PADDING;
        int xHeader = screenW - RIGHT_PADDING - mc.font.width(headerComp);
        gfx.drawString(mc.font, headerComp, xHeader, y, 0xFFFFFFFF, true);

        y += line + LINE_GAP;
        int xSpell = screenW - RIGHT_PADDING - mc.font.width(spellComp);
        gfx.drawString(mc.font, spellComp, xSpell, y, 0xFFFFFFFF, true);

        y += line + LINE_GAP;
        int xHint = screenW - RIGHT_PADDING - mc.font.width(hintComp);
        gfx.drawString(mc.font, hintComp, xHint, y, 0xFFFFFFFF, true);
    }
}
