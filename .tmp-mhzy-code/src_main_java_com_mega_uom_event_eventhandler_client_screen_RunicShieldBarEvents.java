package com.mega.uom.event.eventhandler.client.screen;

import com.mega.endinglib.mixin.accessor.AccessorGuiGraphics;
import com.mega.uom.Config;
import com.mega.uom.common.capability.ModCapabilities;
import com.mega.uom.common.capability.entity.runic.IFeShieldCapability;
import com.mega.uom.common.capability.entity.runic.IRunicShieldCapability;
import com.mega.uom.client.component.MegaFont;
import com.mega.uom.common.items.armor.FantasyEndingArmorItem;
import com.mega.uom.client.render.MyGuiGraphics;
import com.mega.uom.client.render.RendererContext;
import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RunicShieldBarEvents {
    static Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void renderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay().id() == VanillaGuiOverlay.HOTBAR.id()) {
            if (mc.player != null) {
                IRunicShieldCapability capability = ModCapabilities.getCapability(mc.player, ModCapabilities.MAGIC_SHIELD_EC);
                IFeShieldCapability feShieldCapability = ModCapabilities.getCapability(mc.player, ModCapabilities.FE_SHIELD_EC);
                boolean isFeShield = feShieldCapability != null && feShieldCapability.getLevel() == 4936 && FantasyEndingArmorItem.is4Armor(mc.player);
                boolean enable = (capability != null && capability.getLevel() > 0) || (feShieldCapability != null && isFeShield);
                if (enable) {
                    IRunicShieldCapability inst = isFeShield ? feShieldCapability : capability;
                    float hurtPartials = Math.max(0F, inst.hurtTime() - mc.getPartialTick());
                    float cooldownsPartial = Math.max(0F, inst.getCooldowns() - mc.getPartialTick());
                    float scaleWidth = 0.290625F / (RendererContext.RUNIC_SHIELD_FRAME.width() / (float) mc.getWindow().getGuiScaledWidth());
                    float scaleHeight = 0.026392F / (RendererContext.RUNIC_SHIELD_FRAME.height() / (float) mc.getWindow().getGuiScaledHeight());
                    float startX = Config.Client.runic_shield_x;
                    float screenHeight = mc.getWindow().getGuiScaledHeight();
                    float startY = Config.Client.runic_shield_upY + screenHeight - (3 + RendererContext.RUNIC_SHIELD_FRAME.height()) * scaleHeight;
                    MyGuiGraphics guiGraphics = new MyGuiGraphics(event.getGuiGraphics());
                    PoseStack stack = guiGraphics.pose;
                    stack.pushPose();
                    stack.scale(scaleWidth, scaleHeight, 1.0F);
                    String text = Component.translatable(inst.getTranslationKeyName()).getString() + " " + Utils.stringTruncation((inst.maxLifeTime() - inst.tickCount()) / 20.0F, 1) + "s";
                    float textWidth = MegaFont.FantasyFont.width(text);
                    MegaFont.FantasyFont.drawInBatch8xOutline(FormattedCharSequence.forward(text, Style.EMPTY), startX + RendererContext.RUNIC_SHIELD_FRAME.width() / 2F - textWidth / 2F, startY / scaleHeight - MegaFont.FantasyFont.lineHeight, MegaFont.getDarkColor(0x567283), 0x596460, guiGraphics.pose.last().pose(), guiGraphics.bufferSource, 15728880);
                    ((AccessorGuiGraphics) guiGraphics).callFlushIfUnmanaged();
                    stack.popPose();
                    stack.pushPose();
                    stack.translate(startX, startY, 0);
                    stack.scale(scaleWidth, scaleHeight, 1.0F);
                    guiGraphics.blit(RendererContext.RUNIC_SHIELD_FRAME.texture(), 0.0001F, 0.0001F, RendererContext.RUNIC_SHIELD_FRAME.startX(), RendererContext.RUNIC_SHIELD_FRAME.startY(), RendererContext.RUNIC_SHIELD_FRAME.width(), RendererContext.RUNIC_SHIELD_FRAME.height());
                    stack.translate(RendererContext.RUNIC_SHIELD_ENERGY.startX(), (RendererContext.RUNIC_SHIELD_FRAME.height() - RendererContext.RUNIC_SHIELD_ENERGY.height()) / 2F, 0);
                    guiGraphics.blit(RendererContext.RUNIC_SHIELD_ENERGY.texture(), 0.0001F, 0.00001F, RendererContext.RUNIC_SHIELD_ENERGY.startX(), RendererContext.RUNIC_SHIELD_ENERGY.startY(), RendererContext.RUNIC_SHIELD_ENERGY.width(), RendererContext.RUNIC_SHIELD_ENERGY.height(), 256, 256);
                    if (hurtPartials > 0)
                        BossbarEvents.blitHurtBar(guiGraphics.pose(), RendererContext.RUNIC_SHIELD_ENERGY.texture(), 0.0001F, 0.00001F, RendererContext.RUNIC_SHIELD_ENERGY.startX(), RendererContext.RUNIC_SHIELD_ENERGY.startY(), (hurtPartials / 10F) * RendererContext.RUNIC_SHIELD_ENERGY.width(), RendererContext.RUNIC_SHIELD_ENERGY.height(), 256, 256, mc.player.tickCount);
                    if (cooldownsPartial > 0)
                        guiGraphics.blit(RendererContext.RUNIC_SHIELD_ENERGY_COOLDOWN.texture(), 0.0001F, 0.00001F, (float) RendererContext.RUNIC_SHIELD_ENERGY_COOLDOWN.startX(), (float) RendererContext.RUNIC_SHIELD_ENERGY_COOLDOWN.startY(), (cooldownsPartial / inst.getPerCooldowns()) * RendererContext.RUNIC_SHIELD_ENERGY_COOLDOWN.width(), (float) RendererContext.RUNIC_SHIELD_ENERGY_COOLDOWN.height());
                    stack.popPose();
                }
            }
        }
    }
}
