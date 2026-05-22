package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Transcend.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class HeartRenderHandler {

    private static final ResourceLocation HEARTS = new ResourceLocation(Transcend.MODID, "textures/gui/hearts.png");
    private static final ResourceLocation VANILLA = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final int HEART_SIZE = 9;
    private static final int COLORS = 11;

    private static int lastHealth = 0;
    private static int displayHealth = 0;
    private static long healthBlinkTime = 0;
    private static long lastHealthTime = 0;
    private static final Random rand = new Random();

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void renderHealthbar(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.isCanceled() || event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) return;
        if (mc.player == null || mc.options.hideGui) return;
        if (!(mc.gui instanceof ForgeGui gui) || !gui.shouldDrawSurvivalElements()) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;
        Player player = mc.player;

        int health = Mth.ceil(player.getHealth());
        int maxHealth = Math.max(Mth.ceil(player.getAttributeValue(Attributes.MAX_HEALTH)), health);
        int absorb = Mth.ceil(player.getAbsorptionAmount());

        if (maxHealth <= 20 && absorb <= 20) return;

        gui.setupOverlayRenderState(true, false);
        mc.getProfiler().push("transcend_health");

        int tickCount = mc.gui.getGuiTicks();

        boolean highlight = healthBlinkTime > tickCount && (healthBlinkTime - tickCount) / 3L % 2L == 1L;
        long systemTime = Util.getMillis();
        if (player.invulnerableTime > 0) {
            if (health < lastHealth) {
                lastHealthTime = systemTime;
                healthBlinkTime = tickCount + 20;
            } else if (health > lastHealth) {
                lastHealthTime = systemTime;
                healthBlinkTime = tickCount + 10;
            }
        }
        if (systemTime - lastHealthTime > 1000L) {
            displayHealth = health;
            lastHealthTime = systemTime;
        }
        lastHealth = health;

        rand.setSeed(tickCount * 312871L);

        int left = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = mc.getWindow().getGuiScaledHeight() - gui.leftHeight;

        int regen = player.hasEffect(MobEffects.REGENERATION) ? tickCount % 25 : -1;
        boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
        int potionOffset = getPotionOffset(player);

        boolean wiggle = health + absorb <= 4;

        int showHealth = Math.min(maxHealth, 20);
        int showHearts = (showHealth + 1) / 2;

        GuiGraphics graphics = event.getGuiGraphics();

        // === backgrounds ===
        int bgU = highlight ? 25 : 16;
        int bgV = hardcore ? 45 : 0;
        for (int i = 0; i < showHearts; i++) {
            int x = left + i * 8;
            int y = top + getOffset(i, wiggle, regen);
            graphics.blit(VANILLA, x, y, bgU, bgV, HEART_SIZE, HEART_SIZE);
        }

        // === extra health layers (colored hearts from texture) ===
        int healthLayer = health / 20;
        int healthRemainder = health % 20;
        if (healthRemainder == 0 && health > 0) {
            healthLayer--;
            healthRemainder = 20;
        }

        if (healthLayer > 0) {
            int prevColor = (healthLayer - 1) % COLORS;
            for (int i = 0; i < 10; i++) {
                int x = left + i * 8;
                int y = top + getOffset(i, wiggle, regen);
                graphics.blit(HEARTS, x, y, 18 * prevColor, potionOffset, HEART_SIZE, HEART_SIZE);
            }
        }

        // === current layer hearts ===
        int topHearts = healthRemainder / 2;
        boolean topHalf = healthRemainder % 2 == 1;
        int MARGIN = 16 + potionOffset;

        if (healthLayer == 0) {
            for (int i = 0; i < topHearts; i++) {
                int x = left + i * 8;
                int y = top + getOffset(i, wiggle, regen);
                graphics.blit(VANILLA, x, y, MARGIN + 36, bgV, HEART_SIZE, HEART_SIZE);
            }
            if (topHalf) {
                int x = left + topHearts * 8;
                int y = top + getOffset(topHearts, wiggle, regen);
                graphics.blit(VANILLA, x, y, MARGIN + 45, bgV, HEART_SIZE, HEART_SIZE);
            }
        } else {
            int topColor = healthLayer % COLORS;
            for (int i = 0; i < topHearts; i++) {
                int x = left + i * 8;
                int y = top + getOffset(i, wiggle, regen);
                graphics.blit(HEARTS, x, y, 18 * topColor, potionOffset, HEART_SIZE, HEART_SIZE);
            }
            if (topHalf) {
                int x = left + topHearts * 8;
                int y = top + getOffset(topHearts, wiggle, regen);
                graphics.blit(HEARTS, x, y, 9 + 18 * topColor, potionOffset, HEART_SIZE, HEART_SIZE);
            }
        }

        // === health layer text ===
        if (healthLayer > 0) {
            drawSmallText(graphics, mc.font, "x" + (healthLayer + 1), left, top, getLayerColor(healthLayer));
        }

        gui.leftHeight += 10;

        // === absorption ===
        if (absorb > 0) {
            int absorbTop = mc.getWindow().getGuiScaledHeight() - gui.leftHeight;

            int absorbLayer = absorb / 20;
            int absorbRemainder = absorb % 20;
            if (absorbRemainder == 0) {
                absorbLayer--;
                absorbRemainder = 20;
            }
            int absorbHearts = absorbRemainder / 2;
            boolean absorbHalf = absorbRemainder % 2 == 1;

            int absorbMargin = 16;

            // backgrounds (no wiggle, no regen bounce)
            for (int i = 0; i < 10; i++) {
                int x = left + i * 8;
                graphics.blit(VANILLA, x, absorbTop, 16, bgV, HEART_SIZE, HEART_SIZE);
            }

            // bottom layer tint
            if (absorbLayer > 0) {
                float[] tint = getAbsorbColor((absorbLayer - 1) % 7);
                RenderSystem.setShaderColor(tint[0], tint[1], tint[2], 1.0F);
                for (int i = 0; i < 10; i++) {
                    int x = left + i * 8;
                    graphics.blit(VANILLA, x, absorbTop, absorbMargin + 144, bgV, HEART_SIZE, HEART_SIZE);
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            // current layer
            if (absorbLayer > 0) {
                float[] tint = getAbsorbColor(absorbLayer % 7);
                RenderSystem.setShaderColor(tint[0], tint[1], tint[2], 1.0F);
            }
            for (int i = 0; i < absorbHearts; i++) {
                int x = left + i * 8;
                graphics.blit(VANILLA, x, absorbTop, absorbMargin + 144, bgV, HEART_SIZE, HEART_SIZE);
            }
            if (absorbHalf) {
                int x = left + absorbHearts * 8;
                graphics.blit(VANILLA, x, absorbTop, absorbMargin + 153, bgV, HEART_SIZE, HEART_SIZE);
            }
            if (absorbLayer > 0) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            if (absorbLayer > 0) {
                drawSmallText(graphics, mc.font, "x" + (absorbLayer + 1), left, absorbTop, 0xFFFF00);
            }

            gui.leftHeight += 10;
        }

        RenderSystem.setShaderTexture(0, VANILLA);
        event.setCanceled(true);
        RenderSystem.disableBlend();
        mc.getProfiler().pop();

        MinecraftForge.EVENT_BUS.post(new RenderGuiOverlayEvent.Post(event.getWindow(), graphics, event.getPartialTick(), VanillaGuiOverlay.PLAYER_HEALTH.type()));
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void renderArmorbar(RenderGuiOverlayEvent.Pre event) {
        if (event.isCanceled() || event.getOverlay() != VanillaGuiOverlay.ARMOR_LEVEL.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (!(mc.gui instanceof ForgeGui gui) || !gui.shouldDrawSurvivalElements()) return;
        if (mc.player.isCreative() || mc.player.isSpectator()) return;

        Player player = mc.player;
        int armor = player.getArmorValue();
        if (armor <= 0) return;

        gui.setupOverlayRenderState(true, false);

        GuiGraphics graphics = event.getGuiGraphics();
        int left = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = mc.getWindow().getGuiScaledHeight() - gui.leftHeight;

        int displayArmor = armor % 20;
        if (displayArmor == 0) displayArmor = 20;
        int armorLayer = (armor - 1) / 20;

        float[] tint = getArmorColor(armor);
        RenderSystem.setShaderColor(tint[0], tint[1], tint[2], 1.0F);

        for (int i = 0; i < 10; i++) {
            int x = left + i * 8;
            if (i * 2 + 1 < displayArmor)
                graphics.blit(VANILLA, x, top, 34, 9, 9, 9);
            else if (i * 2 + 1 == displayArmor)
                graphics.blit(VANILLA, x, top, 25, 9, 9, 9);
            else
                graphics.blit(VANILLA, x, top, 16, 9, 9, 9);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (armorLayer > 0) {
            drawSmallText(graphics, mc.font, "x" + (armorLayer + 1), left, top, getArmorTextColor(armor));
        }

        gui.leftHeight += 10;

        RenderSystem.setShaderTexture(0, VANILLA);
        event.setCanceled(true);
        RenderSystem.disableBlend();
    }

    private static int getOffset(int index, boolean wiggle, int regen) {
        int off = 0;
        if (wiggle) off = rand.nextInt(2);
        if (index == regen) off -= 2;
        return off;
    }

    private static void drawSmallText(GuiGraphics gui, Font font, String text, int left, int top, int color) {
        gui.pose().pushPose();
        gui.pose().scale(0.75F, 0.75F, 1.0F);
        float inv = 1.0F / 0.75F;
        int tx = (int) ((left - font.width(text) * 0.75F - 1) * inv);
        int ty = (int) ((top + 2) * inv);
        gui.drawString(font, text, tx, ty, color, true);
        gui.pose().popPose();
    }

    private static int getPotionOffset(Player player) {
        if (player.hasEffect(MobEffects.WITHER)) return 18;
        if (player.hasEffect(MobEffects.POISON)) return 9;
        return 0;
    }

    private static int getLayerColor(int layer) {
        return switch (layer % 7) {
            case 0 -> 0xFF5555;
            case 1 -> 0xFFAA00;
            case 2 -> 0xFFFF55;
            case 3 -> 0x55FF55;
            case 4 -> 0x55FFFF;
            case 5 -> 0x5555FF;
            default -> 0xFF55FF;
        };
    }

    private static float[] getAbsorbColor(int index) {
        return switch (index % 7) {
            case 0 -> new float[]{1.0F, 0.9F, 0.3F};
            case 1 -> new float[]{1.0F, 0.7F, 0.2F};
            case 2 -> new float[]{1.0F, 0.5F, 0.1F};
            case 3 -> new float[]{0.6F, 1.0F, 0.4F};
            case 4 -> new float[]{0.4F, 1.0F, 1.0F};
            case 5 -> new float[]{0.8F, 0.5F, 1.0F};
            default -> new float[]{1.0F, 0.4F, 0.6F};
        };
    }

    private static float[] getArmorColor(int armor) {
        float t = Math.min(armor / 40.0F, 1.0F);
        return new float[]{
                0.85F * (1 - t) + 0.36F * t,
                0.85F * (1 - t) + 0.93F * t,
                0.85F * (1 - t) + 0.96F * t
        };
    }

    private static int getArmorTextColor(int armor) {
        float t = Math.min(armor / 40.0F, 1.0F);
        int r = (int) (0xD8 * (1 - t) + 0x5D * t);
        int g = (int) (0xD8 * (1 - t) + 0xEC * t);
        int b = (int) (0xD8 * (1 - t) + 0xF5 * t);
        return (r << 16) | (g << 8) | b;
    }
}
