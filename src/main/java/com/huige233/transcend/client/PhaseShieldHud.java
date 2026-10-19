package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.network.PhaseShieldClientState;
import com.huige233.transcend.tech.shield.PhaseShieldStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.systems.RenderSystem;

/** 护盾供氧条：护盾存在且有电时持续供氧，并显示护盾电量。 */
@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class PhaseShieldHud {
    private static final ResourceLocation ICONS = Transcend.rl("textures/gui/tech_oxygen.png");
    private PhaseShieldHud() {}

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "phase_shield_oxygen", PhaseShieldHud::render);
    }

    private static void render(ForgeGui forgeGui, GuiGraphics gui, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || !mc.player.isAlive()) return;
        PhaseShieldStatus status = PhaseShieldClientState.status(mc.player.getUUID());
        if (status == PhaseShieldStatus.ABSENT || status == PhaseShieldStatus.INACTIVE) return;
        int right = width / 2 + 91;
        int row = Math.max(forgeGui.leftHeight, forgeGui.rightHeight);
        int y = height - row;
        float charge = PhaseShieldClientState.charge(mc.player.getUUID());
        int bubbles = Math.max(0, Math.min(10, (int) Math.ceil(charge * 10.0F)));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < 10; i++) {
            gui.blit(ICONS, right - i * 8 - 9, y, i < bubbles ? 0 : 9, 0, 9, 9, 32, 16);
        }
        gui.setColor(1, 1, 1, 1);
        Component label = Component.translatable("hud.transcend.phase_shield.oxygen");
        gui.drawString(mc.font, label, right - 83 - mc.font.width(label), y, 0x73E7FF, true);
        RenderSystem.disableBlend();
        forgeGui.rightHeight = row + 10;
    }
}
