package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.items.ManaStorageItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ManaStorageHudOverlay {

    private static float smoothRatio = 0.0F;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;

        Player player = mc.player;

        int totalStored = 0;
        int totalCapacity = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof ManaStorageItem) {
                totalStored += ManaStorageItem.getStoredMana(stack);
                totalCapacity += ManaStorageItem.getMaxMana(stack);
            }
        }

        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.getItem() instanceof ManaStorageItem) {
                totalStored += ManaStorageItem.getStoredMana(stack);
                totalCapacity += ManaStorageItem.getMaxMana(stack);
            }
        }

        if (totalCapacity <= 0) return;

        GuiGraphics graphics = event.getGuiGraphics();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int healthBarTop;
        if (mc.gui instanceof ForgeGui gui) {
            healthBarTop = screenHeight - gui.leftHeight;
        } else {
            healthBarTop = screenHeight - 49;
        }

        int barX = screenWidth / 2 - 41;
        int barY = healthBarTop - 6;
        int barW = 82;
        int barH = 3;

        float targetRatio = (float) totalStored / totalCapacity;
        smoothRatio += (targetRatio - smoothRatio) * 0.1F;

        int fillWidth = Math.round(barW * smoothRatio);

        // Background
        graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A28);
        // Fill
        if (fillWidth > 0) {
            graphics.fill(barX, barY, barX + fillWidth, barY + barH, 0xFF3FA0FF);
        }
        // Border
        graphics.fill(barX - 1, barY - 1, barX + barW + 1, barY, 0xFF2060A0);
        graphics.fill(barX - 1, barY + barH, barX + barW + 1, barY + barH + 1, 0xFF2060A0);
        graphics.fill(barX - 1, barY, barX, barY + barH, 0xFF2060A0);
        graphics.fill(barX + barW, barY, barX + barW + 1, barY + barH, 0xFF2060A0);

        // Text
        String text = "CM: " + totalStored + "/" + totalCapacity;
        int textWidth = mc.font.width(text);
        int textX = screenWidth / 2 - textWidth / 2;
        int textY = barY - mc.font.lineHeight;
        graphics.drawString(mc.font, text, textX, textY, 0xFFB0D0FF, true);
    }
}
