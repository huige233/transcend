package com.mega.uom.event.eventhandler.client.screen;

import com.mega.endinglib.mixin.accessor.AccessorClientLevel;
import com.mega.endinglib.util.time.TimeContext;
import com.mega.endinglib.util.time.TimeStopRandom;
import com.mega.endinglib.util.time.TimeStopUtils;
import com.mega.uom.Config;
import com.mega.uom.client.Easing;
import com.mega.uom.client.component.MegaFont;
import com.mega.uom.client.music.UomBossMusicHandler;
import com.mega.uom.common.entity.boss.uom.UomWither;
import com.mega.uom.event.eventhandler.client.ClientHandler;
import com.mega.uom.client.render.entity.layer.UomLayer;
import com.mega.uom.client.render.shader.cosmic.CosmicItemShaders;
import com.mega.uom.util.other.MathUtils;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.UUID;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BossbarEvents {
    private static final RandomSource source = new TimeStopRandom(TimeContext.Client.generateUniqueSeed());
    static Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void bossbar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        if (event.getBossEvent().getName().getString().equals(Component.translatable("entity.fantasy_ending.ultimate_order_manager").getString())) {
            event.setCanceled(true);
            RenderSystem.enableBlend();
            UUID uuid = event.getBossEvent().getId();
            if (mc.level != null && ((AccessorClientLevel) mc.level).getEntityStorage().getEntityGetter().get(uuid) instanceof UomWither wither)
                drawBar(event.getX(), event.getY(), event.getBossEvent(), wither);
            RenderSystem.disableBlend();
            event.setIncrement(14 + MegaFont.FantasyFont.lineHeight);
        }
    }

    static float modifySeenAnim(float posY, Entity boss) {
        float partialTickCount = Mth.lerp(mc.timer.partialTick, boss.tickCount, boss.tickCount + 1);
        if (boss.tickCount < 20) {
            float percent = partialTickCount / 20;
            posY = Easing.OUT_BOUNCE.interpolate(percent, 0F, 1F) * posY - 4 * (1 - percent);
        }
        return posY;
    }

    public static void drawBar(int startX, int startY, BossEvent event, UomWither wither) {
        UomBossMusicHandler.tickExist += 5;
        if (UomBossMusicHandler.tickExist > 5)
            UomBossMusicHandler.tickExist = 5;
        float barWidth = 208.0F;
        float f = (wither.getHealth() / wither.getMaxHealth());
        int i = (int) ((TimeStopUtils.isTimeStop ? f : event.getProgress()) * (barWidth - 2.0F * 2F));
        float yOffset = 6.0F;
        float healthYOffset = 35.0F;
        float renderY = startY - 4;
        renderY = modifySeenAnim(renderY, wither);
        GuiGraphics guiGraphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        PoseStack stack = guiGraphics.pose();
        stack.pushPose();

        //for partial
        stack.translate(0D, renderY, 0D);

        float power = Mth.clamp(wither.getLastHurtDamage() / wither.getMaxDamagePertick(), 0.1F, 2.5F);
        int renderingHurtTime = wither.hurtDuration - wither.hurtTime;
        boolean shake = wither.hurtTime > 0 && renderingHurtTime < (wither.hurtDuration - 3) * power + 3;
        if (wither.finalSkillComing())
            shake = false;
        if (shake) {
            float f0 = power * Mth.lerp(mc.timer.partialTick, wither.hurtTime - 1, wither.hurtTime);
            stack.translate(source.triangle(0, 3) * f0 / 5F, source.triangle(0, 3) * f0 / 5F, 0);
            stack.pushPose();
            stack.translate(source.triangle(0, 11) * f0 / 5F, source.triangle(0, 5) * f0 / 5F, 0);
            guiGraphics.blit(ClientHandler.BAR, startX - 9, 0, 0.0F, yOffset, (int) barWidth, 20, 256, 256);
            stack.translate(source.triangle(0, 11) * f0 / 5F, source.triangle(0, 5) * f0 / 5F, 0);
            guiGraphics.blit(ClientHandler.BAR, startX - 9, 0, 0.0F, yOffset, (int) barWidth, 20, 256, 256);
            stack.translate(source.triangle(0, 10) * f0 / 5F, source.triangle(0, 5) * f0 / 5F, 0);
            guiGraphics.blit(ClientHandler.BAR, startX - 9, 0, 0.0F, yOffset, (int) barWidth, 20, 256, 256);
            stack.popPose();
        }
        guiGraphics.blit(ClientHandler.BAR, startX - 9, 0, 0.0F, yOffset, (int) barWidth, 20, 256, 256);
        if (i > 0) {
            blitCosmicBar(guiGraphics.pose(), ClientHandler.BAR, startX - 7, 2 + (int) yOffset, 0.0F, healthYOffset, i, 5, 256, 256, shake);
            if (shake)
                blitHurtBar(guiGraphics.pose(), ClientHandler.BAR, startX - 7, 2 + (int) yOffset, 0.0F, healthYOffset, i, 5, 256, 256, wither.tickCount);

        }
        Component component = event.getName();
        int l = MegaFont.FantasyFont.width(component);
        int i1 = guiGraphics.guiWidth() / 2 - l / 2;
        int j1 = -5; // equals no tranlateY startY - 9
        guiGraphics.drawString(MegaFont.FantasyFont, component, i1, j1, 16777215);
        if (wither.hurtTime > 0 && wither.getLastHurtDamage() > 0) {
            Component damageComponent = Component.literal(" -" + String.format("%.2f", wither.getLastHurtDamage())).withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC);
            MegaFont.FantasyFont.drawInBatch8xOutline(damageComponent.getVisualOrderText(), i1 + l + 1, j1, 0xF0F0F0, getDarkColor(0x800000), stack.last().pose(), guiGraphics.bufferSource(), 0xF000F0);
        }
        stack.popPose();
        //RenderSystem.setShaderTexture(0, BAR);
        //pPoseStack.blit(BAR, pX, pY, 0.0F, 0.0F, 188, 9, 256, 256);

    }

    static int getDarkColor(int i) {
        double d0 = 0.4D;
        int j = (int) ((double) FastColor.ARGB32.red(i) * d0);
        int k = (int) ((double) FastColor.ARGB32.green(i) * d0);
        int l = (int) ((double) FastColor.ARGB32.blue(i) * d0);
        return FastColor.ARGB32.color(0, j, k, l);
    }

    public static void blitCosmicBar(PoseStack stack, ResourceLocation p_283272_, float p_283605_, float p_281879_, float p_282809_, float p_282942_, int p_281922_, int p_282385_, int p_282596_, int p_281699_, boolean shake) {
        blitCosmicBar(stack, p_283272_, p_283605_, p_281879_, p_281922_, p_282385_, p_282809_, p_282942_, p_281922_, p_282385_, p_282596_, p_281699_, shake);
    }

    public static void blitCosmicBar(PoseStack stack, ResourceLocation p_282034_, float p_283671_, float p_282377_, int p_282058_, int p_281939_, float p_282285_, float p_283199_, int p_282186_, int p_282322_, int p_282481_, int p_281887_, boolean shake) {
        blitCosmicBar(stack, p_282034_, p_283671_, p_283671_ + p_282058_, p_282377_, p_282377_ + p_281939_, 0, p_282186_, p_282322_, p_282285_, p_283199_, p_282481_, p_281887_, shake);
    }

    static void blitCosmicBar(PoseStack stack, ResourceLocation p_282639_, float p_282732_, float p_283541_, float p_281760_, float p_283298_, int p_283429_, int p_282193_, int p_281980_, float p_282660_, float p_281522_, int p_282315_, int p_281436_, boolean shake) {
        blitCosmicBar(stack, p_282639_, p_282732_, p_283541_, p_281760_, p_283298_, p_283429_, (p_282660_ + 0.0F) / (float) p_282315_, (p_282660_ + (float) p_282193_) / (float) p_282315_, (p_281522_ + 0.0F) / (float) p_281436_, (p_281522_ + (float) p_281980_) / (float) p_281436_, shake);
    }

    static void blitCosmicBar(PoseStack stack, ResourceLocation p_283461_, float p_281399_, float p_283222_, float p_283615_, float p_283430_, int p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_, boolean shake) {
        RenderSystem.setShaderTexture(0, p_283461_);
        float yaw = TimeContext.Client.currentSeconds() / 1000F;
        float pitch = TimeContext.Client.currentSeconds() / 1000F;
        float scale = 100F;
        enableCosmicShader(yaw, pitch, scale);
        Matrix4f matrix4f = stack.last().pose();
        Vector4f color = shake ? new Vector4f(1F, 1f, 1f, 2f) : new Vector4f(1F);
        if (shake) {
            mc.gameRenderer.lightTexture().turnOnLightLayer();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        bufferbuilder.vertex(matrix4f, p_281399_, p_283615_, (float) p_281729_).color(color.x, color.y, color.z, color.w).uv(p_283247_, p_282883_).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferbuilder.vertex(matrix4f, p_281399_, p_283430_, (float) p_281729_).color(color.x, color.y, color.z, color.w).uv(p_283247_, p_283017_).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferbuilder.vertex(matrix4f, p_283222_, p_283430_, (float) p_281729_).color(color.x, color.y, color.z, color.w).uv(p_282598_, p_283017_).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferbuilder.vertex(matrix4f, p_283222_, p_283615_, (float) p_281729_).color(color.x, color.y, color.z, color.w).uv(p_282598_, p_282883_).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
        if (shake) {
            RenderSystem.defaultBlendFunc();
            mc.gameRenderer.lightTexture().turnOffLightLayer();
        }
    }

    static void enableCosmicShader(float yaw, float pitch, float scale) {

        CosmicItemShaders.cosmicTime.set(Util.getMillis() / (100F / (float) Config.Client.cosmic_speed_multiplier));
        CosmicItemShaders.cosmicYaw.set(yaw);
        CosmicItemShaders.cosmicPitch.set(pitch);
        CosmicItemShaders.cosmicExternalScale.set(scale);
        CosmicItemShaders.cosmicOpacity.set(1.0F);
        CosmicItemShaders.cosmicShader.setCosmicIcon();
        CosmicItemShaders.cosmicShader.apply();
        RenderSystem.setShader(() -> CosmicItemShaders.cosmicShader);

    }

    public static void blitHurtBar(PoseStack stack, ResourceLocation p_283272_, float p_283605_, float p_281879_, float p_282809_, float p_282942_, float p_281922_, float p_282385_, float p_282596_, float p_281699_, float tick) {
        blitHurtBar(stack, p_283272_, p_283605_, p_281879_, p_281922_, p_282385_, p_282809_, p_282942_, p_281922_, p_282385_, p_282596_, p_281699_, tick);
    }

    public static void blitHurtBar(PoseStack stack, ResourceLocation p_282034_, float p_283671_, float p_282377_, float p_282058_, float p_281939_, float p_282285_, float p_283199_, float p_282186_, float p_282322_, float p_282481_, float p_281887_, float tick) {
        blitHurtBar(stack, p_282034_, p_283671_, p_283671_ + p_282058_, p_282377_, p_282377_ + p_281939_, 0, p_282186_, p_282322_, p_282285_, p_283199_, p_282481_, p_281887_, tick);
    }

    static void blitHurtBar(PoseStack stack, ResourceLocation p_282639_, float p_282732_, float p_283541_, float p_281760_, float p_283298_, float p_283429_, float p_282193_, float p_281980_, float p_282660_, float p_281522_, float p_282315_, float p_281436_, float tick) {
        blitHurtBar(stack, p_282639_, p_282732_, p_283541_, p_281760_, p_283298_, p_283429_, (p_282660_ + 0.0F) / p_282315_, (p_282660_ + p_282193_) / p_282315_, (p_281522_ + 0.0F) / p_281436_, (p_281522_ + p_281980_) / p_281436_, tick);
    }

    static void blitHurtBar(PoseStack stack, ResourceLocation p_283461_, float p_281399_, float p_283222_, float p_283615_, float p_283430_, float p_281729_, float p_283247_, float p_282598_, float p_282883_, float p_283017_, float tickCount) {
        RenderSystem.setShaderTexture(0, p_283461_);
        float f = tickCount + mc.timer.partialTick;
        Matrix4f matrix4f = stack.last().pose();
        Vector4f color = new Vector4f(0F, 1f, 1f, 0.2f);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        RenderType rendertype = RenderType.energySwirl(UomLayer.getPreparationTimeStopTextureLocation(), MathUtils.cos(f * 0.02F) * 3.0F % 1.0F, f * 0.01F % 1.0F);
        VertexConsumer bufferbuilder = bufferSource.getBuffer(rendertype);
        bufferbuilder.vertex(matrix4f, p_281399_, p_283615_, p_281729_).color(color.x, color.y, color.z, color.w).uv(p_283247_, p_282883_).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferbuilder.vertex(matrix4f, p_281399_, p_283430_, p_281729_).color(color.x, color.y, color.z, color.w).uv(p_283247_, p_283017_ * 2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferbuilder.vertex(matrix4f, p_283222_, p_283430_, p_281729_).color(color.x, color.y, color.z, color.w).uv(p_282598_ * 3, p_283017_ * 2).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferbuilder.vertex(matrix4f, p_283222_, p_283615_, p_281729_).color(color.x, color.y, color.z, color.w).uv(p_282598_ * 3, p_282883_).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0).normal(0f, 0f, 0f).endVertex();
        bufferSource.endBatch(rendertype);
    }
}
