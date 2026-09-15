package com.mega.uom.event.eventhandler.client.screen;

import com.mega.endinglib.mixin.accessor.AccessorGuiGraphics;
import com.mega.endinglib.util.time.TimeContext;
import com.mega.uom.client.component.MegaFont;
import com.mega.uom.event.ClientProgramTickEvent;
import com.mega.uom.common.items.misc.cb.BoneTrigger;
import com.mega.uom.common.items.misc.cb.CreativeBoneItem;
import com.mega.uom.proxy.ClientProxy;
import com.mega.uom.client.render.MyGuiGraphics;
import com.mega.uom.util.other.ColorUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedList;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CreativeBoneHandler {
    public static final int APPEAR_DISAPPEAR_TIME = 15;
    public static int ticks = 0;
    public static int lastTicks = 0;
    public static int maxSize = 8;
    public static ItemStack selected;
    public static float abilityWidth = -1F;
    public static float abilityHeight = -1F;
    static Minecraft mc = Minecraft.getInstance();

    static float partialTicks() {
        return Mth.lerp(TimeContext.Client.alwaysPartial(), lastTicks, ticks);
    }

    static float normalizePartial() {
        return partialTicks() / APPEAR_DISAPPEAR_TIME;
    }

    public static float getAbilityComponent(Font font) {
        if (abilityWidth == -1F) {
            float width = 0F;
            for (Component component : ClientProxy.abilities.keySet()) {
                if (width < font.width(component))
                    width = font.width(component);
            }
            abilityWidth = width;
        }
        return abilityWidth;
    }

    public static float getAbilityComponentHeight(Font font) {
        if (abilityHeight == -1F) {
            abilityHeight = font.lineHeight * ClientProxy.abilities.size();
        }
        return abilityHeight;
    }

    @SubscribeEvent
    public static void renderTick(ClientProgramTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            LocalPlayer player = mc.player;
            lastTicks = ticks;
            if (player != null && player.getMainHandItem().getItem() instanceof CreativeBoneItem bone) {
                if (AbilityText.linkedList.isEmpty())
                    AbilityText.init(bone);
                if (ticks < 0)
                    ticks = 0;
                if (ticks < APPEAR_DISAPPEAR_TIME)
                    ticks++;
                selected = player.getMainHandItem();
            } else {
                if (ticks > APPEAR_DISAPPEAR_TIME)
                    ticks = APPEAR_DISAPPEAR_TIME;
                ticks--;
                if (ticks < 0)
                    selected = ItemStack.EMPTY;
            }
            AbilityText.linkedList.forEach(AbilityText::tick);
        }
    }

    @SubscribeEvent
    public static void renderAbilityTooltip(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type())
            if (ticks > -1) {
                if (selected.getItem() instanceof CreativeBoneItem) {
                    float width = getAbilityComponent(mc.font);
                    float height = getAbilityComponentHeight(mc.font);
                    float posX = event.getGuiGraphics().guiWidth() - normalizePartial() * width * 1.15F;
                    float posY = 4;
                    MyGuiGraphics guiGraphics = new MyGuiGraphics(Minecraft.getInstance(), event.getGuiGraphics().pose(), event.getGuiGraphics().bufferSource());
                    PoseStack poseStack = guiGraphics.pose;
                    if (normalizePartial() > 0 && (selected != null && !selected.isEmpty())) {
                        poseStack.pushPose();
                        guiGraphics.drawManaged(() -> {
                            MyGuiGraphics.BACKGROUND_COLOR = 0;
                            Vector2i color = ColorUtils.getBorderColor();
                            MyGuiGraphics.BORDER_COLOR_TOP = color.x;
                            MyGuiGraphics.BORDER_COLOR_BOTTOM = color.y;
                            MyGuiGraphics.renderTooltipBackground(guiGraphics, posX, posY, width, height, 400);
                            MyGuiGraphics.BACKGROUND_COLOR = -267386864;
                            MyGuiGraphics.BORDER_COLOR_TOP = 1347420415;
                            MyGuiGraphics.BORDER_COLOR_BOTTOM = 1344798847;
                        });
                        poseStack.translate(0D, 0D, 400D);
                        drawAbility(posX, posY, (int) width, event.getGuiGraphics());
                        poseStack.popPose();
                    }
                }
            }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onKeyPress(InputEvent.MouseButton event) {
        if (ticks > -1 && event.getAction() == 1 && event instanceof InputEvent.MouseButton.Pre) {
            if (mc.player != null && mc.player.getMainHandItem().getItem() instanceof CreativeBoneItem && mc.screen == null) {
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    if (Screen.hasShiftDown()) {
                        AbilityText.linkedList.addFirst(AbilityText.linkedList.removeLast());
                    } else if (AbilityText.currentTrigger() != null)
                        AbilityText.currentTrigger().onLeftClick(mc.player);
                }
                if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    if (Screen.hasShiftDown()) {
                        AbilityText.linkedList.addLast(AbilityText.linkedList.removeFirst());
                    } else if (AbilityText.currentTrigger() != null)
                        AbilityText.currentTrigger().onRightClick(mc.player);

                }
            }
        }
    }

    public static void drawAbility(float posX, float posY, int widthOfTooltip, GuiGraphics guiGraphics) {
        for (int i = 0; i < AbilityText.linkedList.size(); i++) {
            AbilityText.linkedList.get(i).draw(posX, posY, widthOfTooltip, guiGraphics);
        }
    }

    public static class AbilityText {
        public static LinkedList<AbilityText> linkedList = new LinkedList<>();
        public Component component;
        public BoneTrigger trigger;
        public int index;
        public float dYOld;
        public float dY;
        public Font font = MegaFont.FantasyFont;

        public AbilityText(Component component) {

            AbilityText.linkedList.add(this);
            this.component = component;
            this.index = linkedList.indexOf(this);
        }

        public static BoneTrigger currentTrigger() {
            int i = (maxSize % 2 == 0 ? maxSize / 2 - 1 : maxSize / 2);
            if (i > linkedList.size()) i = 0;
            return linkedList.get(i).trigger;
        }

        public static void init(CreativeBoneItem bone) {
            for (Component component : ClientProxy.abilities.keySet())
                new AbilityText(component).setTrigger(ClientProxy.abilities.get(component));
        }

        public AbilityText setTrigger(BoneTrigger trigger) {
            this.trigger = trigger;
            return this;
        }

        public void tick() {
            index = linkedList.indexOf(this);
            dYOld = dY;
            dY = index * font.lineHeight;
        }

        public float getPartialDY() {
            return Mth.lerp(Minecraft.getInstance().getPartialTick(), dYOld, dY);
        }

        public void draw(float posX, float posY, float widthOfTooltip, GuiGraphics guiGraphics) {
            int i = (maxSize % 2 == 0 ? maxSize / 2 - 1 : maxSize / 2);
            boolean isSelected = index == i;
            if (isSelected) {
                font.drawInBatch8xOutline(FormattedCharSequence.forward(component.getString(), Style.EMPTY), posX + (widthOfTooltip - font.width(component)) / 2F, posY + getPartialDY(), MegaFont.getDarkColor(0x567283), 0x596460, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), 15728880);

            } else
                mc.font.drawInBatch(component, (posX + (widthOfTooltip - font.width(component)) / 2F), (posY + getPartialDY()), FastColor.ARGB32.color(255 - Math.abs((i - index)) * 30, 255, 255, 255), true, guiGraphics.pose().last().pose(), guiGraphics.bufferSource(), Font.DisplayMode.NORMAL, 0, 15728880);

            ((AccessorGuiGraphics) guiGraphics).callFlushIfUnmanaged();
        }
    }

}
