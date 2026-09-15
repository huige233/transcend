package com.mega.uom.event.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class FeRenderTooltipEvent extends Event {
    @NotNull
    protected final ItemStack itemStack;
    protected final GuiGraphics graphics;
    protected final List<ClientTooltipComponent> components;
    protected int x;
    protected int y;
    protected Font font;

    @ApiStatus.Internal
    protected FeRenderTooltipEvent(@NotNull ItemStack itemStack, GuiGraphics graphics, int x, int y, @NotNull Font font, @NotNull List<ClientTooltipComponent> components) {
        this.itemStack = itemStack;
        this.graphics = graphics;
        this.components = Collections.unmodifiableList(components);
        this.x = x;
        this.y = y;
        this.font = font;
    }

    /**
     * {@return the item stack which the tooltip is being rendered for, or an {@linkplain ItemStack#isEmpty() empty
     * item stack} if there is no associated item stack}
     */
    @NotNull
    public ItemStack getItemStack() {
        return itemStack;
    }

    /**
     * {@return the graphics helper for the gui}
     */
    public GuiGraphics getGraphics() {
        return this.graphics;
    }

    /**
     * {@return the unmodifiable list of tooltip components}
     *
     * <p>Use {@link ItemTooltipEvent} or {@link net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents} to modify tooltip contents or components.</p>
     */
    @NotNull
    public List<ClientTooltipComponent> getComponents() {
        return components;
    }

    /**
     * {@return the X position of the tooltip box} By default, this is the mouse X position.
     */
    public int getX() {
        return x;
    }

    /**
     * {@return the Y position of the tooltip box} By default, this is the mouse Y position.
     */
    public int getY() {
        return y;
    }

    /**
     * {@return The font used to render the text}
     */
    @NotNull
    public Font getFont() {
        return font;
    }

    @Cancelable
    public static class PrePre extends FeRenderTooltipEvent {
        private final int screenWidth;
        private final int screenHeight;
        private final ClientTooltipPositioner positioner;
        private final PoseStack poseStack;

        public PrePre(@NotNull ItemStack stack, GuiGraphics graphics, int x, int y, int screenWidth, int screenHeight, @NotNull Font font, @NotNull List<ClientTooltipComponent> components, @NotNull ClientTooltipPositioner positioner, PoseStack poseStack) {
            super(stack, graphics, x, y, font, components);
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.positioner = positioner;
            this.poseStack = poseStack;
        }

        /**
         * {@return the width of the screen}.
         * The lines of text within the tooltip are wrapped to be within the screen width, and the tooltip box itself
         * is moved to be within the screen width.
         */
        public int getScreenWidth() {
            return screenWidth;
        }

        /**
         * {@return the height of the screen}
         * The tooltip box is moved to be within the screen height.
         */
        public int getScreenHeight() {
            return screenHeight;
        }

        public ClientTooltipPositioner getTooltipPositioner() {
            return positioner;
        }

        public PoseStack getPoseStack() {
            return poseStack;
        }

        /**
         * Sets the font to be used to render text.
         *
         * @param fr the new font
         */
        public void setFont(@NotNull Font fr) {
            this.font = fr;
        }

        /**
         * Sets the X origin of the tooltip.
         *
         * @param x the new X origin
         */
        public void setX(int x) {
            this.x = x;
        }

        /**
         * Sets the Y origin of the tooltip.
         *
         * @param y the new Y origin
         */
        public void setY(int y) {
            this.y = y;
        }
    }

    public static class Post extends FeRenderTooltipEvent {
        public final ClientTooltipPositioner positioner;

        public Post(@NotNull ItemStack itemStack, GuiGraphics graphics, int x, int y, @NotNull Font font, @NotNull List<ClientTooltipComponent> components, ClientTooltipPositioner positioner) {
            super(itemStack, graphics, x, y, font, components);
            this.positioner = positioner;
        }
    }
}
