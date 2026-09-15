package com.mega.uom.event.eventhandler.client.screen;

import com.mega.endinglib.mixin.accessor.AccessorGuiGraphics;
import com.mega.endinglib.util.time.TimeContext;
import com.mega.uom.client.Easing;
import com.mega.uom.event.ClientProgramTickEvent;
import com.mega.uom.client.render.MyGuiGraphics;
import com.mega.uom.util.other.ColorUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2i;

import java.util.LinkedList;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChangeInfoHandler {
    static final LinkedList<InfoInstance> infoList = new LinkedList<>();
    public static float startY = 100F;
    /**
     * 信息框右下角X坐标初始值
     */
    public static float endX = 0F;
    static int lastSize;
    static int size;
    static Minecraft mc = Minecraft.getInstance();

    @SubscribeEvent
    public static void renderAbilityTooltip(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type())
            if (!infoList.isEmpty())
                synchronized (infoList) {
                    if (!infoList.isEmpty()) {
                        MyGuiGraphics guiGraphics = new MyGuiGraphics(Minecraft.getInstance(), event.getGuiGraphics().pose(), event.getGuiGraphics().bufferSource());
                        guiGraphics.pose.pushPose();
                        int scaleCount = (int) (mc.getWindow().getGuiScaledHeight() / 1.5F / (6 + mc.font.lineHeight));
                        float partialSize = Mth.lerp(TimeContext.Client.alwaysPartial(), lastSize, size);
                        if (infoList.size() > scaleCount)
                            guiGraphics.pose.scale((float) scaleCount / partialSize, (float) scaleCount / partialSize, 1.0F);
                        {
                            for (InfoInstance infoInstance : infoList)
                                if (infoInstance != null) infoInstance.draw(guiGraphics);
                        }
                        guiGraphics.pose.popPose();
                    }
                }

    }

    @SubscribeEvent
    public static void tickingInfoInstances(ClientProgramTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (mc.player != null && mc.level != null) {
            lastSize = size;
            size = infoList.size();
            synchronized (infoList) {
                if (!infoList.isEmpty())
                    for (InfoInstance infoInstance : infoList)
                        if (infoInstance != null) infoInstance.tick();
            }
        }
    }

    public static void addInfo(Component text, Font font) {
        InfoInstance infoInstance = new InfoInstance(text, font);
        infoInstance.add();
    }

    public static void addInfo(Component text) {
        addInfo(text, mc.font);
    }

    static class InfoInstance {
        public final Font font;
        public final Component text;
        /**
         * 存在时间最大为70ticks
         */
        public final int LIFE = 100;
        /**
         * 开始结束渲染动画时间占比
         */
        public final float beginningAnimationPart = 0.2F;
        public int widthOfText = -1;
        public int tickCount = 0;
        private boolean added;
        /**
         * (上一刻)信息框右下角Y坐标
         */
        private float endYOld = 0F;
        /**
         * 信息框右下角Y坐标
         */
        private float endY = 0F;
        /**
         * 信息框X轴偏移
         */
        private float xOffset = 0F;

        public InfoInstance(Component text, Font font) {
            this.text = text;
            this.font = font;
            this.widthOfText = font.width(text.getString());
        }

        public int getIndex() {
            return this.added ? infoList.indexOf(this) : -1;
        }

        public float partialTicks() {
            return Mth.lerp(TimeContext.Client.alwaysPartial(), tickCount, Math.min(tickCount + 1, LIFE));
        }

        public float getXOffset() {
            float percent = partialTicks() / LIFE;
            if (widthOfText == -1)
                widthOfText = this.font.width(text.getString());
            if (percent < beginningAnimationPart) {
                //开始
                this.xOffset = Easing.OUT_CUBIC.interpolate((percent / beginningAnimationPart), 0F, 1F) * widthOfText;
            }
            if (percent >= (1.0F - beginningAnimationPart)) {
                //结束

                float f = (Easing.INVERSE_OUT_CUBIC.interpolate((percent - (1.0F - beginningAnimationPart)) / beginningAnimationPart, 0, 1));
                this.xOffset = f * widthOfText;
            }
            if (percent >= beginningAnimationPart && percent < 1.0F - beginningAnimationPart) {
                //中间
                this.xOffset = widthOfText;
            }
            return this.xOffset;
        }

        public float getYOffset() {
            return Mth.lerp(TimeContext.Client.alwaysPartial(), endYOld, endY);
        }

        public Component getText() {
            return text;
        }

        public void tick() {
            if (!added) {
                synchronized (infoList) {
                    infoList.remove(this);
                }
                tickCount = 0;
                return;
            }
            if (tickCount < LIFE)
                tickCount++;
            if (tickCount >= LIFE) remove();
            endYOld = endY;
            // 6 : tooltip增加的宽度
            endY = this.getIndex() * (font.lineHeight + 6);
        }

        public void add() {
            synchronized (infoList) {
                if (!added) {
                    infoList.addFirst(this);
                    this.added = true;
                }
            }
        }

        public void remove() {
            if (added) {
                synchronized (infoList) {
                    infoList.remove(this);
                }
                this.added = false;
            }
        }

        public void draw(MyGuiGraphics guiGraphics) {
            float xoffset = getXOffset();
            float x = endX + xoffset - widthOfText;
            float y = startY + getYOffset() - font.lineHeight;
            PoseStack poseStack = guiGraphics.pose;
            poseStack.pushPose();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, xoffset / (float) widthOfText);
            guiGraphics.drawManaged(() -> {
                MyGuiGraphics.BACKGROUND_COLOR = 0;
                Vector2i color = ColorUtils.getBorderColor();
                MyGuiGraphics.BORDER_COLOR_TOP = color.x;
                MyGuiGraphics.BORDER_COLOR_BOTTOM = color.y;
                MyGuiGraphics.renderTooltipBackground(guiGraphics, x * (1F + 3F / (widthOfText + 6)), y, widthOfText, font.lineHeight, 400);
                MyGuiGraphics.BACKGROUND_COLOR = -267386864;
                MyGuiGraphics.BORDER_COLOR_TOP = 1347420415;
                MyGuiGraphics.BORDER_COLOR_BOTTOM = 1344798847;
            });
            guiGraphics.pose.translate(0D, 0D, 400D);
            mc.font.drawInBatch(text, x, y, 0xFFFFFFFF, true, guiGraphics.pose.last().pose(), guiGraphics.bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);
            ((AccessorGuiGraphics) guiGraphics).callFlushIfUnmanaged();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    }
}
