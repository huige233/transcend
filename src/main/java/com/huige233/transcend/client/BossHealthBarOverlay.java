package com.huige233.transcend.client;

import com.huige233.transcend.Transcend;
import com.huige233.transcend.entity.boss.AbstractTranscendBoss;
import com.huige233.transcend.entity.boss.TranscendenceAvatar;
import com.huige233.transcend.entity.boss.VoidWeaver;
import com.huige233.transcend.mixin.BossHealthOverlayAccessor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BossHealthBarOverlay {

    private static final ResourceLocation BAR_TEX = new ResourceLocation(Transcend.MODID, "textures/gui/boss_bar.png");
    private static final Map<UUID, Float> smoothHp = new HashMap<>();
    private static final Map<UUID, Float> delayedHp = new HashMap<>();
    private static final Map<UUID, Float> smoothShield = new HashMap<>();
    private static long tick = 0;
    private static final int SLOT_H = 42;
    private static final int VANILLA_BOSS_SLOT_H = 19;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.BOSS_EVENT_PROGRESS.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        tick++;

        List<AbstractTranscendBoss> bosses = mc.level.getEntitiesOfClass(
                AbstractTranscendBoss.class, mc.player.getBoundingBox().inflate(64), Entity::isAlive);
        if (bosses.isEmpty()) { smoothHp.clear(); delayedHp.clear(); smoothShield.clear(); return; }

        int vanillaBars = getVanillaBossBarCount(mc);
        bosses.sort(Comparator.comparing(Entity::getUUID));
        int topOffset = 6 + vanillaBars * VANILLA_BOSS_SLOT_H;
        int maxBars = Math.max(1, (mc.getWindow().getGuiScaledHeight() - topOffset) / SLOT_H);
        if (bosses.size() > maxBars) bosses = bosses.subList(0, maxBars);

        GuiGraphics gui = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int y = topOffset;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (AbstractTranscendBoss boss : bosses) { renderBar(gui, mc, boss, sw, y); y += SLOT_H; }
        RenderSystem.disableBlend();
    }

    private static int getVanillaBossBarCount(Minecraft mc) {
        BossHealthOverlay overlay = mc.gui.getBossOverlay();
        if (overlay instanceof BossHealthOverlayAccessor accessor) {
            return accessor.transcend$getEvents().size();
        }
        return 0;
    }

    private static void renderBar(GuiGraphics g, Minecraft mc, AbstractTranscendBoss boss, int sw, int y) {
        UUID id = boss.getUUID();
        int bw = 210, bh = 10;
        int bx = sw / 2 - bw / 2;

        float tgt = boss.getDisplayedBossBarProgress();
        float hp = smoothHp.getOrDefault(id, tgt);
        hp += (tgt - hp) * 0.1F;
        if (Math.abs(hp - tgt) < 0.001F) hp = tgt;
        smoothHp.put(id, hp);

        float dhp = delayedHp.getOrDefault(id, tgt);
        if (dhp > hp) { dhp += (hp - dhp) * 0.015F; if (dhp - hp < 0.002F) dhp = hp; } else dhp = hp;
        delayedHp.put(id, dhp);

        // === BOSS NAME with icon frame ===
        Component name = boss.getDisplayName();
        int nw = mc.font.width(name);
        int nameY = y;

        // Icon frame (from texture atlas 0,34 18x18)
        g.blit(BAR_TEX, bx - 22, nameY - 1, 0, 34, 18, 18, 256, 64);

        // Boss faction icon (simple colored fill inside frame)
        int iconColor = getIconColor(boss);
        g.fill(bx - 20, nameY + 1, bx - 6, nameY + 15, iconColor);

        g.drawString(mc.font, name, bx + 2, nameY + 2, getNameColor(boss), true);

        // 元素名显示（紧跟名字右边）
        int nameEndX = bx + 2 + nw + 4;
        var el = boss.getCurrentElement();
        if (el != null) {
            String elName = el.id.substring(0, 1).toUpperCase() + el.id.substring(1);
            int elColor = 0xFF000000 | ((int)(el.particleR * 255) << 16) | ((int)(el.particleG * 255) << 8) | (int)(el.particleB * 255);
            g.drawString(mc.font, "§l" + elName, nameEndX, nameY + 2, elColor, true);
        }

        int pn = boss.getCurrentPhase().ordinal() + 1;
        String ps = (boss.getPhaseTransitionTick() > 0 ? "§e" : "§c") + "Phase " + pn;
        g.drawString(mc.font, ps, bx + bw - mc.font.width(ps), nameY + 2, 0xFFFFFFFF, true);

        y = nameY + 16;

        // === BAR FRAME from texture ===
        // Left endcap
        g.blit(BAR_TEX, bx - 8, y - 3, 0, 0, 16, 16, 256, 64);
        // Right endcap
        g.blit(BAR_TEX, bx + bw - 8, y - 3, 240, 0, 16, 16, 256, 64);
        // Bar body (stretch middle)
        g.blit(BAR_TEX, bx + 8, y - 3, 16, 0, bw - 16, 16, 256, 64);
        // Top bar: shield bar if Avatar has shield, otherwise decorative line
        if (boss instanceof TranscendenceAvatar av && av.getMaxShield() > 0) {
            float ts = av.getShieldHealth() / Math.max(1, av.getMaxShield());
            float cs = smoothShield.getOrDefault(id, ts);
            cs += (ts - cs) * 0.08F;
            if (Math.abs(cs - ts) < 0.002F) cs = ts;
            smoothShield.put(id, cs);
            // Shield background
            g.fill(bx, y - 5, bx + bw, y - 3, 0xFF0A0A20);
            // Shield fill
            int shw = (int)(bw * cs);
            if (shw > 0) {
                for (int px = 0; px < shw; px++) {
                    float sw2 = Mth.sin(px * 0.22F + tick * 0.09F) * 0.12F + 0.88F;
                    g.fill(bx + px, y - 5, bx + px + 1, y - 3,
                            toArgb(0.95F, 0.12F * sw2, 0.30F * sw2, 0.85F * sw2));
                }
            }
        } else {
            g.blit(BAR_TEX, bx, y - 5, 0, 30, bw, 2, 256, 64);
        }

        // Bar inner background
        g.fill(bx + 2, y, bx + bw - 2, y + bh, 0xFF080810);

        // === HP FILL ===
        int dw = (int)((bw - 4) * dhp);
        int hw = (int)((bw - 4) * hp);
        if (dw > hw) {
            g.fillGradient(bx + 2 + hw, y, bx + 2 + dw, y + bh, 0xCC993322, 0xCC771111);
        }
        if (hw > 0) {
            for (int px = 0; px < hw; px++) {
                float pr = (float) px / (bw - 4);
                float w1 = Mth.sin(px * 0.14F - tick * 0.07F) * 0.5F + 0.5F;
                float w2 = Mth.sin(px * 0.21F - tick * 0.05F) * 0.3F + 0.7F;
                int[] c = getBarColor(boss, pr, w1, w2);
                g.fillGradient(bx + 2 + px, y, bx + 3 + px, y + bh,
                        toArgb(0.85F, c[0] * 0.55F / 255, c[1] * 0.55F / 255, c[2] * 0.55F / 255),
                        toArgb(0.95F, c[0] / 255F, c[1] / 255F, c[2] / 255F));
            }
            for (int px = 0; px < hw; px++) {
                float sp = Mth.sin(px * 0.16F - tick * 0.09F) * 0.5F + 0.5F;
                if (sp > 0.78F) {
                    int a = (int)((sp - 0.78F) / 0.22F * 70);
                    g.fill(bx + 2 + px, y, bx + 3 + px, y + 3, (a << 24) | 0xFFFFFF);
                }
            }
        }

        // === PHASE TRANSITION FLASH ===
        if (boss.getPhaseTransitionTick() > 0) {
            float fl = boss.getPhaseTransitionTick() / 60.0F;
            int fa = (int)(fl * fl * 150);
            g.fill(bx + 2, y, bx + bw - 2, y + bh, (fa << 24) | 0xFFFFFF);
        }

        // === Text ===
        int fakeHealthValue = Math.max(0, (int)(boss.getDisplayedBossBarProgress() * boss.getMaxHealth()));
        String ht = fakeHealthValue + " / " + (int) boss.getMaxHealth();
        g.drawString(mc.font, ht, bx + 4, y + 1, 0xBBFFFFFF, true);
        if (boss instanceof TranscendenceAvatar av2 && av2.getShieldHealth() > 0) {
            String st = "◆" + (int) av2.getShieldHealth();
            g.drawString(mc.font, st, bx + bw - 2 - mc.font.width(st), y + 1, 0xBB88BBFF, true);
        }
    }

    private static int getIconColor(AbstractTranscendBoss b) {
        if (b instanceof TranscendenceAvatar) {
            int[] c = hsbToRgb((tick * 0.008F) % 1F, 0.6F, 0.9F);
            return 0xDD000000 | (c[0] << 16) | (c[1] << 8) | c[2];
        }
        if (b instanceof VoidWeaver) return 0xDD5522AA;
        return 0xDDCC9933;
    }

    private static int[] getBarColor(AbstractTranscendBoss b, float p, float w1, float w2) {
        if (b instanceof TranscendenceAvatar)
            return hsbToRgb((p * 0.8F + tick * 0.003F) % 1F, 0.7F + w1 * 0.3F, 0.8F + w2 * 0.2F);
        if (b instanceof VoidWeaver)
            return new int[]{(int)(60 + w1 * 80), (int)(20 + w2 * 30), (int)(140 + w1 * 100)};
        var el = b.getCurrentElement();
        if (el != null) return new int[]{
                Mth.clamp((int)((el.particleR * 200 + w1 * 55) * w2), 0, 255),
                Mth.clamp((int)((el.particleG * 200 + w1 * 55) * w2), 0, 255),
                Mth.clamp((int)((el.particleB * 200 + w1 * 55) * w2), 0, 255)};
        return new int[]{200, 60, 60};
    }

    private static int getNameColor(AbstractTranscendBoss b) {
        if (b instanceof TranscendenceAvatar) {
            int[] c = hsbToRgb((tick * 0.005F) % 1F, 0.5F, 1F);
            return 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
        }
        return b instanceof VoidWeaver ? 0xFFCC77FF : 0xFFFFDD66;
    }

    private static int[] hsbToRgb(float h, float s, float b) {
        int c = java.awt.Color.HSBtoRGB(h, s, b);
        return new int[]{(c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF};
    }

    private static int toArgb(float a, float r, float g, float b) {
        return (cl(a) << 24) | (cl(r) << 16) | (cl(g) << 8) | cl(b);
    }

    private static int cl(float v) { return Math.max(0, Math.min(255, (int)(v * 255))); }
}
