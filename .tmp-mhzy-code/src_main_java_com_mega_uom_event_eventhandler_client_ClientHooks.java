package com.mega.uom.event.eventhandler.client;

import com.mega.endinglib.mixin.accessor.AccessorLivingEntityRenderer;
import com.mega.endinglib.util.time.TimeContext;
import com.mega.uom.client.component.FontTextBuilder;
import com.mega.uom.client.render.MyGuiGraphics;
import com.mega.uom.client.render.RendererUtils;
import com.mega.uom.client.render.shader.MegaRenderType;
import com.mega.uom.common.items.FadedTooltipExpends;
import com.mega.uom.event.screen.FeRenderTooltipEvent;
import com.mega.uom.mixin.ClientTextTooltipAccessor;
import com.mega.uom.util.other.ColorUtils;
import com.mega.uom.util.other.MathUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * GUI内z深度越大离屏幕越近
 */
public class ClientHooks {
    public static boolean isFeItemTooltipNow;
    public static int lastTick;
    public static int tick;
    public static void tooltipDsSpItem(FeRenderTooltipEvent.Post event) {
        try {
            GuiGraphics guiGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());
            PoseStack stack = guiGraphics.pose();
            int i = 0;
            int j = event.getComponents().size() == 1 ? -2 : 0;
            for (ClientTooltipComponent clienttooltipcomponent : event.getComponents()) {
                int k = clienttooltipcomponent.getWidth(event.getFont());
                if (k > i)
                    i = k;
                j += clienttooltipcomponent.getHeight();
            }
            int maxTextWidth = i;
            int totalTextHeight = j;
            Vector2ic vector2ic = event.positioner.positionTooltip(event.getGraphics().guiWidth(), event.getGraphics().guiHeight(), event.getX(), event.getY(), maxTextWidth, totalTextHeight);

            {
                Vector2i color = ColorUtils.getBorderColor();
                MyGuiGraphics.renderTooltipBackground(new MyGuiGraphics(event.getGraphics()), vector2ic.x(), vector2ic.y(), maxTextWidth, j - 1, 399.5F, 0xf0100010, 0xf0100010, color.x, color.y);
            }

            stack.pushPose();
            stack.translate(vector2ic.x() + maxTextWidth / 1.5F, vector2ic.y() + totalTextHeight / 2F, 400);
            stack.scale(2.5f * maxTextWidth / 100, 2.5f * maxTextWidth / 100, 2.5f * maxTextWidth / 100);
            stack.mulPose(Axis.ZP.rotationDegrees(RendererUtils.getRenderRotation()));
            Vector4f color1 = ColorUtils.rainbowV4(4936.0F, 0.3F, 2.0F);
            RendererUtils.renderStar(stack, guiGraphics.bufferSource(), new Vector4f(color1.x, color1.y, color1.z, 1F), false);
            stack.popPose();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    static String CTCtoString(ClientTooltipComponent component) {
        if (component instanceof ClientTextTooltip)
            return FontTextBuilder.formattedCharSequenceToString(((ClientTextTooltipAccessor) component).text());
        return "";
    }

    public static int feTooltipExpends(ItemStack stack, ClientTooltipComponent component, List<ClientTooltipComponent> list) {
        int expend = 0;
        if (stack.getItem() instanceof FadedTooltipExpends expends)
            expend = expends.tooltips();
        return expend;
    }

    public static int feTooltipBaseExpends(ItemStack stack, List<ClientTooltipComponent> list) {
        int expend = 2;
        if (stack.getItem() instanceof FadedTooltipExpends expends) {
            if (expends.crossFadedCheck()) return 1 + expends.expends();
            expend = expends.expends();
        }
        return expend;
    }

    public static void tooltipFeItem(FeRenderTooltipEvent.PrePre event) {
        int i = 0;
        int j = event.getComponents().size() == 1 ? -2 : 0;
        int j_ = j;
        int times = 0;
        int expend = feTooltipBaseExpends(event.getItemStack(), event.getComponents());
        for (ClientTooltipComponent clienttooltipcomponent : event.getComponents()) {
            int k = clienttooltipcomponent.getWidth(event.getFont());
            if (k > i)
                i = k;
            j += clienttooltipcomponent.getHeight();
            if (times < (expend + (Screen.hasShiftDown() ? feTooltipExpends(event.getItemStack(), clienttooltipcomponent, event.getComponents()) : 0))) {
                times++;
                j_ += clienttooltipcomponent.getHeight();
            }
        }
        int i2 = i;
        int j2 = j;
        int j3 = j_;
        Vector2ic vector2ic = event.getTooltipPositioner().positionTooltip(event.getScreenWidth(), event.getScreenHeight(), event.getX(), event.getY(), i2, j2);
        PoseStack stack = event.getPoseStack();
        float posX = vector2ic.x() + i2 / 2F - 4;
        float posY = vector2ic.y() + j2 / 2F;
        float scale = 32F;
        float rp7sidesWidth = 5F;
        int scissorX = (int) (posX-rp7sidesWidth*scale) - 1;
        int scissorY = (int) (posY-rp7sidesWidth*scale) - 1;
        float percent = Mth.lerp(TimeContext.Client.alwaysPartial(), lastTick, tick) / 20.0F;
        MyGuiGraphics myGuiGraphics = new MyGuiGraphics(event.getGraphics());
        myGuiGraphics.enableScissor(0, 0, (int) ((event.getScreenWidth() - scissorX) * percent + scissorX), (int) ((event.getScreenHeight() - scissorY)*percent + scissorY));
        myGuiGraphics.drawManaged(() -> {
            {
                Vector2i color = ColorUtils.getBorderColor();
                MyGuiGraphics.renderTooltipBackground(myGuiGraphics, vector2ic.x(), vector2ic.y(), i2, j3 - 1, 399.5F, 0xf0100010, 0xf0100010, color.x, color.y);
            }

            float rotation = (Util.getMillis()) / 39.99F;

            //big triangle
            stack.pushPose();
            stack.translate(posX, posY, 399.2F);
            stack.scale(scale, scale, scale);
            stack.mulPose(Axis.ZP.rotationDegrees(rotation));
            RendererUtils.renderRegularPolygon(stack, Minecraft.getInstance().renderBuffers().bufferSource(), 3.0F, 3, 3.0F, 1, 1F, 1F, 1F, 1F, MegaRenderType.END_PORTAL_TRANSLUCENT_RAW(RendererUtils.star2), false);
            stack.popPose();

            //small triangle 1
            stack.pushPose();
            stack.translate(posX - 109, posY + 15, 399.2F);
            stack.scale(8, 15, 2);
            stack.mulPose(Axis.XP.rotationDegrees(rotation));
            stack.mulPose(Axis.YP.rotationDegrees(rotation * 0.4F));
            RendererUtils.renderRegularPolygon(stack, Minecraft.getInstance().renderBuffers().bufferSource(), 3.0F, 3, 3.0F, 1, 1F, 1F, 1F, 1F, MegaRenderType.END_PORTAL_RAW(RendererUtils.star2), false);
            stack.popPose();

            //small triangle 2
            stack.pushPose();
            stack.translate(posX + 109, posY - 25, 399.2F);
            stack.scale(19, 10, 2);
            stack.mulPose(Axis.XP.rotationDegrees(rotation * 0.7F));
            stack.mulPose(Axis.YP.rotationDegrees(rotation * 1.1F));
            RendererUtils.renderRegularPolygon(stack, Minecraft.getInstance().renderBuffers().bufferSource(), 3.0F, 3, 3.0F, 1, 1F, 1F, 1F, 1F, MegaRenderType.END_PORTAL_RAW(RendererUtils.star2), false);
            stack.popPose();

            //xyz rotation 7 sides
            stack.pushPose();
            stack.translate(posX, posY, 399.1F);
            stack.scale(scale, scale, scale);
            stack.mulPose(Axis.ZP.rotationDegrees((float) (rotation * (1F + Math.PI * 0.15F))));
            stack.mulPose(Axis.YP.rotationDegrees((float) (rotation * (1F + Math.PI * 0.15F))));
            stack.mulPose(Axis.XP.rotationDegrees((float) (rotation * (1F + Math.PI * 0.15F))));
            RendererUtils.renderRegularPolygon(stack, Minecraft.getInstance().renderBuffers().bufferSource(), 5.0F, 7, 0.7F, 1, 1F, 1F, 1F, 1F, MegaRenderType.END_PORTAL_RAW(RendererUtils.star1), false);
            stack.popPose();
            {
                MyGuiGraphics.renderTooltipBackground(myGuiGraphics, vector2ic.x()+3, vector2ic.y()+j3+6+2, i2-6, j2-j3-3, 399.5F, 0x30101010, 0x30101010, 0, 0);
            }
        });
        myGuiGraphics.disableScissor();
    }

    public static BufferBuilder.RenderedBuffer drawStars(BufferBuilder p_234260_, CallbackInfoReturnable<BufferBuilder.RenderedBuffer> cir) {
        RandomSource randomsource = RandomSource.create(10842L);
        RenderSystem.setShaderTexture(0, RendererUtils.star1);
        RenderSystem.setShader(GameRenderer::getRendertypeEndPortalShader);
        p_234260_.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (int i = 0; i < 1500; ++i) {
            double d0 = randomsource.nextFloat() * 2.0F - 1.0F;
            double d1 = randomsource.nextFloat() * 2.0F - 1.0F;
            double d2 = randomsource.nextFloat() * 2.0F - 1.0F;
            double d3 = 0.15F + randomsource.nextFloat() * 0.1F;
            double d4 = d0 * d0 + d1 * d1 + d2 * d2;
            if (d4 < 1.0D && d4 > 0.01D) {
                d4 = 1.0D / Math.sqrt(d4);
                d0 *= d4;
                d1 *= d4;
                d2 *= d4;
                double d5 = d0 * 100.0D;
                double d6 = d1 * 100.0D;
                double d7 = d2 * 100.0D;
                double d8 = Math.atan2(d0, d2);
                double d9 = MathUtils.sin(d8);
                double d10 = MathUtils.cos(d8);
                double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
                double d12 = MathUtils.sin(d11);
                double d13 = MathUtils.cos(d11);
                double d14 = randomsource.nextDouble() * Math.PI * 2.0D;
                double d15 = MathUtils.sin(d14);
                double d16 = MathUtils.cos(d14);

                for (int j = 0; j < 4; ++j) {
                    double d17 = 0.0D;
                    double d18 = (double) ((j & 2) - 1) * d3;
                    double d19 = (double) ((j + 1 & 2) - 1) * d3;
                    double d20 = 0.0D;
                    double d21 = d18 * d16 - d19 * d15;
                    double d22 = d19 * d16 + d18 * d15;
                    double d23 = d21 * d12 + 0.0D * d13;
                    double d24 = 0.0D * d12 - d21 * d13;
                    double d25 = d24 * d9 - d22 * d10;
                    double d26 = d22 * d9 + d24 * d10;
                    p_234260_.vertex(d5 + d25, d6 + d23, d7 + d26)
                            .color(randomsource.nextInt(255), randomsource.nextInt(255), randomsource.nextInt(255), randomsource.nextInt(255))
                            .uv(1F, 1F)
                            .endVertex();
                }
            }
        }
        return (p_234260_.end());
    }

    public static <T extends LivingEntity> void afterLevelBeforeRenderStackModify(PoseStack stack, LivingEntityRenderer<T, ?> renderer, float partial, boolean crouching, T living) {
        if (living.hasPose(Pose.SLEEPING)) {
            Direction direction = living.getBedOrientation();
            if (direction != null) {
                float f4 = living.getEyeHeight(Pose.STANDING) - 0.1F;
                stack.translate((float) (-direction.getStepX()) * f4, 0.0F, (float) (-direction.getStepZ()) * f4);
            }
        }
        AccessorLivingEntityRenderer accessor = (AccessorLivingEntityRenderer) renderer;
        accessor.callSetupRotations(living, stack, accessor.callGetBob(living, partial), Mth.lerp(partial, living.yBodyRotO, living.yBodyRot), partial);
        stack.scale(-1.0F, -1.0F, 1.0F);
        accessor.callScale(living, stack, partial);
        stack.translate(0.0F, -1.501F, 0.0F);

        if (crouching) {
            stack.mulPose(Axis.XP.rotation(.55F));
        }
    }
}
